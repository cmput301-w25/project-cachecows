package com.example.feelink.model;

import java.util.Date;
/**
 * Represents a user comment on a mood event with engagement metrics
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 05.02.01 - Commenting on mood events</li>
 *   <li>US 05.03.01 - Displaying social interactions</li>
 * </ul>
 */

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