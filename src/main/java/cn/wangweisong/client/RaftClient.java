package cn.wangweisong.client;

import cn.wangweisong.raft.concurrent.SleepHelper;
import cn.wangweisong.raft.entity.ClientKVRequest;
import cn.wangweisong.raft.entity.ClientKVResponse;
import cn.wangweisong.raft.rpc.Request;
import cn.wangweisong.raft.rpc.Response;
import cn.wangweisong.raft.rpc.RpcClient;
import cn.wangweisong.raft.rpc.impl.DefaultRpcClient;
import com.alipay.remoting.exception.RemotingException;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author wang
 * @date 2019/11/17 周日 下午5:18
 */
public class RaftClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftClient.class);

    private static final RpcClient CLIENT = new DefaultRpcClient();

    static String addr = "localhost:8775";

    static List<String> list = Lists.newArrayList("localhost:8775", "localhost:8776", "localhost:8777");

    public static void main(String[] args) throws RemotingException, InterruptedException {
        AtomicLong count = new AtomicLong(3);
        Random random = new Random();
        List<String> commitKV = new ArrayList<>();
        for (int i = 0; i < 199 ; i++) {
            try {
                // 随机向集群发送信息
                int index = random.nextInt(3);
                addr = list.get(index);

                ClientKVRequest param = ClientKVRequest.newBuilder()
                        .key("key-" + i)
                        .value("value-" + i)
                        .type(ClientKVRequest.PUT)
                        .build();
                Request<ClientKVRequest> request = new Request<>();
                request.setCmd(Request.CLIENT_REQUEST);
                request.setUrl(addr);
                request.setObj(param);
                Response<ClientKVResponse> response = null;

                try {
                    response = CLIENT.send(request);
                } catch (Exception e) {
                    //
                }
                if (response != null && "ClientKVResponse(result=ok)".equals(response.getResult().toString())) {
                    LOGGER.info("成功将 key = '{}' 和 value = '{}' 发送到节点 {}", param.getKey(), param.getValue(), addr);
                    commitKV.add(param.getKey() + " " + param.getValue());
                }
            } catch (Exception e) {
                LOGGER.error("节点{}可能已经崩溃，换一个节点继续尝试", addr);
                i = i - 1;
            }
            SleepHelper.sleepMS(3000);
        }

        // 将确定提交的的写入文件
        try (FileOutputStream out = new FileOutputStream(new File("commit-kv.txt"))) {
            for (int i = 0; i < commitKV.size(); i++) {
                out.write((commitKV.get(i) + "\n").getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
