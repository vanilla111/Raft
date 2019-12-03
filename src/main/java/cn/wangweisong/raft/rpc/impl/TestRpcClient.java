package cn.wangweisong.raft.rpc.impl;

import cn.wangweisong.raft.rpc.Request;
import cn.wangweisong.raft.rpc.Response;
import cn.wangweisong.raft.rpc.RpcClient;
import com.alipay.remoting.exception.RemotingException;

/**
 * @author wang
 * @date 2019/11/17 周日 下午4:53
 */
public class TestRpcClient implements RpcClient {

    private final static com.alipay.remoting.rpc.RpcClient CLIENT = new com.alipay.remoting.rpc.RpcClient();

    static {
        CLIENT.init();
    }

    @Override
    public Response send(Request request) {
        return send(request, 200000);
    }

    @Override
    public Response send(Request request, int timeout) {
        Response response = null;
        try {
            response = (Response)CLIENT.invokeSync(request.getUrl(), request, timeout);
        } catch (RemotingException | InterruptedException e) {
            e.printStackTrace();
        }
        return (response);
    }
}
