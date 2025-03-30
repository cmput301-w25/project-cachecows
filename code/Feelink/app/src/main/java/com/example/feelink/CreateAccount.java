package com.example.feelink;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateAccount extends AppCompatActivity {
    private EditText nameEditText, usernameEditText, dobEditText,
            emailEditText, passwordEditText, repeatedPasswordEditText;
    private TextView usernameFeedbackText, tvAddPhoto;
    private Button createButton;
    private ImageView backButton;
    private static final String TAG = "CreateAccountActivity";
    private CollectionReference usersRef;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean usernameAvailable = false;
    private boolean isEditMode = false;
    private String existingUsername;

    // Regex patterns
    private static final String VALID_USERNAME = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,25}$";
    private static final String VALID_PASSWORD = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9!@#$%^&*()_+\\-=]{6,}$";
    private static boolean SKIP_AUTH_FOR_TESTING_CREATE_ACCOUNT = false;
    private ActivityResultLauncher<Intent> editProfileImageLauncher; // Launcher for editing the profile image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");

        // Check if in edit mode
        isEditMode = getIntent().getBooleanExtra("EDIT_MODE", false);

        initializeViews();
        setupUIForMode();
        setupDatePicker();
        setupUsernameValidation();

        if (isEditMode) {
            editProfileImageLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null && data.hasExtra("imageUrl")) {
                                String imageUrl = data.getStringExtra("imageUrl");
                                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                                    // Update Firestore with the new profile picture
                                    String userId = mAuth.getCurrentUser().getUid();
                                    new FirestoreManager(userId).updateUserProfileImage(userId, imageUrl,
                                            aVoid -> Toast.makeText(CreateAccount.this, "Profile picture updated", Toast.LENGTH_SHORT).show(),
                                            e -> Toast.makeText(CreateAccount.this, "Failed to update profile picture", Toast.LENGTH_SHORT).show());
                                } else {
                                    Toast.makeText(CreateAccount.this, "No valid image selected", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
            );
        }

        createButton.setOnClickListener(v -> {
            if (isEditMode) {
                updateProfile();
            } else {
                createNewAccount();
            }
        });

        backButton.setOnClickListener(v -> handleBackNavigation());
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.create_name_text);
        usernameEditText = findViewById(R.id.create_username_text);
        dobEditText = findViewById(R.id.create_date_of_birth_text);
        emailEditText = findViewById(R.id.create_email_text);
        passwordEditText = findViewById(R.id.create_user_password_text);
        repeatedPasswordEditText = findViewById(R.id.repeat_user_password_text);
        usernameFeedbackText = findViewById(R.id.create_username_feedback);
        createButton = findViewById(R.id.create_button);
        backButton = findViewById(R.id.back_button);
        tvAddPhoto = findViewById(R.id.tvAddPhoto);
    }

    private void setupUIForMode() {
        if (isEditMode) {
            TextView title = findViewById(R.id.create_profile_text);
            title.setText("Edit Profile");
            createButton.setText("Save Changes");
            tvAddPhoto.setVisibility(View.VISIBLE);
            tvAddPhoto.setOnClickListener(v -> {
                Intent intent = new Intent(CreateAccount.this, UploadImageActivity.class);
                editProfileImageLauncher.launch(intent);
            });

            // Hide email and password fields
            emailEditText.setVisibility(View.GONE);
            passwordEditText.setVisibility(View.GONE);
            repeatedPasswordEditText.setVisibility(View.GONE);

            loadUserData();
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            nameEditText.setText(documentSnapshot.getString("name"));
                            usernameEditText.setText(documentSnapshot.getString("username"));
                            dobEditText.setText(documentSnapshot.getString("dob"));
                            existingUsername = documentSnapshot.getString("username");
                        }
                    });
        }
    }

    private void setupDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        final DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel(calendar);
        };

        dobEditText.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    CreateAccount.this,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void setupUsernameValidation() {
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkUsername(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateDateLabel(Calendar calendar) {
        String dateFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        dobEditText.setText(sdf.format(calendar.getTime()));
    }

    private void checkUsername(String username) {
        if (!ValidationUtils.isUsernameValid(username)) {
            usernameFeedbackText.setText("Invalid username! Use 3-25 characters (letters, numbers, underscores)");
            usernameAvailable = false;
            return;
        }

        if (isEditMode && username.equals(existingUsername)) {
            usernameAvailable = true;
            usernameFeedbackText.setText("Available");
            return;
        }

        db.collection("usernames").document(username).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean exists = task.getResult().exists();
                        usernameAvailable = !exists;
                        usernameFeedbackText.setText(exists ? "Username taken" : "Available");
                    } else {
                        usernameAvailable = true;
                        usernameFeedbackText.setText("Available");
                    }
                });
    }

    private boolean isValidPassword(String password) {
        return ValidationUtils.isPasswordValid(password);
    }

    void createNewAccount() {
        String name = nameEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String repeatedPassword = repeatedPasswordEditText.getText().toString().trim();

        if (validateNewAccountFields(name, username, dob, email, password, repeatedPassword)) return;

        if (SKIP_AUTH_FOR_TESTING_CREATE_ACCOUNT) {
            handleTestingMode(name, username, dob, email);
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        addUserToFirestore(user, name, username, dob, email);
                        Intent intent = new Intent(CreateAccount.this, UploadProfilePictureActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        showSnackbar("Registration failed: " + task.getException().getMessage());
                    }
                });
    }

    private boolean validateNewAccountFields(String name, String username, String dob, String email, String password, String repeatedPassword) {
        if (name.isEmpty() || username.isEmpty() || dob.isEmpty() ||
                email.isEmpty() || password.isEmpty() || repeatedPassword.isEmpty()) {
            showSnackbar("Please fill all fields!");
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValidationUtils.isDateValid(dob)) {
            showSnackbar("Invalid date format. Use dd/mm/yyyy.");
            return true;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showSnackbar("Invalid email format!");
            return true;
        }

        if (!isValidPassword(password)) {
            showSnackbar("Password must contain at least 6 characters, one uppercase letter, one lowercase letter, and one number.");
            return true;
        }

        if (!password.equals(repeatedPassword)) {
            showSnackbar("Passwords do not match!");
            return true;
        }

        if (!usernameAvailable) {
            showSnackbar("Username not available.");
            return true;
        }
        return false;
    }

    private void updateProfile() {
        String name = nameEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();

        if (name.isEmpty() || username.isEmpty() || dob.isEmpty()) {
            showSnackbar("Please fill all fields!");
            return;
        }

        if (!username.equals(existingUsername) && !usernameAvailable) {
            showSnackbar("Username not available.");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            WriteBatch batch = db.batch();
            DocumentReference userRef = db.collection("users").document(user.getUid());

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("username", username);
            updates.put("dob", dob);
            batch.update(userRef, updates);

            if (!username.equals(existingUsername)) {
                updateUsernameReferences(batch, user.getUid(), username);
            }

            batch.commit()
                    .addOnSuccessListener(unused -> {
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> showSnackbar("Update failed: " + e.getMessage()));
        }
    }

    private void updateUsernameReferences(WriteBatch batch, String uid, String newUsername) {
        // Remove old username
        DocumentReference oldUsernameRef = db.collection("usernames").document(existingUsername);
        batch.delete(oldUsernameRef);

        // Add new username
        DocumentReference newUsernameRef = db.collection("usernames").document(newUsername);
        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", uid);
        usernameData.put("email", mAuth.getCurrentUser().getEmail());
        batch.set(newUsernameRef, usernameData);
    }

    private void handleTestingMode(String name, String username, String dob, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("username", username);
        userData.put("dob", dob);
        userData.put("email", email);

        Intent intent = new Intent(CreateAccount.this, FeedManagerActivity.class);
        startActivity(intent);
        finish();
    }

    void addUserToFirestore(FirebaseUser user, String name, String username, String dob, String email) {
        WriteBatch batch = db.batch();

        DocumentReference userRef = db.collection("users").document(user.getUid());
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("dob", dob);
        userData.put("name", name);
        batch.set(userRef, userData);

        DocumentReference usernameRef = db.collection("usernames").document(username);
        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", user.getUid());
        usernameData.put("email", email);
        batch.set(usernameRef, usernameData);

        batch.commit()
                .addOnSuccessListener(unused -> {})
                .addOnFailureListener(e -> {
                    user.delete();
                    showSnackbar("Registration failed: " + e.getMessage());
                });
    }

    private void handleBackNavigation() {
        if (isEditMode) {
            finish();
        } else {
            startActivity(new Intent(CreateAccount.this, MainActivity.class));
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}