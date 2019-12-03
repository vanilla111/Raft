package cn.wangweisong.raft.core.impl;

import cn.wangweisong.raft.common.NodeStatus;
import cn.wangweisong.raft.common.Peer;
import cn.wangweisong.raft.core.ClusterMembershipChanges;
import cn.wangweisong.raft.core.membership.Result;
import cn.wangweisong.raft.entity.LogEntry;
import cn.wangweisong.raft.rpc.Request;
import cn.wangweisong.raft.rpc.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wang
 * @date 2019/11/16 周六 下午1:28
 */
public class ClusterMembershipChangesImpl implements ClusterMembershipChanges {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterMembershipChangesImpl.class);

    private final DefaultNode node;

    public ClusterMembershipChangesImpl(DefaultNode node) {
        this.node = node;
    }

    /**
     * 必须互斥，一次增加一个节点
     * @param newPeer
     * @return
     */
    @Override
    public synchronized Result addPeer(Peer newPeer) {
        if (node.getPeerSetManager().getPeerSetWithOutSelf().contains(newPeer)) {
            // 已经存在
            return new Result();
        }
        node.getPeerSetManager().getPeerSetWithOutSelf().add(newPeer);
        if (node.getStatus().getCode() == NodeStatus.LEADER.getCode()) {
            node.getNextIndices().put(newPeer, 0L);
            node.getMatchIndices().put(newPeer, 0L);
            for (long i = 0; i < node.getLogModule().getLastIndex(); i++) {
                LogEntry entry = node.getLogModule().read(i);
                if (entry != null) {
                    node.replication(newPeer, entry);
                }
            }

            for (Peer peer : node.getPeerSetManager().getPeerSetWithOutSelf()) {
                Request request = Request.newBuilder()
                        .cmd(Request.CHANGE_CONFIG_ADD)
                        .url(newPeer.getAddress())
                        .obj(newPeer)
                        .build();
                Response response = node.getRpcClient().send(request);
                Result result = (Result) response.getResult();
                if (request != null && result.getStatus() == Result.Status.SUCCESS.getCode()) {
                    LOGGER.info("The leader finds that a new node {} has joined and informs the follower that {} has succeeded!", newPeer, peer);
                } else {
                    LOGGER.warn("The leader notices that a new node {} has joined and informs the follower that {} has failed!", newPeer, peer);
                }
            }
        }
        return new Result();
    }

    /**
     * 必须互斥，一次删除一个节点
     * @param oldPeer
     * @return
     */
    @Override
    public synchronized Result removePeer(Peer oldPeer) {
        node.getPeerSetManager().getPeerSetWithOutSelf().remove(oldPeer);
        node.getNextIndices().remove(oldPeer);
        node.getMatchIndices().remove(oldPeer);
        return new Result();
    }
}
