package cn.wangweisong.raft.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wang
 * @date 2019/11/16 周六 上午11:13
 */
public class RaftThreadPoolExecutor extends ThreadPoolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftThreadPoolExecutor.class);

    private static final ThreadLocal<Long> COST_TIME_WATCH = ThreadLocal.withInitial(System::currentTimeMillis);

    public RaftThreadPoolExecutor(int cpu, int maxPoolSize, long keepTime,
                                  TimeUnit keepTimeUnit, BlockingQueue<Runnable> workQueue,
                                  RaftThreadPool.NameThreadFactory nameThreadFactory) {
        super(cpu, maxPoolSize, keepTime, keepTimeUnit, workQueue, nameThreadFactory);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        COST_TIME_WATCH.get();
        // LOGGER.info("Raft thread pool before execute");
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        // LOGGER.debug("Raft thread pool after execute, cost time : {}", System.currentTimeMillis() - COST_TIME_WATCH.get());
        COST_TIME_WATCH.remove();
    }

    @Override
    protected void terminated() {
        // LOGGER.info("active count : {}, queue size : {}, pool size : {}", getActiveCount(), getQueue().size(), getPoolSize());
    }
}
