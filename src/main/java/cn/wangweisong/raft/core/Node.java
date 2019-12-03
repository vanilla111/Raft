package cn.wangweisong.raft.core;

import cn.wangweisong.raft.common.NodeConfig;
import cn.wangweisong.raft.entity.*;

/**
 * @author wang
 * @date 2019/11/13 周三 下午1:52
 */
public interface Node<T> extends LifeCycle {

    /**
     * 配置节点
     * @param config 自身端口和其他节点的地址
     */
    void setConfig(NodeConfig config);

    /**
     * 处理请求投票RPC
     * @param voteParam 其他节点发起的投票
     * @return
     */
    VoteResult handlerRequestVote(VoteParam voteParam);

    /**
     * 处理追加日志请求
     * @param entryParam 新的日志
     * @return
     */
    EntryResult handlerAppendEntries(EntryParam entryParam);

    /**
     * 处理客户端请求
     * @param request 客户端请求
     * @return
     */
    ClientKVResponse handlerClientRequest(ClientKVRequest request);

    /**
     * 转发客户端请求
     * @param request 客户端请求
     * @return
     */
    ClientKVResponse redirect(ClientKVRequest request);
}
