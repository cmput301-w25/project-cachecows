package com.example.feelink.model;

import com.google.firebase.firestore.DocumentSnapshot;
/**
 * Contains user profile data and social metrics
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 01.02.01 - Profile management</li>
 *   <li>US 03.02.01 - User search functionality</li>
 * </ul>
 *
 * <p>Maintains case-insensitive username matching through lowercase variant</p>
 */

public class User {
    private String id;
    private String username;
    private String username_lowercase;
    private String profileImageUrl;
    private long moodPosts;
    private long followers;
    private long following;

    // Required empty constructor for Firestore
    public User() {}

    public User(String username) {
        this.username = username;
        this.username_lowercase = username.toLowerCase();
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

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public long getFollowers() {
        return followers;
    }

    public void setFollowers(Long followers) {
        this.followers = (followers != null) ? followers : 0L;
    }

    public void setFollowing(Long following) {
        this.following = (following != null) ? following : 0L;
    }

    public long getFollowing() {
        return following;
    }



    // Add to User class
    public static User fromDocument(DocumentSnapshot doc) {
        User user = new User();
        user.setId(doc.getId());
        user.setUsername(doc.getString("username"));
        user.setProfileImageUrl(doc.getString("profileImageUrl"));
        // Safely handle followers/following fields
        user.setFollowers(doc.contains("followers") ? doc.getLong("followers") : 0L);
        user.setFollowing(doc.contains("following") ? doc.getLong("following") : 0L);
        return user;
    }
}
