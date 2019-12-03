package cn.wangweisong.raft.core.impl;

import cn.wangweisong.raft.common.NodeConfig;
import cn.wangweisong.raft.common.NodeStatus;
import cn.wangweisong.raft.common.Peer;
import cn.wangweisong.raft.common.PeerSetManager;
import cn.wangweisong.raft.concurrent.RaftThreadPool;
import cn.wangweisong.raft.core.*;
import cn.wangweisong.raft.core.membership.Result;
import cn.wangweisong.raft.entity.*;
import cn.wangweisong.raft.exception.RaftRemotingException;
import cn.wangweisong.raft.rpc.Request;
import cn.wangweisong.raft.rpc.Response;
import cn.wangweisong.raft.rpc.RpcClient;
import cn.wangweisong.raft.rpc.RpcServer;
import cn.wangweisong.raft.rpc.impl.DefaultRpcClient;
import cn.wangweisong.raft.rpc.impl.DefaultRpcServer;
import cn.wangweisong.raft.util.RaftUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 节点抽象类，初始角色为 follower
 * @author wang
 * @date 2019/11/13 周三 上午12:10
 */
@Setter
@Getter
public class DefaultNode<T> implements Node<T>, ClusterMembershipChanges {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNode.class);

    /** 选举时间间隔基数 */
    private volatile long electionTime = 15 * 1000;
    /** 上一次选举时间 */
    private volatile long preElectionTime = 0;
    /** 上一次心跳时间戳 */
    private volatile long preHeartBeatTime = 0;
    /** 心跳间隔时间基数 */
    private final long heartBeatTick = 5 * 1000;

    /**
     * 节点当前状态
     * @see NodeStatus
     */
    private volatile NodeStatus status = NodeStatus.FOLLOWER;

    private PeerSetManager peerSetManager;

    /*====================所有节点持久保存的数据/对象=======================*/
    /** 节点所知道的最新的任期编号 */
    private volatile long currentTerm = 0;
    /** 当前投票给的对象，即当前获得该节点选票的候选人 */
    private volatile String votedFor;
    /** 每一给日志条目包含一个状态执行命令和一个任期编号 */
    LogModule logModule;
    /** 状态机 */
    StateMachine stateMachine;

    /*====================所有节点经常变更的数据=======================*/
    /** 当前节点已知的最大的已经确认提交的日志条目索引值 */
    private volatile long commitIndex;
    /** 最后被写入到状态机的日志条目索引值（严格递增） */
    private volatile long lastApplied = 0;

    /*====================领导者节点经常变更的数据======================*/
    /** 对于所有节点，需要发送给它的下一个日志条目的索引值（初始化为领导者时最后索引值+1）*/
    private Map<Peer, Long> nextIndices;
    /** 对于所有节点，已经复制给它的日志条目的最高索引值 */
    private Map<Peer, Long> matchIndices;

    /*====================RPC和节点配置======================*/

    private volatile boolean started;
    private NodeConfig nodeConfig;
    private static RpcServer RPC_SERVER;
    private RpcClient rpcClient;

    /*====================一致性模块======================*/

    Consensus consensus;
    ClusterMembershipChanges delegate;

    /*====================任务模块======================*/

    private HeartBeatTask heartBeatTask = new HeartBeatTask();
    private ElectionTask electionTask = new ElectionTask();
    private ReplicationFailQueueConsumer replicationFailQueueConsumer = new ReplicationFailQueueConsumer();
    /** 复制失败任务队列 */
    private LinkedBlockingDeque<ReplicationFailModel> replicationFailQueue = new LinkedBlockingDeque<>(2048);

    private DefaultNode() {
    }

    public static DefaultNode getInstance() {
        return DefaultNodeLazyHolder.INSTANCE;
    }

    private static class DefaultNodeLazyHolder {
        private static final DefaultNode INSTANCE = new DefaultNode();
    }

    @Override
    public void init() {
        if (started) {
            return;
        }
        synchronized (this) {
            if (started) {
                return ;
            }
            RPC_SERVER.start();

            consensus = new DefaultConsensus(this);
            delegate = new ClusterMembershipChangesImpl(this);

            RaftThreadPool.scheduleWithFixedDelay(heartBeatTask, 500);
            RaftThreadPool.scheduleAtFixedRate(electionTask, 6000, 500);
            RaftThreadPool.execute(replicationFailQueueConsumer);

            LogEntry logEntry = logModule.getLast();
            if (logEntry != null) {
                currentTerm = logEntry.getTerm();
            }
            started = true;
            LOGGER.info("Node started successfully, self ID : {}", peerSetManager.getSelf());
        }
    }

    @Override
    public void setConfig(NodeConfig config) {
        this.nodeConfig = config;
        stateMachine = DefaultStateMachine.getInstance();
        logModule = DefaultLogModule.getInstance();
        peerSetManager = PeerSetManager.getInstance();
        for (String peerAddress : nodeConfig.getPeerAddress()) {
            Peer peer = new Peer(peerAddress);
            peerSetManager.addPeer(peer);
            if (peerAddress.equals("localhost:" + config.getSelfPort())) {
                peerSetManager.setSelf(peer);
            }
        }
        RPC_SERVER = new DefaultRpcServer(config.selfPort, this);
        rpcClient = new DefaultRpcClient();
    }

    @Override
    public VoteResult handlerRequestVote(VoteParam voteParam) {
        LOGGER.warn("The request will be processed by the consensus module with the parameter {}.", voteParam);
        return consensus.requestVote(voteParam);
    }

    @Override
    public EntryResult handlerAppendEntries(EntryParam entryParam) {
        if (entryParam.getEntries() != null) {
            LOGGER.warn("Request for append log entries received from node {}, and the content = {}", entryParam.getLeaderId(), entryParam.getEntries());
        }
        return consensus.appendEntries(entryParam);
    }

    /**
     * 客户端的每一个请求都包含一个状态机执行的命令
     * 领导者把这条指令作为一条新的日志追加到日志中去，然后并行的发起追加日志条目RPC给其他的节点，让他们复制这条日志条目
     * 如果跟随者崩溃或者运行缓慢，再或者网络延迟、丢包
     * 领导者会不断尝试追加日志条目RPC给他的追随者（即使追随者已经收到并且给了答复），直到所有的跟随者都存储了这些日志条目
     * 当日志条目被安全地复制，领导者会应用这条日志到它的状态机中然后把客户请求的执行结果返回给客户端
     * @param request 客户端请求
     * @return
     */
    @Override
    public synchronized ClientKVResponse handlerClientRequest(ClientKVRequest request) {
        LOGGER.warn("Handle client requests, operation : [{}], key : [{}], value : [{}]",
                ClientKVRequest.RequestType.value(request.getType()), request.getKey(), request.getValue());
        // 非leader节点收到请求，转发给leader
        if (status != NodeStatus.LEADER) {
            return redirect(request);
        }

        if (request.getType() == ClientKVRequest.GET) {
            LogEntry logEntry = stateMachine.get(request.getKey());
            if (logEntry != null) {
                return new ClientKVResponse(logEntry.getCommand());
            }
            return new ClientKVResponse(null);
        }
        // 追加日志
        LogEntry logEntry = LogEntry.newBuilder()
                .command(Command.newBuilder()
                        .key(request.getKey())
                        .value(request.getValue())
                        .build())
                .term(currentTerm)
                .build();
        // 预提交到本地，并设置索引值
        logModule.write(logEntry);
        final AtomicInteger success = new AtomicInteger(0);
        List<Future<Boolean>> futureList = new CopyOnWriteArrayList<>();
        int count = 0;
        // 复制到其他节点
        for (Peer peer : peerSetManager.getPeerSetWithOutSelf()) {
            count++;
            futureList.add(replication(peer, logEntry));
        }

        CountDownLatch latch = new CountDownLatch(futureList.size());
        List<Boolean> resultList = new CopyOnWriteArrayList<>();
        getRpcAppendResult(futureList, latch, resultList);

        try {
            latch.await(4000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Boolean aBoolean : resultList) {
            if (aBoolean) {
                success.incrementAndGet();
            }
        }

        // 如果存在一个满足N > commitIndex的 N，并且大多数的matchIndex[i] ≥ N成立，
        // 并且log[N].term == currentTerm成立，那么令 commitIndex 等于这个 N （5.3 和 5.4 节）
        List<Long> matchIndexList = new ArrayList<>(matchIndices.values());
        int median = 0;
        // 小于2，没有意义
        if (matchIndexList.size() >= 2) {
            Collections.sort(matchIndexList);
            median = matchIndexList.size() / 2;
        }
        Long N = matchIndexList.get(median);
        if (N > commitIndex) {
            LogEntry entry = logModule.read(N);
            if (entry != null && entry.getTerm() == currentTerm) {
                commitIndex = N;
            }
        }

        // 响应客户端，成功一半
        if (success.get() >= (count / 2)) {
            commitIndex = logEntry.getIndex();
            getStateMachine().apply(logEntry);
            lastApplied = commitIndex;
            return ClientKVResponse.ok();
        } else {
            logModule.removeOnStartIndex(logEntry.getIndex());
            return ClientKVResponse.fail();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClientKVResponse redirect(ClientKVRequest request) {
        Request<ClientKVRequest> r = Request.newBuilder()
                .obj(request)
                .url(peerSetManager.getLeader().getAddress())
                .cmd(Request.CLIENT_REQUEST)
                .build();
        Response response = rpcClient.send(r);
        return (ClientKVResponse) response.getResult();
    }

    private void getRpcAppendResult(List<Future<Boolean>> futureList, CountDownLatch latch, List<Boolean> resultList) {
        for (Future<Boolean> future : futureList) {
            RaftThreadPool.execute(() -> {
                try {
                    resultList.add(future.get(3000, TimeUnit.MILLISECONDS));
                } catch (CancellationException | TimeoutException | ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    resultList.add(false);
                } finally {
                    latch.countDown();
                }
            });
        }
    }

    public Future<Boolean> replication(Peer peer, LogEntry logEntry) {
        return RaftThreadPool.submit(() -> {
            long start = System.currentTimeMillis(), end = start;
            // 20秒重试时间
            while (end - start < 20 * 1000L) {
                EntryParam entryParam = EntryParam.newBuilder()
                        .term(currentTerm)
                        .serverId(peer.getAddress())
                        .leaderId(peerSetManager.getSelf().getAddress())
                        .leaderCommit(commitIndex)
                        .build();
                // 以该节点，即领导者，为准
                Long nextIndex = nextIndices.get(peer);
                LinkedList<LogEntry> logEntries = new LinkedList<>();
                if (logEntry.getIndex() >= nextIndex) {
                    for (long i = nextIndex; i <= logEntry.getIndex(); i++) {
                        LogEntry temp = logModule.read(i);
                        if (temp != null) {
                            logEntries.add(temp);
                        }
                    }
                } else {
                    logEntries.add(logEntry);
                }
                // 注意索引值最小的那个日志
                LogEntry preLog = getPreLog(logEntries.getFirst());
                entryParam.setPrevLogIndex(preLog.getIndex());
                entryParam.setPrevLogTerm(preLog.getTerm());
                entryParam.setEntries(logEntries.toArray(new LogEntry[0]));

                Request request = Request.newBuilder()
                        .cmd(Request.APPEND_ENTRIES)
                        .obj(entryParam)
                        .url(peer.getAddress())
                        .build();
                try {
                    Response response = getRpcClient().send(request);
                    if (response == null) {
                        return false;
                    }
                    EntryResult result = (EntryResult) response.getResult();
                    if (result != null && result.isSuccess()) {
                        // 该peer追加日志条目成功
                        nextIndices.put(peer, logEntry.getIndex() + 1);
                        matchIndices.put(peer, logEntry.getIndex());
                        return true;
                    } else if (result != null) {
                        if (result.getTerm() > currentTerm) {
                            // 对方的任期编号比该节点大，所以该节点变为跟随者
                            currentTerm = result.getTerm();
                            status = NodeStatus.FOLLOWER;
                            return false;
                        } else {
                            // 没该节点大却失败了，说明index term中有一个不匹配
                            if (nextIndex == 0) {
                                nextIndex = 1L;
                            }
                            nextIndices.put(peer, nextIndex - 1);
                        }
                    }
                    end = System.currentTimeMillis();
                } catch (Exception e) {
                    LOGGER.error("Fatal error, replication log failed.");
                    // TODO 失败的追加日志请求放入失败队列
                    return false;
                }
            }
            // 超时，直接放弃
            return false;
        });
    }

    private LogEntry getPreLog(LogEntry logEntry) {
        LogEntry entry = logModule.read(logEntry.getIndex() - 1);
        if (entry == null) {
            // 没有前一个日志条目
            entry = LogEntry.newBuilder().index(0L).term(0L).command(null).build();
        }
        return entry;
    }

    @Override
    public void destroy() throws Throwable {
        RPC_SERVER.stop();
    }

    @Override
    public Result addPeer(Peer newPeer) {
        return delegate.addPeer(newPeer);
    }

    @Override
    public Result removePeer(Peer oldPeer) {
        return delegate.removePeer(oldPeer);
    }

    class ReplicationFailQueueConsumer implements Runnable {
        /** 一分钟 */
        long intervalTime = 1000 * 60;

        @Override
        public void run() {
            while (true) {
                try {
                    ReplicationFailModel model = replicationFailQueue.take();
                    if (status.getCode() != NodeStatus.LEADER.getCode()) {
                        replicationFailQueue.clear();
                        continue;
                    }
                    LOGGER.warn("Replication fail, now will retry replication, content detail : [{}]", model.logEntry);
                    long offerTime = model.offerTime;
                    if (System.currentTimeMillis() - offerTime > intervalTime) {
                        LOGGER.warn("A retry timeout of more than 1 minute may indicate that the queue is may be full, indicating a serious problem.");
                    }
                    Callable callable = model.callable;
                    Future<Boolean> future = RaftThreadPool.submit(callable);
                    Boolean r = future.get(3000, TimeUnit.MILLISECONDS);
                    if (r) {
                        tryApplyStateMachine(model);
                    }
                } catch (InterruptedException e) {

                } catch (ExecutionException | TimeoutException e) {
                    LOGGER.warn(e.getMessage());
                }
            }
        }
    }

    private void tryApplyStateMachine(ReplicationFailModel model) {
        String success = stateMachine.getString(model.successKey);
        stateMachine.setString(model.successKey, String.valueOf(Integer.parseInt(success) + 1));
        String count = stateMachine.getString(model.countKey);
        if (Integer.parseInt(success) >= Integer.parseInt(count) / 2) {
            stateMachine.apply(model.logEntry);
            stateMachine.delString(model.countKey, model.successKey);
        }
    }

    class HeartBeatTask implements Runnable {
        @Override
        public void run() {
            if (status.getCode() != NodeStatus.LEADER.getCode()) {
                return;
            }
            long current = System.currentTimeMillis();
            if (current - preHeartBeatTime < heartBeatTick) {
                return;
            }
            LOGGER.info("============NextIndex============");
            for (Peer peer : peerSetManager.getPeerSetWithOutSelf()) {
                LOGGER.info("Peer {} nextIndex = {}", peer.getAddress(), nextIndices.get(peer));
            }
            LOGGER.info("=================================");
            preHeartBeatTime = System.currentTimeMillis();

            for (Peer peer : peerSetManager.getPeerSetWithOutSelf()) {
                // 空日志，表示心跳
                EntryParam param = EntryParam.newBuilder()
                        .entries(null)
                        .leaderId(peerSetManager.getSelf().getAddress())
                        .serverId(peer.getAddress())
                        .term(currentTerm)
                        .build();
                Request<EntryParam> request = new Request<>(Request.APPEND_ENTRIES, param, peer.getAddress());
                RaftThreadPool.execute(() -> {
                    try {
                        Response response = getRpcClient().send(request);
                        EntryResult entryResult = (EntryResult) response.getResult();
                        long term = entryResult.getTerm();
                        if (term > currentTerm) {
                            LOGGER.error("This node will become follower, " +
                                    "because it receives a larger term {} than itself {}", term, currentTerm);
                            currentTerm = term;
                            votedFor = "";
                            status = NodeStatus.FOLLOWER;
                        }
                    } catch (Exception e) {
                        LOGGER.error("HeartBeatTask RPC fail, request address: {}", request.getUrl());
                    }
                }, false);
            }
        }
    }

    /**
     * 1. 在转变成候选人后就立即开始选举过程
     *      自增当前的任期号（currentTerm）
     *      给自己投票
     *      重置选举超时计时器
     *      发送请求投票的 RPC 给其他所有服务器
     * 2. 如果接收到大多数服务器的选票，那么就变成领导人
     * 3. 如果接收到来自新的领导人的附加日志 RPC，转变成跟随者
     * 4. 如果选举过程超时，再次发起一轮选举
     */
    class ElectionTask implements Runnable {
        @Override
        public void run() {
            if (status.getCode() == NodeStatus.LEADER.getCode()){
                return;
            }
            long current = System.currentTimeMillis();
            // 使用随机时间，解决选举冲突
            electionTime = electionTime + ThreadLocalRandom.current().nextInt(50);
            if (current - preElectionTime < electionTime) {
                return;
            }
            status = NodeStatus.CANDIDATE;
            LOGGER.error("Node {} will become candidate and start election leader," +
                    " current term : [{}], last entry : [{}]", peerSetManager.getSelf(), currentTerm, logModule.getLast());
            preElectionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(200) + 150;
            currentTerm = currentTerm + 1;
            // 推荐自己
            votedFor = peerSetManager.getSelf().getAddress();
            Set<Peer> peers = peerSetManager.getPeerSetWithOutSelf();
            ArrayList<Future> futureArrayList = new ArrayList<>();
            LOGGER.info("peer set size : {}, peer set content : {}", peers.size(), peers);
            // 发送请求
            for (Peer peer : peers) {
                futureArrayList.add(RaftThreadPool.submit(() -> {
                    long lastTerm = 0L;
                    LogEntry lastEntry = logModule.getLast();
                    if (lastEntry != null) {
                        lastTerm = lastEntry.getTerm();
                    }
                    VoteParam param = VoteParam.newBuilder()
                            .term(currentTerm)
                            .candidateId(peerSetManager.getSelf().getAddress())
                            .lastLogIndex(RaftUtil.convert(logModule.getLastIndex()))
                            .lastLogTerm(lastTerm)
                            .build();

                    Request request = Request.newBuilder()
                            .cmd(Request.REQUEST_VOTE)
                            .obj(param)
                            .url(peer.getAddress())
                            .build();
                    try {
                        @SuppressWarnings("unchecked")
                        Response<VoteResult> response = getRpcClient().send(request);
                        return response;
                    } catch (RaftRemotingException e) {
                        LOGGER.error("ElectionTask RPC fail, address : {}", peer.getAddress());
                        return null;
                    }
                }));
            }

            AtomicInteger count = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(futureArrayList.size());
            // 等待异步执行的结果
            for (Future future : futureArrayList) {
                RaftThreadPool.submit(() -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Response<VoteResult> response = (Response<VoteResult>) future.get(3000, TimeUnit.MILLISECONDS);
                        if (response == null) {
                            return -1;
                        }
                        boolean isVotedGranted = response.getResult().isVoteGranted();
                        if (isVotedGranted) {
                            count.incrementAndGet();
                        } else {
                            // 如果没有投票给该节点，检查是不是因为自己的任期编号比较小
                            long resultTerm = response.getResult().getTerm();
                            if (resultTerm > currentTerm) {
                                currentTerm = resultTerm;
                            }
                        }
                        return 0;
                    } catch (Exception e) {
                        LOGGER.error("Future.get() error, message : {}", e.getMessage());
                        return -1;
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await(3500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOGGER.warn("The main thread of ElectionTask have an InterruptedException");
            }

            int success = count.get();
            LOGGER.info("Node {} maybe become leader, votes received [{}], status : {}",
                    peerSetManager.getSelf().getAddress(), success, status);
            // 如果投票期间由于收到了 appendEntry
            // 该节点就有可能变成FOLLOWER，此时应该停止
            if (status.getCode() == NodeStatus.FOLLOWER.getCode()) {
                return;
            }
            // 如果得到的选票足够多就成为LEADER，否则重新选举
            if (success >= peers.size() / 2) {
                LOGGER.warn("Node {} will become leader,", peerSetManager.getSelf());
                status = NodeStatus.LEADER;
                peerSetManager.setLeader(peerSetManager.getSelf());
                votedFor = "";
                newLeaderToDoSomething();
            } else {
                votedFor = "";
            }
        }
    }

    /**
     * 初始化所有的 nextIndex 值为自己的最后一条日志的 index + 1. 如果下次 RPC 时, 跟随者和leader 不一致,就会失败.
     * 那么 leader 尝试递减 nextIndex 并进行重试.最终将达成一致.
     */
    private void newLeaderToDoSomething() {
        nextIndices = new ConcurrentHashMap<>();
        matchIndices = new ConcurrentHashMap<>();
        for (Peer peer : peerSetManager.getPeerSetWithOutSelf()) {
            nextIndices.put(peer, logModule.getLastIndex() + 1);
            matchIndices.put(peer, 0L);
        }
    }
}
