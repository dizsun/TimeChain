package com.dizsun.timechain.component;

import java.io.Serializable;

/**
 * 用于在不同peer间传递消息的类(bean)
 */
public class Message implements Serializable {
    private Integer type;
    private String data;
    private String sourceIp;
    private Long messageId;

    public Message() {
    }

    public Message(Integer type, String sourceIp, Long messageId) {
        this.type = type;
        this.sourceIp = sourceIp;
        this.messageId = messageId;
    }

    public Message(Integer type, String data, String sourceIp, Long messageId) {
        this.type = type;
        this.data = data;
        this.sourceIp = sourceIp;
        this.messageId = messageId;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getType() {
        return type;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }


    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
