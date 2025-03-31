package com.example.feelink.model;

import java.util.Date;
import java.util.List;

/**
 * Manages chat conversation metadata between users
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 04.01.01 - Direct message initiation</li>
 *   <li>US 04.02.01 - Conversation history tracking</li>
 * </ul>
 */

public class Conversation {
    private String id;
    private List<String> participants;
    private String lastMessage;
    private Date timestamp;

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}