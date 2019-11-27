package com.dizsun.timechain.service;

import com.alibaba.fastjson.JSON;
import com.dizsun.timechain.component.ACK;
import com.dizsun.timechain.component.Block;
import com.dizsun.timechain.component.Message;
import com.dizsun.timechain.constant.Config;
import com.dizsun.timechain.constant.R;
import com.dizsun.timechain.util.RSAUtil;
import org.java_websocket.WebSocket;

import java.util.List;

public class MessageHelper {
    private BlockService blockService;
    private PeerService peerService;
    private RSAUtil rsaUtil;
    private Config config;

    private static class Holder {
        private static final MessageHelper messageHelper = new MessageHelper();
    }

    public static MessageHelper getInstance() {
        return Holder.messageHelper;
    }

    private MessageHelper() {
    }

    public void init() {
        blockService = BlockService.getInstance();
        peerService = PeerService.getInstance();
        rsaUtil = RSAUtil.getInstance();
        config = Config.getInstance();
    }

    public String queryAllMsg() {
        return JSON.toJSONString(new Message(R.QUERY_ALL_BLOCKS, JSON.toJSONString(blockService.getBlockChain()), config.getLocalHost(), R.getIncrementMessageId()));
    }


    public String queryChainLengthMsg() {
        return JSON.toJSONString(new Message(R.QUERY_LATEST_BLOCK, config.getLocalHost(), R.getIncrementMessageId()));
    }

    public String queryAllPeers() {
        return JSON.toJSONString(new Message(R.QUERY_ALL_PEERS, JSON.toJSONString(peerService.getPeerArray()), config.getLocalHost(), R.getIncrementMessageId()));
    }

    public String requestNegotiation() {
        return JSON.toJSONString(new Message(R.REQUEST_NEGOTIATION, config.getLocalHost(), R.getIncrementMessageId()));
    }

    public String responseChainMsg() {
        return JSON.toJSONString(new Message(R.RESPONSE_BLOCK_CHAIN, JSON.toJSONString(blockService.getBlockChain()), config.getLocalHost(), R.getIncrementMessageId()));
    }

    public String responseLatestMsg() {
        Block[] blocks = {blockService.getLatestBlock()};
        return JSON.toJSONString(new Message(R.RESPONSE_BLOCK_CHAIN, JSON.toJSONString(blocks), config.getLocalHost(), R.getIncrementMessageId()));
    }

    public String responseBlock() {
        return JSON.toJSONString(new Message(R.RESPONSE_BLOCK, JSON.toJSONString(blockService.getBlockChain()), config.getLocalHost(), R.getIncrementMessageId()));
    }

    public String responseAllPeers() {
        return JSON.toJSONString(new Message(R.RESPONSE_ALL_PEERS, JSON.toJSONString(peerService.getPeerArray()), config.getLocalHost(), R.getIncrementMessageId()));
    }

    public String responseACK() {
        int vn = R.getViewNumber();
        ACK ack = new ACK();
        ack.setVN(vn);
        ack.setPublicKey(rsaUtil.getPublicKeyBase64());
        ack.setSign(rsaUtil.encrypt(rsaUtil.getPublicKeyBase64() + vn));
        return JSON.toJSONString(new Message(R.RESPONSE_ACK, JSON.toJSONString(ack), config.getLocalHost(), R.getIncrementMessageId()));
    }

    public void handleBlock(WebSocket ws, String message) {
        Block receivedBlock = JSON.parseObject(message, Block.class);
        R.getBlockReadLock().lock();
        Block latestBlock = blockService.getLatestBlock();
        if (receivedBlock.getIndex() <= latestBlock.getIndex()) return;
        R.getBlockReadLock().unlock();

        if (latestBlock.getIndex() + 1 == receivedBlock.getIndex()) {
            R.getBlockWriteLock().lock();
            blockService.addBlock(receivedBlock);
            R.getBlockWriteLock().unlock();
        } else {
            peerService.write(ws, queryAllMsg());
        }
    }

    public void handleBlockChain(WebSocket ws, String message) {
        List<Block> receivedBlocks = JSON.parseArray(message, Block.class);
        R.getBlockReadLock().lock();
        Block latestBlockReceived = receivedBlocks.get(receivedBlocks.size() - 1);
        Block latestBlock = blockService.getLatestBlock();
        if (latestBlockReceived.getIndex() <= latestBlock.getIndex()) return;
        R.getBlockReadLock().unlock();

        R.getBlockWriteLock().lock();
        blockService.replaceChain(receivedBlocks);
        R.getBlockWriteLock().unlock();
    }

    /**
     * 处理接收到的节点
     *
     * @param message
     */
    public void handlePeersResponse(String message) {
        List<String> peers = JSON.parseArray(message, String.class);
        for (String peer : peers) {
            peerService.connectToPeer(peer);
        }
    }
}
