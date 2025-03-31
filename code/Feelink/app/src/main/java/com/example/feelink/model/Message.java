package com.example.feelink.model;

import java.util.Date;

/**
 * Encapsulates chat message data and delivery status
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 04.02.01 - Message sending functionality</li>
 *   <li>US 05.03.01 - Real-time message synchronization</li>
 * </ul>
 */
public class Message {

    private String id;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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