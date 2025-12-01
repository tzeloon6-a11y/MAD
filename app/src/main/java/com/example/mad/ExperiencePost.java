package com.example.mad;

public class ExperiencePost {
    public static final String TYPE_EXPERIENCE = "experience";
    public static final String TYPE_JOB = "job";

    private String id;        // unique id (timestamp or UUID)
    private String studentEmail; // or studentName/recruiterName
    private String title;
    private String content;
    private long createdAt;
    private String type; // "experience" or "job"
    
    private String mediaUri; // URI as String
    private String mediaType; // "image" or "video"

    public ExperiencePost() {}

    public ExperiencePost(String id, String studentEmail, String title, String content, long createdAt, String type, String mediaUri, String mediaType) {
        this.id = id;
        this.studentEmail = studentEmail;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.type = type;
        this.mediaUri = mediaUri;
        this.mediaType = mediaType;
    }

    // Constructors for backward compatibility
    public ExperiencePost(String id, String studentEmail, String title, String content, long createdAt, String type) {
        this(id, studentEmail, title, content, createdAt, type, null, null);
    }

    public ExperiencePost(String id, String studentEmail, String title, String content, long createdAt) {
        this(id, studentEmail, title, content, createdAt, TYPE_EXPERIENCE, null, null);
    }

    // Getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMediaUri() { return mediaUri; }
    public void setMediaUri(String mediaUri) { this.mediaUri = mediaUri; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
}
