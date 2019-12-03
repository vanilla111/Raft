package cn.wangweisong.raft.core;

import cn.wangweisong.raft.entity.EntryParam;
import cn.wangweisong.raft.entity.EntryResult;
import cn.wangweisong.raft.entity.VoteParam;
import cn.wangweisong.raft.entity.VoteResult;

/**
 * @author wang
 * @date 2019/11/15 周五 下午9:58
 */
public interface Consensus {

    /**
     * 请求投票RPC
     * 接受者逻辑：
     *  如果 term < currentTerm，返回false （5.2节）
     *  否则，如果 (votedFor为空 or 为candidateId) and 候选人的日志至少和自己一样新，那么投票给他（5.2 5.4节）
     * @param param
     * @return
     */
    VoteResult requestVote(VoteParam param);

    /**
     * 追加日志RPC
     * 接受者逻辑：
     *  如果 term < currentTerm，返回false（5.1节）
     *  如果日志在prevLogIndex位置处的日志条目任期编号和pervLogTerm 不匹配，则返回false（5.3节）
     *  如果已经存在的日志条目和新的产生冲突（索引值相同但是任期编号不同），删除这一条和之后所有的日志条目（5.3节）
     *  附加日志中尚未存在的所有新条目
     *  如果 leaderCommit > commitIndex，令commitIndex等于leaderCommit 和 新日志条目中索引值较小的一个
     * @param param
     * @return
     */
    EntryResult appendEntries(EntryParam param);
}
