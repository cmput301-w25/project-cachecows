package com.example.feelink;

import android.annotation.SuppressLint;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OtherUserProfileActivity extends AppCompatActivity {
    private static final String TAG = "OtherUserProfileActivity";
    private ImageView profileImageView;
    private TextView usernameTextView, bioTextView, followerCountTextView, followingCountTextView;
    private Button followButton;
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
        bioTextView = findViewById(R.id.bio);
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
    }
    private void fetchTotalMoodEvents(String userId) {
        // Use existing firestoreManager instance and filter public moods
        firestoreManager.getMoodEvents(true, new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                // Directly use filtered count from FirestoreManager
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

    private void displayUserData(DocumentSnapshot documentSnapshot) {
        String username = documentSnapshot.getString("username");
        String bio = documentSnapshot.getString("bio");
        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
        Long followers = documentSnapshot.getLong("followers");
        Long following = documentSnapshot.getLong("following");

        // Update UI with user data
        usernameTextView.setText(username);
        bioTextView.setText(bio);

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this).load(profileImageUrl).into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.ic_nav_profile);
        }

        // Update follower and following counts
        followerCountTextView.setText(followers != null ? followers.toString() : "0");
        followingCountTextView.setText(following != null ? following.toString() : "0");
    }

    private void checkIfFollowing() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(currentUserId)
                .collection("following").document(profileUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Current user is following this profile
                        followButton.setText("Unfollow");
                    } else {
                        // Current user is not following this profile
                        followButton.setText("Follow");
                    }
                });
    }

    private void toggleFollow() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if already following
        db.collection("users").document(currentUserId)
                .collection("following").document(profileUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Already following, so unfollow
                        unfollowUser();
                    } else {
                        // Not following, so follow
                        followUser();
                    }
                });
    }

    private void followUser() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Add to current user's following collection
        Map<String, Object> followingData = new HashMap<>();
        followingData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("users").document(currentUserId)
                .collection("following").document(profileUserId)
                .set(followingData)
                .addOnSuccessListener(aVoid -> {
                    // Add to profile user's followers collection
                    Map<String, Object> followerData = new HashMap<>();
                    followerData.put("timestamp", FieldValue.serverTimestamp());

                    db.collection("users").document(profileUserId)
                            .collection("followers").document(currentUserId)
                            .set(followerData)
                            .addOnSuccessListener(aVoid1 -> {
                                // Update UI
                                followButton.setText("Unfollow");
                                Toast.makeText(this, "Following user", Toast.LENGTH_SHORT).show();

                                // Increment followers count
                                db.collection("users").document(profileUserId)
                                        .update("followers", FieldValue.increment(1));

                                // Increment following count
                                db.collection("users").document(currentUserId)
                                        .update("following", FieldValue.increment(1));
                            });
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