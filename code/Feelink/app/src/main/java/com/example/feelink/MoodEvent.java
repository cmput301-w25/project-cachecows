package com.example.feelink;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Date;
import com.google.firebase.firestore.DocumentSnapshot;

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
 * @see FirestoreManager
 * @see AddMoodEventActivity
 */
public class MoodEvent implements Parcelable {
    private long id;
    private Date timestamp;
    private String emotionalState;
    private String socialSituation;
    private String documentId;
    private String reason;
    private String userId;  // Added to support authentication later
    private String imageUrl;
    private boolean isPublic = true; // Default to public
    private Double latitude;  // Location latitude
    private Double longitude; // Location longitude
    private String locationName; // Optional location name/address
    private String tempLocalImagePath;
    private boolean isPendingSync;
    private String username;
    private String userProfileImageUrl;

    // Parcelable constructor
    protected MoodEvent(Parcel in) {
        id = in.readLong();
        timestamp = new Date(in.readLong());
        emotionalState = in.readString();
        socialSituation = in.readString();
        documentId = in.readString();
        reason = in.readString();
        userId = in.readString();
        imageUrl = in.readString();
        isPublic = in.readByte() != 0;
        latitude = in.readDouble();
        longitude = in.readDouble();
        locationName = in.readString();
        tempLocalImagePath = in.readString();
        isPendingSync = in.readByte() != 0;
        username = in.readString();
        userProfileImageUrl = in.readString();
    }

    public static final Creator<MoodEvent> CREATOR = new Creator<MoodEvent>() {
        @Override
        public MoodEvent createFromParcel(Parcel in) {
            return new MoodEvent(in);
        }

        @Override
        public MoodEvent[] newArray(int size) {
            return new MoodEvent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(timestamp.getTime());
        dest.writeString(emotionalState);
        dest.writeString(socialSituation);
        dest.writeString(documentId);
        dest.writeString(reason);
        dest.writeString(userId);
        dest.writeString(imageUrl);
        dest.writeByte((byte) (isPublic ? 1 : 0));
        dest.writeDouble(latitude != null ? latitude : 0.0);
        dest.writeDouble(longitude != null ? longitude : 0.0);
        dest.writeString(locationName);
        dest.writeString(tempLocalImagePath);
        dest.writeByte((byte) (isPendingSync ? 1 : 0));
        dest.writeString(username);
        dest.writeString(userProfileImageUrl);
    }

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
     * Full social context constructor
     * @param emotionalState Selected emotional state
     * @param socialSituation Social situation category
     */
    public MoodEvent(String emotionalState, String socialSituation) {
        this();
        this.emotionalState = emotionalState;
        this.socialSituation = socialSituation;
    }

    /**
     * Complete event constructor with all user-facing fields
     * @param emotionalState Validated emotional state
     * @param socialSituation Selected social situation
     * @param reason Mood reason (max 20 chars/3 words)
     */
    public MoodEvent(String emotionalState, String socialSituation, String reason) {
        this();
        this.emotionalState = emotionalState;
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
     * @return Location latitude coordinate
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * @param latitude Location latitude coordinate
     */
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /**
     * @return Location longitude coordinate
     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * @param longitude Location longitude coordinate
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /**
     * @return Location name/address
     */
    public String getLocationName() {
        return locationName;
    }

    /**
     * @param locationName Location name/address
     */
    public void setLocationName(String locationName) {
        this.locationName = locationName;
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

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserProfileImageUrl() {
        return userProfileImageUrl;
    }

    public void setUserProfileImageUrl(String userProfileImageUrl) {
        this.userProfileImageUrl = userProfileImageUrl;
    }

    /**
     * Creates a MoodEvent instance from a Firestore DocumentSnapshot
     * @param document The Firestore DocumentSnapshot
     * @return A new MoodEvent instance
     */
    public static MoodEvent fromDocument(DocumentSnapshot document) {
        String emotionalState = document.getString("emotionalState");
        String socialSituation = document.getString("socialSituation");
        String reason = document.getString("reason");
        
        MoodEvent moodEvent = new MoodEvent(emotionalState, socialSituation, reason);
        
        // Set additional fields
        String id = document.getId();
        moodEvent.setId(id.hashCode());
        moodEvent.setDocumentId(id);
        moodEvent.setUserId(document.getString("userId"));
        moodEvent.setTimestamp(document.getDate("timestamp"));
        moodEvent.setImageUrl(document.getString("imageUrl"));
        
        // Handle location data
        Double latitude = document.getDouble("latitude");
        Double longitude = document.getDouble("longitude");
        String locationName = document.getString("locationName");
        moodEvent.setLatitude(latitude);
        moodEvent.setLongitude(longitude);
        moodEvent.setLocationName(locationName);
        
        // Handle privacy setting (default to true for legacy data)
        Boolean isPublic = document.getBoolean("isPublic");
        moodEvent.setPublic(isPublic != null ? isPublic : true);
        
        // Handle temporary local image path if exists
        String tempPath = document.getString("tempLocalImagePath");
        if (tempPath != null && !tempPath.isEmpty()) {
            moodEvent.setTempLocalImagePath(tempPath);
        }
        
        return moodEvent;
    }
}