package com.example.projectcachecows;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

/**
 * User account creation process
 * Validates user input, checks username availability, creates Firebase Authentication accounts and stores data in Firestore
 */
public class CreateAccount extends AppCompatActivity {
    private EditText nameEditText, usernameEditText, dobEditText,
        emailEditText, passwordEditText, repeatedPasswordEditText;
    private TextView usernameFeedbackText;
    private Button createButton;
    private ImageView backButton;
    private static final String TAG = "CreateAccountActivity";
    private CollectionReference usersRef;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean usernameAvailable = false;

    // Adapted regex from Stackoverflow by JvdV:
    // https://stackoverflow.com/questions/62361928/how-to-validate-username-and-password-on-android-using-java-regex
    private static final String VALID_USERNAME = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,25}$"; //at least 1 letter, min 3 characters, max 25
    private static final String VALID_PASSWORD = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9!@#$%^&*()_+\\-=]{6,}$"; //at least 1 digit, 1 lowercase letter, 1 uppercase, min 6 characters

    /**
     * Called when activity is created
     * Initializes UI components, Firebase & event listeners
     * Real time username validation
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");

        nameEditText= findViewById(R.id.create_name_text);
        usernameEditText = findViewById(R.id.create_username_text);
        dobEditText = findViewById(R.id.create_date_of_birth_text);
        emailEditText = findViewById(R.id.create_email_text);
        passwordEditText = findViewById(R.id.create_user_password_text);
        repeatedPasswordEditText = findViewById(R.id.repeat_user_password_text);
        usernameFeedbackText = findViewById(R.id.create_username_feedback);
        createButton = findViewById(R.id.create_button);
        backButton = findViewById(R.id.back_button);

        //real-time username validation using textWatcher
        //https://www.geeksforgeeks.org/how-to-implement-textwatcher-in-android/
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

        createButton.setOnClickListener(v -> createNewAccount());

        backButton.setOnClickListener(v->{
            startActivity(new Intent(CreateAccount.this, MainActivity.class));
        });

    }

    /**
     * checks if the username being entered is valid and available in Firestore
     * @param username
     */
    private void checkUsername(String username){

        if (!username.matches(VALID_USERNAME)){
            usernameFeedbackText.setText("Invalid username! Use 3-25 characters (letters, numbers, underscores only)!");
            usernameAvailable = false;
            return;
        }


        if (username.isEmpty()) {
            usernameFeedbackText.setText("Username cannot be empty!");
            usernameAvailable = false;
            return;
        }

        usersRef.whereEqualTo("username", username).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().isEmpty()) {
                    usernameFeedbackText.setText("Available");
                    usernameAvailable = true;
                } else {
                    usernameFeedbackText.setText("Username already taken");
                    usernameAvailable = false;
                }
            } else {
                usernameFeedbackText.setText("Error checking username.");
                usernameAvailable = false;
            }
        });
    }

    /**
     * confirms the given password with regex
     * @param password
     * @return
     */
    private boolean isValidPassword(String password) {
        return password.matches(VALID_PASSWORD);
    }

    /**
     * User account creation process.
     * Validates all user input and registers the user with Firebase
     */
    private void createNewAccount(){
        String name = nameEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String repeatedPassword = repeatedPasswordEditText.getText().toString().trim();

        if (name.isEmpty() || username.isEmpty() || dob.isEmpty() ||
                email.isEmpty() || password.isEmpty() || repeatedPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        //Email validation
        //Based on a StackOverflow answer by gaurav jain:
        //https://stackoverflow.com/questions/77226668/how-allow-email-using-email-validation-regex-in-android
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "Password must contain at least 6 characters, one uppercase letter, one lowercase letter, and one number.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.equals(repeatedPassword)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!usernameAvailable) {
            Toast.makeText(this, "Username not available.", Toast.LENGTH_SHORT).show();
            return;
        }


       // Create user with Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        addUserToFirestore(user, name, username, dob, email);
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(CreateAccount.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

    }

    //Adapted from:
    // https://stackoverflow.com/questions/68910946/how-to-check-if-the-particular-username-exists-in-the-firebase-java-android

    /**
     * Stores all the information about the user in the Firebase user object
     *
     * @param user authenticated FirebaseUser object
     * @param name user's full name
     * @param username unique username
     * @param dob user's date of birth
     * @param email user's email address
     */
    private void addUserToFirestore(FirebaseUser user, String name, String username, String dob, String email){
        HashMap<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("username", username);
        userData.put("dob", dob);
        userData.put("email", email);
        userData.put("uid", user.getUid());


        usersRef.document(user.getUid()).set(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Registration successful!");
                        Intent i = new Intent(CreateAccount.this,FeedManagerActivity.class);
                        startActivity(i);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error:", e);
                    }
                });

    }
}
