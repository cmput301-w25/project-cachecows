package com.example.feelink;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoodMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private MapView mapView;
    private GoogleMap googleMap;
    private FirestoreManager firestoreManager;
    private String userId;
    private Map<String, Marker> moodMarkers = new HashMap<>();
    private RadioGroup filterRadioGroup;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float NEARBY_DISTANCE_KM = 5.0f;
    private ArrayList<MoodEvent> moodEventsList;
    private List<Marker> currentMarkers = new ArrayList<>();
    private static final String TAG = "MoodMapActivity";
    private static final int REQUEST_CHECK_SETTINGS = 2;
    private static final float DEFAULT_ZOOM = 13f;  // Shows roughly 5km radius
    private boolean filterByWeek = false;
    private String selectedEmotion = null;
    private String searchReasonQuery = null;

    // Map mood types to marker colors
    private final Map<String, Float> moodColors = new HashMap<String, Float>() {{
        put("Happy", BitmapDescriptorFactory.HUE_GREEN);
        put("Sad", BitmapDescriptorFactory.HUE_BLUE);
        put("Angry", BitmapDescriptorFactory.HUE_RED);
        put("Surprised", BitmapDescriptorFactory.HUE_YELLOW);
        put("Confused", BitmapDescriptorFactory.HUE_ORANGE);
        put("Disgusted", BitmapDescriptorFactory.HUE_VIOLET);
        put("Fear", BitmapDescriptorFactory.HUE_MAGENTA);
        put("Shame", BitmapDescriptorFactory.HUE_ROSE);
    }};

    // Map mood types to drawable resources
    private final Map<String, Integer> moodIconMap = new HashMap<String, Integer>() {{
        put("Happy", R.drawable.ic_mood_happy);
        put("Sad", R.drawable.ic_mood_sad);
        put("Angry", R.drawable.ic_mood_angry);
        put("Surprised", R.drawable.ic_mood_surprised);
        put("Confused", R.drawable.ic_mood_confused);
        put("Disgusted", R.drawable.ic_mood_disgusted);
        put("Fear", R.drawable.ic_mood_fear);
        put("Shame", R.drawable.ic_mood_shame);
    }};

    // Cache for marker icons
    private final Map<String, BitmapDescriptor> markerIcons = new HashMap<>();

    private BitmapDescriptor createMarkerIcon(String emotionalState) {
        // Check if we already have this marker icon cached
        if (markerIcons.containsKey(emotionalState)) {
            return markerIcons.get(emotionalState);
        }

        // Get the drawable resource for this emotional state
        Integer drawableRes = moodIconMap.get(emotionalState);
        if (drawableRes == null) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        }

        // Convert drawable to bitmap
        Drawable drawable = ContextCompat.getDrawable(this, drawableRes);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, 100, 100);
        drawable.draw(canvas);

        // Convert to BitmapDescriptor
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(bitmap);
        markerIcons.put(emotionalState, icon);
        return icon;
    }

    private BroadcastReceiver moodUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Reload the appropriate map view when a new mood is added
            if (intent.getAction() != null && intent.getAction().equals("MOOD_EVENT_ADDED")) {
                String mapViewType = getIntent().getStringExtra("mapViewType");
                boolean showMyMoods = getIntent().getBooleanExtra("showMyMoods", false);
                
                if ("nearby".equals(mapViewType)) {
                    loadNearbyFollowingMoods();
                } else if (showMyMoods) {
                    loadMyMoods();
                } else {
                    loadFollowingMoods();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_map);

        // Register broadcast receiver for mood updates
        IntentFilter filter = new IntentFilter("MOOD_EVENT_ADDED");
        LocalBroadcastManager.getInstance(this).registerReceiver(moodUpdateReceiver, filter);

        // Get userId from intent
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            // If no userId provided, get from current user
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            userId = currentUser != null ? currentUser.getUid() : "test_user_id";
        }

        // Initialize FirestoreManager with userId
        firestoreManager = new FirestoreManager(userId);

        // Get filter states from intent
        filterByWeek = getIntent().getBooleanExtra("filterByWeek", false);
        selectedEmotion = getIntent().getStringExtra("selectedEmotion");
        searchReasonQuery = getIntent().getStringExtra("searchReasonQuery");

        // Initialize location services first
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize map
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Get passed mood events
        moodEventsList = getIntent().getParcelableArrayListExtra("moodEvents");
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void loadAppropriateEvents() {
        boolean showMyMoods = getIntent().getBooleanExtra("showMyMoods", false);
        String mapViewType = getIntent().getStringExtra("mapViewType");
        
        if (moodEventsList != null && !moodEventsList.isEmpty()) {
            displayMoodEventsOnMap();
        } else if (showMyMoods) {
            loadMyMoods();
        } else if ("nearby".equals(mapViewType)) {
            if (currentLocation != null) {
                loadNearbyFollowingMoods();
            } else {
                Toast.makeText(MoodMapActivity.this, 
                    "Location services must be enabled for nearby moods", 
                    Toast.LENGTH_LONG).show();
            }
        } else {
            loadFollowingMoods();
        }
    }

    private void displayMoodEventsOnMap() {
        if (googleMap == null || moodEventsList == null) return;

        // Clear existing markers
        for (Marker marker : currentMarkers) {
            marker.remove();
        }
        currentMarkers.clear();

        // Add markers for each mood event with location
        for (MoodEvent event : moodEventsList) {
            // Only add markers for moods that have both valid latitude and longitude
            if (event.getLatitude() != null && event.getLongitude() != null && 
                event.getLatitude() != 0.0 && event.getLongitude() != 0.0) {
                addMarkerForMoodEvent(event);
            }
        }
    }

    private void loadMyMoods() {
        // Clear existing markers
        clearMarkers();
        
        // Load user's mood events
        firestoreManager.getMoodEvents(true, new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                moodEventsList = new ArrayList<>(moodEvents);
                displayMoodEventsOnMap();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MoodMapActivity.this, "Error loading moods: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFollowingMoods() {
        // Clear any existing markers first
        clearMarkers();
        
        // Get mood events passed from FeedManagerActivity
        ArrayList<MoodEvent> passedMoodEvents = getIntent().getParcelableArrayListExtra("moodEvents");
        
        if (passedMoodEvents != null && !passedMoodEvents.isEmpty()) {
            // Filter out moods without location data
            ArrayList<MoodEvent> moodsWithLocation = new ArrayList<>();
            for (MoodEvent event : passedMoodEvents) {
                if (event.getLatitude() != null && event.getLongitude() != null) {
                    moodsWithLocation.add(event);
                }
            }
            moodEventsList = moodsWithLocation;
            displayMoodEventsOnMap();
        } else {
            // If no mood events were passed, load them from Firestore with filters
            firestoreManager.getFollowingMoodEvents(filterByWeek, selectedEmotion, searchReasonQuery, 
                new FirestoreManager.OnMoodEventsListener() {
                    @Override
                    public void onSuccess(List<MoodEvent> moodEvents) {
                        // Filter out moods without location data
                        ArrayList<MoodEvent> moodsWithLocation = new ArrayList<>();
                        for (MoodEvent event : moodEvents) {
                            if (event.getLatitude() != null && event.getLongitude() != null) {
                                moodsWithLocation.add(event);
                            }
                        }
                        moodEventsList = moodsWithLocation;
                        displayMoodEventsOnMap();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(MoodMapActivity.this, 
                            "Failed to load mood events: " + errorMessage, 
                            Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private void loadNearbyFollowingMoods() {
        // Clear existing markers
        clearExistingMarkers();

        // Get the list of users being followed
        firestoreManager.getFollowingIds(new FirestoreManager.OnFollowingIdsListener() {
            @Override
            public void onSuccess(List<String> followingIds) {
                if (followingIds.isEmpty()) {
                    showNoNearbyMoodsMessage();
                    return;
                }

                // Get shared mood events (already filtered for public moods)
                firestoreManager.getSharedMoodEvents(new FirestoreManager.OnMoodEventsListener() {
                    @Override
                    public void onSuccess(List<MoodEvent> moodEvents) {
                        int nearbyCount = 0;
                        
                        // Create a map to store the most recent mood for each user
                        Map<String, MoodEvent> mostRecentMoods = new HashMap<>();
                        
                        // Find the most recent mood for each user
                        for (MoodEvent moodEvent : moodEvents) {
                            if (followingIds.contains(moodEvent.getUserId())) {
                                String userId = moodEvent.getUserId();
                                if (!mostRecentMoods.containsKey(userId) || 
                                    moodEvent.getTimestamp().after(mostRecentMoods.get(userId).getTimestamp())) {
                                    mostRecentMoods.put(userId, moodEvent);
                                }
                            }
                        }
                        
                        // Now check which of these most recent moods are nearby
                        for (MoodEvent moodEvent : mostRecentMoods.values()) {
                            if (moodEvent.getLatitude() != null && moodEvent.getLongitude() != null) {
                                float[] results = new float[1];
                                Location.distanceBetween(
                                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                                    moodEvent.getLatitude(), moodEvent.getLongitude(),
                                    results
                                );

                                // If within 5km (5000m), add marker
                                if (results[0] <= 5000) {
                                    nearbyCount++;
                                    addMoodMarker(moodEvent);
                                }
                            }
                        }

                        if (nearbyCount == 0) {
                            showNoNearbyMoodsMessage();
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showNoNearbyMoodsMessage();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                showNoNearbyMoodsMessage();
            }
        });
    }

    private void displayMoodEvents(List<MoodEvent> moodEvents, boolean showUsername) {
        for (MoodEvent moodEvent : moodEvents) {
            // Only show moods with location data
            if (moodEvent.getLatitude() != null && moodEvent.getLongitude() != null) {
                LatLng position = new LatLng(moodEvent.getLatitude(), moodEvent.getLongitude());
                
                // Get username for the mood event
                firestoreManager.getUsernameById(moodEvent.getUserId(), new FirestoreManager.OnUsernameListener() {
                    @Override
                    public void onSuccess(String username) {
                        runOnUiThread(() -> {
                            // Create marker with mood icon and username
                            MarkerOptions markerOptions = new MarkerOptions()
                                .position(position)
                                .title(moodEvent.getEmotionalState())
                                .snippet(username)  // Use username instead of reason
                                .icon(createMarkerIcon(moodEvent.getEmotionalState()));

                            // Add marker to map and store reference
                            Marker marker = googleMap.addMarker(markerOptions);
                            if (marker != null) {
                                currentMarkers.add(marker);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String fallbackName) {
                        runOnUiThread(() -> {
                            // Use userId as fallback if username fetch fails
                            MarkerOptions markerOptions = new MarkerOptions()
                                .position(position)
                                .title(moodEvent.getEmotionalState())
                                .snippet(moodEvent.getUserId())  // Use userId as fallback
                                .icon(createMarkerIcon(moodEvent.getEmotionalState()));

                            // Add marker to map and store reference
                            Marker marker = googleMap.addMarker(markerOptions);
                            if (marker != null) {
                                currentMarkers.add(marker);
                            }
                        });
                    }
                });
            }
        }
    }

    private void clearMarkers() {
        for (Marker marker : moodMarkers.values()) {
            marker.remove();
        }
        moodMarkers.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(moodUpdateReceiver);
        
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize map with location
                if (googleMap != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        googleMap.setMyLocationEnabled(true);
                        getCurrentLocation();
                    }
                }
            } else {
                // Permission denied, load events without location
                loadAppropriateEvents();
                Toast.makeText(this, "Location permission denied. Some features may be limited.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10000)
            .setFastestInterval(5000);

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocation = location;
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM));
                    loadAppropriateEvents();
                } else {
                    // Try getLastLocation as fallback
                    fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, lastLocation -> {
                            if (lastLocation != null) {
                                currentLocation = lastLocation;
                                LatLng lastLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, DEFAULT_ZOOM));
                            }
                            loadAppropriateEvents();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error getting last location: " + e.getMessage());
                            loadAppropriateEvents();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting current location: " + e.getMessage());
                loadAppropriateEvents();
            });
    }

    private void checkLocationPermissionAndLoadNearbyMoods() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)        // 10 seconds
                .setFastestInterval(5000); // 5 seconds

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this)
                .checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // Location settings are satisfied, load nearby moods
            loadNearbyFollowingMoods();
        }).addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MoodMapActivity.this,
                            REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // Location settings have been satisfied, load nearby moods
                loadNearbyFollowingMoods();
            } else {
                // User has not enabled location settings
                Toast.makeText(this, "Location settings must be enabled to view nearby moods", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void addMarkerForMoodEvent(MoodEvent moodEvent) {
        // Double check location data is valid
        if (moodEvent.getLatitude() == null || moodEvent.getLongitude() == null || 
            moodEvent.getLatitude() == 0.0 || moodEvent.getLongitude() == 0.0) {
            return;
        }

        LatLng position = new LatLng(moodEvent.getLatitude(), moodEvent.getLongitude());
        
        // Get username for the mood event
        firestoreManager.getUsernameById(moodEvent.getUserId(), new FirestoreManager.OnUsernameListener() {
            @Override
            public void onSuccess(String username) {
                runOnUiThread(() -> {
                    BitmapDescriptor icon = createMarkerIcon(moodEvent.getEmotionalState());
                    MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title(moodEvent.getEmotionalState())
                        .snippet(username)
                        .icon(icon);

                    Marker marker = googleMap.addMarker(markerOptions);
                    if (marker != null) {
                        currentMarkers.add(marker);
                    }
                });
            }

            @Override
            public void onFailure(String fallbackName) {
                runOnUiThread(() -> {
                    BitmapDescriptor icon = createMarkerIcon(moodEvent.getEmotionalState());
                    MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title(moodEvent.getEmotionalState())
                        .snippet(moodEvent.getUserId())
                        .icon(icon);

                    Marker marker = googleMap.addMarker(markerOptions);
                    if (marker != null) {
                        currentMarkers.add(marker);
                    }
                });
            }
        });
    }

    private void adjustCameraToShowAllMarkers() {
        if (currentMarkers.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : currentMarkers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private void clearExistingMarkers() {
        runOnUiThread(() -> {
            for (Marker marker : currentMarkers) {
                marker.remove();
            }
            currentMarkers.clear();
        });
    }

    private void showNoNearbyMoodsMessage() {
        runOnUiThread(() -> Toast.makeText(MoodMapActivity.this, 
            "No nearby moods found within 5km", Toast.LENGTH_SHORT).show());
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Use Android's built-in distance calculation
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        // Convert meters to kilometers
        return results[0] / 1000.0;
    }

    private void addMoodMarker(MoodEvent moodEvent) {
        if (moodEvent.getLatitude() != null && moodEvent.getLongitude() != null) {
            LatLng position = new LatLng(moodEvent.getLatitude(), moodEvent.getLongitude());
            
            // Get username for the mood event
            firestoreManager.getUsernameById(moodEvent.getUserId(), new FirestoreManager.OnUsernameListener() {
                @Override
                public void onSuccess(String username) {
                    runOnUiThread(() -> {
                        BitmapDescriptor icon = createMarkerIcon(moodEvent.getEmotionalState());
                        MarkerOptions markerOptions = new MarkerOptions()
                            .position(position)
                            .title(moodEvent.getEmotionalState())
                            .snippet(username)
                            .icon(icon);

                        Marker marker = googleMap.addMarker(markerOptions);
                        if (marker != null) {
                            currentMarkers.add(marker);
                        }
                    });
                }

                @Override
                public void onFailure(String fallbackName) {
                    runOnUiThread(() -> {
                        BitmapDescriptor icon = createMarkerIcon(moodEvent.getEmotionalState());
                        MarkerOptions markerOptions = new MarkerOptions()
                            .position(position)
                            .title(moodEvent.getEmotionalState())
                            .snippet(moodEvent.getUserId())
                            .icon(icon);

                        Marker marker = googleMap.addMarker(markerOptions);
                        if (marker != null) {
                            currentMarkers.add(marker);
                        }
                    });
                }
            });
        }
    }
} 