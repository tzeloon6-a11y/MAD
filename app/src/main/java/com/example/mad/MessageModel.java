package com.example.mad;

public class MessageModel {

    private String messageId;
    private String senderId;
    private String text;
    private String timestamp;

    public MessageModel(String messageId, String senderId, String text, String timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getText() {
        return text;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

