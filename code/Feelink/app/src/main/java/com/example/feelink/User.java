package com.example.feelink;

public class User {
    private String id;
    private String username;
    private String username_lowercase;
    private String bio;
    private String profileImageUrl;
    private long moodPosts;
    private long followers;
    private long following;

    // Required empty constructor for Firestore
    public User() {}

    public User(String username, String bio) {
        this.username = username;
        this.username_lowercase = username.toLowerCase();
        this.bio = bio;
        this.moodPosts = 0;
        this.followers = 0;
        this.following = 0;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        this.username_lowercase = username.toLowerCase();
    }

    public String getUsername_lowercase() {
        return username_lowercase;
    }

    public void setUsername_lowercase(String username_lowercase) {
        this.username_lowercase = username_lowercase;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public long getMoodPosts() {
        return moodPosts;
    }

    public void setMoodPosts(long moodPosts) {
        this.moodPosts = moodPosts;
    }

    public long getFollowers() {
        return followers;
    }

    public void setFollowers(long followers) {
        this.followers = followers;
    }

    public long getFollowing() {
        return following;
    }

    public void setFollowing(long following) {
        this.following = following;
    }
}
