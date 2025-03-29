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
import android.view.inputmethod.InputMethodManager;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private androidx.appcompat.widget.SearchView searchView;
    private ConnectivityReceiver connectivityReceiver;
    static boolean SKIP_AUTH_FOR_TESTING = false;
    static boolean SKIP_AUTH_FOR_TESTING_CREATE_ACCOUNT = false;
    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fetchUserMoodEvents(currentUserId);
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

        searchView = findViewById(R.id.searchView);

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

        findViewById(R.id.followersLayout).setOnClickListener(v -> openFollowList("followers"));
        findViewById(R.id.followingLayout).setOnClickListener(v -> openFollowList("following"));


        // Set up RecyclerView
        moodEventsList = new ArrayList<>();
        moodEventAdapter = new MoodEventAdapter(moodEventsList, this);
        moodEventAdapter.setMyMoodSection(true);
        moodEventAdapter.setPublicFeed(false);
        followerCountTextView = findViewById(R.id.followerCount);
        followingCountTextView = findViewById(R.id.followingCount);
        recyclerMoodEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerMoodEvents.setAdapter(moodEventAdapter);

        // Initialize FirestoreManager
        if (FirebaseAuth.getInstance().getCurrentUser() != null || SKIP_AUTH_FOR_TESTING) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : "test_user_id";
            firestoreManager = new FirestoreManager(currentUserId);
        } else {
            handleUnauthorizedAccess();
            return;
        }
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

        // Fetch user data from Firestore
        fetchUserData(currentUserId);
        fetchTotalMoodEvents(currentUserId);
        fetchUserMoodEvents(currentUserId);
    }

    private void openFollowList(String type) {
        Intent intent = new Intent(this, FollowListActivity.class);
        intent.putExtra("userId", currentUserId);
        intent.putExtra("type", type);
        startActivity(intent);
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

            if (id == R.id.filter_search_reason) {
                filterByWeek = false;
                selectedEmotion = null;
                searchView.setVisibility(View.VISIBLE);
                searchView.setQuery("", false);
                searchView.requestFocus();

                // Show keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
                }

                // Set up search listener
                searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterMoodEventsByReason(newText);
                        return false;
                    }
                });
                fetchUserMoodEvents(currentUserId);
            }
            else if (id == R.id.filter_week) {
                filterByWeek = true;
                selectedEmotion = null;
                searchView.setVisibility(View.GONE);
                searchView.setQuery("", false);
                fetchUserMoodEvents(currentUserId);
            }
            else if (id == R.id.filter_all) {
                filterByWeek = false;
                selectedEmotion = null;
                searchView.setVisibility(View.GONE);
                searchView.setQuery("", false);
                fetchUserMoodEvents(currentUserId);
            }
            else {
                filterByWeek = false;
                selectedEmotion = getEmotionFromId(id);
                searchView.setVisibility(View.GONE);
                searchView.setQuery("", false);
                fetchUserMoodEvents(currentUserId);
            }
            return true;
        });
        popup.show();
    }
    private void filterMoodEventsByReason(String query) {
        List<MoodEvent> filteredList = new ArrayList<>();
        String queryLower = query.toLowerCase().trim(); // Normalize the query
        if (queryLower.isEmpty()) {
            moodEventAdapter.updateMoodEvents(moodEventsList); // Show all if query is empty
            return;
        }

        // Regex to match exact word with word boundaries, case-insensitive
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(queryLower) + "\\b", Pattern.CASE_INSENSITIVE);

        for (MoodEvent event : moodEventsList) {
            if (event.getReason() != null) {
                Matcher matcher = pattern.matcher(event.getReason().toLowerCase());
                if (matcher.find()) {
                    filteredList.add(event);
                }
            }
        }
        moodEventAdapter.updateMoodEvents(filteredList);
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
        firestoreManager.getMoodEvents(isPublicMode, filterByWeek, selectedEmotion, new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                // Filter for test user if in testing mode
                if (getIntent().getBooleanExtra("TEST_MODE", false)) {
                    List<MoodEvent> filteredEvents = new ArrayList<>();
                    for (MoodEvent event : moodEvents) {
                        if (event.getUserId().equals("test_user_id")) {
                            filteredEvents.add(event);
                        }
                    }
                    moodEvents = filteredEvents;
                }

                moodEventsList.clear();
                moodEventsList.addAll(moodEvents);

                // Update adapter with new data
                moodEventAdapter.updateMoodEvents(moodEventsList);
                moodEventAdapter.notifyDataSetChanged();

                // Re-apply search filter if search is active
                if (searchView.getVisibility() == View.VISIBLE) {
                    String currentQuery = searchView.getQuery().toString();
                    filterMoodEventsByReason(currentQuery);
                }

                if (moodEvents.isEmpty()) {
                    Toast.makeText(UserProfileActivity.this,
                            "No " + (isPublicMode ? "public" : "private") + " moods found",
                            Toast.LENGTH_SHORT).show();
                }
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