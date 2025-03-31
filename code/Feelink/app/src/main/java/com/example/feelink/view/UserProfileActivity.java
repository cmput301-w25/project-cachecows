package com.example.feelink.view;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.feelink.controller.ConnectivityReceiver;
import com.example.feelink.controller.FirestoreManager;
import com.example.feelink.adapter.MoodEventAdapter;
import com.example.feelink.R;
import com.example.feelink.model.MoodEvent;
import com.example.feelink.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class UserProfileActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "PersonalProfileActivity";
    private ImageView profileImageView;
    private TextView usernameTextView, bioTextView, followerCountTextView, followingCountTextView;
    private String currentUserId;
    private RecyclerView recyclerMoodEvents;
    public MoodEventAdapter moodEventAdapter;
    private List<MoodEvent> moodEventsList;
    private TextView moodPostsTextView;
    private FirestoreManager firestoreManager;
    private FirebaseAuth mAuth;
    private FloatingActionButton fabAddMood;

    private ToggleButton togglePrivacy;
    boolean isPublicMode = true; // Default to public

    private boolean filterByWeek = false;
    private String selectedEmotion = null;
    private androidx.appcompat.widget.SearchView searchView;
    private ConnectivityReceiver connectivityReceiver;
    public static boolean SKIP_AUTH_FOR_TESTING = false;
    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fetchUserMoodEvents(currentUserId);
        }
    };

    private MapView mapView;
    @VisibleForTesting
    GoogleMap googleMap;
    List<Marker> currentMarkers = new ArrayList<>();
    private Map<String, Integer> moodIconMap = new HashMap<String, Integer>() {{
        put("Happy", R.drawable.ic_mood_happy);
        put("Sad", R.drawable.ic_mood_sad);
        put("Angry", R.drawable.ic_mood_angry);
        put("Excited", R.drawable.ic_mood_inspired);
        put("Tired", R.drawable.ic_mood_exhausted);
        put("Fear", R.drawable.ic_mood_fear);
        put("Shame", R.drawable.ic_mood_shame);
        put("Surprised", R.drawable.ic_mood_surprised);
        put("Confused", R.drawable.ic_mood_confused);
        put("Disgusted", R.drawable.ic_mood_disgusted);
    }};

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    private List<MoodEvent> originalMoodEventsList; // Add this field at the class level

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        profileImageView = findViewById(R.id.profileImage);
        usernameTextView = findViewById(R.id.username);
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
        originalMoodEventsList = new ArrayList<>(); // Initialize the original list
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

        // Set up map button click handler
        ImageView mapButton = findViewById(R.id.mapButton);
        if (mapButton != null) {
            mapButton.setOnClickListener(v -> {
                Intent intent = new Intent(UserProfileActivity.this, MoodMapActivity.class);
                intent.putExtra("userId", currentUserId);
                intent.putExtra("showMyMoods", true);
                // Pass the filtered mood events
                ArrayList<MoodEvent> filteredEvents = new ArrayList<>(moodEventsList);
                intent.putParcelableArrayListExtra("moodEvents", filteredEvents);
                startActivity(intent);
            });
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

        // Initialize MapView
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
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
        if (mapView != null) {
            mapView.onDestroy();
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
        String queryLower = query.toLowerCase().trim(); // Normalize the query
        if (queryLower.isEmpty()) {
            // If query is empty, show all events
            moodEventsList.clear();
            moodEventsList.addAll(originalMoodEventsList);
            moodEventAdapter.updateMoodEvents(moodEventsList);
            displayMoodEventsOnMap();
            return;
        }

        // Regex to match exact word with word boundaries, case-insensitive
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(queryLower) + "\\b", Pattern.CASE_INSENSITIVE);
        List<MoodEvent> filteredList = new ArrayList<>();

        for (MoodEvent event : originalMoodEventsList) {
            if (event.getReason() != null) {
                Matcher matcher = pattern.matcher(event.getReason().toLowerCase());
                if (matcher.find()) {
                    filteredList.add(event);
                }
            }
        }

        // Update the display list with filtered results
        moodEventsList.clear();
        moodEventsList.addAll(filteredList);
        moodEventAdapter.updateMoodEvents(filteredList);
        displayMoodEventsOnMap();
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
        if (mapView != null) {
            mapView.onResume();
        }
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

    @VisibleForTesting
    @SuppressLint("NotifyDataSetChanged")
    public void fetchUserMoodEvents(String userId) {
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

                // Update both lists
                originalMoodEventsList.clear();
                originalMoodEventsList.addAll(moodEvents);
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

                displayMoodEventsOnMap();
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

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Enable location button if permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        displayMoodEventsOnMap();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                currentLocation = location;
                                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12));
                            }
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(true);
                    getCurrentLocation();
                }
            } else {
                Toast.makeText(this, "Location permission is required to center the map", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayMoodEventsOnMap() {
        if (googleMap == null) return;

        // Clear existing markers
        for (Marker marker : currentMarkers) {
            marker.remove();
        }
        currentMarkers.clear();

        // Add markers for each mood event with location
        for (MoodEvent event : moodEventsList) {
            if (event.getLatitude() != null && event.getLongitude() != null) {
                LatLng position = new LatLng(event.getLatitude(), event.getLongitude());
                BitmapDescriptor icon = createMarkerIcon(event.getEmotionalState());

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title(event.getEmotionalState())
                        .snippet(event.getLocationName())
                        .icon(icon);

                Marker marker = googleMap.addMarker(markerOptions);
                if (marker != null) {
                    currentMarkers.add(marker);
                }
            }
        }

        // If there are markers and we have current location, adjust bounds to include current location
        if (!currentMarkers.isEmpty() && currentLocation != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
            for (Marker marker : currentMarkers) {
                builder.include(marker.getPosition());
            }
            LatLngBounds bounds = builder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
    }

    private BitmapDescriptor createMarkerIcon(String emotionalState) {
        Integer resourceId = moodIconMap.get(emotionalState);
        if (resourceId == null) {
            return BitmapDescriptorFactory.defaultMarker();
        }

        Drawable drawable = ContextCompat.getDrawable(this, resourceId);
        if (drawable == null) {
            return BitmapDescriptorFactory.defaultMarker();
        }

        drawable.setBounds(0, 0, 60, 60);
        Bitmap bitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    //for testing
    public List<Marker> getCurrentMarkers() {
        return currentMarkers;
    }

    public GoogleMap getGoogleMap() {
        return googleMap;
    }

}