package com.example.feelink;

import java.util.Date;

public class Comment {
    private String id;
    private String text;
    private String userId;
    private Date timestamp;
    private String username;

    public Comment() {}

    public Comment(String text, String userId) {
        this.text = text;
        this.userId = userId;
        this.timestamp = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}