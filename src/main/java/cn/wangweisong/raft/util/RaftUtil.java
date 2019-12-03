package cn.wangweisong.raft.util;

/**
 * @author wang
 * @date 2019/11/15 周五 下午11:49
 */
public class RaftUtil {

    public static long convert(Long l) {
        if (l == null) {
            return 0;
        }
        return l;
    }

    public static String LongToString(Long l) {
        if (l == null) {
            return "0";
        }
        return l.toString();
    }
}
