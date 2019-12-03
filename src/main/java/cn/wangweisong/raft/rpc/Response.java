package cn.wangweisong.raft.rpc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author wang
 * @date 2019/11/12 周二 下午11:31
 */
@Getter
@Setter
public class Response<T> implements Serializable {

    private T result;

    public Response(T result) {
        this.result = result;
    }

    private Response(Builder builder) {
        result = (T) builder.result;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Response ok() {
        return new Response<String>("ok");
    }

    public static Response fail() {
        return new Response<String>("fail");
    }

    @Override
    public String toString() {
        return "Response{ result = " + result + " }";
    }

    public static final class Builder {
        private Object result;

        private Builder() {}

        public Builder result(Object res) {
            result = res;
            return this;
        }

        public Response build() {
            return new Response(this);
        }
    }

}
