package com.example.feelink;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

/**
 * Implements login functionality using Firestore
 * Users log in with their username and password, after which corresponding email is retrieved to perform Firebase Authentication
 * Navigation to Forgot Username and Forgot Password Activities
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
     * Called when activity is created
     * Initializes UI components, Firebase & event listeners
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
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

        //Navigation to Forgot username/password activities or return back
        forgotUsername.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, com.example.feelink.ForgotUsernameActivity.class));
        });

        forgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, com.example.feelink.ForgotPasswordActivity.class));
        });

        backButton.setOnClickListener(v->{
            startActivity(new Intent(Login.this, com.example.feelink.MainActivity.class));
        });
    }

    /**
     * log in the user by retrieving their email from Firestore
     */
    private void loginUserAccount() {
        String username = usernameTextView.getText().toString().trim();
        String password = passwordTextView.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_LONG).show();
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
                            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Invalid username", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //https://firebase.google.com/docs/auth/android/password-auth#java

    /**
     * Authenticates the user with Firebase using the email and password.
     * @param email     email registered with the username
     * @param password  The user's password
     */
    private void logInWithEmail(String email, String password){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Login.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(Login.this, com.example.feelink.FeedManagerActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(Login.this,
                                    "Login failed" ,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}



