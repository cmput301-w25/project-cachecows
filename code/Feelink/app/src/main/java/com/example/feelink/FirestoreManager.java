package com.example.feelink;

import android.util.Log;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Tasks;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.common.api.Status;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * Central Firestore operations handler managing all CRUD operations for mood events and user data
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 1.01.01.02 - Mood event storage logic</li>
 *   <li>US 1.04.01.02 - Mood event data retrieval</li>
 *   <li>US 1.05.01.03 - Mood event update operations</li>
 *   <li>US 1.06.01.02 - Mood event deletion</li>
 *   <li>US 02.02.01.03 - Image URL storage integration</li>
 *   <li>US 03.01.01.02 - Username/UID resolution</li>
 * </ul>
 *
 * @see MoodEvent
 * @see FirebaseFirestore
 */
public class FirestoreManager {
    private static final String TAG = "FirestoreManager";

    // Collection names
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_MOOD_EVENTS = "mood_events";

    // Default user ID (until auth system is implemented)
    private String userId; // Add this

    private FirebaseFirestore db;

    /**
     * Constructs FirestoreManager with user context
     * @param userId Authenticated user's unique identifier
     */
    public FirestoreManager(String userId) { // Modified constructor
        db = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    public FirestoreManager(String userId, FirebaseFirestore firestore) {
        this.db = firestore;
        this.userId = userId;
    }

    public void updateAllUsersWithLowercaseUsername() {
        Log.d(TAG, "Starting update of all users with lowercase usernames");

        db.collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    int updatedCount = 0;
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String username = document.getString("username");
                        if (username != null) {
                            String usernameLowercase = username.toLowerCase();

                            // Update the document with the lowercase username
                            document.getReference().update("username_lowercase", usernameLowercase)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Updated lowercase username for user: " + document.getId());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating lowercase username for user: " + document.getId(), e);
                                    });

