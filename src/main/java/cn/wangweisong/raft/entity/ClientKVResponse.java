package cn.wangweisong.raft.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author wang
 * @date 2019/11/13 周三 下午2:03
 */
@Getter
@Setter
@ToString
public class ClientKVResponse implements Serializable {
    Object result;

    public ClientKVResponse(Object result) {
        this.result = result;
    }

    private ClientKVResponse(Builder builder) {
        setResult(builder.result);
    }

    public static ClientKVResponse ok() {
        return new ClientKVResponse("ok");
    }

    public static ClientKVResponse fail() {
        return new ClientKVResponse("fail");
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {

        private Object result;

        private Builder() {}

        public Builder result(Object val) {
            result = val;
            return this;
        }

        public ClientKVResponse build() {
            return new ClientKVResponse(this);
        }
    }
}
