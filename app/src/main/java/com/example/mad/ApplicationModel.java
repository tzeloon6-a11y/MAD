package com.example.mad;

public class ApplicationModel {

    private String applicationId;
    private String studentId;
    private String studentName;
    private String recruiterId;
    private String jobId;
    private String status;
    private String initialMessage;
    private String timestamp;
    private String studentEmail;
    private String studentPhone;
    private String studentBio;
    private String studentResumeUrl;

    public ApplicationModel(String applicationId, String studentId, String studentName,
                            String recruiterId, String jobId, String status,
                            String initialMessage, String timestamp) {
        this.applicationId = applicationId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.recruiterId = recruiterId;
        this.jobId = jobId;
        this.status = status;
        this.initialMessage = initialMessage;
        this.timestamp = timestamp;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getRecruiterId() {
        return recruiterId;
    }

    public String getJobId() {
        return jobId;
    }

    public String getStatus() {
        return status;
    }

    public String getInitialMessage() {
        return initialMessage;
    }

    public String getTimestamp() {
        return timestamp;
    }
    
    public String getStudentEmail() {
        return studentEmail;
    }
    
    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }
    
    public String getStudentPhone() {
        return studentPhone;
    }
    
    public void setStudentPhone(String studentPhone) {
        this.studentPhone = studentPhone;
    }
    
    public String getStudentBio() {
        return studentBio;
    }
    
    public void setStudentBio(String studentBio) {
        this.studentBio = studentBio;
    }
    
    public String getStudentResumeUrl() {
        return studentResumeUrl;
    }
    
    public void setStudentResumeUrl(String studentResumeUrl) {
        this.studentResumeUrl = studentResumeUrl;
    }
}

