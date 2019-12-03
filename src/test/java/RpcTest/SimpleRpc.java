package RpcTest;

import cn.wangweisong.raft.rpc.Request;
import cn.wangweisong.raft.rpc.Response;
import cn.wangweisong.raft.rpc.RpcClient;
import cn.wangweisong.raft.rpc.impl.TestRpcClient;
import cn.wangweisong.raft.rpc.impl.TestRpcServer;
import org.junit.After;
import org.junit.Test;

/**
 * @author wang
 * @date 2019/11/17 周日 下午4:57
 */
public class SimpleRpc {

    private static final TestRpcServer rpcServer;

    static {
        rpcServer = new TestRpcServer(8888);
        rpcServer.start();
    }

    @Test
    public void send() {
        RpcClient rpcClient = new TestRpcClient();
        Request request = Request.newBuilder()
                .url("localhost:8888")
                .obj("hello")
                .build();
        Response response = rpcClient.send(request);
        if (response != null ) System.out.println(response);
    }

    @After
    public void stopServer() {
        rpcServer.stop();
    }

}
