package ConsensusTest;

import cn.wangweisong.raft.entity.LogEntry;
import com.alibaba.fastjson.JSON;
import org.junit.Before;
import org.junit.Test;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

/**
 * @author wang
 * @date 2019/11/17 周日 下午5:56
 */
public class CheckAllLogEntries {

    private static String dbDir = "/Users/wang/IdeaProjects/raft-kv/rocksDB/";
    private static String logDir = "logs_module/";
    private static String stateDir = "/state_machine/";
    private static RocksDB rocksDB_8775;
    private static RocksDB rocksDB_8776;
    private static RocksDB rocksDB_8777;
    private static RocksDB rocksDB_8778;
    private static RocksDB rocksDB_8779;
    private static String[] ports = {"8775", "8776", "8777", "8778", "8779"};

    static {
        RocksDB.loadLibrary();
    }

    @Before
    public void before() {
        try {
            rocksDB_8775 = RocksDB.open(new Options().setCreateIfMissing(true), dbDir + ports[0] + stateDir);
            rocksDB_8776 = RocksDB.open(new Options().setCreateIfMissing(true), dbDir + ports[1] + stateDir);
            rocksDB_8777 = RocksDB.open(new Options().setCreateIfMissing(true), dbDir + ports[2] + stateDir);
            rocksDB_8778 = RocksDB.open(new Options().setCreateIfMissing(true), dbDir + ports[3] + stateDir);
            rocksDB_8779 = RocksDB.open(new Options().setCreateIfMissing(true), dbDir + ports[4] + stateDir);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void checkKeyValue() {
        String key = "key-";
        for (int i = 0; i < 199 ; i++) {
            try {
                byte[] b1 = rocksDB_8775.get((key + i).getBytes());
                byte[] b2 = rocksDB_8776.get((key + i).getBytes());
                byte[] b3 = rocksDB_8777.get((key + i).getBytes());
                byte[] b4 = rocksDB_8778.get((key + i).getBytes());
                byte[] b5 = rocksDB_8779.get((key + i).getBytes());
                if (b1 == null || b2 == null || b3 == null || b4 == null || b5 == null) {
                    if (b1 == null && b2 == null && b3 == null && b4 == null && b5 == null) {
                        continue;
                    } else {
                        System.out.println("有不一致的状态");
                    }
                }
                LogEntry s1 = JSON.parseObject(b1, LogEntry.class);
                LogEntry s2 = JSON.parseObject(b2, LogEntry.class);
                LogEntry s3 = JSON.parseObject(b3, LogEntry.class);
                LogEntry s4 = JSON.parseObject(b4, LogEntry.class);
                LogEntry s5 = JSON.parseObject(b5, LogEntry.class);

                if (s1.equals(s2) && s2.equals(s3) && s3.equals(s4) && s4.equals(s5)) {
                    System.out.println(key + i + "对应的值为" + s1 + " , 五个节点数据保持一致");
                } else {
                    System.out.println("五个状态机不一致");
                    break;
                }
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void printAllStateMachine() {
        LogEntry log;
        String key = "key-";
        try {
            for (int i = 0; i < 199; i++) {
                byte[] b = rocksDB_8775.get((key + i).getBytes());
                if (b == null) continue;
                log = JSON.parseObject(b, LogEntry.class);
                System.out.println(log);
            }
            System.out.println("========================");
            for (int i = 0; i < 199; i++) {
                byte[] b = rocksDB_8776.get((key + i).getBytes());
                if (b == null) continue;
                log = JSON.parseObject(b, LogEntry.class);
                System.out.println(log);
            }
            System.out.println("========================");
            for (int i = 0; i < 199; i++) {
                byte[] b = rocksDB_8777.get((key + i).getBytes());
                if (b == null) continue;
                log = JSON.parseObject(b, LogEntry.class);
                System.out.println(log);
            }
            System.out.println("========================");
            for (int i = 0; i < 199; i++) {
                byte[] b = rocksDB_8778.get((key + i).getBytes());
                if (b == null) continue;
                log = JSON.parseObject(b, LogEntry.class);
                System.out.println(log);
            }
            System.out.println("========================");
            for (int i = 0; i < 199; i++) {
                byte[] b = rocksDB_8779.get((key + i).getBytes());
                if (b == null) continue;
                log = JSON.parseObject(b, LogEntry.class);
                System.out.println(log);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }




}
