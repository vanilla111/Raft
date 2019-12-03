package cn.wangweisong.raft.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author wang
 * @date 2019/11/13 周三 下午2:02
 */
@Setter
@Getter
@ToString
public class ClientKVRequest implements Serializable {

    public static int PUT = 0;
    public static int GET = 1;

    int type;

    String key;

    String value;

    private ClientKVRequest(Builder builder) {
        setType(builder.type);
        setKey(builder.key);
        setValue(builder.value);
    }

    public enum RequestType {
        /**
         * PUT 追加请求，GET 获取请求
         */
        PUT(0), GET(1);

        int code;

        RequestType(int code) {
            this.code = code;
        }

        public static RequestType value(int code) {
            for (RequestType value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
            return null;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private int type;
        private String key;
        private String value;

        private Builder() {}

        public Builder type(int val) {
            type = val;
            return this;
        }

        public Builder key(String val) {
            key = val;
            return this;
        }

        public Builder value(String val) {
            value = val;
            return this;
        }

        public ClientKVRequest build() {
            return new ClientKVRequest(this);
        }
    }
}
