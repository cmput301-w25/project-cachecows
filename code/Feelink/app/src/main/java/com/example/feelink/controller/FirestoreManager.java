package com.example.feelink.controller;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.feelink.model.Comment;
import com.example.feelink.model.Conversation;
import com.example.feelink.model.FollowRequest;
import com.example.feelink.model.Message;
import com.example.feelink.model.MoodEvent;
import com.example.feelink.model.Notification;
import com.example.feelink.model.User;
import com.google.android.gms.tasks.Tasks;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public interface OnImageUploadListener {
        void onImageUploadSuccess(String newImageUrl);
        void onImageUploadFailure(String error);
    }
    private static final String TAG = "FirestoreManager";

    // Collection names
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_MOOD_EVENTS = "mood_events";

    // Default user ID (until auth system is implemented)
    private String userId; // Add this

    private FirebaseFirestore db;

    public String getUserId() {
        return userId;
    }

    /**
     * Constructs FirestoreManager with user context
     * @param userId Authenticated user's unique identifier
     */
    public FirestoreManager(String userId) { // Modified constructor
        db = FirebaseFirestore.getInstance();
        this.userId = userId;
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
        moodData.put("isPublic", moodEvent.isPublic());


        // Only add optional fields if they're not null or empty
        if (moodEvent.getReason() != null && !moodEvent.getReason().isEmpty()) {
            moodData.put("reason", moodEvent.getReason());
        }

        if (moodEvent.getSocialSituation() != null && !moodEvent.getSocialSituation().isEmpty()) {
            moodData.put("socialSituation", moodEvent.getSocialSituation());
        }

        if (moodEvent.getImageUrl() != null && !moodEvent.getImageUrl().isEmpty()) {
            moodData.put("imageUrl", moodEvent.getImageUrl());
        }

        if (moodEvent.getTempLocalImagePath() != null && !moodEvent.getTempLocalImagePath().isEmpty()) {
            moodData.put("tempLocalImagePath", moodEvent.getTempLocalImagePath());
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

        if (moodEvent.getTempLocalImagePath() != null && !moodEvent.getTempLocalImagePath().isEmpty()) {
            moodData.put("tempLocalImagePath", moodEvent.getTempLocalImagePath());
        }

        // Add to Firestore
        db.collection(COLLECTION_MOOD_EVENTS)
                .add(moodData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Set ID in the mood event
                        moodEvent.setId(documentReference.getId().hashCode());
                        moodEvent.setDocumentId(documentReference.getId());

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
     *
     *
     */
    public void getMoodEvents(Boolean showPublic, final OnMoodEventsListener listener) {
        getMoodEvents(showPublic, false, null, listener); // Default: filterByWeek = false
    }
    public void getMoodEvents(Boolean showPublic, boolean filterByWeek, String emotionFilter, final OnMoodEventsListener listener) {
        // Calculate timestamp for 7 days ago
        long oneWeekAgoMillis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        Date oneWeekAgo = new Date(oneWeekAgoMillis);

        // Build base query
        Query query = db.collection(COLLECTION_MOOD_EVENTS)
                .whereEqualTo("userId", this.userId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        // Add date filter if needed
        if (filterByWeek) {
            query = query.whereGreaterThanOrEqualTo("timestamp", oneWeekAgo);
        }

        // Emotion filter
        if (emotionFilter != null && !emotionFilter.isEmpty()) {
            query = query.whereEqualTo("emotionalState", emotionFilter);
        }

        query.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<MoodEvent> moodEvents = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Existing field extraction
                                String id = document.getId();
                                Date timestamp = document.getDate("timestamp");
                                String emotionalState = document.getString("emotionalState");
                                String socialSituation = document.getString("socialSituation");
                                String reason = document.getString("reason");
                                String userId = document.getString("userId");
                                String imageUrl = document.getString("imageUrl");
                                String tempPath = document.getString("tempLocalImagePath");

                                Double latitude = document.getDouble("latitude");
                                Double longitude = document.getDouble("longitude");
                                String locationName = document.getString("locationName");

                                // Handle legacy moods (missing isPublic field)
                                Boolean isPublic = document.getBoolean("isPublic");
                                if (isPublic == null) isPublic = true;

                                // Preserve original public/private filtering logic
                                boolean shouldInclude = (showPublic == null) ||
                                        (showPublic && isPublic) ||
                                        (!showPublic && !isPublic);

                                if (shouldInclude) {
                                    MoodEvent moodEvent = new MoodEvent(emotionalState, socialSituation, reason);
                                    moodEvent.setUserId(userId);
                                    moodEvent.setId(id.hashCode());
                                    moodEvent.setTimestamp(timestamp);
                                    moodEvent.setDocumentId(id);
                                    moodEvent.setImageUrl(imageUrl);
                                    moodEvent.setLatitude(latitude);
                                    moodEvent.setLongitude(longitude);
                                    moodEvent.setLocationName(locationName);
                                    moodEvent.setPublic(isPublic);
                                    if (tempPath != null && !tempPath.isEmpty()) {
                                        moodEvent.setTempLocalImagePath(tempPath);
                                    }
                                    moodEvents.add(moodEvent);
                                }
                            }
                            listener.onSuccess(moodEvents);
                        } else {
                            Log.w(TAG, "Error getting mood events", task.getException());
                            listener.onFailure(task.getException().getMessage());
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
                                String socialSituation = document.getString("socialSituation");
                                String reason = document.getString("reason");
                                String userId = document.getString("userId");
                                String imageUrl = document.getString("imageUrl");
                                Double latitude = document.getDouble("latitude");
                                Double longitude = document.getDouble("longitude");
                                String locationName = document.getString("locationName");

                                // Handle missing isPublic field (default to true)
                                Boolean isPublic = document.getBoolean("isPublic");
                                if (isPublic == null) {
                                    isPublic = true; // Treat old moods as public
                                }

                                // Only add public moods
                                if (isPublic) {
                                    MoodEvent moodEvent = new MoodEvent(emotionalState, socialSituation, reason);
                                    moodEvent.setUserId(userId);
                                    moodEvent.setId(id.hashCode());
                                    moodEvent.setTimestamp(timestamp);
                                    moodEvent.setImageUrl(imageUrl);
                                    moodEvent.setPublic(isPublic);
                                    moodEvent.setLatitude(latitude);
                                    moodEvent.setLongitude(longitude);
                                    moodEvent.setLocationName(locationName);// Set the privacy status

                                    moodEvents.add(moodEvent);
                                }
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

        String tempPath = moodEvent.getTempLocalImagePath();
        if (tempPath != null && !tempPath.isEmpty()) {
            moodData.put("tempLocalImagePath", tempPath);
        } else {
            moodData.put("tempLocalImagePath", FieldValue.delete());
        }

        moodData.put("isPublic", moodEvent.isPublic());

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
                    if (listener != null) {
                        listener.onSuccess(moodEvent);
                    }
                })
                .addOnFailureListener(e -> {
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

    public void sendFollowRequest(String receiverId, OnFollowRequestListener listener) {
        // Fetch the sender's username
        getUsernameById(this.userId, new OnUsernameListener() {
            @Override
            public void onSuccess(String username) {
                createFollowRequest(receiverId, username, listener);
            }

            @Override
            public void onFailure(String fallbackName) {
                // Fallback to using userId as the name
                createFollowRequest(receiverId, userId, listener);
            }
        });
    }

    private void createFollowRequest(String receiverId, String senderName, OnFollowRequestListener listener) {
        Map<String, Object> request = new HashMap<>();
        request.put("senderId", this.userId);
        request.put("senderName", senderName);
        request.put("receiverId", receiverId);
        request.put("status", "pending");
        request.put("timestamp", FieldValue.serverTimestamp());

        db.collection("follow_requests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    public void getFollowRequests(OnFollowRequestsListener listener) {
        db.collection("follow_requests")
                .whereEqualTo("receiverId", this.userId)
                .whereEqualTo("status", "pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<FollowRequest> requests = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            FollowRequest request = document.toObject(FollowRequest.class);
                            request.setId(document.getId());
                            requests.add(request);
                        }
                        listener.onSuccess(requests);
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }


    public void createFollowRelationship(String targetUserId, String targetUsername, OnFollowRequestListener listener) {
        final String currentUserId = this.userId;
        // Get current user's username
        getUsernameById(this.userId, new OnUsernameListener() {
            @Override
            public void onSuccess(String currentUsername) {
                // Current user's following document
                WriteBatch batch = db.batch();
                DocumentReference followingRef = db.collection("users")
                        .document(currentUserId)
                        .collection("following")
                        .document(targetUserId);

                Map<String, Object> followingData = new HashMap<>();
                followingData.put("uid", targetUserId);
                followingData.put("username", targetUsername);
                followingData.put("timestamp", FieldValue.serverTimestamp());
                batch.set(followingRef, followingData);

                // Target user's followers document
                DocumentReference followersRef = db.collection("users")
                        .document(targetUserId)
                        .collection("followers")
                        .document(currentUserId);

                Map<String, Object> followersData = new HashMap<>();
                followersData.put("uid", currentUserId);
                followersData.put("username", currentUsername);
                followersData.put("timestamp", FieldValue.serverTimestamp());
                batch.set(followersRef, followersData);

                // Update counts
                DocumentReference targetUserRef = db.collection("users").document(targetUserId);
                DocumentReference currentUserRef = db.collection("users").document(userId);

                batch.update(targetUserRef, "followers", FieldValue.increment(1));
                batch.update(currentUserRef, "following", FieldValue.increment(1));

                batch.commit().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess();
                    } else {
                        listener.onFailure("Batch commit failed: " + task.getException().getMessage());
                    }
                });
            }

            @Override
            public void onFailure(String fallbackName) {
                listener.onFailure("Failed to resolve username");
            }
        });
    }


    public void getFollowedUsersMoodEvents(List<String> followedUserIds, boolean filterByWeek, String emotionFilter, OnMoodEventsListener listener) {
        if (followedUserIds.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        List<List<String>> chunks = partitionList(followedUserIds, 10);

        // Calculate timestamp for 7 days ago if needed
        Date oneWeekAgo = null;
        if (filterByWeek) {
            long oneWeekAgoMillis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
            oneWeekAgo = new Date(oneWeekAgoMillis);
        }

        for (List<String> chunk : chunks) {
            Query query = db.collection(COLLECTION_MOOD_EVENTS)
                    .whereIn("userId", chunk)
                    .orderBy("timestamp", Query.Direction.DESCENDING);

            // Add week filter
            if (filterByWeek && oneWeekAgo != null) {
                query = query.whereGreaterThanOrEqualTo("timestamp", oneWeekAgo);
            }

            // Add emotion filter
            if (emotionFilter != null && !emotionFilter.isEmpty()) {
                query = query.whereEqualTo("emotionalState", emotionFilter);
            }

            tasks.add(query.get());
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(querySnapshots -> {
                    List<MoodEvent> allEvents = new ArrayList<>();
                    for (Object result : querySnapshots) {
                        QuerySnapshot snapshot = (QuerySnapshot) result;
                        for (QueryDocumentSnapshot document : snapshot) {
                            MoodEvent event = parseDocumentToMoodEvent(document);
                            if (event.isPublic()) {
                                allEvents.add(event);
                            }
                        }
                    }
                    // Sort combined results by timestamp
                    allEvents.sort((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()));
                    listener.onSuccess(allEvents);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // Add this helper method
    private MoodEvent parseDocumentToMoodEvent(QueryDocumentSnapshot document) {
        String id = document.getId();
        Date timestamp = document.getDate("timestamp");
        String emotionalState = document.getString("emotionalState");
        String socialSituation = document.getString("socialSituation");
        String reason = document.getString("reason");
        String userId = document.getString("userId");
        String imageUrl = document.getString("imageUrl");
        Boolean isPublic = document.getBoolean("isPublic");
        if (isPublic == null) isPublic = true;

        // Get location data
        Double latitude = document.getDouble("latitude");
        Double longitude = document.getDouble("longitude");
        String locationName = document.getString("locationName");

        MoodEvent moodEvent = new MoodEvent(emotionalState, socialSituation, reason);
        moodEvent.setUserId(userId);
        moodEvent.setId(id.hashCode());
        moodEvent.setTimestamp(timestamp);
        moodEvent.setDocumentId(id);
        moodEvent.setImageUrl(imageUrl);
        moodEvent.setPublic(isPublic);
        moodEvent.setLatitude(latitude);
        moodEvent.setLongitude(longitude);
        moodEvent.setLocationName(locationName);

        return moodEvent;
    }

    // Add method to get followed user IDs
    public void getFollowedUserIds(OnFollowedUserIdsListener listener) {
        List<String> userIds = new ArrayList<>();
        CollectionReference followingRef = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection("following");

        followingRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String uid = doc.getString("uid");
                        if (uid != null) userIds.add(uid);
                    }

                    // Check if there are more documents to fetch
                    if (querySnapshot.size() >= 100) { // Firestore's default limit
                        fetchAllFollowedUsersPaginated(followingRef, querySnapshot, userIds, listener);
                    } else {
                        listener.onSuccess(userIds);
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    private void fetchAllFollowedUsersPaginated(CollectionReference ref, QuerySnapshot lastSnapshot,
                                                List<String> userIds, OnFollowedUserIdsListener listener) {
        Query nextQuery = ref.startAfter(lastSnapshot.getDocuments()
                .get(lastSnapshot.size() - 1));

        nextQuery.get().addOnSuccessListener(snapshot -> {
            for (QueryDocumentSnapshot doc : snapshot) {
                String uid = doc.getString("uid");
                if (uid != null) userIds.add(uid);
            }

            if (snapshot.size() >= 100) {
                fetchAllFollowedUsersPaginated(ref, snapshot, userIds, listener);
            } else {
                listener.onSuccess(userIds);
            }
        }).addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getComments(String moodEventId, OnCommentsListener listener) {
        db.collection("mood_events").document(moodEventId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Comment> comments = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Comment comment = document.toObject(Comment.class);
                            comment.setId(document.getId());
                            // Directly add comment without username resolution here
                            comments.add(comment);
                        }
                        listener.onSuccess(comments);
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    public void addComment(String moodEventId, Comment comment, OnCommentListener listener) {
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("text", comment.getText());
        commentData.put("userId", userId);
        commentData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("mood_events").document(moodEventId).collection("comments")
                .add(commentData)
                .addOnSuccessListener(documentReference -> {
                    comment.setId(documentReference.getId());
                    listener.onSuccess(comment);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void checkFollowStatus(String targetUserId, OnFollowCheckListener listener) {
        db.collection("users")
                .document(userId)
                .collection("following")
                .document(targetUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess(task.getResult().exists());
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }


    public void createCommentNotification(String receiverId, String moodEventId, String commentText, OnNotificationListener listener) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "COMMENT");
        notification.put("senderId", this.userId);
        notification.put("receiverId", receiverId);
        notification.put("moodEventId", moodEventId);
        notification.put("text", commentText);
        notification.put("timestamp", FieldValue.serverTimestamp());
        notification.put("read", false);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getCommentNotifications(OnNotificationsListener listener) {
        db.collection("notifications")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("type", "COMMENT")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Notification> notifications = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Notification notification = new Notification();
                            notification.setId(document.getId());
                            notification.setType(Notification.Type.COMMENT);
                            notification.setMessage(document.getString("text"));
                            notification.setSenderId(document.getString("senderId"));
                            notification.setTimestamp(document.getDate("timestamp").getTime());
                            notifications.add(notification);
                        }
                        listener.onSuccess(notifications);
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    public interface OnNotificationsListener {
        void onSuccess(List<Notification> notifications);
        void onFailure(String errorMessage);
    }

    public interface OnNotificationListener {
        void onSuccess();
        void onFailure(String error);
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

    public interface OnFollowRequestListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnFollowRequestsListener {
        void onSuccess(List<FollowRequest> requests);
        void onFailure(String error);
    }

    public interface OnFollowedUserIdsListener {
        void onSuccess(List<String> userIds);
        void onFailure(String errorMessage);
    }

    public interface OnUserInfoListener {
        void onSuccess(User user);
        void onFailure(String error);
    }
    private List<List<String>> partitionList(List<String> list, int chunkSize) {
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return chunks;
    }

    public interface OnCommentsListener {
        void onSuccess(List<Comment> comments);
        void onFailure(String errorMessage);
    }

    public interface OnCommentListener {
        void onSuccess(Comment comment);
        void onFailure(String errorMessage);
    }

    public interface OnFollowCheckListener {
        void onSuccess(boolean isFollowing);
        void onFailure(String errorMessage);
    }


    public interface OnFollowListListener {
        void onSuccess(List<User> users);
        void onFailure(String errorMessage);
    }

    public interface OnConversationsListener {
        void onSuccess(List<Conversation> conversations);
        void onFailure(String error);
    }

    public interface OnMessagesListener {
        void onMessagesReceived(List<Message> messages);

        void onFailure(String error);
    }



    public void getFollowRelations(String type, OnFollowListListener listener) {
        CollectionReference ref = db.collection("users")
                .document(userId)
                .collection(type); // "followers" or "following"

        ref.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<User> users = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String targetUserId = doc.getString("uid");
                    db.collection("users").document(targetUserId).get()
                            .addOnSuccessListener(userDoc -> {
                                User user = User.fromDocument(userDoc);
                                users.add(user);
                                if(users.size() == task.getResult().size()) {
                                    listener.onSuccess(users);
                                }
                            });
                }
                if(task.getResult().isEmpty()) listener.onSuccess(users);
            } else {
                listener.onFailure(task.getException().getMessage());
            }
        });
    }


    /**
     * Adds a mood event to Firestore with a specific document ID.
     * This method is used to ensure that the document ID remains consistent between offline and online states.
     * It allows you to generate a temporary ID locally and later sync the event with Firestore using the same ID.
     *
     * @param moodEvent The mood event to be added to Firestore.
     * @param documentId The document ID to use for the mood event. This ID should be consistent
     *                   between offline and online states
     * @param listener The listener to notify when the operation completes. Can be null if no callback is needed.
     */
    public void addMoodEventWithId(MoodEvent moodEvent, String documentId, final OnMoodEventListener listener) {
        Map<String, Object> moodData = new HashMap<>();
        moodData.put("userId", this.userId);
        moodData.put("timestamp", moodEvent.getTimestamp());
        moodData.put("emotionalState", moodEvent.getEmotionalState());
        moodData.put("isPublic", moodEvent.isPublic());

        if (moodEvent.getReason() != null && !moodEvent.getReason().isEmpty()) {
            moodData.put("reason", moodEvent.getReason());
        }

        if (moodEvent.getSocialSituation() != null && !moodEvent.getSocialSituation().isEmpty()) {
            moodData.put("socialSituation", moodEvent.getSocialSituation());
        }
        // Only add imageUrl if available (for online case)
        if (moodEvent.getImageUrl() != null && !moodEvent.getImageUrl().isEmpty()) {
            moodData.put("imageUrl", moodEvent.getImageUrl());
        }
        // Add the local image path and flag if image is saved locally
        if (moodEvent.getTempLocalImagePath() != null && !moodEvent.getTempLocalImagePath().isEmpty()) {
            moodData.put("tempLocalImagePath", moodEvent.getTempLocalImagePath());
        }

        if (moodEvent.getLocationName() != null && !moodEvent.getLocationName().isEmpty()) {
            moodData.put("locationName", moodEvent.getLocationName());
            moodData.put("latitude", moodEvent.getLatitude());
            moodData.put("longitude", moodEvent.getLongitude());
        }

        db.collection(COLLECTION_MOOD_EVENTS)
                .document(documentId)
                .set(moodData)
                .addOnSuccessListener(aVoid -> {
                    moodEvent.setDocumentId(documentId);
                    moodEvent.setId(documentId.hashCode());
                    if (listener != null) listener.onSuccess(moodEvent);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * Syncs a pending mood event with Firestore by fetching the event using its document ID.
     * This method is typically used when the app regains connectivity and needs to sync locally saved mood events
     * with Firestore. It fetches the mood event from Firestore and notifies the caller of the result.

     * @param documentId The document ID of the mood event to sync. Must not be null or empty.
     * @param listener The listener to notify when the operation completes. Can be null if no callback is needed.
     */
    public void syncPendingMoodEvent(String documentId, final OnMoodEventListener listener) {
        db.collection(COLLECTION_MOOD_EVENTS)
                .document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Convert the document to a MoodEvent object
                        MoodEvent moodEvent = documentSnapshot.toObject(MoodEvent.class);
                        if (moodEvent != null) {
                            moodEvent.setDocumentId(documentId);
                            listener.onSuccess(moodEvent);
                        } else {
                            listener.onFailure("Failed to convert document to MoodEvent.");
                        }
                    } else {
                        listener.onFailure("Mood event document does not exist.");
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void uploadLocalFileThenUpdateDocument(String localPath, String documentId, OnImageUploadListener listener) {
        // Create a reference similar to the one in UploadImageActivity
        File file = new File(localPath);
        String extension = localPath.substring(localPath.lastIndexOf("."));
        String fileName = "img_" + System.currentTimeMillis() + extension;
        StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                .child("user-mood-images").child(fileName);

        fileRef.putFile(Uri.fromFile(file))
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        // Update Firestore document with the download URL and remove the local image path
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("imageUrl", downloadUri.toString());
                        updateData.put("tempLocalImagePath", FieldValue.delete());
                        db.collection(COLLECTION_MOOD_EVENTS)
                                .document(documentId)
                                .update(updateData)
                                .addOnSuccessListener(aVoid -> listener.onImageUploadSuccess(downloadUri.toString()))
                                .addOnFailureListener(e -> listener.onImageUploadFailure(e.getMessage()));
                    }).addOnFailureListener(e -> listener.onImageUploadFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onImageUploadFailure(e.getMessage()));
    }

    public void updateUserProfileImage(String userId, String imageUrl,
                                       OnSuccessListener<Void> success,
                                       OnFailureListener failure) {
        db.collection("users").document(userId)
                .update("profileImageUrl", imageUrl)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }

    public void getUserInfo(String userId, OnUserInfoListener listener) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()){
                        User user = User.fromDocument(documentSnapshot);
                        listener.onSuccess(user);
                    } else {
                        listener.onFailure("User not found");
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void sendMessage(String conversationId, String text, String receiverId) {
        // Create message in subcollection
        Map<String, Object> message = new HashMap<>();
        message.put("text", text);
        message.put("senderId", userId);
        message.put("timestamp", FieldValue.serverTimestamp());

        // Update conversation document
        Map<String, Object> conversationUpdate = new HashMap<>();
        conversationUpdate.put("lastMessage", text);
        conversationUpdate.put("timestamp", FieldValue.serverTimestamp());
        conversationUpdate.put("participants", Arrays.asList(userId, receiverId));

        db.collection("conversations").document(conversationId)
                .set(conversationUpdate, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    db.collection("conversations").document(conversationId)
                            .collection("messages")
                            .add(message);
                });
    }

    public void getMessages(String conversationId, OnMessagesListener listener) {
        CollectionReference messagesRef = db.collection("conversations")
                .document(conversationId)
                .collection("messages");

        messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onFailure(error.getMessage());
                        return;
                    }

                    List<Message> messages = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Message message = new Message(
                                doc.getString("text"),
                                doc.getString("senderId"),
                                doc.getDate("timestamp")
                        );
                        message.setId(doc.getId());
                        messages.add(message);
                    }
                    listener.onMessagesReceived(messages);
                });
    }

    public void getConversations(OnConversationsListener listener) {
        db.collection("conversations")
                .whereArrayContains("participants", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Conversation> conversations = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Conversation conv = new Conversation();
                        conv.setId(doc.getId());
                        conv.setParticipants((List<String>) doc.get("participants"));
                        conv.setLastMessage(doc.getString("lastMessage"));
                        conv.setTimestamp(doc.getDate("timestamp"));
                        conversations.add(conv);
                    }
                    listener.onSuccess(conversations);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }


    /**
     * Get mood events from users that the current user is following, with optional filters
     * @param filterByWeek Filter to show only events from the past week
     * @param selectedEmotion Filter by specific emotion
     * @param searchReasonQuery Filter by reason text
     * @param listener Callback for the result
     */
    public void getFollowingMoodEvents(boolean filterByWeek, String selectedEmotion, String searchReasonQuery, OnMoodEventsListener listener) {
        getFollowingIds(new OnFollowingIdsListener() {
            @Override
            public void onSuccess(List<String> followingIds) {
                if (followingIds.isEmpty()) {
                    listener.onSuccess(new ArrayList<>());
                    return;
                }

                db.collection(COLLECTION_MOOD_EVENTS)
                    .whereIn("userId", followingIds)
                    .whereEqualTo("isPublic", true)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<MoodEvent> moodEvents = new ArrayList<>();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            MoodEvent moodEvent = MoodEvent.fromDocument(document);
                            boolean shouldInclude = true;

                            // Apply week filter
                            if (filterByWeek) {
                                long weekInMillis = 7 * 24 * 60 * 60 * 1000L;
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - moodEvent.getTimestamp().getTime() > weekInMillis) {
                                    shouldInclude = false;
                                }
                            }

                            // Apply emotion filter
                            if (selectedEmotion != null && !selectedEmotion.isEmpty()) {
                                if (!moodEvent.getEmotionalState().equals(selectedEmotion)) {
                                    shouldInclude = false;
                                }
                            }

                            // Apply reason search filter
                            if (searchReasonQuery != null && !searchReasonQuery.isEmpty()) {
                                String reason = moodEvent.getReason();
                                if (reason == null || !reason.toLowerCase().contains(searchReasonQuery.toLowerCase())) {
                                    shouldInclude = false;
                                }
                            }

                            if (shouldInclude) {
                                moodEvents.add(moodEvent);
                            }
                        }
                        listener.onSuccess(moodEvents);
                    })
                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
            }

            @Override
            public void onFailure(String error) {
                listener.onFailure(error);
            }
        });
    }

    public interface OnFollowingIdsListener {
        void onSuccess(List<String> followingIds);
        void onFailure(String error);
    }

    /**
     * Get the list of user IDs that the current user is following
     * @param listener Callback for the result
     */
    public void getFollowingIds(OnFollowingIdsListener listener) {
        db.collection("users")
            .document(userId)
            .collection("following")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<String> followingIds = new ArrayList<>();
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    String followedUserId = document.getString("uid");
                    if (followedUserId != null) {
                        followingIds.add(followedUserId);
                    }
                }
                listener.onSuccess(followingIds);
            })
            .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }
}