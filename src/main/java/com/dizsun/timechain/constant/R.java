package com.dizsun.timechain.constant;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class R {
    public static final int QUERY_LATEST_BLOCK = 0;
    public static final int QUERY_ALL_BLOCKS = 1;
    public static final int RESPONSE_BLOCK_CHAIN = 2;
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
    public static final int INDEX = 0;
    // 默认主节点
    public static final String DEFAULT_MAIN_NODE = "127.0.0.1";

    public static final String LOG_FILE_PATH = "/info/";


    private static AtomicLong messageId = new AtomicLong(0);
    private static AtomicInteger viewNumber = new AtomicInteger(0);
    private static ReentrantReadWriteLock blockChainLock = new ReentrantReadWriteLock();    // 读写锁


    public static Long getMessageId() {
        return messageId.get();
    }

    public static Long getIncrementMessageId() {
        return messageId.incrementAndGet() % Long.MAX_VALUE;
    }

    public static Integer getViewNumber() {
        return viewNumber.get();
    }

    public static Integer getIncrementViewNumber() {
        return viewNumber.incrementAndGet() % Integer.MAX_VALUE;
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
}
