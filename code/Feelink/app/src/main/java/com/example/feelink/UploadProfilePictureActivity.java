package com.example.feelink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class UploadProfilePictureActivity extends AppCompatActivity {
    public static boolean SKIP_AUTH_FOR_TESTING = false;
    public static String FORCE_USER_ID = null;

    private ActivityResultLauncher<Intent> uploadProfilePictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_profile_image);

        // Register the activity result launcher using ActivityResultContracts.StartActivityForResult
        uploadProfilePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.hasExtra("imageUrl")) {
                            String imageUrl = data.getStringExtra("imageUrl");
                            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                                // Update Firestore with the new profile picture
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                                //For UI testing
                                String userId;
                                if (SKIP_AUTH_FOR_TESTING) {
                                    if (FORCE_USER_ID != null) {
                                        userId = FORCE_USER_ID;
                                    } else {
                                        userId = "test_user_id";
                                    }
                                } else {
                                    userId = Objects.requireNonNull(currentUser).getUid();
                                }
                                FirestoreManager fm = new FirestoreManager(userId);
                                fm.updateUserProfileImage(userId, imageUrl,
                                        aVoid -> {
                                            Intent intent = new Intent(UploadProfilePictureActivity.this, UserProfileActivity.class);
                                            startActivity(intent);
                                            finish();
                                        },
                                        e -> {
                                            Toast.makeText(UploadProfilePictureActivity.this, "Failed to update profile image.", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(UploadProfilePictureActivity.this, UserProfileActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                );
                            } else {
                                Toast.makeText(UploadProfilePictureActivity.this, "No valid image selected.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        Button btnUploadPicture = findViewById(R.id.btnUploadPicture);
        Button btnSkipForNow = findViewById(R.id.btnSkipForNow);

        // Launch UploadImageActivity when the user taps "Upload Picture"
        btnUploadPicture.setOnClickListener(v -> {
            Intent intent = new Intent(UploadProfilePictureActivity.this, UploadImageActivity.class);
            uploadProfilePictureLauncher.launch(intent);
        });

        // If the user chooses to skip, navigate to the FeedManagerActivity
        btnSkipForNow.setOnClickListener(v -> {
            Intent intent = new Intent(UploadProfilePictureActivity.this, FeedManagerActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
