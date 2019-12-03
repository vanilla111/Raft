package cn.wangweisong.raft.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 请求投票结果
 * @author wang
 * @date 2019/11/13 周三 下午1:26
 */
@Setter
@Getter
public class VoteResult implements Serializable {

    /** 当前的任期编号，用户候选者更新自己的任期编号 */
    long term;

    /** 是否投票给候选者 */
    boolean voteGranted;

    public VoteResult(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    private VoteResult(Builder builder) {
        setTerm(builder.term);
        setVoteGranted(builder.voteGranted);
    }

    public static VoteResult fail() {
        return new VoteResult(false);
    }

    public static VoteResult ok() {
        return new VoteResult(true);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private long term;
        private boolean voteGranted;

        private Builder() { }

        public Builder term(long term) {
            this.term = term;
            return this;
        }

        public Builder voteGranted(boolean voteGranted) {
            this.voteGranted = voteGranted;
            return this;
        }

        public VoteResult build() {
            return new VoteResult(this);
        }
    }
}
