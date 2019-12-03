package cn.wangweisong.raft;

import cn.wangweisong.raft.common.NodeConfig;
import cn.wangweisong.raft.core.Node;
import cn.wangweisong.raft.core.impl.DefaultNode;

import java.util.Arrays;

/**
 * @author wang
 * @date 2019/11/17 周日 下午2:49
 */
public class RaftNodeBootstrap {

    public static void main(String[] args) throws Throwable {
        start();
    }

    public static void start() throws Throwable {
        String[] peerAddr = {"localhost:8775","localhost:8776","localhost:8777","localhost:8778","localhost:8779"};

        NodeConfig config = new NodeConfig();

        // 自身节点
        config.setSelfPort(Integer.parseInt(System.getProperty("serverPort")));

        // 其他节点地址
        config.setPeerAddress(Arrays.asList(peerAddr));
        Node node = DefaultNode.getInstance();
        node.setConfig(config);
        node.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                node.destroy();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }));
    }
}
