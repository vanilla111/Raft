package cn.wangweisong.raft.core.impl;

import cn.wangweisong.raft.core.StateMachine;
import cn.wangweisong.raft.entity.Command;
import cn.wangweisong.raft.entity.LogEntry;
import com.alibaba.fastjson.JSON;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 状态机实现
 * @author wang
 * @date 2019/11/15 周五 下午10:29
 */
public class DefaultStateMachine implements StateMachine {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStateMachine.class);

    private static String dbDir;
    private static String stateMachineDir;
    private static RocksDB machineDB;

    static {
        if (dbDir == null) {
            dbDir = "./rocksDB/" + System.getProperty("serverPort");
        }
        if (stateMachineDir == null) {
            stateMachineDir = dbDir + "/state_machine";
        }
        RocksDB.loadLibrary();
    }

    private DefaultStateMachine() {
        synchronized (this) {
            try {
                File file = new File(stateMachineDir);
                if (!file.exists()) {
                    if (file.mkdirs()) {
                        LOGGER.warn("Create a new directory: " + stateMachineDir);
                    }
                }
                Options options = new Options();
                options.setCreateIfMissing(true);
                machineDB = RocksDB.open(options, stateMachineDir);
            } catch (RocksDBException e) {
                LOGGER.warn(e.getMessage());
            }
        }
    }

    public static DefaultStateMachine getInstance() {
        return DefaultStateMachineLazyHolder.INSTANCE;
    }

    private static class DefaultStateMachineLazyHolder {
        private static final DefaultStateMachine INSTANCE = new DefaultStateMachine();
    }

    @Override
    public void apply(LogEntry logEntry) {
        try {
            Command command = logEntry.getCommand();
            if (command == null) {
                throw new IllegalArgumentException("Command can not be null, LogEntry: \n" + logEntry.toString());
            }
            String key = command.getKey();
            // 将整个logEntry放入数据库，方便观察日志条目详情
            machineDB.put(key.getBytes(), JSON.toJSONBytes(logEntry));
        } catch (RocksDBException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    @Override
    public LogEntry get(String key) {
        try {
            byte[] result = machineDB.get(key.getBytes());
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            LOGGER.warn(e.getMessage());
        }
        return null;
    }

    @Override
    public String getString(String key) {
        try {
            byte[] result = machineDB.get(key.getBytes());
            if (result != null) {
                return new String(result);
            }
        } catch (RocksDBException e) {
            LOGGER.warn(e.getMessage());
        }
        return "";
    }

    @Override
    public void setString(String key, String value) {
        try {
            machineDB.put(key.getBytes(), value.getBytes());
        } catch (RocksDBException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    @Override
    public void delString(String... keys) {
        try {
            for (String key : keys) {
                machineDB.delete(key.getBytes());
            }
        } catch (RocksDBException e) {
            LOGGER.warn(e.getMessage());
        }
    }
}
