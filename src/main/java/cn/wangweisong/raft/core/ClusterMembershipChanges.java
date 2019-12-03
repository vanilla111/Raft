package cn.wangweisong.raft.core;

import cn.wangweisong.raft.common.Peer;
import cn.wangweisong.raft.core.membership.Result;

/**
 * @author wang
 * @date 2019/11/16 周六 上午12:18
 */
public interface ClusterMembershipChanges {

    /**
     * 新加入一个节点
     * @param newPeer
     * @return
     */
    Result addPeer(Peer newPeer);

    /**
     * 由于某种原因，需要删除一个节点
     * @param oldPeer
     * @return
     */
    Result removePeer(Peer oldPeer);
}
