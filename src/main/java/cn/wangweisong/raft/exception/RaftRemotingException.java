package cn.wangweisong.raft.exception;

/**
 * @author wang
 * @date 2019/11/13 周三 上午12:01
 */
public class RaftRemotingException extends RuntimeException {
    public RaftRemotingException() {
        super();
    }

    public RaftRemotingException(String message) {
        super(message);
    }
}
