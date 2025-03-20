package com.example.feelink;

public class CommentLike {
    private final String actionText;
    private final String timestamp;

    public CommentLike(String actionText, String timestamp) {
        this.actionText = actionText;
        this.timestamp = timestamp;
    }

    public String getActionText() { return actionText; }
    public String getTimestamp() { return timestamp; }
}