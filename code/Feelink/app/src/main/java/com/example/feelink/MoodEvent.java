package com.example.feelink;

import java.util.Date;

/**
 * Core data model representing a user's mood event with temporal and contextual metadata
 *
 * <p>Serves as the primary domain object for:</p>
 * <ul>
 *   <li>Mood event creation/editing (US 1.01.01.02)</li>
 *   <li>Emotional state modeling (US 1.02.01.01)</li>
 *   <li>Data storage/retrieval operations (US 1.04.01.02)</li>
 *   <li>Extended attribute integration (US 02.01.01.03, US 02.02.01.03)</li>
 * </ul>
 *
 * @author Your Name
 * @version 1.3
 * @see FirestoreManager
 * @see AddMoodEventActivity
 */
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
    private String tempLocalImagePath;
    private boolean isPendingSync;

    /**
     * Default constructor initializes required temporal fields
     *
     * <p>Sets:
     * <ul>
     *   <li>Timestamp to current datetime</li>
     *   <li>User ID to temporary default value</li>
     * </ul>
     */
    public MoodEvent() {
        this.timestamp = new Date(); // Set current date and time by default
        this.userId = "default_user"; // Default user ID until auth is implemented
    }

    /**
     * Minimal valid constructor for emotional state recording
     * @param emotionalState Required emotional state from predefined values
     */
    public MoodEvent(String emotionalState) {
        this();
        this.emotionalState = emotionalState;
    }

    /**
     * Extended constructor for basic mood tracking
     * @param emotionalState Validated emotional state
     * @param trigger User-provided trigger context
     */
    public MoodEvent(String emotionalState, String trigger) {
        this();
        this.emotionalState = emotionalState;
        this.trigger = trigger;
    }

    /**
     * Full social context constructor
     * @param emotionalState Selected emotional state
     * @param trigger Trigger description
     * @param socialSituation Social situation category
     */
    public MoodEvent(String emotionalState, String trigger, String socialSituation) {
        this();
        this.emotionalState = emotionalState;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
    }

    /**
     * Complete event constructor with all user-facing fields
     * @param emotionalState Validated emotional state
     * @param trigger Trigger context (max 100 chars)
     * @param socialSituation Selected social situation
     * @param reason Mood reason (max 20 chars/3 words)
     */
    public MoodEvent(String emotionalState, String trigger, String socialSituation, String reason) {
        this();
        this.emotionalState = emotionalState;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        this.reason = reason;
    }

    /**
    * @return Mood reason text (max 20 chars/3 words)
     */
    public String getReason() {
        return reason;
    }

    /**
     * @param reason Validated reason text (US 02.01.01.02)
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * @return Unique event identifier (auto-generated)
     */
    public long getId() {
        return id;
    }

    /**
     * @param id Database-assigned unique identifier
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return Timestamp of event creation
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp Custom timestamp for historical edits
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return Emotional state from predefined values (US 1.02.01.01)
     */
    public String getEmotionalState() {
        return emotionalState;
    }

    /**
     * @param emotionalState Validated emotional state value
     */
    public void setEmotionalState(String emotionalState) {
        this.emotionalState = emotionalState;
    }

    /**
     * @return User-provided trigger context (max 100 chars)
     */
    public String getTrigger() {
        return trigger;
    }

    /**
     * @param trigger Trigger description text
     */
    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    /**
     * @return Social situation category from predefined options
     */
    public String getSocialSituation() {
        return socialSituation;
    }

    /**
     * @param socialSituation Selected social situation value
     */
    public void setSocialSituation(String socialSituation) {
        this.socialSituation = socialSituation;
    }

    /**
     * @return Associated user ID from authentication system
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @param userId Validated user reference (US 03.01.01.*)
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * @return Cloud storage URL for associated image (US 02.02.01.03)
     */
    public String getImageUrl() {return imageUrl;}

    /**
     * @param imageUrl Validated image storage path
     * @see UploadImageActivity
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * @return Firestore-generated document ID (US 1.01.01.02)
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * @param documentId Firestore document reference
     * @see FirestoreManager
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }


    public void setTempLocalImagePath(String path) {
        this.tempLocalImagePath = path;
    }
    public String getTempLocalImagePath() {
        return tempLocalImagePath;
    }

    public boolean isPendingSync() {
        return isPendingSync;
    }

    public void setPendingSync(boolean pendingSync) {
        isPendingSync = pendingSync;
    }
}