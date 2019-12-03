package cn.wangweisong.raft.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author wang
 * @date 2019/11/16 周六 下午1:06
 */
public class SleepHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SleepHelper.class);

    public static void sleepMS(int ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    public static void sleepS(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            LOGGER.warn(e.getMessage());
        }
    }
}
