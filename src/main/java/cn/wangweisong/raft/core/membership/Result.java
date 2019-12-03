package cn.wangweisong.raft.core.membership;

import lombok.Getter;
import lombok.Setter;

/**
 * @author wang
 * @date 2019/11/16 周六 上午12:11
 */
@Setter
@Getter
public class Result {
    public static final int FAIL = 0;
    public static final int SUCCESS = 1;

    private int status;
    private String leaderHint;

    public Result() {
    }

    private Result(Builder builder) {
        setStatus(builder.status);
        setLeaderHint(builder.leaderHint);
    }

    @Getter
    public enum Status {
        /**
         * 成员变更成功与否
         */
        FAIL(0), SUCCESS(1);

        int code;

        Status(int code) {
            this.code = code;
        }

        public static Status value(int v) {
            for (Status value : values()) {
                if (value.code == v) {
                    return value;
                }
            }
            return null;
        }
    }

    public static final class Builder {
        private int status;
        private String leaderHint;

        private Builder() {}

        public Builder status(int val) {
            status = val;
            return this;
        }

        public Builder leaderHint(String val) {
            leaderHint = val;
            return this;
        }

        public Result build() {
            return new Result(this);
        }
    }
}
