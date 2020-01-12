package com.dizsun.timechain.component;

import java.io.Serializable;

/**
 * 区块组件类
 */
public class Block implements Serializable {
    private int index;
    private String previousHash;
    private long timestamp;
    private String data;
    private String hash;
    private int VN;

    public Block() {
    }

//    public Block(int index, String previousHash, long timestamp, String data, String hash) {
//        this.index = index;
//        this.previousHash = previousHash;
//        this.timestamp = timestamp;
//        this.data = data;
//        this.hash = hash;
//    }

    public Block(int index, String previousHash, long timestamp, String data, String hash, int VN) {
        this.index = index;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.data = data;
        this.hash = hash;
        this.VN=VN;
    }
    public Block(int index, String previousHash, long timestamp, String data, String hash) {
        this.index = index;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.data = data;
        this.hash = hash;
    }

    @Override
    public boolean equals(Object obj) {
        Block block2=(Block)obj;
        if(this.hash.equals(block2.hash))
            return true;
        else return false;
    }

    @Override
    public String toString() {
        return "Block{" +
                "index=" + index +
                ", previousHash='" + previousHash + '\'' +
                ", timestamp=" + timestamp +
                ", data='" + data + '\'' +
                ", hash='" + hash + '\'' +
                ", VN=" + VN +
                '}';
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }


    public int getVN() {
        return VN;
    }

    public void setVN(int VN) {
        this.VN = VN;
    }
}
