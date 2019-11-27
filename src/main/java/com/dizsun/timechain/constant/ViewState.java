package com.dizsun.timechain.constant;

public enum ViewState {
    Negotiation,    //协商状态,此时各个节点协商是否开始竞选
    WaitingNegotiation,  //等待协商状态,此时等待其他节点发送协商请求
    WaitingACK,     //等待其他节点协商同意
    //        WaitingVACK,     //等待其他节点协商同意
    Running,    //系统正常运行状态
    WritingBlock,   //写区块
    WaitingBlock,   //等待区块
//        WritingVBlock   //写虚区块
}
