package com.example.feelink;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmailAddress;
    private Button btnRecoverPassword;
    private ProgressBar progressBar;
    private ImageView backArrow;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Bind XML components using your existing IDs
        etEmailAddress = findViewById(R.id.etEmailAddress);
        btnRecoverPassword = findViewById(R.id.btnRecoverPassword);
        progressBar = findViewById(R.id.progressBar);
        backArrow = findViewById(R.id.backArrow);

        // Set click listener for recover password button
        btnRecoverPassword.setOnClickListener(v -> resetPassword());

        // Handle back arrow click
        backArrow.setOnClickListener(v -> finish());
    }

    private void resetPassword() {
        String email = etEmailAddress.getText().toString().trim();

        // Validate email input
        if (email.isEmpty()) {
            etEmailAddress.setError("Email is required");
            etEmailAddress.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailAddress.setError("Please provide valid email");
            etEmailAddress.requestFocus();
            return;
        }

        // Show progress and send reset email
        progressBar.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Check your email to reset your password",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Failed to send reset email. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}