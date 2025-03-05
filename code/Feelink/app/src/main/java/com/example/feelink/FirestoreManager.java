package com.example.feelink;

import android.util.Log;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Tasks;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager class for all Firestore database operations
 */
public class FirestoreManager {
    private static final String TAG = "FirestoreManager";

    // Collection names
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_MOOD_EVENTS = "mood_events";

    // Default user ID (until auth system is implemented)
    private String userId; // Add this

    private FirebaseFirestore db;

    public FirestoreManager(String userId) { // Modified constructor
        db = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    /**
     * Add a new mood event to Firestore
     * @param moodEvent The mood event to add
     * @param listener Callback for success/failure
     */
    public void addMoodEvent(MoodEvent moodEvent, final OnMoodEventListener listener) {
        // Convert MoodEvent to Map
        Map<String, Object> moodData = new HashMap<>();
        moodData.put("userId", this.userId);
        moodData.put("timestamp", moodEvent.getTimestamp());
        moodData.put("emotionalState", moodEvent.getEmotionalState());

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
     * Get all mood events for the current user
     * @param listener Callback with the list of mood events
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

                                MoodEvent moodEvent = new MoodEvent(emotionalState, trigger, socialSituation, reason);
                                moodEvent.setUserId(userId);
                                moodEvent.setId(id.hashCode());
                                moodEvent.setTimestamp(timestamp);

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
     * Get mood events shared by other users
     * @param listener Callback with the list of mood events
     */
    public void getSharedMoodEvents(final OnMoodEventsListener listener) {
        // Query all mood events not belonging to current user
        db.collection(COLLECTION_MOOD_EVENTS)
                .whereNotEqualTo("userId", this.userId)
//                .orderBy("userId")  // Required for the not equals query to work
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


                                MoodEvent moodEvent = new MoodEvent(emotionalState, trigger, socialSituation, reason);
                                moodEvent.setUserId(userId);
                                moodEvent.setId(id.hashCode());
                                moodEvent.setTimestamp(timestamp);

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
     * Delete a mood event
     * @param moodEventId The ID of the mood event to delete
     * @param listener Callback for success/failure
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
     * Get a username from Firestore based on userId
     * @param userId The ID of the user to look up
     * @param callback Callback with the username
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

    // Add this interface to the FirestoreManager class
    public interface OnDeleteListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    // Callback interfaces
    public interface OnMoodEventListener {
        void onSuccess(MoodEvent moodEvent);
        void onFailure(String errorMessage);
    }

    public interface OnMoodEventsListener {
        void onSuccess(List<MoodEvent> moodEvents);
        void onFailure(String errorMessage);
    }
    public interface OnUsernameListener {
        void onSuccess(String username);
        void onFailure(String fallbackName);
    }
}