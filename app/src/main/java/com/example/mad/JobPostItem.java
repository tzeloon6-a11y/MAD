package com.example.mad;

public class JobPostItem {

    private int id;
    private String userId;   // ✅ UUID STRING FIXED
    private String title;
    private String description;
    private String mediaUrl;
    private String recruiterName;
    private String recruiterEmail;
    private String createdAt;

    public JobPostItem(int id, String userId, String title, String description,
                       String mediaUrl, String recruiterName,
                       String recruiterEmail, String createdAt) {

        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.mediaUrl = mediaUrl;
        this.recruiterName = recruiterName;
        this.recruiterEmail = recruiterEmail;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getUserId() {      // ✅ RETURN STRING
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public String getRecruiterName() {
        return recruiterName;
    }

    public String getRecruiterEmail() {
        return recruiterEmail;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
