package com.example.feelink;

import java.util.Date;
/**
 * Represents a follow request between users with status tracking. Used in notification systems
 * and follow approval workflows.
 * <p>
 * Directly enables:
 * <ul>
 *   <li>US 05.02.02 (View follow requests)</li>
 *   <li>US 03.01.01 (Username display in requests)</li>
 * </ul>
 */

public class FollowRequest {
    private String id;
    private String senderId;
    private String senderName; // Added field
    private String receiverId;
    private String status;
    private Date timestamp;

    // Getters and Setters
    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    // Existing getters and setters...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}