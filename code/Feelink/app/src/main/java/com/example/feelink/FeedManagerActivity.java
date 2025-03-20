package com.example.feelink;



import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;


/**
 * Main activity for managing and displaying mood event feeds
 *
 * <p>Handles:
 * <ul>
 *   <li>Tabbed navigation between personal/shared moods</li>
 *   <li>RecyclerView management with MoodEventAdapter</li>
 *   <li>Firestore data synchronization</li>
 *   <li>Add mood event FAB integration</li>
 * </ul>
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 1.04.01.01 - Mood event display UI</li>
 *   <li>US 1.04.01.02 - Mood event data binding</li>
 *   <li>US 1.03.01.02 - Centralized asset integration</li>
 *   <li>US 1.06.01.01 - Delete UI integration</li>
 * </ul>
 * @see MoodEventAdapter
 * @see FirestoreManager
 * @see AddMoodEventActivity
 */
public class FeedManagerActivity extends AppCompatActivity {
    private Button btnTheirMood, btnMyMood;
    private ImageButton btnFilter;
    private TextView tvShareInfo, tvEmptyState, tvOfflineIndicator;
    private RecyclerView recyclerMoodEvents;
    private FloatingActionButton fabAddMood;
    private FirebaseAuth mAuth;
    private FirestoreManager firestoreManager;
    private MoodEventAdapter adapter;
    private boolean isShowingMyMood = false;

    static boolean SKIP_AUTH_FOR_TESTING = false;
    private static boolean SKIP_AUTH_FOR_TESTING_CREATE_ACCOUNT = false;
    private ConnectivityReceiver connectivityReceiver;

    /**
     * Initializes feed UI and authentication checks
     *
     * <p>Key operations:
     * <ol>
     *   <li>Edge-to-edge display configuration</li>
     *   <li>Firebase authentication verification</li>
     *   <li>View initialization</li>
     *   <li>Default data loading</li>
     * </ol>
     *
     * @param savedInstanceState Persisted state data
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed_manager);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null || SKIP_AUTH_FOR_TESTING || SKIP_AUTH_FOR_TESTING_CREATE_ACCOUNT) {
            // Initialize with test UID if needed
            String uid = currentUser != null ? currentUser.getUid() : "test_user_id";
            firestoreManager = new FirestoreManager(uid);
        } else {
            // Handle user not logged in
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        setupRecyclerView();

        // Default to "Their Mood" tab
        loadTheirMoodEvents();

        // Initialize navigation buttons
        ImageView navSearch = findViewById(R.id.navSearch);
        ImageView navProfile = findViewById(R.id.navProfile);

        // Set click listener for Search navigation
        navSearch.setOnClickListener(v -> navigateToSearch());

        // Set click listener for Profile navigation (existing code)
        navProfile.setOnClickListener(v -> navigateToProfile());

        // Check initial network state
        boolean isConnected = ConnectivityReceiver.isNetworkAvailable(this);
        if (!isConnected) {
            tvOfflineIndicator.setVisibility(View.VISIBLE);
        } else {
            tvOfflineIndicator.setVisibility(View.GONE);
        }

        // Register ConnectivityReceiver
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        connectivityReceiver = new ConnectivityReceiver(new ConnectivityReceiver.ConnectivityReceiverListener() {
            @Override
            public void onNetworkConnectionChanged(boolean isConnected) {
                if (isConnected) {
                    //Hide the offline indicator, "Back Online" will not show up until the first occurrence of offline indicator
                    //since we require network to authenticate in the first place
                    tvOfflineIndicator.setText(R.string.back_online);
                    tvOfflineIndicator.setBackgroundColor(getResources().getColor(R.color.online_indicator_background));
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // reset text/background for next offline stater
                            tvOfflineIndicator.setText(R.string.you_are_currently_offline);
                            tvOfflineIndicator.setBackgroundColor(getResources().getColor(R.color.offline_indicator_background));
                            tvOfflineIndicator.setVisibility(View.GONE);
                        }
                    }, 3000); // 3 sec

                    // Refresh the adapter to update the UI
                    if (isShowingMyMood) {
                        loadMyMoodEvents();
                    } else {
                        loadTheirMoodEvents();
                    }
                } else {
                    //Show the offline indicator
                    tvOfflineIndicator.setText(R.string.you_are_currently_offline);
                    tvOfflineIndicator.setBackgroundColor(getResources().getColor(R.color.offline_indicator_background)); // Use the original background color
                    tvOfflineIndicator.setVisibility(View.VISIBLE);
                }
            }
        });
        registerReceiver(connectivityReceiver, intentFilter);

    }

    /**
     * Refreshes feed data on activity resume
     *
     * <p>Ensures data consistency when returning from:
     * <ul>
     *   <li>AddMoodEventActivity</li>
     *   <li>Edit operations</li>
     *   <li>Authentication flows</li>
     * </ul>
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        if (isShowingMyMood) {
            loadMyMoodEvents();
        } else {
            loadTheirMoodEvents();
        }
    }

    /**
     * Unregisters ConnectivityReceiver on activity destroy
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityReceiver != null) {
            unregisterReceiver(connectivityReceiver);
        }
    }

    /**
     * Binds view references from layout XML
     *
     * <p>Initializes:
     * <ul>
     *   <li>Tab buttons</li>
     *   <li>RecyclerView</li>
     *   <li>Floating action button</li>
     *   <li>Empty state indicators</li>
     * </ul>
     */
    private void initializeViews() {
        btnTheirMood = findViewById(R.id.btnTheirMood);
        btnMyMood = findViewById(R.id.btnMyMood);
        btnFilter = findViewById(R.id.btnFilter);
        tvShareInfo = findViewById(R.id.tvShareInfo);
        recyclerMoodEvents = findViewById(R.id.recyclerMoodEvents);
        fabAddMood = findViewById(R.id.fabAddMood);
        tvOfflineIndicator = findViewById(R.id.tvOfflineIndicator);
    }

