package cn.wangweisong.raft.concurrent;

import java.util.concurrent.*;

/**
 * @author wang
 * @date 2019/11/16 周六 上午11:00
 */
public class RaftThreadPool {
    private static int cpu = Runtime.getRuntime().availableProcessors();
    private static int maxPoolSize = cpu * 2;
    private static final int QUEUE_SIZE = 1024;
    private static final long KEEP_TIME = 1000 * 60;
    private static TimeUnit keepTimeUnit = TimeUnit.MILLISECONDS;

    private static ScheduledExecutorService executorService = getExecutorService();
    private static ThreadPoolExecutor poolExecutor = getPoolExecutor();

    private static ScheduledExecutorService getExecutorService() {
        return new ScheduledThreadPoolExecutor(cpu, new NameThreadFactory());
    }

    private static ThreadPoolExecutor getPoolExecutor() {
        return new RaftThreadPoolExecutor(
                cpu,
                maxPoolSize,
                KEEP_TIME,
                keepTimeUnit,
                new LinkedBlockingQueue<>(QUEUE_SIZE),
                new NameThreadFactory()
        );
    }

    static class NameThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new RaftThread("Raft thread", r);
            thread.setDaemon(true);
            thread.setPriority(5);
            return thread;
        }
    }

    public static void scheduleAtFixedRate(Runnable r, long initialDelay, long period) {
        // 延迟initialDelay时间之后，每隔period执行一次。若任务时间大于period，那么下一次执行会被推迟
        executorService.scheduleAtFixedRate(r, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    public static void scheduleWithFixedDelay(Runnable r, long delay) {
        // 立刻执行，前一次任务执行完成后间隔delay再执行下一次。
        executorService.scheduleWithFixedDelay(r, 0, delay, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("unchecked")
    public static <T> Future<T> submit(Callable c) {
        return poolExecutor.submit(c);
    }

    public static void execute(Runnable r) {
        poolExecutor.execute(r);
    }

    public static void execute(Runnable r, boolean sync) {
        if (sync) {
            r.run();
        } else {
            poolExecutor.execute(r);
        }
    }
}
