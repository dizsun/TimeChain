package com.dizsun.timechain.service;

import com.alibaba.fastjson.JSON;
import com.dizsun.timechain.component.ACK;
import com.dizsun.timechain.component.Block;
import com.dizsun.timechain.util.CryptoUtil;

import java.util.ArrayList;
import java.util.List;

//import com.dizsun.util.SQLUtil;


public class BlockService {
    private List<Block> blockChain;
//    private SQLUtil sqlUtil;
    private static BlockService blockService=null;
    private BlockService() {
//        this.sqlUtil=new SQLUtil();
        this.blockChain = new ArrayList<Block>();
        blockChain.add(this.getFirstBlock());

//        List<Block> dbBlocks = sqlUtil.queryBlocks();
//        List<Block> dbBlocks = new ArrayList<>();
//        if(dbBlocks==null){
//            blockChain.add(this.getFirstBlock());
//            sqlUtil.initBlocks(blockChain);
//        }else{
//            blockChain=dbBlocks;
//        }
    }
    public static BlockService newBlockService(){
        if(blockService==null){
            blockService=new BlockService();
        }
        return blockService;
    }

    /**
     * 计算区块hash
     * 将(索引+前一个区块hash+时间戳+数据)进行hash
     * @param index
     * @param previousHash
     * @param timestamp
     * @param data
     * @return
     */
    private String calculateHash(int index, String previousHash, long timestamp, String data) {
        StringBuilder builder = new StringBuilder(index);
        builder.append(previousHash).append(timestamp).append(data);
        return CryptoUtil.getSHA256(builder.toString());
    }

    public Block getLatestBlock() {
        return blockChain.get(blockChain.size() - 1);
    }

    private Block getFirstBlock() {
        return new Block(1, "0", 0, "Hello Block", "1db6aa3c81dc4b05a49eaed6feba99ed4ef07aa418d10bfbbc12af68cab6fb2a",0);
    }

    /**
     * 生成新区块
     * @param blockData
     * @return
     */
    public Block generateNextBlock(String blockData, int VN) {
        Block previousBlock = this.getLatestBlock();
        int nextIndex = previousBlock.getIndex() + 1;
        long nextTimestamp = System.currentTimeMillis();
        String nextHash = calculateHash(nextIndex, previousBlock.getHash(), nextTimestamp, blockData);
        //int proof=createProofOfWork(previousBlock.getProof(),previousBlock.getHash());
        return new Block(nextIndex, previousBlock.getHash(), nextTimestamp, blockData, nextHash,VN);
    }

    public void addBlock(Block newBlock) {
        if (isValidNewBlock(newBlock, getLatestBlock())) {
//            sqlUtil.addBlock(newBlock);
            blockChain.add(newBlock);
        }
    }

    /**
     * 验证新区块是否合法
     * @param newBlock
     * @param previousBlock
     * @return
     */
    private boolean isValidNewBlock(Block newBlock, Block previousBlock) {
        if (previousBlock.getIndex() + 1 != newBlock.getIndex()) {
            System.out.println("无效的 index");
            return false;
        } else if (!previousBlock.getHash().equals(newBlock.getPreviousHash())) {
            System.out.println("无效的 previoushash");
            return false;
        } else {
            String hash = calculateHash(newBlock.getIndex(), newBlock.getPreviousHash(), newBlock.getTimestamp(),
                    newBlock.getData());
            if (!hash.equals(newBlock.getHash())) {
                System.out.println("无效的 hash: " + hash + " " + newBlock.getHash());
                return false;
            }
        }
        return true;
    }

    /**
     * 用新链替换旧链
     * @param newBlocks
     */
    public void replaceChain(List<Block> newBlocks) {
        if (isValidBlocks(newBlocks) && newBlocks.size() > blockChain.size()) {
//            sqlUtil.replaceChain(newBlocks);
            blockChain = newBlocks;
        } else {
            System.out.println("收到的区块链为无效链");
        }
    }

    /**
     * 验证区块链是否合法
     * @param newBlocks
     * @return
     */
    private boolean isValidBlocks(List<Block> newBlocks) {
        Block firstBlock = newBlocks.get(0);
        if (!firstBlock.equals(getFirstBlock())) {
            return false;
        }

        for (int i = 1; i < newBlocks.size(); i++) {
            if (!isValidNewBlock(newBlocks.get(i), newBlocks.get(i-1))){
                return false;
            }
        }
        return true;
    }

    /**
     * 验证工作量是否正确
     * TODO 工作量应该是有动态调节功能保证建立区块的时间稳定的,此处暂时没有写此功能
     * @param lastProof
     * @param proof
     * @param previousHash
     * @return
     */
    private boolean isValidProof(int lastProof,int proof,String previousHash){
        String guess=""+lastProof+proof+previousHash;
        String result = CryptoUtil.getSHA256(guess);
        return result.startsWith("0000");
    }

    /**
     * 创建工作量证明
     * @param lastProof
     * @param previousHash
     * @return
     */
    private int createProofOfWork(int lastProof,String previousHash){
        int proof=0;
        while (!isValidProof(lastProof,proof,previousHash))
            proof++;
        return proof;
    }

    public void rollback(){
        if(this.blockChain.size()>0){
            this.blockChain.remove(this.blockChain.size()-1);
        }
    }

    public String getJSONData(List<ACK> acks){
        return JSON.toJSONString(acks);
    }

    public List<Block> getBlockChain() {
        return blockChain;
    }
}
