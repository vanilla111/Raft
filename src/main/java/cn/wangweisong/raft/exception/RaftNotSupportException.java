package cn.wangweisong.raft.exception;

/**
 * @author wang
 * @date 2019/11/13 周三 上午12:02
 */
public class RaftNotSupportException extends RuntimeException {
    public RaftNotSupportException() {
        super();
    }

    public  RaftNotSupportException(String message) {
        super(message);
    }
}
