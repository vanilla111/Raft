package cn.wangweisong.raft.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 追加日志返回值
 * @author wang
 * @date 2019/11/13 周三 下午1:41
 */
@Setter
@Getter
@ToString
public class EntryResult implements Serializable {

    /** 当前的任期编号，用于领导者去更新自己 */
    long term;

    /** 跟随者的日志条目与prevLogIndex prevLogTerm 相匹配时为真 */
    boolean success;

    public EntryResult(long term) {
        this.term = term;
    }

    public EntryResult(boolean success) {
        this.success = success;
    }

    private EntryResult(Builder builder) {
        setTerm(builder.term);
        setSuccess(builder.success);
    }

    public static EntryResult fail() {
        return new EntryResult(false);
    }

    public static EntryResult ok() {
        return new EntryResult(true);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {

        private long term;
        private boolean success;

        private Builder() {}

        public Builder term(long val) {
            term = val;
            return this;
        }

        public Builder success(boolean val) {
            success = val;
            return this;
        }

        public EntryResult build() {
            return new EntryResult(this);
        }
    }
}
