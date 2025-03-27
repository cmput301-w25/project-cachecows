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


/**
 * Handles user authentication flow with username/email reconciliation
 *
 * <p>Manages:
 * <ul>
 *   <li>Username-based login resolution</li>
 *   <li>Firebase Authentication integration</li>
 *   <li>Credential error handling</li>
 *   <li>Password recovery navigation</li>
 * </ul>
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 03.01.01.01 - Username validation integration</li>
 *   <li>US 03.01.01.02 - Username/email mapping verification</li>
 *   <li>US 03.01.01.03 - Username constraint enforcement</li>
 * </ul>
 *
 */
public class Login extends AppCompatActivity {
    private EditText usernameTextView, passwordTextView;
    private Button loginButton;
    private ImageView backButton;
    private TextView forgotUsername, forgotPassword;
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    /**
     * Initializes authentication UI and service connections
     *
     * <p>Configures:
     * <ol>
     *   <li>Firebase service instances</li>
     *   <li>View binding</li>
     *   <li>Click handlers for:
     *     <ul>
     *       <li>Primary login</li>
     *       <li>Password recovery</li>
     *       <li>Username recovery (Yet to be Implemented)</li>
     *       <li>Back navigation</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param savedInstanceState Persisted state data
     */
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
    /**
     * Initiates authentication sequence with username resolution
     *
     * <p>Workflow:
     * <ol>
     *   <li>Validates input presence</li>
     *   <li>Resolves username to email via Firestore</li>
     *   <li>Triggers email/password authentication</li>
     * </ol>
     *
     * <p>Implements username constraint validation through Firestore lookup
     * (US 03.01.01.03)</p>
     */
    private void loginUserAccount() {
        String username = usernameTextView.getText().toString().trim();
        String password = passwordTextView.getText().toString().trim();

        if (!ValidationUtils.areCredentialsValid(username, password)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.empty_field, Snackbar.LENGTH_LONG).setDuration(5000).show();
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
                            Snackbar.make(findViewById(android.R.id.content), R.string.invalid_cred, Snackbar.LENGTH_SHORT).setDuration(5000).show(); // invalid user data
                        }
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), R.string.invalid_cred, Snackbar.LENGTH_SHORT).setDuration(5000).show(); // invalid username
                    }
                });
    }

    /**
     * Handles email-based authentication after username resolution
     *
     * <p>Ensures clean authentication state by:
     * <ol>
     *   <li>Clearing cached credentials</li>
     *   <li>Attempting fresh login</li>
     * </ol>
     *
     * @param email Resolved email address from username lookup
     * @param password Raw password input
     */
    private void logInWithEmail(String email, String password) {
        // Initial login attempt - clear cached credentials
        mAuth.signOut();
        attemptLogin(email, password);
    }

    /**
     * Executes Firebase authentication with error handling
     *
     * <p>Manages:
     * <ul>
     *   <li>Success routing to main feed</li>
     *   <li>Invalid credential recovery flows</li>
     *   <li>Error state feedback</li>
     * </ul>
     *
     * @param email Verified email address
     * @param password Raw password input
     */

    private void attemptLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Snackbar.make(findViewById(android.R.id.content), R.string.successful_login, Snackbar.LENGTH_SHORT).setDuration(5000).show();
                        startActivity(new Intent(Login.this, FeedManagerActivity.class));
                        finish();
                    } else {
                        if (task.getException().getMessage().contains("INVALID_LOGIN_CREDENTIALS")) {
                            handleInvalidCredentials(email, password);
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), R.string.invalid_cred, Snackbar.LENGTH_SHORT).setDuration(5000).show(); //"Error: " + task.getException().getMessage()
                        }
                    }
                });
    }

    /**
     * Handles credential mismatch scenarios with email verification
     *
     * <p>Checks for email updates in usernames collection and retries
     * authentication if needed. Implements US 03.01.01.02 validation
     * through real-time username/email mapping checks.</p>
     *
     * @param originalEmail Initially resolved email address
     * @param password Raw password input
     */
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
                            Snackbar.make(findViewById(android.R.id.content), R.string.invalid_cred, Snackbar.LENGTH_SHORT).setDuration(5000).show();
                        }
                    }
                });
    }
}