package com.dizsun.timechain.service;

import com.dizsun.timechain.component.Peer;
import com.dizsun.timechain.util.Config;
import com.dizsun.timechain.util.ICheckDelay;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理peer节点的连接和移除及通信
 */
public class PeerService implements ICheckDelay {
    private ArrayList<Peer> peers;
    //    private HashSet<String> peers;
//    private ArrayList<WebSocket> sockets;
    private static PeerService peerService;
    private String localHost;
    private P2PService p2PService;

    private PeerService(P2PService _p2PService) {
//        peers = new HashSet<>();
//        sockets = new ArrayList<>();
        peers = new ArrayList<>();
        this.p2PService = _p2PService;
    }

    public static PeerService newPeerService(P2PService _p2PService) {
        if (peerService == null) {
            peerService = new PeerService(_p2PService);
        }
        return peerService;
    }

    public boolean addPeer(WebSocket webSocket) {
        String host = webSocket.getRemoteSocketAddress().getHostString();
        localHost = webSocket.getLocalSocketAddress().getHostString();
        if (!contains(host) && !host.equals(localHost)) {
            Peer p = new Peer();
            p.setWebSocket(webSocket);
            p.setIp(host);
            peers.add(p);
            return true;
        }
        return false;
    }

    public void removePeer(WebSocket webSocket) {
        if (webSocket != null) {
            for (int i = 0; i < peers.size(); i++) {
                if (peers.get(i).getWebSocket().equals(webSocket))
                    peers.remove(i);
            }
        }
    }

    public void write(WebSocket webSocket, String msg) {
        if (webSocket != null && webSocket.isOpen())
            webSocket.send(msg);
    }

    public void broadcast(String msg) {
        for (Peer peer : peers) {
            this.write(peer.getWebSocket(), msg);
        }
    }

    public boolean contains(String host) {
        for (int i = 0; i < peers.size(); i++) {
            if (peers.get(i).getIp().equals(host)) return true;
        }
        return false;
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
                    if(!addPeer(this)){
                        this.close();
                    }
                    write(this, p2PService.queryChainLengthMsg());
                    write(this, p2PService.queryAllPeers());
                }

                @Override
                public void onMessage(String s) {
                    //handleMessage(this, s);
                    p2PService.handleMsgThread(this, s);
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

    public int length() {
        return peers.size();
    }

//    public ArrayList<WebSocket> getSockets() {
//        return sockets;
//    }

    public Object[] getPeerArray() {
        ArrayList<String> ips = new ArrayList<>();
        peers.sort(new Comparator<Peer>() {
            @Override
            public int compare(Peer o1, Peer o2) {
                return o2.getStability()-o1.getStability();
            }
        });
        for (Peer peer: peers){
            ips.add(peer.getIp());
        }
        return ips.toArray();
    }

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

    public void updateSI(WebSocket webSocket,int _stability){
        for (int i = 0; i < peers.size(); i++) {
            if(peers.get(i).getWebSocket().equals(webSocket)){
                peers.get(i).addStability(_stability);
                break;
            }
        }
    }

    public void updateDelay(){
        for (Peer peer:peers){
            new DelayHandler(this,peer).start();
        }
    }

    @Override
    public void checkDelay(Peer peer, double delay) {
        for (int i = 0; i < peers.size(); i++) {
            if(peer.getIp().equals(peers.get(i).getIp())){
                peers.get(i).setDelay(delay);
            }
        }
    }

    private class DelayHandler extends Thread{
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
                Socket client = new Socket(InetAddress.getByName(peer.getIp()), Config.NTPPORT);
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                DataInputStream dis = new DataInputStream(client.getInputStream());
                dos.writeBoolean(true);
                t1=System.nanoTime();
                dos.flush();
                if(dis.readBoolean()){
                    t2=System.nanoTime();
                }
                delay=(t2-t1)/2.0;
                context.checkDelay(peer,delay);
                LogUtil.writeLog(peer.getIp()+":"+delay.intValue(),LogUtil.NTP);
                dis.close();
                dos.close();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}

