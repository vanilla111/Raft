package cn.wangweisong.raft.rpc;

/**
 * @author wang
 * @date 2019/11/12 周二 下午11:29
 */
public interface RpcClient {
    /**
     * 发送请求
     * @param request
     * @return
     */
    Response send(Request request);

    /**
     * 发送请求并设置超时时间限制
     * @param request
     * @param timeout
     * @return
     */
    Response send(Request request, int timeout);
}
