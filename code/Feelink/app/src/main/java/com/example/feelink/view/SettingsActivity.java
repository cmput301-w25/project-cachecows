package com.example.feelink.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.feelink.R;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    // Account section
    private Button editProfileButton;
    private Button securityButton;
    private Button notificationsButton;
    private Button privacyButton;

    // Support & About section
    private Button subscriptionButton;
    private Button supportButton;
    private Button termsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Log.d(TAG, "Settings Activity created");

        // Initialize all views
        initializeViews();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        // Account section
        editProfileButton = findViewById(R.id.editProfile);
        securityButton = findViewById(R.id.security);
        notificationsButton = findViewById(R.id.notifications);
        privacyButton = findViewById(R.id.privacy);

        // Support & About section
        subscriptionButton = findViewById(R.id.subscription);
        supportButton = findViewById(R.id.support);
        termsButton = findViewById(R.id.terms);
    }

    private void setupClickListeners() {
        // Account section listeners
        editProfileButton.setOnClickListener(v -> {
            // Navigate to Edit Profile screen
            Toast.makeText(this, "Edit Profile clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
            // startActivity(intent);
        });

        securityButton.setOnClickListener(v -> {
            // Navigate to Security settings
            Toast.makeText(this, "Security clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(SettingsActivity.this, SecurityActivity.class);
            // startActivity(intent);
        });

        notificationsButton.setOnClickListener(v -> {
            // Navigate to Notifications settings
            Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(SettingsActivity.this, NotificationsActivity.class);
            // startActivity(intent);
        });

        privacyButton.setOnClickListener(v -> {
            // Navigate to Privacy settings
            Toast.makeText(this, "Privacy clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(SettingsActivity.this, PrivacyActivity.class);
            // startActivity(intent);
        });

        // Support & About section listeners
        subscriptionButton.setOnClickListener(v -> {
            // Navigate to Subscription details
            Toast.makeText(this, "My Subscription clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(SettingsActivity.this, SubscriptionActivity.class);
            // startActivity(intent);
        });

        supportButton.setOnClickListener(v -> {
            // Navigate to Help & Support
            Toast.makeText(this, "Help & Support clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(SettingsActivity.this, SupportActivity.class);
            // startActivity(intent);
        });

        termsButton.setOnClickListener(v -> {
            // Navigate to Terms and Policies
            Toast.makeText(this, "Terms and Policies clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(SettingsActivity.this, TermsActivity.class);
            // startActivity(intent);
        });
    }

    // Add back button functionality to return to UserProfileActivity
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // This will use the default back stack behavior
    }
}