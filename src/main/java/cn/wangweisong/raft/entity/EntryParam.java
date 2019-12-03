package cn.wangweisong.raft.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 追加日志请求
 * @author wang
 * @date 2019/11/13 周三 下午1:33
 */
@Getter
@Setter
@ToString
public class EntryParam extends BaseParam {

    /** 领导者的ID（IP） */
    String leaderId;

    /** 新日志前一条日志的索引值 */
    long prevLogIndex;

    /** 新日志前一条日志的任期编号 */
    long prevLogTerm;

    /** 准备存储的日志条目（表示心跳时为空；一次发送多个是为了提高效率） */
    LogEntry[] entries;

    /** 领导者已经确认提交的日志索引值 */
    long leaderCommit;

    public EntryParam() {}

    private EntryParam(Builder builder) {
        setTerm(builder.term);
        setServerId(builder.serverId);
        setLeaderId(builder.leaderId);
        setPrevLogIndex(builder.prevLogIndex);
        setPrevLogTerm(builder.prevLogTerm);
        setEntries(builder.entries);
        setLeaderCommit(builder.leaderCommit);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {

        private long term;
        private String serverId;
        private String leaderId;
        private long prevLogIndex;
        private long prevLogTerm;
        private LogEntry[] entries;
        private long leaderCommit;

        private Builder() {
        }

        public Builder term(long val) {
            term = val;
            return this;
        }

        public Builder serverId(String val) {
            serverId = val;
            return this;
        }

        public Builder leaderId(String val) {
            leaderId = val;
            return this;
        }

        public Builder preLogIndex(long val) {
            prevLogIndex = val;
            return this;
        }

        public Builder preLogTerm(long val) {
            prevLogTerm = val;
            return this;
        }

        public Builder entries(LogEntry[] val) {
            entries = val;
            return this;
        }

        public Builder leaderCommit(long val) {
            leaderCommit = val;
            return this;
        }

        public EntryParam build() {
            return new EntryParam(this);
        }
    }
}
