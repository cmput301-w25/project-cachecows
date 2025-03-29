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
import android.widget.ToggleButton;

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
    private static final int LOCATION_PICKER_REQUEST_CODE = 2;

    // UI references
    private TextView tvGreeting, tvAddPhoto, tvAddLocation;
    private LinearLayout moodHappy, moodSad, moodAngry, moodSurprised,
            moodConfused, moodDisgusted, moodShame, moodFear;
    private EditText etReason;
    private Spinner socialSituationSpinner;
    private Button btnAddMood;
    private ToggleButton togglePrivacy;

    //State
    String selectedMood = null;
    private String uploadedImageUrl = null;
    private String tempLocalImagePath = null;
    private FirestoreManager firestoreManager;
    private PendingSyncManager pendingSyncManager;
    private String docId;
    private Date currentDateTime;
    private boolean isEditMode = false;
    private long moodEventId = -1;

    private ImageView btnDeletePhoto;
    private static boolean SKIP_AUTH_FOR_TESTING = false;
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;

    private ImageView btnDeleteLocation;

    public static void enableTestMode(boolean enabled) {
        SKIP_AUTH_FOR_TESTING = enabled;
    }

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
            pendingSyncManager = new PendingSyncManager(this);

            // Fetch the username
            firestoreManager.getUsernameById(uid, new FirestoreManager.OnUsernameListener() {
                @Override
                public void onSuccess(String username) {
                    // Set greeting with the actual username
                    tvGreeting.setText("Hey " + username + "!");
                }

                @Override
                public void onFailure(String fallbackName) {
                    // If username fetch fails, use the UID or a fallback
                    tvGreeting.setText("Hey " + fallbackName + "!");
                }
            });
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
            String socialSituation = intent.getStringExtra("SOCIAL_SITUATION");
            String imageUrl = intent.getStringExtra("IMAGE_URL");
            String locationName = intent.getStringExtra("LOCATION_NAME");
            selectedLatitude = intent.getDoubleExtra("LATITUDE", 0.0);
            selectedLongitude = intent.getDoubleExtra("LONGITUDE", 0.0);
            boolean isPublic = intent.getBooleanExtra("IS_PUBLIC", true);

            preFillFields(emotionalState, reason, socialSituation, imageUrl, locationName, isPublic);
        }

        setupAddButton();

        // Set greeting with username (this would normally come from user data)
        tvGreeting.setText("Hey there!"); // Temporary placeholder
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
     * @param socialSituation Selected social situation
     * @param imageUrl Stored image reference
     * @param locationName Stored location name
     */
    private void preFillFields(String emotionalState, String reason, String socialSituation, String imageUrl, String locationName, boolean isPublic) {
        // Set the selected mood
        selectedMood = emotionalState;
        highlightSelectedMood(emotionalState);

        // Set reason
        etReason.setText(reason);



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

        togglePrivacy.setChecked(!isPublic);  // Toggle is "Private" when checked

        // Handle location data
        if (locationName != null && !locationName.isEmpty()) {
            tvAddLocation.setText(locationName);
            tvAddLocation.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            btnDeleteLocation.setVisibility(View.VISIBLE);
        } else {
            tvAddLocation.setText("Add Location (Optional)");
            tvAddLocation.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            btnDeleteLocation.setVisibility(View.GONE);
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
        togglePrivacy = findViewById(R.id.togglePrivacy);
        socialSituationSpinner = findViewById(R.id.socialSituationSpinner);
        btnAddMood = findViewById(R.id.btnAddMood);
        tvAddPhoto = findViewById(R.id.tvAddPhoto);
        tvAddLocation = findViewById(R.id.tvAddLocation);

        // Mood selectors
        moodHappy = findViewById(R.id.moodHappy);
        moodSad = findViewById(R.id.moodSad);
        moodAngry = findViewById(R.id.moodAngry);
        moodSurprised = findViewById(R.id.moodSurprised);
        moodConfused = findViewById(R.id.moodConfused);
        moodDisgusted = findViewById(R.id.moodDisgusted);
        moodShame = findViewById(R.id.moodShame);
        moodFear = findViewById(R.id.moodFear);

        // Add location click listener
        tvAddLocation.setOnClickListener(v -> {
            Intent locationIntent = new Intent(this, LocationPickerActivity.class);
            startActivityForResult(locationIntent, LOCATION_PICKER_REQUEST_CODE);
        });

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

        btnDeleteLocation = findViewById(R.id.btnDeleteLocation);
        btnDeleteLocation.setOnClickListener(v -> deleteLocation());
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
            etReason.setError("Reason must be limited to 200 characters");
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
        btnAddMood.setOnClickListener(v -> {
            if (!validateInputs(v)) {
                return;
            }

            btnAddMood.setEnabled(false); // Disable button to prevent multiple clicks

            MoodEvent moodEvent = createMoodEventFromInputs();

            // Set the location data
            String locationName = tvAddLocation.getText().toString();
            if (!locationName.equals("Add Location (Optional)") && !locationName.equals("Add Location")) {
                moodEvent.setLocationName(locationName);
                moodEvent.setLatitude(selectedLatitude);
                moodEvent.setLongitude(selectedLongitude);
            }

            if (isEditMode) {
                handleEditMode(v, moodEvent);
            } else {
                handleAddMode(v, moodEvent);
            }
        });
    }

    // In validateInputs method
    private boolean validateInputs(View v) {
        if (selectedMood == null) {
            Snackbar.make(v, "Please select a mood", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        String reason = etReason.getText().toString().trim();
        if (reason.length() > 200) {
            etReason.setError("Reason must be limited to 200 characters");
            btnAddMood.setEnabled(true);
            return false;
        }

        return true;
    }

    private MoodEvent createMoodEventFromInputs() {
        String reason = etReason.getText().toString().trim();
        String selectedValue = socialSituationSpinner.getSelectedItem().toString();
        String socialSituation = selectedValue.equals("None") ? "" : selectedValue;

        MoodEvent moodEvent = new MoodEvent(selectedMood, socialSituation, reason);
        moodEvent.setTimestamp(new Date());
        moodEvent.setPublic(!togglePrivacy.isChecked());

        if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
            moodEvent.setImageUrl(uploadedImageUrl);
        } else if (tempLocalImagePath != null && !tempLocalImagePath.isEmpty()) {
            moodEvent.setTempLocalImagePath(tempLocalImagePath);
        }

        return moodEvent;
    }

    private void handleEditMode(View v, MoodEvent moodEvent) {
        String documentId = getIntent().getStringExtra("DOCUMENT_ID");
        if (documentId == null) {
            Snackbar.make(v, "Error: Cannot find mood event", Snackbar.LENGTH_SHORT).show();
            btnAddMood.setEnabled(true);
            return;
        }

        moodEvent.setId(moodEventId);
        if (ConnectivityReceiver.isNetworkAvailable(AddMoodEventActivity.this)) {
            updateMoodEventOnline(v, moodEvent, documentId);
        } else {
            updateMoodEventOffline(v, moodEvent, documentId);
        }
    }

    private void updateMoodEventOnline(View v, MoodEvent moodEvent, String documentId) {
        moodEvent.setPendingSync(false);
        firestoreManager.updateMoodEvent(moodEvent, documentId, new FirestoreManager.OnMoodEventListener() {
            @Override
            public void onSuccess(MoodEvent moodEvent) {
                new PendingSyncManager(AddMoodEventActivity.this).removePendingId(moodEvent.getDocumentId());
                Snackbar.make(v, "Mood updated successfully!", Snackbar.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                Snackbar.make(v, "Error: " + errorMessage, Snackbar.LENGTH_SHORT).show();
                btnAddMood.setEnabled(true);
            }
        });
    }

    private void updateMoodEventOffline(View v, MoodEvent moodEvent, String documentId) {
        moodEvent.setPendingSync(true);
        new PendingSyncManager(AddMoodEventActivity.this).addPendingId(documentId);
        firestoreManager.updateMoodEvent(moodEvent, documentId, new FirestoreManager.OnMoodEventListener() {
            @Override
            public void onSuccess(MoodEvent moodEvent) {
                // No action needed for offline success
            }

            @Override
            public void onFailure(String errorMessage) {
                // No action needed for offline failure
            }
        });
        Toast.makeText(AddMoodEventActivity.this, "You are offline. Your changes have been saved locally!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void handleAddMode(View v, MoodEvent moodEvent) {
        if (ConnectivityReceiver.isNetworkAvailable(AddMoodEventActivity.this)) {
            addMoodEventOnline(v, moodEvent);
        } else {
            addMoodEventOffline(v, moodEvent);
        }
    }

    private void addMoodEventOnline(View v, MoodEvent moodEvent) {
        moodEvent.setPendingSync(false);
        firestoreManager.addMoodEvent(moodEvent, new FirestoreManager.OnMoodEventListener() {
            @Override
            public void onSuccess(MoodEvent moodEvent) {
                if (tempLocalImagePath != null) {
                    new PendingSyncManager(AddMoodEventActivity.this).removePendingId(moodEvent.getDocumentId());
                }
                Snackbar.make(v, "Mood added successfully!", Snackbar.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                Snackbar.make(v, "Error: " + errorMessage, Snackbar.LENGTH_SHORT).show();
                btnAddMood.setEnabled(true);
            }
        });
    }

    private void addMoodEventOffline(View v, MoodEvent moodEvent) {
        moodEvent.setPendingSync(true);
        if (moodEvent.getDocumentId() == null || moodEvent.getDocumentId().isEmpty()) {
            moodEvent.setDocumentId(UUID.randomUUID().toString());
        }
        new PendingSyncManager(AddMoodEventActivity.this).addPendingId(moodEvent.getDocumentId());
        firestoreManager.addMoodEventWithId(moodEvent, moodEvent.getDocumentId(), new FirestoreManager.OnMoodEventListener() {
            @Override
            public void onSuccess(MoodEvent moodEvent) {
                // No action needed for offline success
            }

            @Override
            public void onFailure(String errorMessage) {
                // No action needed for offline failure
            }
        });
        Toast.makeText(AddMoodEventActivity.this, "You are offline. Your changes have been saved locally!", Toast.LENGTH_SHORT).show();
        finish();
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
            if (downloadedUrl != null && ConnectivityReceiver.isNetworkAvailable(this)) {
                Snackbar.make(findViewById(android.R.id.content), "Image uploaded! URL:\n" + downloadedUrl, Snackbar.LENGTH_SHORT)
                        .setDuration(5000).show();
                this.uploadedImageUrl = downloadedUrl;
                tempLocalImagePath = null;
                btnDeletePhoto.setVisibility(View.VISIBLE);
            } else{
                String localPath = data.getStringExtra("localImagePath");
                if (localPath != null) {
                    this.uploadedImageUrl = null;
                    this.tempLocalImagePath = localPath;
                    btnDeletePhoto.setVisibility(View.VISIBLE);
                    Snackbar.make(findViewById(android.R.id.content),
                                    "Offline mode: image saved locally and will be uploaded later.", Snackbar.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == LOCATION_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                selectedLatitude = data.getDoubleExtra("latitude", 0.0);
                selectedLongitude = data.getDoubleExtra("longitude", 0.0);
                String locationName = data.getStringExtra("locationName");
                tvAddLocation.setText(locationName);
                tvAddLocation.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                btnDeleteLocation.setVisibility(View.VISIBLE);
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

    private void deleteLocation() {
        selectedLatitude = 0.0;
        selectedLongitude = 0.0;
        tvAddLocation.setText("Add Location");
        btnDeleteLocation.setVisibility(View.GONE);
        Snackbar.make(findViewById(R.id.layoutBottomNav), "Location removed", Snackbar.LENGTH_SHORT).show();
    }
}
