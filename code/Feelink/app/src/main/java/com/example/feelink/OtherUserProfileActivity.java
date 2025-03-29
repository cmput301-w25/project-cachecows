package com.example.feelink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OtherUserProfileActivity extends AppCompatActivity {
    private static final String TAG = "OtherUserProfileActivity";
    private ImageView profileImageView;
    private TextView usernameTextView, followerCountTextView, followingCountTextView;
    private Button followButton, messageButton;
    private String currentUserId, profileUserId;
    private TextView moodPostsTextView;

    private RecyclerView recyclerMoodEvents;
    private MoodEventAdapter moodEventAdapter;
    private List<MoodEvent> moodEventsList;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.other_user_profiles);

        // Initialize views
        profileImageView = findViewById(R.id.profileImage);
        usernameTextView = findViewById(R.id.username);
        followerCountTextView = findViewById(R.id.followerCount);
        followingCountTextView = findViewById(R.id.followingCount);
        moodPostsTextView = findViewById(R.id.moodPosts);


        followButton = findViewById(R.id.followButton);

        // Get current user ID
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get the profile user ID from the intent
        profileUserId = getIntent().getStringExtra("userId");
        if (profileUserId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Initialize FirestoreManager with profile user's ID
        firestoreManager = new FirestoreManager(profileUserId);

        // Initialize RecyclerView
        recyclerMoodEvents = findViewById(R.id.recyclerMoodEvents); // Add this ID to your layout
        moodEventsList = new ArrayList<>();
        moodEventAdapter = new MoodEventAdapter(moodEventsList, this);
        recyclerMoodEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerMoodEvents.setAdapter(moodEventAdapter);

        // Load mood events
        fetchUserMoodEvents(profileUserId);

        // Fetch user data from Firestore
        fetchUserData(profileUserId);
        // Fetch total mood events from Firestore
        fetchTotalMoodEvents(profileUserId);


        // Check if the current user is already following the profile user
        checkIfFollowing();

        // Set follow button click listener
        followButton.setOnClickListener(v -> toggleFollow());
        messageButton = findViewById(R.id.btnMessage);
        messageButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("OTHER_USER_ID", profileUserId);
            startActivity(intent);
        });
    }
    private void fetchTotalMoodEvents(String userId) {
        FirestoreManager firestoreManager = new FirestoreManager(userId);
        // Pass null for showPublic to get ALL moods
        firestoreManager.getMoodEvents(null, new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                // Update UI with TOTAL count (public + private)
                moodPostsTextView.setText(String.valueOf(moodEvents.size()));
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error fetching moods: " + errorMessage);
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchUserMoodEvents(String userId) {
        // Get only public moods (including legacy)
        firestoreManager.getMoodEvents(true, new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                moodEventsList.clear();
                moodEventsList.addAll(moodEvents);
                moodEventAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error loading moods: " + errorMessage);
                Toast.makeText(OtherUserProfileActivity.this, "Failed to load moods", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserData(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        displayUserData(documentSnapshot);
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data: ", e);
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    // Update displayUserData()
    private void displayUserData(DocumentSnapshot documentSnapshot) {
        User user = User.fromDocument(documentSnapshot);
        usernameTextView.setText(user.getUsername());

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this).load(user.getProfileImageUrl()).into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.ic_nav_profile);
        }

        followerCountTextView.setText(String.valueOf(user.getFollowers()));
        followingCountTextView.setText(String.valueOf(user.getFollowing()));
    }

    private void checkIfFollowing() {
        // Check existing following status
        FirebaseFirestore.getInstance().collection("users")
                .document(currentUserId)
                .collection("following")
                .document(profileUserId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) return;

                    if (doc != null && doc.exists()) {
                        followButton.setText("Unfollow");
                    } else {
                        // Check for pending follow requests
                        FirebaseFirestore.getInstance().collection("follow_requests")
                                .whereEqualTo("senderId", currentUserId)
                                .whereEqualTo("receiverId", profileUserId)
                                .whereEqualTo("status", "pending")
                                .addSnapshotListener((querySnapshot, e) -> {
                                    if (e != null) return;

                                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                        followButton.setText("Requested");
                                    } else {
                                        followButton.setText("Follow");
                                    }
                                });
                    }
                });
    }

    private void toggleFollow() {
        String currentState = followButton.getText().toString();

        if (currentState.equals("Requested")) {
            cancelFollowRequest();
        } else if (currentState.equals("Follow")) {
            sendFollowRequest();
        } else if (currentState.equals("Unfollow")) {
            unfollowUser();
        }
    }

    private void sendFollowRequest() {
        FirestoreManager firestoreManager = new FirestoreManager(currentUserId);
        firestoreManager.sendFollowRequest(profileUserId, new FirestoreManager.OnFollowRequestListener() {
            @Override
            public void onSuccess() {
                followButton.setText("Requested");
                Toast.makeText(OtherUserProfileActivity.this, "Follow request sent", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(OtherUserProfileActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelFollowRequest() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("follow_requests")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", profileUserId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        followButton.setText("Follow");
                                        Toast.makeText(OtherUserProfileActivity.this, "Request canceled", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(OtherUserProfileActivity.this, "Failed to cancel request", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(OtherUserProfileActivity.this, "Error checking requests", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void followUser() {
        String targetUsername = usernameTextView.getText().toString();

        FirestoreManager firestoreManager = new FirestoreManager(currentUserId);
        firestoreManager.createFollowRelationship(profileUserId, targetUsername, new FirestoreManager.OnFollowRequestListener() {
            @Override
            public void onSuccess() {
                followButton.setText("Following");
                Toast.makeText(OtherUserProfileActivity.this, "Now following", Toast.LENGTH_SHORT).show();
                // Refresh counts
                fetchUserData(profileUserId);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(OtherUserProfileActivity.this, "Follow failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unfollowUser() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Remove from current user's following collection
        db.collection("users").document(currentUserId)
                .collection("following").document(profileUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from profile user's followers collection
                    db.collection("users").document(profileUserId)
                            .collection("followers").document(currentUserId)
                            .delete()
                            .addOnSuccessListener(aVoid1 -> {
                                // Update UI
                                followButton.setText("Follow");
                                Toast.makeText(this, "Unfollowed user", Toast.LENGTH_SHORT).show();

                                // Decrement followers count
                                db.collection("users").document(profileUserId)
                                        .update("followers", FieldValue.increment(-1));

                                // Decrement following count
                                db.collection("users").document(currentUserId)
                                        .update("following", FieldValue.increment(-1));
                            });
                });
    }
}