package com.example.feelink;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

public class Login extends AppCompatActivity {
    private EditText usernameTextView, passwordTextView;
    private Button loginButton;
    private ImageView backButton;
    private TextView forgotUsername, forgotPassword;
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        usernameTextView = findViewById(R.id.username_text);
        passwordTextView = findViewById(R.id.password_text);
        backButton = findViewById(R.id.back_button);
        loginButton = findViewById(R.id.create_button);
        forgotUsername = findViewById(R.id.forgot_username_link);
        forgotPassword = findViewById(R.id.forgot_password_link);

        loginButton.setOnClickListener(v -> loginUserAccount());

        // Navigation to Forgot username/password activities or return back
        forgotUsername.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, com.example.feelink.ForgotUsernameActivity.class));
        });

        forgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, com.example.feelink.ForgotPasswordActivity.class));
        });

        backButton.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, com.example.feelink.MainActivity.class));
        });
    }
    // changing warning messages to "invalid credentials"
    private void loginUserAccount() {
        String username = usernameTextView.getText().toString().trim();
        String password = passwordTextView.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), R.string.empty_field, Snackbar.LENGTH_LONG).show();
            return;
        }
        db.collection("usernames").document(username).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String uid = document.getString("uid");
                        String email = document.getString("email"); // Retrieve email from usernames
                        if (email != null && !email.isEmpty()) {
                            logInWithEmail(email, password);
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), R.string.invalid_cred, Snackbar.LENGTH_SHORT).show(); // invalid user data
                        }
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), R.string.invalid_cred, Snackbar.LENGTH_SHORT).show(); // invalid username
                    }
                });
    }

    private void logInWithEmail(String email, String password) {
        // Initial login attempt - clear cached credentials
        mAuth.signOut();
        attemptLogin(email, password);
    }

    private void attemptLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Snackbar.make(findViewById(android.R.id.content), "Login Successful!", Snackbar.LENGTH_SHORT).show();
                        startActivity(new Intent(Login.this, FeedManagerActivity.class));
                        finish();
                    } else {
                        if (task.getException().getMessage().contains("INVALID_LOGIN_CREDENTIALS")) {
                            handleInvalidCredentials(email, password);
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), "Error: " + task.getException().getMessage(), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void handleInvalidCredentials(String originalEmail, String password) {
        db.collection("usernames").document(usernameTextView.getText().toString().trim())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String updatedEmail = document.getString("email");
                        if (updatedEmail != null && !updatedEmail.equals(originalEmail)) {
                            // Retry without signing out
                            attemptLogin(updatedEmail, password);
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), R.string.invalid_cred, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}