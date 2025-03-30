package com.example.feelink;



import android.app.AlertDialog;
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
import android.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;


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
import java.util.regex.Pattern;


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
    private static final String TAG = "FeedManagerActivity";
    private Button btnTheirMood, btnMyMood;
    private ImageButton filterButton;
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
    private boolean showThreeMostRecent = false;
    private boolean filterByWeek = false;
    private String selectedEmotion = null;
    private String searchReasonQuery = null;
    private SearchView searchView;
    private Spinner viewMapSpinner;


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
        setupMapViewSpinner();
        setupListeners();
        setupRecyclerView();
        updateTabSelection(false);

        // Default to "Their Mood" tab
        loadTheirMoodEvents();

        // Initialize navigation buttons
        ImageView navSearch = findViewById(R.id.navSearch);
        ImageView navProfile = findViewById(R.id.navProfile);
        ImageView navChats = findViewById(R.id.navChats);
        navChats.setOnClickListener(v -> navigateToNotifications());

        // Find navChats view
        ImageView navMap = findViewById(R.id.navMap);

        // Set click listener for Search navigation
        navSearch.setOnClickListener(v -> navigateToSearch());

        // Set click listener for Profile navigation (existing code)
        navProfile.setOnClickListener(v -> navigateToProfile());

        // Set up map button click listener
        navMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, MoodMapActivity.class);
            intent.putExtra("userId", currentUser.getUid());
            intent.putExtra("showMyMoods", false); // Show all moods, not just user's
            startActivity(intent);
        });

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

        // Add map button click handler
        ImageButton mapButton = findViewById(R.id.mapButton);
        if (mapButton != null) {
            mapButton.setOnClickListener(v -> {
                String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "test_user_id";
                Intent intent = new Intent(FeedManagerActivity.this, MoodMapActivity.class);
                intent.putExtra("userId", currentUserId);
                intent.putExtra("showMyMoods", false); // Show all moods, not just user's
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Map button not found in layout");
        }


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
        btnTheirMood = findViewById(R.id.btnAllMoods);
        btnMyMood = findViewById(R.id.btnFollowingMoods);
        filterButton = findViewById(R.id.btnFilter);
        searchView = findViewById(R.id.searchView);
        recyclerMoodEvents = findViewById(R.id.recyclerMoodEvents);
        fabAddMood = findViewById(R.id.fabAddMood);
        tvOfflineIndicator = findViewById(R.id.tvOfflineIndicator);
        findViewById(R.id.btnChat).setOnClickListener(v -> navigateToConversationsList());
        viewMapSpinner = findViewById(R.id.viewMapSpinner);
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

        filterButton.setOnClickListener(v -> {
            // Implement filter functionality
            // This could show a dialog with filter options
            showFilterMenu();
        });

        fabAddMood.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null || SKIP_AUTH_FOR_TESTING || SKIP_AUTH_FOR_TESTING_CREATE_ACCOUNT) {
                navigateToAddMood();
            } else {
                handleUnauthorizedAccess();
            }
        });
    }

    private void navigateToConversationsList() {
        startActivity(new Intent(FeedManagerActivity.this, ConversationsListActivity.class));
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

        // Add search view visibility control
        if (!showMyMood) { // When switching to "All Moods"
            searchView.setVisibility(View.GONE);
            searchView.setQuery("", false); // Clear search text
            searchReasonQuery = null; // Reset filter
        }

        viewMapSpinner.setVisibility(showMyMood ? View.VISIBLE : View.GONE);
        filterButton.setVisibility(showMyMood ? View.VISIBLE : View.GONE);
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
        adapter.setMyMoodSection(false);
        adapter.setPublicFeed(false);
        firestoreManager.getFollowedUserIds(new FirestoreManager.OnFollowedUserIdsListener() {
            @Override
            public void onSuccess(List<String> followedUserIds) {
                Log.d("FeedManager", "Retrieved followed user IDs: " + followedUserIds.size());
                adapter.setMyMoodSection(false);

                firestoreManager.getFollowedUsersMoodEvents(followedUserIds, filterByWeek, selectedEmotion,
                        new FirestoreManager.OnMoodEventsListener() {
                            @Override
                            public void onSuccess(List<MoodEvent> moodEvents) {
                                Log.d("FeedManager", "Initial followed moods count: " + moodEvents.size());

                                // Apply client-side filters
                                if (searchReasonQuery != null && !searchReasonQuery.isEmpty()) {
                                    Log.d("FeedManager", "Applying reason filter: " + searchReasonQuery);
                                    moodEvents = filterByReason(moodEvents, searchReasonQuery);
                                    Log.d("FeedManager", "Post-reason filter count: " + moodEvents.size());
                                }

                                // Apply 3 most recent if needed
                                if (showThreeMostRecent) {
                                    int originalSize = moodEvents.size();
                                    moodEvents = moodEvents.size() > 3 ?
                                            moodEvents.subList(0, 3) : moodEvents;
                                    Log.d("FeedManager", "Showing " + moodEvents.size() +
                                            "/" + originalSize + " recent moods");
                                }

                                adapter.updateMoodEvents(moodEvents);
                                checkEmptyState(moodEvents);
                                Log.d("FeedManager", "Final displayed moods: " + moodEvents.size());
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e("FeedManager", "Error getting followed moods: " + errorMessage);
                                adapter.updateMoodEvents(new ArrayList<>());
                                checkEmptyState(new ArrayList<>());
                                Toast.makeText(FeedManagerActivity.this,
                                        "Failed to load moods",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("FeedManager", "Error getting followed user IDs: " + errorMessage);
                adapter.updateMoodEvents(new ArrayList<>());
                checkEmptyState(new ArrayList<>());
                Toast.makeText(FeedManagerActivity.this,
                        "Failed to load followed users",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTheirMoodEvents() {
        adapter.setMyMoodSection(false);
        adapter.setPublicFeed(true);
        loadSharedMoodEvents();
    }


    // Add this method to map menu IDs to emotion strings
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

    // Add this method to filter by reason text
    private List<MoodEvent> filterByReason(List<MoodEvent> events, String query) {
        List<MoodEvent> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        String[] searchTerms = lowerQuery.split("\\s+");

        for (MoodEvent event : events) {
            if (event.getReason() != null) {
                String reason = event.getReason().toLowerCase();

                // Check if any search term appears as whole word
                for (String term : searchTerms) {
                    if (!term.isEmpty() &&
                            reason.matches(".*\\b" + Pattern.quote(term) + "\\b.*")) {
                        filtered.add(event);
                        break; // Add once if any term matches
                    }
                }
            }
        }
        return filtered;
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
        if (tvEmptyState == null) return;

        if (moodEvents.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(isShowingMyMood ?
                    "No public moods from people you follow" :
                    "No shared moods available");
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    // Modified showFilterMenu
    private void showFilterMenu() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.btnFilter));
        popup.getMenuInflater().inflate(R.menu.feed_filter_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            // Handle all filters
            if (id == R.id.filter_three_recent) {
                showThreeMostRecent = true;
                filterByWeek = false;
                selectedEmotion = null;
                searchReasonQuery = null;
                searchView.setVisibility(View.GONE);
            } else if (id == R.id.filter_week) {
                showThreeMostRecent = false;
                filterByWeek = true;
                selectedEmotion = null;
                searchReasonQuery = null;
                searchView.setVisibility(View.GONE);
            } else if (id == R.id.filter_all) {
                showThreeMostRecent = false;
                filterByWeek = false;
                selectedEmotion = null;
                searchReasonQuery = null;
                searchView.setVisibility(View.GONE);
            } else if (id == R.id.filter_search_reason) {
                showThreeMostRecent = false;
                filterByWeek = false;
                selectedEmotion = null;
                searchView.setVisibility(View.VISIBLE);
                searchView.setQuery("", false);
                searchView.requestFocus();

                // Set up search listener
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        searchReasonQuery = newText.trim();
                        loadCurrentFeed();
                        return false;
                    }
                });
            } else {
                // Emotion filters
                showThreeMostRecent = false;
                filterByWeek = false;
                selectedEmotion = getEmotionFromId(id);
                searchReasonQuery = null;
                searchView.setVisibility(View.GONE);
            }

            loadCurrentFeed();
            return true;
        });
        popup.show();
    }
    private void loadCurrentFeed() {
        if (isShowingMyMood) {
            loadMyMoodEvents();
        } else {
            loadSharedMoodEvents();
        }
    }

//    private void handleSearchReason() {
//        SearchView searchView = new SearchView(this);
//        AlertDialog.Builder builder = new AlertDialog.Builder(this)
//                .setTitle("Search by Exact Words")
//                .setView(searchView)
//                .setPositiveButton("Search", (dialog, which) -> {
//                    // Clear other filters when searching
//                    searchReasonQuery = searchView.getQuery().toString().trim();
//                    showThreeMostRecent = false;
//                    filterByWeek = false;
//                    selectedEmotion = null;
//                    loadMyMoodEvents();
//                })
//                .setNegativeButton("Cancel", (dialog, which) -> {
//                    searchReasonQuery = null;
//                    loadMyMoodEvents();
//                });
//
//        builder.show();
//    }
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
    private void navigateToNotifications(){
        startActivity(new Intent(FeedManagerActivity.this, NotificationsActivity.class));
    }

    private void setupMapViewSpinner() {
        Spinner viewMapSpinner = findViewById(R.id.viewMapSpinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.map_view_options, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        viewMapSpinner.setAdapter(spinnerAdapter);

        viewMapSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // If not "View on Map"
                    String currentUserId = mAuth.getCurrentUser() != null ? 
                            mAuth.getCurrentUser().getUid() : "test_user_id";
                    Intent intent = new Intent(FeedManagerActivity.this, MoodMapActivity.class);
                    intent.putExtra("userId", currentUserId);
                    intent.putExtra("showMyMoods", false);

                    if (position == 1) { // Following Moods
                        // Only pass filters for Following Moods view
                        intent.putExtra("mapViewType", "following");
                        intent.putExtra("filterByWeek", filterByWeek);
                        intent.putExtra("selectedEmotion", selectedEmotion);
                        intent.putExtra("searchReasonQuery", searchReasonQuery);
                        
                        // Pass the current mood events list from the MoodEventAdapter
                        if (adapter != null && adapter.getCurrentMoodEvents() != null) {
                            ArrayList<MoodEvent> currentMoodEvents = new ArrayList<>(adapter.getCurrentMoodEvents());
                            intent.putParcelableArrayListExtra("moodEvents", currentMoodEvents);
                        }
                    } else { // Nearby Moods
                        intent.putExtra("mapViewType", "nearby");
                        // Don't pass any filters for nearby moods
                    }

                    startActivity(intent);
                    // Reset spinner to "View on Map"
                    viewMapSpinner.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }


}