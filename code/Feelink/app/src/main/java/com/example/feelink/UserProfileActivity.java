package com.example.feelink;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.widget.PopupMenu;

import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {
    private static final String TAG = "PersonalProfileActivity";
    private ImageView profileImageView;
    private TextView usernameTextView, bioTextView, followerCountTextView, followingCountTextView;
    private String currentUserId;
    private RecyclerView recyclerMoodEvents;
    private MoodEventAdapter moodEventAdapter;
    private List<MoodEvent> moodEventsList;
    private TextView moodPostsTextView;
    private FirestoreManager firestoreManager;
    private FirebaseAuth mAuth;
    private FloatingActionButton fabAddMood;

    private ToggleButton togglePrivacy;
    private boolean isPublicMode = true; // Default to public

    private boolean filterByWeek = false;
    private String selectedEmotion = null;
    private ConnectivityReceiver connectivityReceiver;
    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String docId = intent.getStringExtra("DOCUMENT_ID");
            removeAnimation(docId);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        // Initialize views
        profileImageView = findViewById(R.id.profileImage);
        usernameTextView = findViewById(R.id.username);
        bioTextView = findViewById(R.id.bio);
        moodPostsTextView = findViewById(R.id.moodPosts);
        fabAddMood = findViewById(R.id.fabAddMood);
        recyclerMoodEvents = findViewById(R.id.recyclerMoodEvents);
        togglePrivacy = findViewById(R.id.togglePrivacy);
        ImageButton filterButton = findViewById(R.id.filterButton);

        // Inside UserProfileActivity's onCreate() after initializing views
        ImageView navSearch = findViewById(R.id.navSearch);
        ImageView navHome = findViewById(R.id.navHome);
        ImageView navChats = findViewById(R.id.navChats);
        ImageView navMap = findViewById(R.id.navMap);
        TextView tvOfflineIndicator = findViewById(R.id.tvOfflineIndicator);

        // Set up connectivity receiver
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        connectivityReceiver = new ConnectivityReceiver(new ConnectivityReceiver.ConnectivityReceiverListener() {
            @Override
            public void onNetworkConnectionChanged(boolean isConnected) {
                ConnectivityReceiver.handleBanner(isConnected, tvOfflineIndicator, UserProfileActivity.this);
            }
        });
        registerReceiver(connectivityReceiver, filter);

        boolean initiallyConnected = ConnectivityReceiver.isNetworkAvailable(this);
        if (!initiallyConnected) {
            tvOfflineIndicator.setVisibility(View.VISIBLE);
            tvOfflineIndicator.setText(R.string.you_are_currently_offline);
        } else {
            tvOfflineIndicator.setVisibility(View.GONE);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(syncReceiver,
                new IntentFilter("MOOD_EVENT_SYNCED"));

        navSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        navHome.setOnClickListener(v -> startActivity(new Intent(this, FeedManagerActivity.class)));

        navChats.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));


        // Set up RecyclerView
        moodEventsList = new ArrayList<>();
        moodEventAdapter = new MoodEventAdapter(moodEventsList, this);
        moodEventAdapter.setMyMoodSection(true);
        followerCountTextView = findViewById(R.id.followerCount);
        followingCountTextView = findViewById(R.id.followingCount);
        recyclerMoodEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerMoodEvents.setAdapter(moodEventAdapter);

        // Initialize FirestoreManager
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestoreManager = new FirestoreManager(currentUserId);
        mAuth = FirebaseAuth.getInstance();

        ImageButton settingsButton = findViewById(R.id.settingsButton);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                Intent intent = new Intent(UserProfileActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Settings button not found in layout");
        }

        togglePrivacy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isPublicMode = !isChecked; // Toggle state matches text labels
            fetchUserMoodEvents(currentUserId);
        });

        filterButton.setOnClickListener(v -> showFilterMenu());


        fabAddMood.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                navigateToAddMood();
            } else {
                handleUnauthorizedAccess();
            }
        }
        );
        Button editProfileButton = findViewById(R.id.editProfileButton);
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, CreateAccount.class);
            intent.putExtra("EDIT_MODE", true);
            startActivity(intent);
        });
//         Get current user ID
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch user data from Firestore
        fetchUserData(currentUserId);
        fetchTotalMoodEvents(currentUserId);
        fetchUserMoodEvents(currentUserId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityReceiver != null) {
            unregisterReceiver(connectivityReceiver);
        }
    }
    private void showFilterMenu() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.filterButton));
        popup.getMenuInflater().inflate(R.menu.filter_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            // Time filters
            if (id == R.id.filter_week) {
                filterByWeek = true;
                selectedEmotion = null;
            } else if (id == R.id.filter_all) {
                filterByWeek = false;
                selectedEmotion = null;
            }
            // Emotional state filters
            else {
                filterByWeek = false;
                selectedEmotion = getEmotionFromId(id);
            }

            fetchUserMoodEvents(currentUserId);
            return true;
        });
        popup.show();
    }

    private String getEmotionFromId(int id) {
        if (id == R.id.filter_happy) return "Happy";
        if (id == R.id.filter_fear) return "Fear";
        if (id == R.id.filter_shame) return "Shame";
        if (id == R.id.filter_sad) return "Sad";
        if (id == R.id.filter_angry) return "Angry";
        if (id == R.id.filter_surprised) return "Surprised";
        if (id == R.id.filter_confused) return "Confused";
        if (id == R.id.filter_disgusted) return "Disgusted";
        return null;
    }


    @Override
    protected void onResume() {
        super.onResume();
        fetchUserData(currentUserId); // Refresh profile data
        fetchUserMoodEvents(currentUserId); // Refresh mood events

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
        firestoreManager.getMoodEvents(isPublicMode, filterByWeek,selectedEmotion, new FirestoreManager.OnMoodEventsListener()  {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                moodEventsList.clear();
                moodEventsList.addAll(moodEvents);
                moodEventAdapter.notifyDataSetChanged();

                if (moodEvents.isEmpty()) Toast.makeText(UserProfileActivity.this,
                        "No " + (isPublicMode ? "public" : "private") + " moods found",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error fetching moods: " + errorMessage);
                Toast.makeText(UserProfileActivity.this, "Failed to load moods", Toast.LENGTH_SHORT).show();
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
        User user = User.fromDocument(documentSnapshot);
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
        followerCountTextView.setText(String.valueOf(user.getFollowers()));
        followingCountTextView.setText(String.valueOf(user.getFollowing()));
    }

    private void navigateToAddMood() {
        Intent intent = new Intent(UserProfileActivity.this, AddMoodEventActivity.class);
        startActivity(intent);
    }

    private void handleUnauthorizedAccess() {
        // Redirect to login
        startActivity(new Intent(this, Login.class));
        finish();
    }

    private void removeAnimation(String docId) {
        int position = moodEventAdapter.findPositionById(docId);
        if (position != -1) {
            moodEventAdapter.notifyItemChanged(position);
        }
    }
}