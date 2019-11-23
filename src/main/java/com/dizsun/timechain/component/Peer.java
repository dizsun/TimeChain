package com.dizsun.timechain.component;


import org.java_websocket.WebSocket;

public class Peer {
    private String ip;
    private WebSocket webSocket;
    private int stability=0;
    private double delay=0;

    public Peer() {
    }

    public Peer(WebSocket webSocket) {
        this.webSocket = webSocket;
    }


    public int getStability() {
        return stability;
    }

    public void setStability(int stability) {
        this.stability = stability;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public double getDelay() {
        return delay;
    }

    public void setDelay(double delay) {
        this.delay = delay;
    }

    public void addStability(int _stability){
        this.stability+=_stability;
    }
}
