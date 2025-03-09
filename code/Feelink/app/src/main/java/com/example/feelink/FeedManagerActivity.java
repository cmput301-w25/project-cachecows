package com.example.feelink;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

public class FeedManagerActivity extends AppCompatActivity {
    private Button btnTheirMood, btnMyMood;
    private ImageButton btnFilter;
    private TextView tvShareInfo, tvEmptyState;
    private RecyclerView recyclerMoodEvents;
    private FloatingActionButton fabAddMood;
    private FirebaseAuth mAuth;
    private FirestoreManager firestoreManager;
    private MoodEventAdapter adapter;
    private boolean isShowingMyMood = false;

    private static boolean SKIP_AUTH_FOR_TESTING = false;

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
        if (currentUser != null || SKIP_AUTH_FOR_TESTING) {
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
    }

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

    private void initializeViews() {
        btnTheirMood = findViewById(R.id.btnTheirMood);
        btnMyMood = findViewById(R.id.btnMyMood);
        btnFilter = findViewById(R.id.btnFilter);
        tvShareInfo = findViewById(R.id.tvShareInfo);
        recyclerMoodEvents = findViewById(R.id.recyclerMoodEvents);
        fabAddMood = findViewById(R.id.fabAddMood);
    }

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
            if (mAuth.getCurrentUser() != null || SKIP_AUTH_FOR_TESTING) {
                navigateToAddMood();
            } else {
                handleUnauthorizedAccess();
            }
        });
    }

    private void setupRecyclerView() {
        recyclerMoodEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MoodEventAdapter(new ArrayList<>(), this);
        recyclerMoodEvents.setAdapter(adapter);
    }

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

    private void navigateToAddMood() {
        Intent intent = new Intent(FeedManagerActivity.this, AddMoodEventActivity.class);
        startActivity(intent);
    }

    private void handleUnauthorizedAccess() {
        // Redirect to login
        startActivity(new Intent(this, Login.class));
        finish();
    }
}