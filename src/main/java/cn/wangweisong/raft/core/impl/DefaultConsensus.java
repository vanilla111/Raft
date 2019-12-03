package cn.wangweisong.raft.core.impl;

import cn.wangweisong.raft.common.NodeStatus;
import cn.wangweisong.raft.common.Peer;
import cn.wangweisong.raft.core.Consensus;
import cn.wangweisong.raft.entity.*;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wang
 * @date 2019/11/16 周六 上午12:05
 */
public class DefaultConsensus implements Consensus {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConsensus.class);

    private final DefaultNode node;
    private final ReentrantLock voteLock = new ReentrantLock();
    private final ReentrantLock appendLock = new ReentrantLock();

    public DefaultConsensus(DefaultNode node) {
        this.node = node;
    }

    /**
     * 处理请求投票RPC
     * 接收者逻辑：
     *      如果term < currentTerm返回 false （5.2 节）
     *      如果 votedFor 为空或者就是 candidateId，并且候选人的日志至少和自己一样新，那么就投票给他（5.2 节，5.4 节）
     * @param param
     * @return
     */
    @Override
    public VoteResult requestVote(VoteParam param) {
        try {
            VoteResult.Builder builder = VoteResult.newBuilder();
            if (!voteLock.tryLock()) {
                return builder.term(node.getCurrentTerm()).voteGranted(false).build();
            }
            if (param.getTerm() < node.getCurrentTerm()) {
                return builder.term(node.getCurrentTerm()).voteGranted(false).build();
            }
            LOGGER.info("The current node {} has voted for {}, and the candidate address it receives is {}.",
                    node.getPeerSetManager().getSelf(), node.getVotedFor(), param.getCandidateId());
            LOGGER.info("The current node {} has a term number of {}, and the received candidate term number is {}.",
                    node.getPeerSetManager().getSelf(), node.getCurrentTerm(), param.getTerm());
            // 如果该节点还没投票，或者已经投给了该请求来源的节点
            if ((StringUtil.isNullOrEmpty(node.getVotedFor())) || node.getVotedFor().equals(param.getCandidateId())) {
                LogEntry logEntry = node.getLogModule().getLast();
                if ( logEntry != null) {
                    // 对方没有自己日志新
                    if (logEntry.getTerm() > param.getLastLogTerm() || logEntry.getIndex() > param.getLastLogIndex()) {
                        return VoteResult.fail();
                    }
                }
                // 对方更新，切换自己的状态，并更新信息
                node.setStatus(NodeStatus.FOLLOWER);
                node.getPeerSetManager().setLeader(new Peer(param.getCandidateId()));
                node.setCurrentTerm(param.getTerm());
                node.setVotedFor(param.getServerId());
                return builder.term(node.getCurrentTerm()).voteGranted(true).build();
            }
            return builder.term(node.getCurrentTerm()).voteGranted(false).build();
        } finally {
            voteLock.unlock();
        }
    }

    /**
     * 附加日志(多个日志,为了提高效率) RPC
     *
     * 接收者实现：
     *    如果 term < currentTerm 就返回 false （5.1 节）
     *    如果日志在 prevLogIndex 位置处的日志条目的任期号和 prevLogTerm 不匹配，则返回 false （5.3 节）
     *    如果已经存在的日志条目和新的产生冲突（索引值相同但是任期号不同），删除这一条和之后所有的 （5.3 节）
     *    附加任何在已有的日志中不存在的条目
     *    如果 leaderCommit > commitIndex，令 commitIndex 等于 leaderCommit 和 新日志条目索引值中较小的一个
     */
    @Override
    public EntryResult appendEntries(EntryParam param) {
        EntryResult result = EntryResult.fail();
        try {
            if (!appendLock.tryLock()) {
                return result;
            }
            result.setTerm(node.getCurrentTerm());
            // 对方任期编号比自己小
            if (param.getTerm() < node.getCurrentTerm()) {
                return result;
            }

            node.setPreHeartBeatTime(System.currentTimeMillis());
            node.setPreElectionTime(System.currentTimeMillis());
            node.getPeerSetManager().setLeader(new Peer(param.getLeaderId()));

            if (param.getTerm() >= node.getCurrentTerm()) {
                node.setStatus(NodeStatus.FOLLOWER);
            }
            node.setCurrentTerm(param.getTerm());
            // 心跳
            if (param.getEntries() == null || param.getEntries().length == 0) {
                LOGGER.info("The heartbeat message was received successfully from node {}. At this time, the opposite term is [{}] and my term is [{}].",
                        param.getLeaderId(), param.getTerm(), node.getCurrentTerm());
                return EntryResult.newBuilder().term(node.getCurrentTerm()).success(true).build();
            }

            // 附加日志
            if (node.getLogModule().getLastIndex() != 0 && param.getPrevLogIndex() != 0) {
                LogEntry logEntry;
                if ((logEntry = node.getLogModule().read(param.getPrevLogIndex())) != null) {
                    // 如果日志在 prevLogIndex 位置处的日志条目的任期编号和 prevLogTerm 不匹配，返回false
                    // 需要减少 nextIndex 重试
                    if (logEntry.getTerm() != param.getPrevLogTerm()) {
                        return result;
                    }
                } else {
                    // index 不匹配
                    return result;
                }
            }

            // param 的 prevLogIndex prevLogTerm 已经和该节点相匹配
            // 但该节点可能留有上一个领导者发送的未提交的日志
            // 如果已经存在的日志和新的产生冲突，即索引值相同但是任期编号不同，删除这一条之后所有的
            LogEntry existLog = node.getLogModule().read(param.getPrevLogIndex() + 1);
            if (existLog != null && existLog.getTerm().equals(param.getEntries()[0].getTerm())) {
                // 删除这一条和之后所有的
                node.getLogModule().removeOnStartIndex(param.getPrevLogIndex() + 1);
            } else if (existLog != null) {
                // 已经有该日志了，不重复写入
                result.setSuccess(true);
                return result;
            }

            // 将日志应用到状态机
            for (LogEntry entry : param.getEntries()) {
                node.getLogModule().write(entry);
                node.getStateMachine().apply(entry);
            }

            // 如果 leaderCommit > commitIndex, 令 commitIndex = Min(leaderCommit, 最大的新日志索引值)
            if (param.getLeaderCommit() > node.getCommitIndex()) {
                int commitIndex = (int) Math.min(param.getLeaderCommit(), node.getLogModule().getLastIndex());
                node.setCommitIndex(commitIndex);
                node.setLastApplied(commitIndex);
            }
            result.setSuccess(true);
            result.setTerm(node.getCurrentTerm());
            node.setStatus(NodeStatus.FOLLOWER);

            return result;
        } finally {
            appendLock.unlock();
        }
    }
}
