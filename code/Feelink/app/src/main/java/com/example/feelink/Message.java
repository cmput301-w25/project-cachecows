package com.example.feelink;

import java.util.Date;

// Message.java
public class Message {
    private String text, senderId;
    private Date timestamp;

    public Message(String text, String senderId, Date timestamp) {
        this.text = text;
        this.senderId = senderId;
        this.timestamp = timestamp != null ? timestamp : new Date();
    }


    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }


    // Getters
    public boolean isSent(String currentUserId) {
        return senderId.equals(currentUserId);
    }
}