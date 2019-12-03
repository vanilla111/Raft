package cn.wangweisong.raft.rpc;

import cn.wangweisong.raft.exception.RaftNotSupportException;
import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;

/**
 * @author wang
 * @date 2019/11/13 周三 上午12:14
 */
public abstract class RaftUserProcessor<T> extends AbstractUserProcessor<T> {

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
