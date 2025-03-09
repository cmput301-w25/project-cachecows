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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddMoodEventActivity extends AppCompatActivity {
    private static final int IMAGE_REQUEST_CODE = 200;

    // UI references
    private TextView tvGreeting, tvAddPhoto;
    private LinearLayout moodHappy, moodSad, moodAngry, moodSurprised,
            moodConfused, moodDisgusted, moodShame, moodFear;
    private EditText etReason, etTrigger;
    private Spinner socialSituationSpinner;
    private Button btnAddMood;

    //State
    String selectedMood = null;
    private String uploadedImageUrl = null;
    private FirestoreManager firestoreManager;
    private Date currentDateTime;
    private boolean isEditMode = false;
    private long moodEventId = -1;

    private ImageView btnDeletePhoto;
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

        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("EDIT_MODE", false)) {
            isEditMode = true;
            btnAddMood.setText("Save Changes");
            moodEventId = intent.getLongExtra("MOOD_EVENT_ID", -1);
            String emotionalState = intent.getStringExtra("EMOTIONAL_STATE");
            String reason = intent.getStringExtra("REASON");
            String trigger = intent.getStringExtra("TRIGGER");
            String socialSituation = intent.getStringExtra("SOCIAL_SITUATION");
            String imageUrl = intent.getStringExtra("IMAGE_URL");

            preFillFields(emotionalState, reason, trigger, socialSituation, imageUrl);
        }

        // Set greeting with username (this would normally come from user data)
        String username = "User"; // Replace with actual username later
        tvGreeting.setText("Hey " + username + "!");
        currentDateTime = new Date();

    }

    private void preFillFields(String emotionalState, String reason, String trigger, String socialSituation, String imageUrl) {
        // Set the selected mood
        selectedMood = emotionalState;
        highlightSelectedMood(emotionalState);

        // Set reason and trigger
        etReason.setText(reason);
        etTrigger.setText(trigger);


        // Set social situation in spinner
        if (socialSituation != null && !socialSituation.isEmpty()) {
            for (int i = 0; i < socialSituationSpinner.getCount(); i++) {
                if (socialSituationSpinner.getItemAtPosition(i).toString().equals(socialSituation)) {
                    socialSituationSpinner.setSelection(i);
                    break;
                }
            }
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            uploadedImageUrl = imageUrl;
            tvAddPhoto.setText("Edit Photograph");
            tvAddPhoto.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            tvAddPhoto.setText("Add Photograph");
            tvAddPhoto.setTextColor(ContextCompat.getColor(this, R.color.white));
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            btnDeletePhoto.setVisibility(View.VISIBLE);
        } else {
            btnDeletePhoto.setVisibility(View.GONE);
        }
    }

    private void highlightSelectedMood(String emotionalState) {
        resetMoodSelections();
        switch (emotionalState) {
            case "Happy":
                moodHappy.setBackgroundResource(R.drawable.selected_mood_background);
                break;
            case "Sad":
                moodSad.setBackgroundResource(R.drawable.selected_mood_background);
                break;
            case "Angry":
                moodAngry.setBackgroundResource(R.drawable.selected_mood_background);
                break;
            case "Surprised":
                moodSurprised.setBackgroundResource(R.drawable.selected_mood_background);
                break;
            case "Confused":
                moodConfused.setBackgroundResource(R.drawable.selected_mood_background);
                break;
            case "Disgusted":
                moodDisgusted.setBackgroundResource(R.drawable.selected_mood_background);
                break;
            case "Fear":
                moodFear.setBackgroundResource(R.drawable.selected_mood_background);
                break;
            case "Shame":
                moodShame.setBackgroundResource(R.drawable.selected_mood_background);
                break;
        }
    }

    private void initializeViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        etReason = findViewById(R.id.etReason);
        etTrigger = findViewById(R.id.etTrigger);
        socialSituationSpinner = findViewById(R.id.socialSituationSpinner);
        btnAddMood = findViewById(R.id.btnAddMood);
        tvAddPhoto = findViewById(R.id.tvAddPhoto);

        // Mood selectors
        moodHappy = findViewById(R.id.moodHappy);
        moodSad = findViewById(R.id.moodSad);
        moodAngry = findViewById(R.id.moodAngry);
        moodSurprised = findViewById(R.id.moodSurprised);
        moodConfused = findViewById(R.id.moodConfused);
        moodDisgusted = findViewById(R.id.moodDisgusted);
        moodShame = findViewById(R.id.moodShame);
        moodFear = findViewById(R.id.moodFear);


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

        tvAddPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(AddMoodEventActivity.this, UploadImageActivity.class);
            startActivityForResult(intent, IMAGE_REQUEST_CODE);
        });

        btnDeletePhoto = findViewById(R.id.btnDeletePhoto);
        btnDeletePhoto.setOnClickListener(v -> deletePhoto());

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
                    Snackbar.make(v, "Please select a mood", Snackbar.LENGTH_SHORT).show();
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

                MoodEvent moodEvent = new MoodEvent(selectedMood, trigger, socialSituation, reason);
                moodEvent.setTimestamp(currentDateTime);

                //If we have an uploaded image url
                if (uploadedImageUrl != null) {
                    moodEvent.setImageUrl(uploadedImageUrl);
                }

                if (isEditMode) {
                    // Get the document ID from the intent
                    String documentId = getIntent().getStringExtra("DOCUMENT_ID");

                    if (documentId == null) {
                        Snackbar.make(v, "Error: Cannot find mood event", Snackbar.LENGTH_SHORT).show();
                        btnAddMood.setEnabled(true);
                        return;
                    }

                    // Update the existing mood event
                    moodEvent.setId(moodEventId);
                    firestoreManager.updateMoodEvent(moodEvent, documentId, new FirestoreManager.OnMoodEventListener() {
                        @Override
                        public void onSuccess(MoodEvent moodEvent) {
                            Snackbar.make(v, "Mood updated successfully!", Snackbar.LENGTH_SHORT).setDuration(5000).show();
                            finish();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Snackbar.make(v, "Error: " + errorMessage, Snackbar.LENGTH_SHORT).setDuration(5000).show();
                            btnAddMood.setEnabled(true);
                        }
                    });
                } else {
                    // Save a new mood event (existing code remains the same)
                    firestoreManager.addMoodEvent(moodEvent, new FirestoreManager.OnMoodEventListener() {
                        @Override
                        public void onSuccess(MoodEvent moodEvent) {
                            Snackbar.make(v, "Mood added successfully!", Snackbar.LENGTH_SHORT).setDuration(5000).show();
                            finish();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Snackbar.make(v, "Error: " + errorMessage, Snackbar.LENGTH_SHORT).show();
                            btnAddMood.setEnabled(true);
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String downloadedUrl = data.getStringExtra("imageUrl");
            Log.d("AddMoodEventActivity", "downloadedUrl=" + downloadedUrl);
            if (downloadedUrl != null) {
                Snackbar.make(findViewById(android.R.id.content), "Image uploaded! URL:\n" + downloadedUrl, Snackbar.LENGTH_SHORT).setDuration(5000).show();
                this.uploadedImageUrl = downloadedUrl;
            }
            if (downloadedUrl != null) {
                btnDeletePhoto.setVisibility(View.VISIBLE);
            }

        }
    }
        private void deletePhoto () {
            uploadedImageUrl = null;
            tvAddPhoto.setText("Add Photograph");
            btnDeletePhoto.setVisibility(View.GONE);
            Snackbar.make(findViewById(R.id.layoutBottomNav),"Photo removed", Snackbar.LENGTH_SHORT).show();


        }
    }
