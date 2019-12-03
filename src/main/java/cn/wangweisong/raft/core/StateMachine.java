package cn.wangweisong.raft.core;

import cn.wangweisong.raft.entity.LogEntry;

/**
 * @author wang
 * @date 2019/11/15 周五 下午10:21
 */
public interface StateMachine {

    /**
     * 将数据应用到状态机
     * @param logEntry
     */
    void apply(LogEntry logEntry);

    LogEntry get(String key);

    String getString(String key);

    void setString(String key, String value);

    void delString(String ...keys);
}
