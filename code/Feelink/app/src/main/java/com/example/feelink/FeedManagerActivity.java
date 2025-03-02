package com.example.feelink;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.feelink.AddMoodEventActivity;
import com.example.feelink.Login;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;

public class FeedManagerActivity extends AppCompatActivity {
    private FloatingActionButton fabAddMood;
    private FirebaseAuth mAuth;

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
        initializeViews();
        setupFAB();
    }

    private void initializeViews() {
        fabAddMood = findViewById(R.id.fabAddMood);
    }

    private void setupFAB() {
        fabAddMood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAuth.getCurrentUser() != null) {
                    navigateToAddMood();
                } else {
                    handleUnauthorizedAccess();
                }
            }
        });
    }

    private void navigateToAddMood() {
        Intent intent = new Intent(FeedManagerActivity.this, AddMoodEventActivity.class);
        startActivity(intent);
    }

    private void handleUnauthorizedAccess() {
        // Redirect to login or show error
        startActivity(new Intent(this, Login.class));
        finish();
    }
}
