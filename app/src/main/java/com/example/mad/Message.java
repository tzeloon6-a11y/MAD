package com.example.mad;

public class Message {
    private String messageId;
    private String senderId;
    private String content;
    private String timestamp;

    // Constructor
    public Message(String messageId, String senderId, String content, String timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters
    public String getMessageId() { return messageId; }
    public String getSenderId() { return senderId; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
}
