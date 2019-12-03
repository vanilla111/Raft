package cn.wangweisong.raft.rpc.impl;

import cn.wangweisong.raft.exception.RaftRemotingException;
import cn.wangweisong.raft.rpc.Request;
import cn.wangweisong.raft.rpc.Response;
import cn.wangweisong.raft.rpc.RpcClient;
import com.alipay.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wang
 * @date 2019/11/12 周二 下午11:53
 */
public class DefaultRpcClient implements RpcClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(DefaultRpcClient.class);

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
            response = (Response) CLIENT.invokeSync(request.getUrl(), request, timeout);
        } catch (RemotingException e) {
            LOGGER.info("[RemotingException] DefaultRpcClient::send, remoting invoke failed");
            throw new RaftRemotingException();
        } catch (InterruptedException e) {
            LOGGER.warn("[InterruptedException] DefaultRpcClient::send, remoting invoke had been interrupted");
        }
        return (response);
    }
}
