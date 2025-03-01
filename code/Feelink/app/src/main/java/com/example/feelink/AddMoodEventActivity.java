package com.example.feelink;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddMoodEventActivity extends AppCompatActivity {

    private TextView tvGreeting, tvDateTime;
    private LinearLayout moodHappy, moodSad, moodAngry, moodSurprised,
            moodConfused, moodDisgusted, moodShame, moodFear;
    private EditText etReason, etTrigger, etSocialSituation;
    private Button btnAddMood;

    private String selectedMood = null;
    private FirestoreManager firestoreManager;
    private Date currentDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_mood_event);

        // Initialize Firestore manager
        firestoreManager = new FirestoreManager();



        // Initialize views
        initializeViews();
        setupMoodSelectors();
        setupAddButton();

        // Set greeting with username (this would normally come from user data)
        String username = "User"; // Replace with actual username later
        tvGreeting.setText("Hey " + username + "!");
        currentDateTime = new Date();

        // Set current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault());
        tvDateTime.setText(sdf.format(currentDateTime));
    }

    private void initializeViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvDateTime = findViewById(R.id.tvDateTime);

        // Mood selectors
        moodHappy = findViewById(R.id.moodHappy);
        moodSad = findViewById(R.id.moodSad);
        moodAngry = findViewById(R.id.moodAngry);
        moodSurprised = findViewById(R.id.moodSurprised);
        moodConfused = findViewById(R.id.moodConfused);
        moodDisgusted = findViewById(R.id.moodDisgusted);
        moodShame = findViewById(R.id.moodShame);
        moodFear = findViewById(R.id.moodFear);

        // Input fields
        etReason = findViewById(R.id.etReason);
        etTrigger = findViewById(R.id.etTrigger);
        etSocialSituation = findViewById(R.id.etSocialSituation);

        // Button
        btnAddMood = findViewById(R.id.btnAddMood);
    }

    private void setupMoodSelectors() {
        // Set click listeners for all mood options
        View.OnClickListener moodClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset all mood backgrounds
                resetMoodSelections();

                // Highlight selected mood
                v.setBackgroundResource(R.drawable.selected_mood_background);

                // Store selected mood
                if (v.getId() == R.id.moodHappy) {
                    selectedMood = "Happy";
                } else if (v.getId() == R.id.moodSad) {
                    selectedMood = "Sad";
                } else if (v.getId() == R.id.moodAngry) {
                    selectedMood = "Angry";
                } else if (v.getId() == R.id.moodSurprised) {
                    selectedMood = "Surprised";
                } else if (v.getId() == R.id.moodConfused) {
                    selectedMood = "Confused";
                } else if (v.getId() == R.id.moodDisgusted) {
                    selectedMood = "Disgusted";
                } else if (v.getId() == R.id.moodFear) {
                    selectedMood = "Fear";
                } else if (v.getId() == R.id.moodShame) {
                    selectedMood = "Shame";
                }
            }
        };

        // Apply the listener to all mood options
        moodHappy.setOnClickListener(moodClickListener);
        moodSad.setOnClickListener(moodClickListener);
        moodAngry.setOnClickListener(moodClickListener);
        moodSurprised.setOnClickListener(moodClickListener);
        moodConfused.setOnClickListener(moodClickListener);
        moodDisgusted.setOnClickListener(moodClickListener);
        moodFear.setOnClickListener(moodClickListener);
        moodShame.setOnClickListener(moodClickListener);
    }

    private void resetMoodSelections() {
        // Remove background highlight from all mood options
        moodHappy.setBackgroundResource(android.R.color.transparent);
        moodSad.setBackgroundResource(android.R.color.transparent);
        moodAngry.setBackgroundResource(android.R.color.transparent);
        moodSurprised.setBackgroundResource(android.R.color.transparent);
        moodConfused.setBackgroundResource(android.R.color.transparent);
        moodDisgusted.setBackgroundResource(android.R.color.transparent);
        moodShame.setBackgroundResource(android.R.color.transparent);
        moodFear.setBackgroundResource(android.R.color.transparent);
    }

    private void setupAddButton() {
        btnAddMood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedMood == null) {
                    Toast.makeText(AddMoodEventActivity.this, "Please select a mood", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentDateTime = new Date();

                // Show loading state (could add a progress indicator here)
                btnAddMood.setEnabled(false);

                // Get input values
                String reason = etReason.getText().toString().trim();
                String trigger = etTrigger.getText().toString().trim();
                String socialSituation = etSocialSituation.getText().toString().trim();

                // Create a new mood event with the current timestamp
                MoodEvent moodEvent = new MoodEvent(selectedMood, trigger, socialSituation,reason);
                moodEvent.setTimestamp(currentDateTime);

                // Save to Firestore
                firestoreManager.addMoodEvent(moodEvent, new FirestoreManager.OnMoodEventListener() {
                    @Override
                    public void onSuccess(MoodEvent moodEvent) {
                        // Show success message and return to previous activity
                        Toast.makeText(AddMoodEventActivity.this, "Mood added successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // Show error and re-enable button
                        Toast.makeText(AddMoodEventActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                        btnAddMood.setEnabled(true);
                    }
                });
            }
        });
    }
}