                            updatedCount++;
                        }
                    }
                    Log.d(TAG, "Processed " + updatedCount + " users for lowercase username update");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching users for lowercase username update", e);
                });
    }

    /**
     * Persists mood event to Firestore with partial field updates
     *
     * <p>Handles:
     * <ul>
     *   <li>Conversion of MoodEvent object to Firestore document</li>
     *   <li>Optional field omission for null/empty values</li>
     *   <li>Atomic document creation with auto-generated ID</li>
     * </ul>
     *
     * @param moodEvent Valid MoodEvent object to store
     * @param listener Callback for operation results
     *
     * @see OnMoodEventListener
     */
    public void addMoodEvent(MoodEvent moodEvent, final OnMoodEventListener listener) {
        // Convert MoodEvent to Map
        Map<String, Object> moodData = new HashMap<>();
        moodData.put("userId", this.userId);
        moodData.put("timestamp", moodEvent.getTimestamp());
        moodData.put("emotionalState", moodEvent.getEmotionalState());

        // Debug logging for location data
        Log.d(TAG, "Adding mood event with location data:");
        Log.d(TAG, "Latitude: " + moodEvent.getLatitude());
        Log.d(TAG, "Longitude: " + moodEvent.getLongitude());
        Log.d(TAG, "Location Name: " + moodEvent.getLocationName());

        // Only add optional fields if they're not null or empty
        if (moodEvent.getReason() != null && !moodEvent.getReason().isEmpty()) {
            moodData.put("reason", moodEvent.getReason());
        }

        if (moodEvent.getTrigger() != null && !moodEvent.getTrigger().isEmpty()) {
            moodData.put("trigger", moodEvent.getTrigger());
        }

        if (moodEvent.getSocialSituation() != null && !moodEvent.getSocialSituation().isEmpty()) {
            moodData.put("socialSituation", moodEvent.getSocialSituation());
        }

        if (moodEvent.getImageUrl() != null && !moodEvent.getImageUrl().isEmpty()) {
            moodData.put("imageUrl", moodEvent.getImageUrl());
        }

        if (moodEvent.getLocationName() != null && !moodEvent.getLocationName().isEmpty()) {
            moodData.put("locationName", moodEvent.getLocationName());
            Log.d(TAG, "Added locationName to moodData: " + moodEvent.getLocationName());
        }

        // Add latitude and longitude if they exist
        if (moodEvent.getLatitude() != null) {
            moodData.put("latitude", moodEvent.getLatitude());
            Log.d(TAG, "Added latitude to moodData: " + moodEvent.getLatitude());
        }

        if (moodEvent.getLongitude() != null) {
            moodData.put("longitude", moodEvent.getLongitude());
            Log.d(TAG, "Added longitude to moodData: " + moodEvent.getLongitude());
        }

        // Log the complete moodData map
        Log.d(TAG, "Complete moodData being sent to Firestore: " + moodData.toString());

        // Add to Firestore
        db.collection(COLLECTION_MOOD_EVENTS)
                .add(moodData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Set ID in the mood event
                        moodEvent.setId(documentReference.getId().hashCode());


                        Log.d(TAG, "Mood event added with ID: " + documentReference.getId());
                        if (listener != null) {
                            listener.onSuccess(moodEvent);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("FirestoreManager", "Mood Data: " + moodData.toString());
                        Log.w(TAG, "Error adding mood event", e);
                        if (listener != null) {
                            listener.onFailure(e.getMessage());
                        }
                    }
                });
    }

    // Add this method to FirestoreManager.java
    public void updateUserEmail(String username, String newEmail, OnSuccessListener<Void> success, OnFailureListener failure) {
        db.collection("usernames").document(username)
                .update("email", newEmail)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }

    /**
     * Retrieves chronological mood events for current user
     *
     * <p>Query structure:
     * <ol>
     *   <li>Filters by current user ID</li>
     *   <li>Orders by timestamp descending</li>
     *   <li>Maps Firestore documents to MoodEvent objects</li>
     * </ol>
     *
     * @param listener Callback receiving List<MoodEvent>
     */
    public void getMoodEvents(final OnMoodEventsListener listener) {
        db.collection(COLLECTION_MOOD_EVENTS)
                .whereEqualTo("userId", this.userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<MoodEvent> moodEvents = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String id = document.getId();
                                Date timestamp = document.getDate("timestamp");
                                String emotionalState = document.getString("emotionalState");
                                String trigger = document.getString("trigger");
                                String socialSituation = document.getString("socialSituation");
                                String reason = document.getString("reason");
                                String userId = document.getString("userId");
                                String imageUrl = document.getString("imageUrl");
                                Double latitude = document.getDouble("latitude");
                                Double longitude = document.getDouble("longitude");
                                String locationName = document.getString("locationName");

                                MoodEvent moodEvent = new MoodEvent(emotionalState, trigger, socialSituation, reason);
                                moodEvent.setUserId(userId);
                                moodEvent.setId(id.hashCode());
                                moodEvent.setTimestamp(timestamp);
                                moodEvent.setDocumentId(id);
                                moodEvent.setImageUrl(imageUrl);
                                moodEvent.setLatitude(latitude);
                                moodEvent.setLongitude(longitude);
                                moodEvent.setLocationName(locationName);

                                moodEvents.add(moodEvent);
                            }

                            if (listener != null) {
                                listener.onSuccess(moodEvents);
                            }
                        } else {
                            Log.w(TAG, "Error getting mood events", task.getException());
                            if (listener != null) {
                                listener.onFailure(task.getException().getMessage());
                            }
                        }
                    }
                });
    }

    /**
     * Fetches public mood events from other users
     *
     * <p>Security notes:
     * <ul>
     *   <li>Excludes current user's events</li>
     *   <li>Requires composite index for multiple orderBy clauses</li>
     * </ul>
     *
     * @param listener Callback for shared MoodEvent collection
     */
    public void getSharedMoodEvents(final OnMoodEventsListener listener) {
        db.collection(COLLECTION_MOOD_EVENTS)
                .whereNotEqualTo("userId", this.userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .orderBy("userId", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<MoodEvent> moodEvents = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String id = document.getId();
                                Date timestamp = document.getDate("timestamp");
                                String emotionalState = document.getString("emotionalState");
                                String trigger = document.getString("trigger");
                                String socialSituation = document.getString("socialSituation");
                                String reason = document.getString("reason");
                                String userId = document.getString("userId");
                                String imageUrl = document.getString("imageUrl");
                                Double latitude = document.getDouble("latitude");
                                Double longitude = document.getDouble("longitude");
                                String locationName = document.getString("locationName");

                                MoodEvent moodEvent = new MoodEvent(emotionalState, trigger, socialSituation, reason);
                                moodEvent.setUserId(userId);
                                moodEvent.setId(id.hashCode());
                                moodEvent.setTimestamp(timestamp);
                                moodEvent.setDocumentId(id);
                                moodEvent.setImageUrl(imageUrl);
                                moodEvent.setLatitude(latitude);
                                moodEvent.setLongitude(longitude);
                                moodEvent.setLocationName(locationName);

                                moodEvents.add(moodEvent);
                            }

                            if (listener != null) {
                                listener.onSuccess(moodEvents);
                            }
                        } else {
                            Log.w(TAG, "Error getting shared mood events", task.getException());
                            if (listener != null) {
                                listener.onFailure(task.getException().getMessage());
                            }
                        }
                    }
                });
    }




    /**
     * Deletes mood event with ownership verification
     *
     * <p>Validation steps:
     * <ol>
     *   <li>Verify document belongs to current user</li>
     *   <li>Execute deletion if verification passes</li>
     * </ol>
     *
     * @param moodEventId Local app-generated event ID
     * @param listener Deletion result callback
     */
    public void deleteMoodEvent(final long moodEventId, final OnDeleteListener listener) {
        db.collection(COLLECTION_MOOD_EVENTS)
                .whereEqualTo("userId", this.userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            boolean found = false;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.getId().hashCode() == moodEventId) {
                                    found = true;
                                    document.getReference().delete()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    if (listener != null) {
                                                        listener.onSuccess();
                                                    }
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    if (listener != null) {
                                                        listener.onFailure(e.getMessage());
                                                    }
                                                }
                                            });
                                    break;
                                }
                            }

                            if (!found && listener != null) {
                                listener.onFailure("Mood event not found");
                            }
                        } else {
                            if (listener != null) {
                                listener.onFailure(task.getException() != null ?
                                        task.getException().getMessage() : "Failed to query mood events");
                            }
                        }
                    }
                });
    }

    /**
     * Updates existing mood event document with delta changes
     *
     * <p>Key features:
     * <ul>
     *   <li>FieldValue.delete() for empty fields</li>
     *   <li>Partial updates without overwriting unchanged fields</li>
     *   <li>Document ID verification</li>
     * </ul>
     *
     * @param moodEvent Updated MoodEvent object
     * @param documentId Firestore document ID to update
     * @param listener Result callback handler
     */
    public void updateMoodEvent(MoodEvent moodEvent, String documentId, final OnMoodEventListener listener) {
        if (documentId == null || documentId.isEmpty()) {
            if (listener != null) {
                listener.onFailure("Document ID is null or empty");
            }
            return;
        }

        Map<String, Object> moodData = new HashMap<>();
        
        // Handle emotional state
        String emotionalState = moodEvent.getEmotionalState();
        if (emotionalState != null && !emotionalState.isEmpty()) {
            moodData.put("emotionalState", emotionalState);
        } else {
            moodData.put("emotionalState", FieldValue.delete());
        }

        // Handle reason
        String reason = moodEvent.getReason();
        if (reason != null && !reason.isEmpty()) {
            moodData.put("reason", reason);
        } else {
            moodData.put("reason", FieldValue.delete());
        }

        // Handle trigger
        String trigger = moodEvent.getTrigger();
        if (trigger != null && !trigger.isEmpty()) {
            moodData.put("trigger", trigger);
        } else {
            moodData.put("trigger", FieldValue.delete());
        }

        // Handle social situation
        String socialSituation = moodEvent.getSocialSituation();
        if (socialSituation != null && !socialSituation.isEmpty()) {
            moodData.put("socialSituation", socialSituation);
        } else {
            moodData.put("socialSituation", FieldValue.delete());
        }

        // Handle image URL
        String imageUrl = moodEvent.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            moodData.put("imageUrl", imageUrl);
        } else {
            moodData.put("imageUrl", FieldValue.delete());
        }

        // Handle location name
        String locationName = moodEvent.getLocationName();
        if (locationName != null && !locationName.isEmpty()) {
            moodData.put("locationName", locationName);
        } else {
            moodData.put("locationName", FieldValue.delete());
        }

        // Handle latitude
        Double latitude = moodEvent.getLatitude();
        if (latitude != null) {
            moodData.put("latitude", latitude);
        } else {
            moodData.put("latitude", FieldValue.delete());
        }

        // Handle longitude
        Double longitude = moodEvent.getLongitude();
        if (longitude != null) {
            moodData.put("longitude", longitude);
        } else {
            moodData.put("longitude", FieldValue.delete());
        }

        Log.d(TAG, "Updating mood event with data: " + moodData.toString());

        db.collection(COLLECTION_MOOD_EVENTS)
                .document(documentId)
                .update(moodData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Mood event updated successfully");
                    if (listener != null) {
                        listener.onSuccess(moodEvent);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating mood event", e);
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }
    /**
     * Resolves username from UID with fallback handling
     *
     * <p>Implements two-phase lookup:
     * <ol>
     *   <li>Check usernames collection mapping</li>
     *   <li>Fallback to UID if unavailable</li>
     * </ol>
     *
     * @param userId Target user's UID
     * @param callback Username resolution handler
     */
    public void getUsernameById(String userId, OnUsernameListener callback) {
        // First try to get from the usernames collection
        db.collection("usernames")
                .whereEqualTo("uid", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // The document ID is the username
                        String username = task.getResult().getDocuments().get(0).getId();
                        callback.onSuccess(username);
                    } else {
                        // Fall back to userId if no username found
                        callback.onFailure(userId);
                    }
                });
    }


