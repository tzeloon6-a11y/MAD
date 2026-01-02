package com.example.mad;

public class JobModel {
    // These variable names should match your database columns later
    private String id;
    private String title;       // e.g., "Senior Java Developer"
    private String description; // e.g., "We need a dev..."
    private String recruiterId; // This is how we filter "My Jobs"

    public JobModel(String id, String title, String description, String recruiterId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.recruiterId = recruiterId;
    }
    public String getId() {return id;}
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRecruiterId() { return recruiterId; }
}
