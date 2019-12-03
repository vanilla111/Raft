package cn.wangweisong.raft.common;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author wang
 * @date 2019/11/13 周三 上午11:53
 */
@Setter
@Getter
@ToString
public class NodeConfig {

    public int selfPort;

    public List<String> peerAddress;
}
