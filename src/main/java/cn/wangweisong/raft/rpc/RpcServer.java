package cn.wangweisong.raft.rpc;

/**
 * @author wang
 * @date 2019/11/12 周二 下午11:29
 */
public interface RpcServer {
    /**
     * 启动RPC服务
     */
    void start();

    /**
     * 停止PRC服务
     */
    void stop();

    /**
     * 处理请求
     * @param request
     * @return
     */
    Response handlerRequest(Request request);
}
