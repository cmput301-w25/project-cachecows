package com.example.feelink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {
    private static final String TAG = "PersonalProfileActivity";
    private ImageView profileImageView;
    private TextView usernameTextView, bioTextView;
    private String currentUserId;

    private RecyclerView recyclerMoodEvents;
    private MoodEventAdapter moodEventAdapter;
    private List<MoodEvent> moodEventsList;
    private TextView moodPostsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Initialize views
        profileImageView = findViewById(R.id.profileImage);
        usernameTextView = findViewById(R.id.username);
        bioTextView = findViewById(R.id.bio);
        moodPostsTextView = findViewById(R.id.moodPosts);
//        recyclerMoodEvents = findViewById(R.id.recyclerMoodEvents);
//        // Set up RecyclerView (as in FeedManagerActivity)
//        moodEventsList = new ArrayList<>();
//        moodEventAdapter = new MoodEventAdapter(moodEventsList, this);
//        recyclerMoodEvents.setLayoutManager(new LinearLayoutManager(this));
//        recyclerMoodEvents.setAdapter(moodEventAdapter);

//        fetchUserMoodEvents(currentUserId);

        ImageButton settingsButton = findViewById(R.id.settingsButton);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                Intent intent = new Intent(UserProfileActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Settings button not found in layout");
        }
        // Get current user ID
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch user data from Firestore
        fetchUserData(currentUserId);
        fetchTotalMoodEvents(currentUserId);
    }
    private void fetchTotalMoodEvents(String userId) {
        FirestoreManager firestoreManager = new FirestoreManager(userId);
        firestoreManager.getMoodEvents(new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                // Update UI with dynamic count
                moodPostsTextView.setText(String.valueOf(moodEvents.size()));
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error fetching moods: " + errorMessage);
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
                        // Handle the case where the user document doesn't exist
                        Toast.makeText(UserProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting document: ", e);
                    Toast.makeText(UserProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayUserData(DocumentSnapshot documentSnapshot) {
        String username = documentSnapshot.getString("username");
        String bio = documentSnapshot.getString("bio");
        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

        // Update UI with user data
        if (username != null) {
            usernameTextView.setText(username);
        }

        if (bio != null) {
            bioTextView.setText(bio);
        }

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this).load(profileImageUrl).into(profileImageView);
        }
    }

//    @SuppressLint("NotifyDataSetChanged")
//    private void fetchUserMoodEvents(String userId) {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        db.collection("mood_events")
//                .whereEqualTo("userId", userId)
//                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    moodEventsList.clear();
//                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
//                        MoodEvent moodEvent = doc.toObject(MoodEvent.class);
//                        moodEvent.setId(Long.parseLong(doc.getId()));
//                        moodEventsList.add(moodEvent);
//                    }
//                    moodEventAdapter.notifyDataSetChanged();
//
//                    if (moodEventsList.isEmpty()) {
//                        Toast.makeText(this, "No moods found", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(TAG, "Error fetching moods", e);
//                    Toast.makeText(this, "Failed to load moods", Toast.LENGTH_SHORT).show();
//                });
//    } tried to implement

}