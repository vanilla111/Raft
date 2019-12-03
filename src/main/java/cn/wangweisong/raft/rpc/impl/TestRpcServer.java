package cn.wangweisong.raft.rpc.impl;

import cn.wangweisong.raft.exception.RaftNotSupportException;
import cn.wangweisong.raft.rpc.Request;
import cn.wangweisong.raft.rpc.Response;
import cn.wangweisong.raft.rpc.RpcServer;
import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;

/**
 * @author wang
 * @date 2019/11/17 周日 下午4:47
 */
public class TestRpcServer implements RpcServer {

    private volatile boolean flag;

    private com.alipay.remoting.rpc.RpcServer rpcServer;

    public TestRpcServer(int port) {
        if (flag) {
            return;
        }
        synchronized (this) {
            if (flag) {
                return;
            }

            rpcServer = new com.alipay.remoting.rpc.RpcServer(port, false, false);
            rpcServer.registerUserProcessor(new AbstractProcessor<Request>() {
                @Override
                public Object handleRequest(BizContext bizContext, Request request) throws Exception {
                    return handlerRequest(request);
                }
            });
        }
        flag = true;
    }

    abstract static class AbstractProcessor<T> extends AbstractUserProcessor<T> {
        @Override
        public void handleRequest(BizContext bizContext, AsyncContext asyncContext, T request) {
            throw new RaftNotSupportException(
                    "not implement AbstractUserProcessor.handlerRequest(" +
                            "BizContext bizContext, AsyncContext asyncContext, T request)");
        }

        @Override
        public String interest() {
            return Request.class.getName();
        }
    }

    @Override
    public void start() {
        rpcServer.start();
    }

    @Override
    public void stop() {
        rpcServer.stop();
    }

    @Override
    public Response handlerRequest(Request request) {
        System.out.println(request.getObj());
        return Response.ok();
    }
}
