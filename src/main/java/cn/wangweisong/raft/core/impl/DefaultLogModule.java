package cn.wangweisong.raft.core.impl;

import cn.wangweisong.raft.core.LogModule;
import cn.wangweisong.raft.entity.LogEntry;
import cn.wangweisong.raft.util.RaftUtil;
import com.alibaba.fastjson.JSON;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * 日志实现，不关心key，只关心index
 * @author wang
 * @date 2019/11/15 周五 下午10:46
 */
public class DefaultLogModule implements LogModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLogModule.class);

    private static String dbDir;
    private static String logsDir;
    private static RocksDB logDB;

    private static final byte[] LAST_INDEX_KEY = "LAST_INDEX_KEY".getBytes();

    /** 重入锁 */
    private ReentrantLock lock = new ReentrantLock();

    static {
        if (dbDir == null) {
            dbDir = "./rocksDB/" + System.getProperty("serverPort");
        }
        if (logsDir == null) {
            logsDir = dbDir + "/logs_module";
        }
        RocksDB.loadLibrary();
    }

    private DefaultLogModule() {
        Options options = new Options();
        options.setCreateIfMissing(true);
        File file = new File(logsDir);
        if (!file.exists()) {
            if (file.mkdirs()) {
                LOGGER.warn("Create a new directory: " + logsDir);
            }
        }
        try {
            logDB = RocksDB.open(options, logsDir);
        } catch (RocksDBException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    public static DefaultLogModule getInstance() {
        return DefaultLogsLazyHolder.INSTANCE;
    }

    private static class DefaultLogsLazyHolder {
        private static final DefaultLogModule INSTANCE = new DefaultLogModule();
    }

    @Override
    public void write(LogEntry logEntry) {
        // logEntry 的索引 index 就是 key, 严格递增
        boolean success = false;
        try {
            lock.tryLock(3000, MILLISECONDS);
            logEntry.setIndex(getLastIndex() + 1);
            logDB.put(RaftUtil.LongToString(logEntry.getIndex()).getBytes(), JSON.toJSONBytes(logEntry));
            success = true;
            LOGGER.info("DefaultLogModule write log entry success, logEntry : {}", logEntry);
        } catch (RocksDBException | InterruptedException e) {
            LOGGER.warn(e.getMessage());
        } finally {
            if (success) {
                updateLastIndex(logEntry.getIndex());
            }
            lock.unlock();
        }
    }

    @Override
    public LogEntry read(Long index) {
        try {
            byte[] result = logDB.get(RaftUtil.LongToString(index).getBytes());
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void removeOnStartIndex(Long startIndex) {
        boolean success = false;
        int count = 0;
        try {
            lock.tryLock(3000, MILLISECONDS);
            long lastIndex = getLastIndex();
            for (long i = startIndex; i <= lastIndex; i++) {
                logDB.delete(RaftUtil.LongToString(i).getBytes());
                ++count;
            }
            success = true;
            LOGGER.warn("A total of {} LogEntries deletions from {} to {} were successful.",
                    count, startIndex, lastIndex);
        } catch (InterruptedException | RocksDBException e) {
            LOGGER.warn(e.getMessage());
        } finally {
            if (success) {
                updateLastIndex(getLastIndex() - count);
            }
            lock.unlock();
        }
    }

    @Override
    public LogEntry getLast() {
        try {
            byte[] result = logDB.get(RaftUtil.LongToString(getLastIndex()).getBytes());
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Long getLastIndex() {
        byte[] lastIndex = "-1".getBytes();
        try {
            lastIndex = logDB.get(LAST_INDEX_KEY);
            if (lastIndex == null) {
                lastIndex = "-1".getBytes();
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return Long.valueOf(new String(lastIndex));
    }

    private void updateLastIndex(Long index) {
        try {
            logDB.put(LAST_INDEX_KEY, index.toString().getBytes());
        } catch (RocksDBException e) {
            e.printStackTrace();
        }

    }
}
