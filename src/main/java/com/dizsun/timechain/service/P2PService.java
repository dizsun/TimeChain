package com.dizsun.timechain.service;


import com.alibaba.fastjson.JSON;
import com.dizsun.timechain.component.ACK;
import com.dizsun.timechain.component.Message;
import com.dizsun.timechain.util.LogUtil;
import com.dizsun.timechain.util.RSAUtil;
import com.dizsun.timechain.interfaces.ISubscriber;
import com.dizsun.timechain.constant.R;
import com.dizsun.timechain.constant.ViewState;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class P2PService implements ISubscriber {
    private BlockService blockService;
    private ExecutorService pool;   //线程池
    private RSAUtil rsaUtil;
    private PeerService peerService;
    private MessageHelper messageHelper;

    private long startTime = 0;
    private long endTime = 0;
    //view number
//    private int VN;
    //节点数3N+0,1,2
    private int N = 1;
    private int stabilityValue = 128;
    private ViewState viewState = ViewState.Running;
    private List<ACK> acks;

    private static class Holder {
        private static P2PService p2pService = new P2PService();
    }

    public static P2PService getInstance() {
        return Holder.p2pService;
    }

    private P2PService() {
    }

    public void initP2PServer(int port) {
        this.blockService = BlockService.getInstance();
        blockService.init();
        this.pool = Executors.newCachedThreadPool();
        this.acks = new ArrayList<>();
//        this.VN = 0;
        this.rsaUtil = RSAUtil.getInstance();
        rsaUtil.init();
        this.peerService = PeerService.getInstance();
        peerService.init();
        this.messageHelper = MessageHelper.getInstance();
        messageHelper.init();
        final WebSocketServer socket = new WebSocketServer(new InetSocketAddress(port)) {
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                peerService.write(webSocket, messageHelper.queryChainLengthMsg());
                String host = webSocket.getRemoteSocketAddress().getHostString();
                if (peerService.contains(host)) {
                    peerService.removePeer(host);
                }
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
                case R.QUERY_LATEST_BLOCK:
//                    System.out.println("对方请求最新block...");
                    peerService.write(webSocket, messageHelper.responseLatestMsg());
                    break;
                case R.QUERY_ALL_BLOCKS:
//                    System.out.println("对方请求所有block...");
                    peerService.write(webSocket, messageHelper.responseChainMsg());
//                    synchronized (blockLock) {
//                        handleBlockChainResponse(webSocket, message.getData());
//                    }
                    break;
                case R.QUERY_ALL_PEERS:
//                    System.out.println("对方请求所有peer...");
                    peerService.write(webSocket, messageHelper.responseAllPeers());
                    messageHelper.handlePeersResponse(message.getData());
                    break;
                case R.RESPONSE_BLOCK_CHAIN:
//                    System.out.println("收到blocks...");
                    messageHelper.handleBlockChain(webSocket, message.getData());
                    break;
                case R.RESPONSE_ALL_PEERS:
//                    System.out.println("收到所有peer...");
                    messageHelper.handlePeersResponse(message.getData());
                    break;
                case R.REQUEST_NEGOTIATION:
                    System.out.println("[P2PService][handleMessage]receive negotiation request...");
                    N = (peerService.length() + 1) / 3;
//                    System.out.println("N的大小:" + N);
                    if (viewState == ViewState.WaitingNegotiation) {
                        startTime = System.nanoTime();
//                        System.out.println("广播ACK");
                        peerService.broadcast(messageHelper.responseACK());
                        viewState = ViewState.WaitingACK;
                    }
                    break;
                case R.RESPONSE_ACK:
//                    System.out.println("收到ACK...");
                    ACK tempACK = new ACK(message.getData());
//                    System.out.println("ACK正确性:" + checkACK(tempACK));
                    if (viewState == ViewState.WaitingACK && checkACK(tempACK)) {
                        if (stabilityValue == 1) {
                            peerService.updateSI(webSocket, 1);
                        } else {
                            peerService.updateSI(webSocket, stabilityValue / 2);
                            stabilityValue /= 2;
                        }
                        acks.add(tempACK);
//                      System.out.println("接收到的ACK数:" + acks.size() + ",是否满足写虚区块条件:" + (acks.size() >= 2 * N));
                        if (acks.size() >= 2 * N) {
                            R.getBlockWriteLock().lock();
                            writeBlock();
                            peerService.broadcast(messageHelper.responseBlock());
                            viewState = ViewState.Running;
                            R.getBlockWriteLock().unlock();
                            endTime = System.nanoTime() - startTime;
                            System.out.println("[P2PService][handleMessage]Consensus duration:" + endTime / 1000000000.0 + "s");
                            LogUtil.writeLog("" + endTime, LogUtil.CONSENSUS);
                        }
                    }
                    break;
                case R.RESPONSE_BLOCK:
                    switch (viewState) {
                        case Running:
                            break;
                        case WritingBlock:
                        case WaitingACK:
                        case Negotiation:
                        case WaitingBlock:
                        case WaitingNegotiation:
                            // TODO 实现路由转发
                            R.getBlockWriteLock().lock();
                            stopWriteBlock();
                            messageHelper.handleBlock(webSocket, message.getData());
                            viewState = ViewState.Running;
                            R.getBlockWriteLock().unlock();
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
        if (viewState == ViewState.WritingBlock) {
            viewState = ViewState.WaitingBlock;
        }
        if (blockService.getLatestBlock().getVN() == R.getViewNumber() + 1) {
            blockService.rollback();
        }
    }

    /**
     * 生成新区块
     */
    private void writeBlock() {
        viewState = ViewState.WritingBlock;
        blockService.addBlock(blockService.generateNextBlock(blockService.getJSONData(acks), R.getIncrementViewNumber()));
        System.out.println("[P2PService]区块");
    }

    public void handleMsgThread(WebSocket webSocket, String msg) {
        Thread thread = new HandleMsgThread(webSocket, msg);
        pool.execute(thread);
    }

    private boolean checkACK(ACK ack) {
//        System.out.println("ack检测一:" + ack.getVN() + "," + this.VN);
        if (ack.getVN() != R.getViewNumber()) {
            return false;
        }
        String sign = rsaUtil.decrypt(ack.getPublicKey(), ack.getSign());
        if (!sign.equals(ack.getPublicKey() + ack.getVN()))
            return false;
        return true;
    }

    @Override
    public void doPerHour00() {
//        System.out.println("进入00,此时VN=" + VN);
        switch (this.viewState) {
            case WaitingNegotiation:
                startTime = System.nanoTime();
                N = (peerService.length() + 1) / 3;
                this.viewState = ViewState.WaitingACK;
                peerService.broadcast(messageHelper.requestNegotiation());
                break;
            default:
                break;
        }
    }

    @Override
    public void doPerHour45() {
        System.out.println("[P2PService]进入45,此时VN=" + R.getViewNumber());
        peerService.broadcast(messageHelper.queryAllPeers());
    }

    @Override
    public void doPerHour59() {
        N = (peerService.length() + 1) / 3;
        System.out.println("[P2PService]进入59,此时VN=" + R.getViewNumber() + ",N=" + N);
        this.viewState = ViewState.WaitingNegotiation;
    }

    @Override
    public void doPerHour01() {
        System.out.println("[P2PService]进入01,此时VN=" + R.getViewNumber());
        // 此处加锁是因为handleMessage中的RESPONSE_ACK中需要判定acks，因此和这里会相互影响
        R.getBlockWriteLock().lock();
        this.viewState = ViewState.Running;
        acks.clear();
        R.getBlockWriteLock().unlock();
        R.getIncrementViewNumber();
        startTime = 0;
        endTime = 0;
        stabilityValue = 128;
        peerService.updateDelay();
        peerService.regularizeSI();
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
