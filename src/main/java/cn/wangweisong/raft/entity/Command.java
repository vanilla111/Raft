package cn.wangweisong.raft.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;

/**
 * k-v存储所需的数据
 * @author wang
 * @date 2019/11/13 周三 下午12:48
 */
@Setter
@Getter
@ToString
public class Command implements Serializable {

    private String key;

    private String value;

    public Command(String key, String value) {
        this.key = key;
        this.value = value;
    }

    private Command(Builder builder) {
        setKey(builder.key);
        setValue(builder.value);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        Command command = (Command) o;
        return Objects.equals(key, command.getKey()) &&
                Objects.equals(value, command.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }


    public static final class Builder {
        private String key;

        private String value;

        public Builder key(String val) {
            key = val;
            return this;
        }

        public Builder value(String val) {
            value = val;
            return this;
        }

        public Command build() {
            return new Command(this);
        }
    }
}
