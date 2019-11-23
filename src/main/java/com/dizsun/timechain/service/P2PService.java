package com.dizsun.timechain.service;


import com.alibaba.fastjson.JSON;
import com.dizsun.timechain.component.ACK;
import com.dizsun.timechain.component.Block;
import com.dizsun.timechain.component.Message;
import com.dizsun.timechain.util.LogUtil;
import com.dizsun.timechain.util.RSAUtil;
import com.dizsun.timechain.util.ISubscriber;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class P2PService implements ISubscriber {
    private long startTime = 0;
    private long endTime = 0;
    //    private List<WebSocket> sockets;    //节点的套接字集合
//    private HashSet<String> peers;  //节点的URI集合
    private BlockService blockService;
    private ExecutorService pool;   //线程池
    private final Object ackLock = new Object();  //接收ack的线程锁
//    private final Object vackLock = new Object();  //接收vack的线程锁
    private final Object mBlockLock = new Object();  //写vblock的线程锁
    private final Object blockLock = new Object();  //写block的线程锁
    private final Object peerLock = new Object();  //写peer的线程锁
    private RSAUtil rsaUtill;
    private PeerService peerService;

    private final static int QUERY_LATEST_BLOCK = 0;
    private final static int QUERY_ALL_BLOCKS = 1;
    private final static int RESPONSE_BLOCKCHAIN = 2;
    private final static int QUERY_ALL_PEERS = 3;
    private final static int RESPONSE_ALL_PEERS = 4;
    private final static int REQUEST_NEGOTIATION = 5;
    private final static int RESPONSE_ACK = 6;
    private final static int RESPONSE_BLOCK = 8;


    private enum ViewState {
        Negotiation,    //协商状态,此时各个节点协商是否开始竞选
        WaitingNegotiation,  //等待协商状态,此时等待其他节点发送协商请求
        WaitingACK,     //等待其他节点协商同意
//        WaitingVACK,     //等待其他节点协商同意
        Running,    //系统正常运行状态
        WritingBlock,   //写区块
        WaitingBlock,   //等待区块
//        WritingVBlock   //写虚区块
    }

    public static final int LT = 1000 * 60 * 60;
    //view number
    private int VN;
    //节点数3N+0,1,2
    private int N = 1;
    private int stabilityValue = 128;
    private ViewState viewState = ViewState.Running;
    private List<ACK> acks;


    public P2PService() {
        this.blockService = BlockService.newBlockService();
//        this.sockets = new ArrayList<>();
//        this.peers = new HashSet<>();
        this.pool = Executors.newCachedThreadPool();
        this.acks = new ArrayList<>();
        this.VN = 0;
        this.rsaUtill = RSAUtil.getInstance();
        this.peerService = PeerService.newPeerService(this);
    }

    public void initP2PServer(int port) {
        final WebSocketServer socket = new WebSocketServer(new InetSocketAddress(port)) {
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                peerService.write(webSocket, queryChainLengthMsg());
                peerService.addPeer(webSocket);
            }

            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
                System.out.println("[P2PService][initP2PServcer]connection failed to peer:" + webSocket.getRemoteSocketAddress());
                peerService.removePeer(webSocket);
            }

            public void onMessage(WebSocket webSocket, String s) {
                Thread thread = new HandleMsgThread(webSocket, s);
                pool.execute(thread);
            }

            public void onError(WebSocket webSocket, Exception e) {
                System.out.println("[P2PService][initP2PServcer]connection failed to peer:" + webSocket.getRemoteSocketAddress());
                peerService.removePeer(webSocket);
            }

            public void onStart() {

            }
        };
        socket.start();
        System.out.println("[P2PService][initP2PServcer]listening websocket p2p port on: " + port);
    }

    /**
     * 相应peer的信息请求
     *
     * @param webSocket
     * @param s
     */
    private void handleMessage(WebSocket webSocket, String s) {
        try {
            Message message = JSON.parseObject(s, Message.class);
//            System.out.println("Received message" + JSON.toJSONString(message));
            switch (message.getType()) {
                case QUERY_LATEST_BLOCK:
//                    System.out.println("对方请求最新block...");
                    peerService.write(webSocket, responseLatestMsg());
                    break;
                case QUERY_ALL_BLOCKS:
//                    System.out.println("对方请求所以block...");
                    peerService.write(webSocket, responseChainMsg());
                    synchronized (blockLock) {
                        handleBlockChainResponse(webSocket, message.getData());
                    }
                    break;
                case QUERY_ALL_PEERS:
//                    System.out.println("对方请求所有peer...");
                    peerService.write(webSocket, responseAllPeers());
                    synchronized (peerLock) {
                        handlePeersResponse(message.getData());
                    }
                    break;
                case RESPONSE_BLOCKCHAIN:
//                    System.out.println("收到blocks...");
                    synchronized (blockLock) {
                        handleBlockChainResponse(webSocket, message.getData());
                    }
                    break;
                case RESPONSE_ALL_PEERS:
//                    System.out.println("收到所有peer...");
                    synchronized (peerLock) {
                        handlePeersResponse(message.getData());
                    }
                    break;
                case REQUEST_NEGOTIATION:
                    System.out.println("[P2PService][handleMessage]receive negotiation request...");
                    N = (peerService.length() + 1) / 3;
//                    System.out.println("N的大小:" + N);
                    if (viewState == ViewState.WaitingNegotiation) {
                        startTime = System.nanoTime();
//                        System.out.println("广播ACK");
                        peerService.broadcast(responseACK());
                        viewState = ViewState.WaitingACK;
                    }
                    break;
                case RESPONSE_ACK:
//                    System.out.println("收到ACK...");
                    ACK tempACK = new ACK(message.getData());
//                    System.out.println("ACK正确性:" + checkACK(tempACK));
                    synchronized (ackLock) {
                        if (viewState == ViewState.WaitingACK && checkACK(tempACK)) {
                            if (stabilityValue == 1) {
                                peerService.updateSI(webSocket, 1);
                            } else {
                                peerService.updateSI(webSocket, stabilityValue / 2);
                                stabilityValue /= 2;
                            }
                            acks.add(tempACK);
//                            System.out.println("接收到的ACK数:" + acks.size() + ",是否满足写虚区块条件:" + (acks.size() >= 2 * N));
                            if (acks.size() >= 2 * N) {
                                viewState = ViewState.WritingBlock;
                                writeBlock();
                                peerService.broadcast(responseBlock());
                                viewState = ViewState.Running;
                                endTime = System.nanoTime() - startTime;
                                System.out.println("[P2PService][handleMessage]Consensus duration:" + endTime / 1000000000.0 + "s");
                                LogUtil.writeLog("" + endTime, LogUtil.CONSENSUS);
                            }
                        }
                    }
                    break;
                case RESPONSE_BLOCK:
                    switch (viewState) {
                        case Running:
                            break;
                        case WritingBlock:
                        case WaitingACK:
                        case Negotiation:
                        case WaitingBlock:
                        case WaitingNegotiation:
                            stopWriteBlock();
                            synchronized (mBlockLock) {
                                handleBlockChainResponse(webSocket, message.getData());
                            }
                            viewState = ViewState.Running;
                            endTime = System.nanoTime() - startTime;
                            System.out.println("[P2PService][handleMessage]Consensus duration:" + endTime / 1000000000.0 + "s");
                            LogUtil.writeLog("" + endTime, LogUtil.CONSENSUS);
                            break;

                    }
                    break;
            }
        } catch (Exception e) {
            System.out.println("[P2PService][handleMessage]hanle message is error:" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 停止写虚区块,若是已经计算完毕则回滚
     */
    private void stopWriteBlock() {
        synchronized (mBlockLock) {
            if (viewState == ViewState.WritingBlock) {
                viewState = ViewState.WaitingBlock;
            }
            if (blockService.getLatestBlock().getVN() == VN + 1) {
                blockService.rollback();
            }
        }
    }

    private void writeBlock() {
        synchronized (mBlockLock){
            if(viewState== ViewState.WritingBlock){
                blockService.addBlock(blockService.generateNextBlock(blockService.getJSONData(acks), VN+1));
            }
        }
        System.out.println("[P2PService]写区块");
    }


    /**
     * 处理接收到的区块链
     *
     * @param message
     */
    private void handleBlockChainResponse(WebSocket ws, String message) {
        List<Block> receiveBlocks = JSON.parseArray(message, Block.class);
        Collections.sort(receiveBlocks, new Comparator<Block>() {
            public int compare(Block o1, Block o2) {
                return o1.getIndex() - o1.getIndex();
            }
        });

        Block latestBlockReceived = receiveBlocks.get(receiveBlocks.size() - 1);
        Block latestBlock = blockService.getLatestBlock();
        if (latestBlockReceived.getIndex() > latestBlock.getIndex()) {
            if (latestBlock.getHash().equals(latestBlockReceived.getPreviousHash())) {
                System.out.println("[P2PService][handleBlockChainResponse]We can append the received block to our chain");
                blockService.addBlock(latestBlockReceived);
                peerService.write(ws, responseLatestMsg());
            } else if (receiveBlocks.size() == 1) {
                System.out.println("[P2PService][handleBlockChainResponse]We have to query the chain from our peer");
                peerService.write(ws, queryAllMsg());
            } else {
                blockService.replaceChain(receiveBlocks);
//                this.VN = receiveBlocks.get(receiveBlocks.size() - 1).getVN();
            }
        } else {
            System.out.println("[P2PService][handleBlockChainResponse]Received blockchain is not longer than received blockchain. Do nothing");
        }
    }

    /**
     * 处理接收到的节点
     *
     * @param message
     */
    private void handlePeersResponse(String message) {
        List<String> _peers = JSON.parseArray(message, String.class);
        for (String _peer : _peers) {
            peerService.connectToPeer(_peer);
        }
    }

    public void handleMsgThread(WebSocket webSocket, String msg) {
        Thread thread = new HandleMsgThread(webSocket, msg);
        pool.execute(thread);
    }

    private boolean checkACK(ACK ack) {
//        System.out.println("ack检测一:" + ack.getVN() + "," + this.VN);
        if (ack.getVN() != this.VN) {
            return false;
        }
        String sign = rsaUtill.decrypt(ack.getPublicKey(), ack.getSign());
        if (!sign.equals(ack.getPublicKey() + ack.getVN()))
            return false;
        return true;
    }


    public String queryAllMsg() {
        return JSON.toJSONString(new Message(QUERY_ALL_BLOCKS, JSON.toJSONString(blockService.getBlockChain())));
    }


    public String queryChainLengthMsg() {
        return JSON.toJSONString(new Message(QUERY_LATEST_BLOCK));
    }

    public String queryAllPeers() {
        return JSON.toJSONString(new Message(QUERY_ALL_PEERS, JSON.toJSONString(peerService.getPeerArray())));
    }

    public String requestNagotiation() {
        return JSON.toJSONString(new Message(REQUEST_NEGOTIATION));
    }

    public String responseChainMsg() {
        return JSON.toJSONString(new Message(RESPONSE_BLOCKCHAIN, JSON.toJSONString(blockService.getBlockChain())));
    }

    public String responseLatestMsg() {
        Block[] blocks = {blockService.getLatestBlock()};
        return JSON.toJSONString(new Message(RESPONSE_BLOCKCHAIN, JSON.toJSONString(blocks)));
    }

    public String responseBlock() {
        return JSON.toJSONString(new Message(RESPONSE_BLOCK, JSON.toJSONString(blockService.getBlockChain())));
    }

    public String responseAllPeers() {
        return JSON.toJSONString(new Message(RESPONSE_ALL_PEERS, JSON.toJSONString(peerService.getPeerArray())));
    }

    public String responseACK() {
        ACK ack = new ACK();
        ack.setVN(this.VN);
        ack.setPublicKey(rsaUtill.getPublicKeyBase64());
        ack.setSign(rsaUtill.encrypt(rsaUtill.getPublicKeyBase64() + this.VN));
        return JSON.toJSONString(new Message(RESPONSE_ACK, JSON.toJSONString(ack)));
    }


    @Override
    public void doPerHour00() {
//        System.out.println("进入00,此时VN=" + VN);
        switch (this.viewState) {
            case WaitingNegotiation:
                startTime = System.nanoTime();
                N = (peerService.length() + 1) / 3;
                this.viewState = ViewState.WaitingACK;
                peerService.broadcast(requestNagotiation());
                break;
            default:
                break;
        }
    }

    @Override
    public void doPerHour45() {
        System.out.println("[P2PService]进入45,此时VN=" + VN);
        peerService.broadcast(queryAllPeers());
    }

    @Override
    public void doPerHour59() {
        N = (peerService.length() + 1) / 3;
        System.out.println("[P2PService]进入59,此时VN=" + VN + ",N=" + N);
        this.viewState = ViewState.WaitingNegotiation;
    }

    @Override
    public void doPerHour01() {
        System.out.println("[P2PService]进入01,此时VN=" + VN);
        this.viewState = ViewState.Running;
        VN = (VN + 1) % 65535;
        acks.clear();
        startTime = 0;
        endTime = 0;
        stabilityValue = 128;
        peerService.updateDelay();
    }

    class HandleMsgThread extends Thread {
        private WebSocket ws;
        private String s;

        public HandleMsgThread(WebSocket ws, String s) {
            this.ws = ws;
            this.s = s;
        }

        @Override
        public void run() {
            handleMessage(ws, s);
        }

    }

    public void connect(String ip) {
        this.peerService.connectToPeer(ip);
    }
}
