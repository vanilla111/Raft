package cn.wangweisong.raft.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 请求选票参数类型
 * @author wang
 * @date 2019/11/13 周三 下午1:16
 */
@Setter
@Getter
public class VoteParam extends BaseParam {

    /** 请求选票的候选人ID（IP） */
    String candidateId;

    /** 候选人最后日志条目的索引值 */
    long lastLogIndex;

    /** 候选人最后日志条目的任期编号 */
    long lastLogTerm;

    private VoteParam(Builder builder) {
        setTerm(builder.term);
        setServerId(builder.serverId);
        setCandidateId(builder.candidateId);
        setLastLogIndex(builder.lastLogIndex);
        setLastLogTerm(builder.lastLogTerm);
    }

    @Override
    public String toString() {
        return "VoteParam{" +
                "candidateId='" + candidateId + '\'' +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                ", term=" + term +
                ", serverId='" + serverId + '\'' +
                '}';
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private long term;
        private String serverId;
        private String candidateId;
        private long lastLogIndex;
        private long lastLogTerm;

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

        public Builder candidateId(String val) {
            candidateId = val;
            return this;
        }

        public Builder lastLogIndex(long val) {
            lastLogIndex = val;
            return this;
        }

        public Builder lastLogTerm(long val) {
            lastLogTerm = val;
            return this;
        }

        public VoteParam build() {
            return new VoteParam(this);
        }
    }
}
