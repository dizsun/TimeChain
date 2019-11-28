package com.dizsun.timechain.service;

import com.dizsun.timechain.component.Peer;
import com.dizsun.timechain.constant.Config;
import com.dizsun.timechain.interfaces.ICheckDelay;
import com.dizsun.timechain.util.LogUtil;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理peer节点的连接和移除及通信
 */
public class PeerService implements ICheckDelay {
    private String localHost;
    private ConcurrentHashMap<String, Peer> peersMap;
    private ArrayList<Peer> peers;
    private MessageHelper messageHelper;


    private PeerService() {
    }

    private static class Holder {
        private final static PeerService peerService = new PeerService();
    }

    public static PeerService getInstance() {
        return Holder.peerService;
    }

    public void init() {
        peersMap = new ConcurrentHashMap<>();
        messageHelper = MessageHelper.getInstance();
        peers = new ArrayList<>();
    }

    public boolean addPeer(WebSocket webSocket) {
        String host = webSocket.getRemoteSocketAddress().getHostString();
        localHost = webSocket.getLocalSocketAddress().getHostString();
        if (contains(host) || host.equals(localHost)) return false;
        Peer p = new Peer();
        p.setWebSocket(webSocket);
        p.setIp(host);
        peersMap.put(host, p);
        peers.add(p);
        return true;
    }

    public void removePeer(WebSocket webSocket) {
        if (webSocket != null) {
            String hostString = webSocket.getRemoteSocketAddress().getHostString();
            Peer peer = peersMap.get(hostString);
            if (peer != null) {
                peersMap.remove(hostString);
                peers.remove(peer);
            }
        }
    }

    public void removePeer(String host) {
        Peer peer = peersMap.get(host);
        if (peer != null) {
            peersMap.remove(host);
            peers.remove(peer);
        }
    }

    public void write(WebSocket webSocket, String msg) {
        if (webSocket != null && webSocket.isOpen())
            webSocket.send(msg);
    }

    public void write(String host, String msg) {
        Peer peer = peersMap.get(host);
        if (peer == null) return;
        WebSocket webSocket = peer.getWebSocket();
        if (webSocket == null || !webSocket.isOpen()) return;
        webSocket.send(msg);
    }

    public void broadcast(String msg) {
        peers.forEach(v -> {
            write(v.getWebSocket(), msg);
        });
    }

    public boolean contains(String host) {
        return peersMap.contains(host);
    }

    public boolean contains(WebSocket webSocket) {
        return contains(webSocket.getRemoteSocketAddress().getHostString());
    }

    /**
     * 连接peer
     *
     * @param host 输入的host格式示例: 192.168.1.1 或者http://192.168.1.1:6001
     */
    public void connectToPeer(String host) {
        if (isIP(host)) {
            if (contains(host) || host.equals(localHost))
                return;
            host = "http://" + host + ":6001";
        }
        try {
            final WebSocketClient socket = new WebSocketClient(new URI(host)) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    if (!addPeer(this)) {
                        this.close();
                    }
                    write(this, messageHelper.queryChainLengthMsg());
                    write(this, messageHelper.queryAllPeers());
                }

                @Override
                public void onMessage(String s) {
                    P2PService.getInstance().handleMsgThread(this, s);
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    System.out.println("[PeerService][connectToPeer]connection failed");
                    removePeer(this);
                }

                @Override
                public void onError(Exception e) {
                    System.out.println("[PeerService][connectToPeer]connection failed");
                    removePeer(this);
                }
            };
            socket.connect();
        } catch (URISyntaxException e) {
            System.out.println("[PeerService][connectToPeer]p2p connect is error:" + e.getMessage());
        }

    }

    /**
     * 目前连接的节点数
     *
     * @return
     */
    public int length() {
        return peersMap.size();
    }

    /**
     * 获取节点IP列表
     *
     * @return
     */
    public Object[] getPeerArray() {
        return peers.toArray();
    }

    /**
     * 判断是否是ip值
     *
     * @param addr
     * @return
     */
    public boolean isIP(String addr) {
        if (addr.length() < 7 || addr.length() > 15 || "".equals(addr)) {
            return false;
        }
        /**
         * 判断IP格式和范围
         */
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

        Pattern pat = Pattern.compile(rexp);

        Matcher mat = pat.matcher(addr);

        boolean ipAddress = mat.find();

        return ipAddress;
    }

    /**
     * 更新稳定指数
     *
     * @param webSocket 目标节点
     * @param stability 要增加的指数值
     */
    public void updateSI(WebSocket webSocket, int stability) {
        String hostString = webSocket.getRemoteSocketAddress().getHostString();
        Peer peer = peersMap.get(hostString);
        if (peer == null) return;
        peer.addStability(stability);
    }

    /**
     * 对SI表进行规整化，即所有SI值减去最小值并排序
     */
    public void regularizeSI() {
        peers.sort(new Comparator<Peer>() {
            @Override
            public int compare(Peer o1, Peer o2) {
                return o2.getStability() - o1.getStability();
            }
        });
        if (peers.get(0).getStability() >= (Integer.MAX_VALUE >>> 2)) {
            for (Peer peer : peers) {
                peer.setStability(peer.getStability() / 2);
            }
        }
    }

    /**
     * 更新与各节点之间的延迟
     */
    public void updateDelay() {
        peersMap.forEach((k, v) -> {
            new DelayHandler(this, v).start();
        });
    }


    @Override
    public void checkDelay(Peer peer, double delay) {
        Peer p1 = peersMap.get(peer.getIp());
        if (p1 == null) return;
        p1.setDelay(delay);
    }

    /**
     * 延时测试机，向对方发送一个消息并等待回应，计算延迟
     */
    private class DelayHandler extends Thread {
        private ICheckDelay context;
        private Peer peer;
        private long t1;
        private long t2;
        private Double delay;

        public DelayHandler(ICheckDelay context, Peer peer) {
            this.context = context;
            this.peer = peer;
        }

        @Override
        public void run() {
            try {
                Socket client = new Socket(InetAddress.getByName(peer.getIp()), Config.getInstance().getNtpListenPort());
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                DataInputStream dis = new DataInputStream(client.getInputStream());
                dos.writeBoolean(true);
                t1 = System.nanoTime();
                dos.flush();
                if (dis.readBoolean()) {
                    t2 = System.nanoTime();
                }
                delay = (t2 - t1) / 2.0;
                context.checkDelay(peer, delay);
                LogUtil.writeLog(peer.getIp() + ":" + delay.intValue(), LogUtil.NTP);
                dis.close();
                dos.close();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}

