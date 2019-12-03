package cn.wangweisong.raft.core;

import cn.wangweisong.raft.entity.LogEntry;

/**
 * @author wang
 * @date 2019/11/15 周五 下午10:19
 */
public interface LogModule {

    /**
     * 写日志条目
     * @param logEntry
     */
    void write(LogEntry logEntry);

    /**
     * 读索引对应日志条目
     * @param index
     * @return
     */
    LogEntry read(Long index);

    /**
     * 移除从startIndex开始和之后所有的日志条目
     * @param startIndex
     */
    void removeOnStartIndex(Long startIndex);

    /**
     * 获取上一个日志条目
     * @return
     */
    LogEntry getLast();

    /**
     * 获取上一个日志条目的索引
     * @return
     */
    Long getLastIndex();
}
