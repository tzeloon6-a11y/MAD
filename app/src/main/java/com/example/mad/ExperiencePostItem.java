package com.example.mad;

public class ExperiencePostItem {
    public int id;
    public int userId;
    public String title;
    public String description;
    public String mediaUrl;
    public String createdAt;

    public ExperiencePostItem(int id, int userId, String title,
                              String description, String mediaUrl, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.mediaUrl = mediaUrl;
        this.createdAt = createdAt;
    }
}

