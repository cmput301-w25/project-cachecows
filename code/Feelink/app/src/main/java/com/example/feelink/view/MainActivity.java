package com.example.feelink.view;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.feelink.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Entry point of the application
 * It displays the main screen with options to log in or create an account
 * User Stories:
 * US 1.01.01.01 (UI for Mood Event creation entry point)
 * Navigates to Logic and Create Account screens
 */
public class MainActivity extends AppCompatActivity {
    private Button loginButton, createAccountButton;

    /**
     * Called when activity is created
     * Initializes UI components and event listeners
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        //Enables Firestore Offline Persistence
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)  //this sets the persistence enabled and makes Firestore cache data locally
                .build();
        db.setFirestoreSettings(settings);


        TextView appName = findViewById(R.id.app_name);
        loginButton = findViewById(R.id.button_login);
        createAccountButton = findViewById(R.id.button_create_account);

        //Applies a gradient shader to the app name text.
        //Based on a StackOverflow answer by Dustin:
        //https://stackoverflow.com/questions/2680607/text-with-gradient-in-android
        Shader shader = new LinearGradient(0,0,0,appName.getLineHeight(),
                0xA9B0D8DA, 0xA9C79BE7, Shader.TileMode.REPEAT);
        appName.getPaint().setShader(shader);


        loginButton.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, Login.class);
            startActivity(i);
        });

        createAccountButton.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, CreateAccount.class);
            startActivity(i);
        });

    }
}