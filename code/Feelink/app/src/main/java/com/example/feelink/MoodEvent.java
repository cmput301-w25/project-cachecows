package com.example.feelink;

import java.util.Date;

public class MoodEvent {
    private long id;
    private Date timestamp;
    private String emotionalState;
    private String trigger;
    private String socialSituation;
    private String documentId;
    private String reason;
    private String userId;  // Added to support authentication later

    private String imageUrl;

    // Default constructor
    public MoodEvent() {
        this.timestamp = new Date(); // Set current date and time by default
        this.userId = "default_user"; // Default user ID until auth is implemented
    }

    // Constructor with required field
    public MoodEvent(String emotionalState) {
        this();
        this.emotionalState = emotionalState;
    }

    public MoodEvent(String emotionalState, String trigger) {
        this();
        this.emotionalState = emotionalState;
        this.trigger = trigger;
    }

    // Full constructor
    public MoodEvent(String emotionalState, String trigger, String socialSituation) {
        this();
        this.emotionalState = emotionalState;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
    }

    public MoodEvent(String emotionalState, String trigger, String socialSituation, String reason) {
        this();
        this.emotionalState = emotionalState;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getEmotionalState() {
        return emotionalState;
    }

    public void setEmotionalState(String emotionalState) {
        this.emotionalState = emotionalState;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getSocialSituation() {
        return socialSituation;
    }

    public void setSocialSituation(String socialSituation) {
        this.socialSituation = socialSituation;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getImageUrl(){return imageUrl;}

    public void setImageUrl(String imageUrl){
        this.imageUrl = imageUrl;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentId() {
        return documentId;
    }
}