package cn.wangweisong.raft.core;

/**
 * @author wang
 * @date 2019/11/13 周三 下午1:53
 */
public interface LifeCycle {
    /**
     * 初始化
     * @throws Throwable
     */
    void init() throws Throwable;

    /**
     * 销毁
     * @throws Throwable
     */
    void destroy() throws Throwable;
}
