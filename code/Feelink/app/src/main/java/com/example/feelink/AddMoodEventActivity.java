package com.example.feelink;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddMoodEventActivity extends AppCompatActivity {
    private static final int IMAGE_REQUEST_CODE = 200;
    private TextView tvGreeting, tvAddPhoto;
    private LinearLayout moodHappy, moodSad, moodAngry, moodSurprised,
            moodConfused, moodDisgusted, moodShame, moodFear;
    private EditText etReason, etTrigger;

    private Spinner socialSituationSpinner;
    private Button btnAddMood;

    private String selectedMood = null;
    private String uploadedImageUrl = null;
    private FirestoreManager firestoreManager;
    private Date currentDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_mood_event);

        // Initialize Firestore manager
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            firestoreManager = new FirestoreManager(user.getUid()); // Pass real UID
        }



        // Initialize views
        initializeViews();
        setupMoodSelectors();
        setupSocialSituationSpinner();
        setupAddButton();

        // Set greeting with username (this would normally come from user data)
        String username = "User"; // Replace with actual username later
        tvGreeting.setText("Hey " + username + "!");
        currentDateTime = new Date();

    }

    private void initializeViews() {
        tvGreeting = findViewById(R.id.tvGreeting);

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
        etReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateReasonField(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
        etTrigger = findViewById(R.id.etTrigger);
        socialSituationSpinner = findViewById(R.id.socialSituationSpinner);

        // Button
        btnAddMood = findViewById(R.id.btnAddMood);

        //Add Photo
        tvAddPhoto = findViewById(R.id.tvAddPhoto);
        tvAddPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(AddMoodEventActivity.this, UploadImageActivity.class);
            startActivityForResult(intent, IMAGE_REQUEST_CODE);
        });


    }

    private void validateReasonField(String text) {
        // Check character limit
        boolean exceedsCharLimit = text.length() > 20;

        // Check word limit
        String[] words = text.trim().split("\\s+");
        boolean exceedsWordLimit = text.trim().length() > 0 && words.length > 3;

        // Show error if either limit is exceeded
        if (exceedsCharLimit || exceedsWordLimit) {
            etReason.setError("Reason must be limited to 20 characters or 3 words");
            btnAddMood.setEnabled(false);
        } else {
            etReason.setError(null);
            btnAddMood.setEnabled(true);
        }
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

    private void setupSocialSituationSpinner() {
        String[] options = {
                "None",
                "Alone",
                "With one other person",
                "With two to several people",
                "With a crowd"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                options
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationSpinner.setAdapter(adapter);
        socialSituationSpinner.setSelection(0);
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
                // Validate reason field one more time before proceeding
                if (reason.length() > 20 || (reason.split("\\s+").length > 3 && !reason.isEmpty())) {
                    etReason.setError("Reason must be limited to 20 characters or 3 words");
                    return;
                }

                String trigger = etTrigger.getText().toString().trim();
                String selectedValue = socialSituationSpinner.getSelectedItem().toString();
                String socialSituation = selectedValue.equals("None") ? "" : selectedValue;

                // Create a new mood event with the current timestamp
                MoodEvent moodEvent = new MoodEvent(selectedMood, trigger, socialSituation,reason);
                moodEvent.setTimestamp(currentDateTime);

                //If we have an uploaded image url
                if (uploadedImageUrl != null) {
                    moodEvent.setImageUrl(uploadedImageUrl);
                }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String downloadedUrl = data.getStringExtra("imageUrl");
            Log.d("AddMoodEventActivity", "downloadedUrl="+downloadedUrl);
            if (downloadedUrl != null) {
                Toast.makeText(this, "Image uploaded! URL:\n" + downloadedUrl, Toast.LENGTH_SHORT).show();
                this.uploadedImageUrl = downloadedUrl;
            }
        }
    }
}