    /**
     * Configures click listeners for UI components
     *
     * <p>Handles:
     * <ul>
     *   <li>Tab selection changes</li>
     *   <li>Filter button placeholder</li>
     *   <li>Add mood FAB navigation</li>
     * </ul>
     */
    private void setupListeners() {
        btnTheirMood.setOnClickListener(v -> {
            updateTabSelection(false);
            loadTheirMoodEvents();
        });

        btnMyMood.setOnClickListener(v -> {
            updateTabSelection(true);
            loadMyMoodEvents();
        });

        btnFilter.setOnClickListener(v -> {
            // Implement filter functionality
            // This could show a dialog with filter options
            showFilterOptions();
        });

        fabAddMood.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null || SKIP_AUTH_FOR_TESTING || SKIP_AUTH_FOR_TESTING_CREATE_ACCOUNT) {
                navigateToAddMood();
            } else {
                handleUnauthorizedAccess();
            }
        });
    }

    /**
     * Initializes RecyclerView with linear layout and adapter
     *
     * <p>Configures:
     * <ul>
     *   <li>LayoutManager</li>
     *   <li>MoodEventAdapter instance</li>
     *   <li>Empty initial dataset</li>
     * </ul>
     */
    private void setupRecyclerView() {
        recyclerMoodEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MoodEventAdapter(new ArrayList<>(), this);
        recyclerMoodEvents.setAdapter(adapter);
    }

    /**
     * Updates UI state for tab selection
     *
     * @param showMyMood True to activate "My Mood" tab styling
     *
     * <p>Implements visual feedback through:
     * <ul>
     *   <li>Background tint changes</li>
     *   <li>Text color updates</li>
     *   <li>Info text visibility</li>
     * </ul>
     */
    private void updateTabSelection(boolean showMyMood) {
        isShowingMyMood = showMyMood;
        adapter.setMyMoodSection(showMyMood);
        // Get color state lists for selected and unselected states
        ColorStateList selectedColor = ColorStateList.valueOf(getResources().getColor(R.color.selected_tab_color)); // Replace with your selected color
        ColorStateList unselectedColor = ColorStateList.valueOf(getResources().getColor(R.color.unselected_tab_color)); // Replace with your unselected color

        if (showMyMood) {
            // Set My Mood button as selected
            btnMyMood.setBackgroundTintList(selectedColor);
            btnMyMood.setTextColor(getResources().getColor(android.R.color.white));

            // Set Their Mood button as unselected
            btnTheirMood.setBackgroundTintList(unselectedColor);
            btnTheirMood.setTextColor(getResources().getColor(android.R.color.black));

            // Hide the share info TextView
        } else {
            // Set Their Mood button as selected
            btnTheirMood.setBackgroundTintList(selectedColor);
            btnTheirMood.setTextColor(getResources().getColor(android.R.color.white));

            // Set My Mood button as unselected
            btnMyMood.setBackgroundTintList(unselectedColor);
            btnMyMood.setTextColor(getResources().getColor(android.R.color.black));

            // Show the share info TextView
        }
    }

    /**
     * Loads current user's mood events from Firestore
     *
     * <p>Implements US 1.04.01.02 requirements through:
     * <ol>
     *   <li>Firestore query by user ID</li>
     *   <li>Chronological ordering</li>
     *   <li>Adapter data updates</li>
     * </ol>
     */
    private void loadMyMoodEvents() {
        firestoreManager.getMoodEvents(new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                Log.d("FeedManagerActivity", "Fetched mood events: " + moodEvents.size());
                adapter.updateMoodEvents(moodEvents);
                checkEmptyState(moodEvents);
            }

            @Override
            public void onFailure(String errorMessage) {
                // Handle error
                Log.e("FeedManagerActivity", "Failed to fetch mood events: " + errorMessage);
                adapter.updateMoodEvents(new ArrayList<>());
                checkEmptyState(new ArrayList<>());
            }
        });
    }

    private void loadTheirMoodEvents() {
        // This would need to be implemented in FirestoreManager to get other users' moods
        // For now, we'll use a placeholder method that could be added to FirestoreManager
        loadSharedMoodEvents();
    }

    /**
     * Loads shared mood events from other users
     *
     * <p>Implements social feed functionality through:
     * <ul>
     *   <li>Firestore exclusion query</li>
     *   <li>Multi-order sorting</li>
     *   <li>Anonymous user handling</li>
     * </ul>
     */
    private void loadSharedMoodEvents() {
        // Placeholder - this should be implemented in FirestoreManager
        // to fetch mood events shared by other users
        firestoreManager.getSharedMoodEvents(new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                adapter.updateMoodEvents(moodEvents);
                checkEmptyState(moodEvents);
            }

            @Override
            public void onFailure(String errorMessage) {
                // Handle error
                adapter.updateMoodEvents(new ArrayList<>());
                checkEmptyState(new ArrayList<>());
            }
        });
    }

    private void checkEmptyState(List<MoodEvent> moodEvents) {
        // Todo: Implement empty state handling
    }

    private void showFilterOptions() {
        // Todo: Implement filter options dialog
    }

    /**
     * Navigates to AddMoodEventActivity
     *
     * <p>Preserves authentication state through FirebaseUser instance
     */
    private void navigateToAddMood() {
        Intent intent = new Intent(FeedManagerActivity.this, AddMoodEventActivity.class);
        startActivity(intent);
    }

    /**
     * Handles unauthorized access scenarios
     *
     * <p>Redirects to login screen when:
     * <ul>
     *   <li>User session expires</li>
     *   <li>Authentication tokens invalid</li>
     * </ul>
     */
    private void handleUnauthorizedAccess() {
        // Redirect to login
        startActivity(new Intent(this, Login.class));
        finish();
    }

    // Inside FeedManagerActivity.java
    // Inside FeedManagerActivity.java
    private void navigateToProfile() {
        Intent intent = new Intent(FeedManagerActivity.this, UserProfileActivity.class);
        startActivity(intent);
    }

    private void navigateToSearch() {
        Intent intent = new Intent(FeedManagerActivity.this, SearchActivity.class);
        startActivity(intent);
    }

}