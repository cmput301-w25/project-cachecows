package com.example.feelink;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import java.util.UUID;

/**
 * Central activity for creating and editing mood events with full CRUD operations.
 * Handles UI rendering, data validation, and Firestore integration.
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 1.01.01.01 - Main mood event form UI</li>
 *   <li>US 1.02.01.02 - Emotional state model integration</li>
 *   <li>US 1.05.01.01 - Mood event edit interface</li>
 *   <li>US 1.05.01.02 - Edit data binding</li>
 *   <li>US 1.05.01.03 - Update/edit logic</li>
 *   <li>US 02.01.01.02 - Reason input validation</li>
 *   <li>US 02.04.01.01 - Social situation UI</li>
 *   <li>US 02.02.01.01 - Photo upload UI</li>
 *   <li>US 02.02.01.03 - Photograph data integration</li>
 * </ul>
 */
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
    private static boolean SKIP_AUTH_FOR_TESTING = false;



    /**
     * Initializes activity components and state
     *
     * <p>Key operations:</p>
     * <ol>
     *   <li>Firestore manager initialization</li>
     *   <li>UI component binding</li>
     *   <li>Edit mode detection and setup</li>
     *   <li>Current user context establishment</li>
     * </ol>
     *
     * @param savedInstanceState Persisted state data
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_mood_event);

        // Initialize Firestore manager
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null || SKIP_AUTH_FOR_TESTING) {
            // Use test user ID if needed for testing
            String uid = user != null ? user.getUid() : "test_user_id";
            firestoreManager = new FirestoreManager(uid);
        }


        // Initialize views
        initializeViews();
        setupMoodSelectors();
        setupSocialSituationSpinner();

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

        setupAddButton();

        // Set greeting with username (this would normally come from user data)
        String username = "User"; // Replace with actual username later
        tvGreeting.setText("Hey " + username + "!");
        currentDateTime = new Date();

    }

    /**
     * Populates form fields with existing data during edit operations
     *
     * <p>Implements US 1.05.01.02 data binding requirements by:
     * <ol>
     *   <li>Restoring emotional state selection</li>
     *   <li>Prefilling text inputs</li>
     *   <li>Setting social situation spinner</li>
     *   <li>Handling image URL state</li>
     * </ol>
     *
     * @param emotionalState Previously saved emotional state
     * @param reason Stored reason text
     * @param trigger Stored trigger text
     * @param socialSituation Selected social situation
     * @param imageUrl Stored image reference
     */
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

    /**
     * Applies visual highlighting to the selected emotional state UI element
     *
     * <p>Implements selection feedback mechanism for emotional state model integration.
     * Works with {@link #resetMoodSelections()} to manage UI state.</p>
     *
     * <h3>User Stories Implemented:</h3>
     * <ul>
     *   <li>US 1.02.01.02 - Emotional state model visualization</li>
     *   <li>US 1.05.01.02 - Edit mode state restoration</li>
     * </ul>
     *
     * @param emotionalState The emotional state to highlight from predefined values:
     *                      "Happy", "Sad", "Angry", "Surprised", "Confused",
     *                      "Disgusted", "Fear", "Shame"
     *
     * @see R.drawable#selected_mood_background
     */
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

    /**
     * Initializes all UI components and sets up text validation
     *
     * <p>Binds view references and configures:</p>
     * <ul>
     *   <li>Reason field input validation (US 02.01.01.02)</li>
     *   <li>Photo upload click handler (US 02.02.01.01)</li>
     *   <li>Photo deletion functionality (US 02.02.01.03)</li>
     * </ul>
     */
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

    /**
     * Validates reason field against business rules
     *
     * <p>Enforces:
     * <ul>
     *   <li>20 character maximum length</li>
     *   <li>3 word maximum limit</li>
     * </ul>
     * Implements US 02.01.01.02 validation requirements.</p>
     *
     * @param text Input to validate
     */
    private void validateReasonField(String text) {

        // Show error if either limit is exceeded
        if (ValidationUtils.isReasonNotValid(text)) {
            etReason.setError("Reason must be limited to 20 characters or 3 words");
            btnAddMood.setEnabled(false);
        } else {
            etReason.setError(null);
            btnAddMood.setEnabled(true);
        }
    }

    /**
     * Configures mood selection UI components
     *
     * <p>Implements emotional state selection logic with visual feedback.
     * Integrates with centralized emotional state model (US 1.02.01.02).</p>
     *
     * @see #resetMoodSelections()
     */
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

    /**
     * Initializes social situation spinner with predefined options
     *
     * <p>Implements US 02.04.01.01 requirements for social situation selection.
     * Uses standardized options from requirements specification.</p>
     */
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

    /**
     * Configures primary action button behavior based on mode (create/edit)
     *
     * <p>Handles:
     * <ul>
     *   <li>Final validation checks (US 1.01.01.03)</li>
     *   <li>MoodEvent object construction</li>
     *   <li>Firestore create/update operations (US 1.01.01.02, US 1.05.01.03)</li>
     *   <li>Error handling and user feedback</li>
     * </ul>
     */
    private void setupAddButton() {
        btnAddMood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ensure a mood is selected
                if (selectedMood == null) {
                    Snackbar.make(v, "Please select a mood", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                currentDateTime = new Date();
                // Show loading state (could add a progress indicator here)
                btnAddMood.setEnabled(false);

                // Get input values
                String reason = etReason.getText().toString().trim();
                if (reason.length() > 20 || (!reason.isEmpty() && reason.split("\\s+").length > 3)) {
                    etReason.setError("Reason must be limited to 20 characters or 3 words");
                    btnAddMood.setEnabled(true);
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
                    // For update mode
                    String documentId = getIntent().getStringExtra("DOCUMENT_ID");
                    if (documentId == null) {
                        Snackbar.make(v, "Error: Cannot find mood event", Snackbar.LENGTH_SHORT).show();
                        btnAddMood.setEnabled(true);
                        return;
                    }
                    moodEvent.setId(moodEventId);
                    if (isNetworkAvailable()) {
                        moodEvent.setPendingSync(false);
                        firestoreManager.updateMoodEvent(moodEvent, documentId, new FirestoreManager.OnMoodEventListener() {
                            @Override
                            public void onSuccess(MoodEvent moodEvent) {
                                // Remove from pending list if present
                                new PendingSyncManager(AddMoodEventActivity.this).removePendingId(moodEvent.getDocumentId());
                                Snackbar.make(v, "Mood added successfully!", Snackbar.LENGTH_SHORT).show();
                                moodEvent.setPendingSync(false);
                                finish();
                            }
                            @Override
                            public void onFailure(String errorMessage) {
                                Snackbar.make(v, "Error: " + errorMessage, Snackbar.LENGTH_SHORT).show();
                                btnAddMood.setEnabled(true);
                            }
                        });
                    } else {
                        moodEvent.setPendingSync(true);
                        // Offline: add the pending document id to the local pending set.
                        new PendingSyncManager(AddMoodEventActivity.this).addPendingId(documentId);
                        firestoreManager.updateMoodEvent(moodEvent, documentId, new FirestoreManager.OnMoodEventListener() {
                            @Override
                            public void onSuccess(MoodEvent moodEvent) {}
                            @Override
                            public void onFailure(String errorMessage) {}
                        });
                        Toast.makeText(AddMoodEventActivity.this, "You are offline. Your changes have been saved locally!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    // Add mode:
                    if (isNetworkAvailable()) {
                        moodEvent.setPendingSync(false);
                        firestoreManager.addMoodEvent(moodEvent, new FirestoreManager.OnMoodEventListener() {
                            @Override
                            public void onSuccess(MoodEvent moodEvent) {
                                new PendingSyncManager(AddMoodEventActivity.this).removePendingId(moodEvent.getDocumentId());
                                Snackbar.make(v, "Mood added successfully!", Snackbar.LENGTH_SHORT).show();
                                moodEvent.setPendingSync(false);
                                finish();
                            }
                            @Override
                            public void onFailure(String errorMessage) {
                                Snackbar.make(v, "Error: " + errorMessage, Snackbar.LENGTH_SHORT).show();
                                btnAddMood.setEnabled(true);
                            }
                        });
                    } else {
                        moodEvent.setPendingSync(true);
                        // If no document ID is available, generate one
                        if (moodEvent.getDocumentId() == null || moodEvent.getDocumentId().isEmpty()) {
                            // Generate a temporary ID
                            String tempId = UUID.randomUUID().toString();
                            moodEvent.setDocumentId(tempId);
                        }
                        // Add this document ID to PendingSyncManager
                        new PendingSyncManager(AddMoodEventActivity.this).addPendingId(moodEvent.getDocumentId());
                        // Use the new method that uses set() with the provided ID:
                        firestoreManager.addMoodEventWithId(moodEvent, moodEvent.getDocumentId(), new FirestoreManager.OnMoodEventListener() {
                            @Override
                            public void onSuccess(MoodEvent moodEvent) {
                                // Offline callback might be delayed
                            }
                            @Override
                            public void onFailure(String errorMessage) {
                                // Optionally handle error
                            }
                        });
                        Toast.makeText(AddMoodEventActivity.this, "You are offline. Your changes have been saved locally!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            }
        });
    }



    /**
     * Handles image upload results from UploadImageActivity
     *
     * <p>Processes image URL results and updates UI state.
     * Implements US 02.02.01.03 photograph integration requirements.</p>
     *
     * @param requestCode Originating request identifier
     * @param resultCode Operation result status
     * @param data Result payload containing image URL
     */
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

    /**
     * Removes associated photograph from current mood event
     *
     * <p>Clears image URL reference and updates UI state.
     * Part of US 02.02.01.03 photograph integration requirements.</p>
     */
    private void deletePhoto () {
        uploadedImageUrl = null;
        tvAddPhoto.setText("Add Photograph");
        btnDeletePhoto.setVisibility(View.GONE);
        Snackbar.make(findViewById(R.id.layoutBottomNav),"Photo removed", Snackbar.LENGTH_SHORT).show();
    }

    // method to check connectivity
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}