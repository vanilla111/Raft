package cn.wangweisong.raft.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * 日志
 * @author wang
 * @date 2019/11/13 周三 下午12:53
 */
@Setter
@Getter
public class LogEntry implements Serializable, Comparable {

    private Long index;

    private Long term;

    private Command command;

    public LogEntry() {}

    public LogEntry(Long index, long term, Command command) {
        this.index = index;
        this.term = term;
        this.command = command;
    }

    public LogEntry(long term, Command command) {
        this.term = term;
        this.command = command;
    }

    private LogEntry(Builder builder) {
        setIndex(builder.index);
        setTerm(builder.term);
        setCommand(builder.command);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return -1;
        }
        LogEntry logEntry = (LogEntry) o;
        if (getIndex() > logEntry.getIndex()) {
            return 1;
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        LogEntry logEntry = (LogEntry) o;
        return Objects.equals(term, logEntry.getTerm()) &&
                Objects.equals(index, logEntry.getIndex()) &&
                Objects.equals(command, logEntry.getCommand());

    }

    @Override
    public String toString() {
        return "{ index=" + index + ", term=" + term + ", command=" + command + '}';
    }

    public static final class Builder {
        private Long index;
        private long term;
        private Command command;

        private Builder() {}

        public Builder index(Long val) {
            index = val;
            return this;
        }

        public Builder term(Long val) {
            term = val;
            return this;
        }

        public Builder command(Command val) {
            command = val;
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
}
