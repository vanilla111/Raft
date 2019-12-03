package cn.wangweisong.raft.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wang
 * @date 2019/11/16 周六 上午10:55
 */
public class RaftThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftThread.class);

    private static final UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER
            = (t, e) -> LOGGER.warn("Exception occurred from thread {}.", t.getName(), e);

    public RaftThread(String threadName, Runnable r) {
        super(r, threadName);
        // 更改线程异常处理
        setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
    }
}