//    public Query getPublicMoodEvents(String userId) {
//        return db.collection("mood_events")
//                .whereEqualTo("userId", userId)
//                .whereEqualTo("isPublic", true); // Only fetch public events
//    }

    /**
     * Fetches all mood events for the current authenticated user (private and public).
     */
//    public Query getAllMoodEvents(String userId) {
//        return db.collection("mood_events")
//                .whereEqualTo("userId", userId); // Fetch all events regardless of visibility
//    }

    void setDb(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Callback interface for deletion operations
     */
    public interface OnDeleteListener {
        /**
         * Called upon successful document removal
         */
        void onSuccess();

        /**
         * @param errorMessage Deletion failure details
         */
        void onFailure(String errorMessage);
    }


    /**
     * Callback interface for single mood event operations
     */
    public interface OnMoodEventListener {
        /**
         * @param moodEvent Successfully processed MoodEvent
         */
        void onSuccess(MoodEvent moodEvent);
        /**
         * @param errorMessage Descriptive failure reason
         */
        void onFailure(String errorMessage);
    }

    /**
     * Callback interface for bulk mood event operations
     */
    public interface OnMoodEventsListener {
        /**
         * @param moodEvents Chronologically ordered list
         */
        void onSuccess(List<MoodEvent> moodEvents);
        /**
         * @param errorMessage Firestore error details
         */
        void onFailure(String errorMessage);
    }
    /**
     * Callback interface for username resolution
     */
    public interface OnUsernameListener {
        /**
         * @param username Resolved username string
         */
        void onSuccess(String username);

        /**
         * @param fallbackName UID used as fallback identifier
         */
        void onFailure(String fallbackName);
    }

    /**
     * Updates location data for an existing mood event
     *
     * @param documentId Firestore document ID of the mood event
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param locationName Optional location name/address
     * @param listener Callback for operation results
     */
    public void updateMoodEventLocation(String documentId, Double latitude, Double longitude, 
            String locationName, final OnMoodEventListener listener) {
        Map<String, Object> locationData = new HashMap<>();
        
        if (latitude != null) {
            locationData.put("latitude", latitude);
        }
        if (longitude != null) {
            locationData.put("longitude", longitude);
        }
        if (locationName != null && !locationName.isEmpty()) {
            locationData.put("locationName", locationName);
        }

        db.collection(COLLECTION_MOOD_EVENTS)
                .document(documentId)
                .update(locationData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Location updated for mood event: " + documentId);
                    if (listener != null) {
                        MoodEvent updatedEvent = new MoodEvent();
                        updatedEvent.setDocumentId(documentId);
                        updatedEvent.setLatitude(latitude);
                        updatedEvent.setLongitude(longitude);
                        updatedEvent.setLocationName(locationName);
                        listener.onSuccess(updatedEvent);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating location for mood event: " + documentId, e);
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Retrieves mood events within a specified geographic radius
     *
     * @param centerLatitude Center point latitude
     * @param centerLongitude Center point longitude
     * @param radiusInKm Radius in kilometers
     * @param listener Callback for mood events within radius
     */
    public void getMoodEventsInRadius(double centerLatitude, double centerLongitude, 
            double radiusInKm, final OnMoodEventsListener listener) {
        // Note: This is a simplified version. For production, you should implement
        // proper geohashing or use a geospatial database
        db.collection(COLLECTION_MOOD_EVENTS)
                .whereEqualTo("userId", this.userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<MoodEvent> moodEvents = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Double eventLat = document.getDouble("latitude");
                            Double eventLng = document.getDouble("longitude");
                            
                            if (eventLat != null && eventLng != null) {
                                // Calculate distance using Haversine formula
                                double distance = calculateDistance(centerLatitude, centerLongitude, 
                                        eventLat, eventLng);
                                
                                if (distance <= radiusInKm) {
                                    MoodEvent moodEvent = createMoodEventFromDocument(document);
                                    moodEvents.add(moodEvent);
                                }
                            }
                        }
                        
                        if (listener != null) {
                            listener.onSuccess(moodEvents);
                        }
                    } else {
                        Log.w(TAG, "Error getting mood events in radius", task.getException());
                        if (listener != null) {
                            listener.onFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    /**
     * Calculates distance between two points using Haversine formula
     *
     * @param lat1 First point latitude
     * @param lon1 First point longitude
     * @param lat2 Second point latitude
     * @param lon2 Second point longitude
     * @return Distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * Creates a MoodEvent object from a Firestore document
     *
     * @param document Firestore document
     * @return MoodEvent object
     */
    private MoodEvent createMoodEventFromDocument(QueryDocumentSnapshot document) {
        String id = document.getId();
        Date timestamp = document.getDate("timestamp");
        String emotionalState = document.getString("emotionalState");
        String trigger = document.getString("trigger");
        String socialSituation = document.getString("socialSituation");
        String reason = document.getString("reason");
        String userId = document.getString("userId");
        String imageUrl = document.getString("imageUrl");
        Double latitude = document.getDouble("latitude");
        Double longitude = document.getDouble("longitude");
        String locationName = document.getString("locationName");

        MoodEvent moodEvent = new MoodEvent(emotionalState, trigger, socialSituation, reason);
        moodEvent.setUserId(userId);
        moodEvent.setId(id.hashCode());
        moodEvent.setTimestamp(timestamp);
        moodEvent.setDocumentId(id);
        moodEvent.setImageUrl(imageUrl);
        moodEvent.setLatitude(latitude);
        moodEvent.setLongitude(longitude);
        moodEvent.setLocationName(locationName);

        return moodEvent;
    }
}