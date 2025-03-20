package com.example.feelink;

public class FollowRequest {
    private final String requestText;
    private final String timestamp;

    public FollowRequest(String requestText, String timestamp) {
        this.requestText = requestText;
        this.timestamp = timestamp;
    }

    public String getRequestText() { return requestText; }
    public String getTimestamp() { return timestamp; }
}