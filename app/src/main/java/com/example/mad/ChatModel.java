package com.example.mad;

public class ChatModel {

    private String chatId;
    private String jobId;
    private String studentId;
    private String recruiterId;
    private String jobTitle;
    private String lastMessage;
    private String timestamp;
    private String studentName;
    private String studentBio;
    private String recruiterName;
    private int unreadCount = 0;

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
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public String getStudentBio() {
        return studentBio;
    }
    
    public void setStudentBio(String studentBio) {
        this.studentBio = studentBio;
    }
    
    public String getRecruiterName() {
        return recruiterName;
    }
    
    public void setRecruiterName(String recruiterName) {
        this.recruiterName = recruiterName;
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}

