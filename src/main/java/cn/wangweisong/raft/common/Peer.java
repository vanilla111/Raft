package cn.wangweisong.raft.common;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * @author wang
 * @date 2019/11/13 周三 上午11:58
 */
@Getter
@Setter
public class Peer {

    /** 对等节点的IP地址 */
    private final String address;

    public Peer(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Peer peer = (Peer) o;
        return Objects.equals(address, peer.address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(address);
    }

    @Override
    public String toString() {
        return "Peer{ address = '" + address + "' }";
    }
}
