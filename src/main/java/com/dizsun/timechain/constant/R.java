package com.dizsun.timechain.constant;

import com.dizsun.timechain.util.LogUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class R {
    /**
     * 各节点之间传输的消息类型
     */
    public static final int QUERY_LATEST_BLOCK = 0;
    public static final int QUERY_ALL_BLOCKS = 1;
    public static final int RESPONSE_ALL_BLOCKS = 2;
    public static final int QUERY_ALL_PEERS = 3;
    public static final int RESPONSE_ALL_PEERS = 4;
    public static final int REQUEST_NEGOTIATION = 5;
    public static final int RESPONSE_ACK = 6;
    public static final int RESPONSE_BLOCK = 8;

    /**
     * config文件中的默认值，如果无法从properties文件读取相应配置，则取此默认值
     */
    // 默认时间中心的ip
    public static final String DEFAULT_TIME_CENTER_IP = "127.0.0.1";
    // 默认时间中心的端口
    public static final int DEFAULT_TIME_CENTER_LISTEN_PORT = 65001;
    // 默认每个节点的ntp服务的端口
    public static final int DEFAULT_NTP_LISTEN_PORT = 9500;
    // 默认本地ip地址
    public static final String DEFAULT_LOCAL_HOST = "127.0.0.1";
    // 默认p2p监听端口
    public static final int DEFAULT_P2P_PORT = 6001;
    // 默认http监听端口
    public static final int DEFAULT_HTTP_PORT = 9000;
    // 默认节点索引
    public static final int INDEX = 100;
    // 默认主节点
    public static final String DEFAULT_MAIN_NODE = "127.0.0.1";

    public static final String LOG_FILE_PATH = "/info/";


    private static AtomicLong messageId = new AtomicLong(0);
    private static AtomicInteger viewNumber = new AtomicInteger(1);
    private static ReentrantReadWriteLock blockChainLock = new ReentrantReadWriteLock();    // 读写锁
    private static long startTime = 0;


    public static Long getMessageId() {
        return messageId.get();
    }

    public static Long getAndIncrementMessageId() {
        return messageId.incrementAndGet() % Long.MAX_VALUE;
    }

    public static Integer getViewNumber() {
        return viewNumber.get();
    }

    public static Integer getAndIncrementViewNumber() {
        return viewNumber.getAndIncrement() % Integer.MAX_VALUE;
    }

    public static void setViewNumber(Integer vn) {
        viewNumber.set(vn);
    }

    public static Lock getBlockReadLock() {
        return blockChainLock.readLock();
    }

    public static Lock getBlockWriteLock() {
        return blockChainLock.writeLock();
    }

    public static void beginConsensus() {
        startTime = System.nanoTime();
    }

    public static void endConsensus() {
        if (startTime == 0) return;
        long duration = System.nanoTime() - startTime;
        LogUtil.writeLog("" + duration, LogUtil.CONSENSUS);
        startTime = 0;
    }
}
