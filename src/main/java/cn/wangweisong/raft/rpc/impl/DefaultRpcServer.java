package cn.wangweisong.raft.rpc.impl;

import cn.wangweisong.raft.common.Peer;
import cn.wangweisong.raft.core.ClusterMembershipChanges;
import cn.wangweisong.raft.core.impl.DefaultNode;
import cn.wangweisong.raft.entity.ClientKVRequest;
import cn.wangweisong.raft.entity.EntryParam;
import cn.wangweisong.raft.entity.VoteParam;
import cn.wangweisong.raft.rpc.RaftUserProcessor;
import cn.wangweisong.raft.rpc.Request;
import cn.wangweisong.raft.rpc.Response;
import cn.wangweisong.raft.rpc.RpcServer;
import com.alipay.remoting.BizContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wang
 * @date 2019/11/13 周三 上午12:05
 */
@SuppressWarnings("unchecked")
public class DefaultRpcServer implements RpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRpcServer.class);

    private volatile boolean flag;

    private DefaultNode node;

    private com.alipay.remoting.rpc.RpcServer rpcServer;

    public DefaultRpcServer(int port, DefaultNode node) {
        if (flag) {
            return;
        }
        synchronized (this) {
            if (flag) {
                return;
            }

            rpcServer = new com.alipay.remoting.rpc.RpcServer(port, false, false);
            rpcServer.registerUserProcessor(new RaftUserProcessor<Request>() {
                @Override
                public Object handleRequest(BizContext bizContext, Request request) throws Exception {
                    return handlerRequest(request);
                }
            });
            LOGGER.info("RPC server start successful");
        }
        this.node = node;
        flag = true;
    }

    @Override
    public Response handlerRequest(Request request) {
        if (request.getCmd() == Request.REQUEST_VOTE) {
            return new Response(node.handlerRequestVote((VoteParam) request.getObj()));
        } else if (request.getCmd() == Request.APPEND_ENTRIES) {
            return new Response(node.handlerAppendEntries((EntryParam) request.getObj()));
        } else if (request.getCmd() == Request.CLIENT_REQUEST) {
            return new Response(node.handlerClientRequest((ClientKVRequest) request.getObj()));
        } else if (request.getCmd() == Request.CHANGE_CONFIG_REMOVE) {
            return new Response(((ClusterMembershipChanges) node).removePeer((Peer) request.getObj()));
        } else if (request.getCmd() == Request.CHANGE_CONFIG_ADD) {
            return new Response(((ClusterMembershipChanges) node).addPeer((Peer) request.getObj()));
        }
        return null;
    }

    @Override
    public void start() {
        rpcServer.start();
    }

    @Override
    public void stop() {
        rpcServer.stop();
    }
}
