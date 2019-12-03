package cn.wangweisong.raft.rpc;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author wang
 * @date 2019/11/12 周二 下午11:31
 */
@Getter
@Setter
@ToString
public class Request<T> implements Serializable {
    /** 请求投票 */
    public static final int REQUEST_VOTE = 0;
    /** 追加日志 */
    public static final int APPEND_ENTRIES = 1;
    /** 客户端请求 */
    public static final int CLIENT_REQUEST = 2;
    /** 配置变更，新增 */
    public static final int CHANGE_CONFIG_ADD = 3;
    /** 配置变更，移除 */
    public static final int CHANGE_CONFIG_REMOVE = 4;
    /** 请求类型 */
    private int cmd = -1;

    /**
     * RPC请求调用的参数
     */
    private T obj;

    String url;

    public Request() {}

    public Request(int cmd, T obj, String url) {
        this.cmd = cmd;
        this.obj = obj;
        this.url = url;
    }

    private Request(Builder builder) {
        this.cmd = builder.cmd;
        this.obj = (T) builder.obj;
        this.url = builder.url;
    }

    public static Builder newBuilder() {
        return new Builder<>();
    }

    public final static class Builder<T> {
        private int cmd;
        private Object obj;
        private String url;

        private Builder() {}

        public Builder cmd(int val) {
            cmd = val;
            return this;
        }

        public Builder obj(Object val) {
            obj = val;
            return this;
        }

        public Builder url(String val) {
            url = val;
            return this;
        }

        public Request<T> build() {
            return new Request<T>(this);
        }
    }
}
