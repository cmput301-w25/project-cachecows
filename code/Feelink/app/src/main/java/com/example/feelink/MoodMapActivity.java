package com.example.feelink;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoodMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private MapView mapView;
    private GoogleMap googleMap;
    private FirestoreManager firestoreManager;
    private String currentUserId;
    private Map<String, Marker> moodMarkers = new HashMap<>();
    private RadioGroup filterRadioGroup;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_map);

        // Initialize Firebase Auth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            firestoreManager = new FirestoreManager(currentUserId);
        }

        // Initialize map
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Initialize filter radio group
        filterRadioGroup = findViewById(R.id.filterRadioGroup);
        filterRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioMyMoods) {
                loadMyMoods();
            } else if (checkedId == R.id.radioFollowing) {
                loadFollowingMoods();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        // Set initial camera position to Edmonton
        LatLng edmonton = new LatLng(53.5461, -113.4937);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(edmonton, 12));
        
        // Load initial data (my moods)
        loadMyMoods();
    }

    private void loadMyMoods() {
        // Clear existing markers
        clearMarkers();
        
        // Load user's mood events
        firestoreManager.getMoodEvents(true, new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                displayMoodEvents(moodEvents, false);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MoodMapActivity.this, "Error loading moods: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFollowingMoods() {
        // Clear existing markers
        clearMarkers();
        
        // Load following's mood events
        firestoreManager.getSharedMoodEvents(new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                displayMoodEvents(moodEvents, true);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MoodMapActivity.this, "Error loading following moods: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayMoodEvents(List<MoodEvent> moodEvents, boolean showUsername) {
        for (MoodEvent moodEvent : moodEvents) {
            // Only show moods with location data
            if (moodEvent.getLatitude() != null && moodEvent.getLongitude() != null) {
                LatLng position = new LatLng(moodEvent.getLatitude(), moodEvent.getLongitude());
                
                // Create marker title with username if needed
                String title = "";
                if (showUsername) {
                    title = moodEvent.getUserId(); // You might want to fetch actual username here
                }

                // Create marker with mood icon
                MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title(title)
                    .snippet(moodEvent.getReason())
                    .icon(createMarkerIcon(moodEvent.getEmotionalState()));

                // Add marker to map and store reference
                Marker marker = googleMap.addMarker(markerOptions);
                moodMarkers.put(moodEvent.getDocumentId(), marker);
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
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
} 