package cn.wangweisong.raft.common;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author wang
 * @date 2019/11/13 周三 下午12:03
 */
public class PeerSetManager implements Serializable {

    private Set<Peer> peerSet = new HashSet<>();

    private volatile Peer leader;

    private volatile Peer self;

    private PeerSetManager() {}

    /**
     * 单例模式，获取节点信息的管理者
     * @return
     */
    public static PeerSetManager getInstance() {
        return PeerSetManagerLazyHolder.INSTANCE;
    }

    private static class PeerSetManagerLazyHolder {
        private static final PeerSetManager INSTANCE = new PeerSetManager();
    }

    public void setSelf(Peer peer) {
        self = peer;
    }

    public Peer getSelf() {
        return self;
    }

    public void addPeer(Peer peer) {
        peerSet.add(peer);
    }

    public void removePeer(Peer peer) {
        peerSet.remove(peer);
    }

    public Set<Peer> getPeerSet() {
        return peerSet;
    }

    public Set<Peer> getPeerSetWithOutSelf() {
        Set<Peer> set2 = new HashSet<>(peerSet);
        set2.remove(self);
        return set2;
    }

    public Peer getLeader() {
        return leader;
    }

    public void setLeader(Peer leader) {
        this.leader = leader;
    }

    @Override
    public String toString() {
        return "PeerSet{ set=" + peerSet + ", leader=" + leader + ", self=" + self + '}';
    }
}
