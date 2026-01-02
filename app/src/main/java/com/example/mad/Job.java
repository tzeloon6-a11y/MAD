package com.example.mad;

public class Job {
    private final String jobId;
    private final String title;
    private final String companyName;
    private final String wage;
    private final String location;
    private final String description;
    private final String recruiterId;

    public Job(String jobId, String title, String companyName, String wage, String location, String description, String recruiterId) {
        this.jobId = jobId;
        this.title = title;
        this.companyName = companyName;
        this.wage = wage;
        this.location = location;
        this.description = description;
        this.recruiterId = recruiterId;
    }

    public String getJobId() { return jobId; }
    public String getTitle() { return title; }
    public String getCompanyName() { return companyName; }
    public String getWage() { return wage; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getRecruiterId() { return recruiterId; }
}
