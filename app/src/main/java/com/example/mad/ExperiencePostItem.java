package com.example.mad;

public class ExperiencePostItem {

    public String id;         // ✅ CHANGED TO STRING
    public String userId;
    public String title;
    public String description;
    public String mediaUrl;
    public String mediaType;
    public String createdAt;

    public ExperiencePostItem(String id, String userId, String title,
                              String description, String mediaUrl,
                              String mediaType, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.createdAt = createdAt;
    }
    
    public String getTitle() {
        return title;
    }

    // Other Getters required by your Adapter
    public String getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }
    public String getMediaUrl() {
        return mediaUrl;
    }

    public String getMediaType() {
        return mediaType;
    }
}
