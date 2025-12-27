package com.example.mad;

public class ChatModel {

    private String chatId;
    private String jobId;
    private String studentId;
    private String recruiterId;
    private String jobTitle;
    private String lastMessage;
    private String timestamp;

    public ChatModel(String chatId, String jobId, String studentId, String recruiterId,
                     String jobTitle, String lastMessage, String timestamp) {
        this.chatId = chatId;
        this.jobId = jobId;
        this.studentId = studentId;
        this.recruiterId = recruiterId;
        this.jobTitle = jobTitle;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public String getChatId() {
        return chatId;
    }

    public String getJobId() {
        return jobId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getRecruiterId() {
        return recruiterId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

