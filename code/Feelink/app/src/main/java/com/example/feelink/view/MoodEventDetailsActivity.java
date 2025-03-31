package com.example.feelink.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.feelink.R;
import com.google.firebase.firestore.FirebaseFirestore;
/**
 * Displays detailed view of individual mood events
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 01.04.01 - Mood history detail viewing</li>
 *   <li>US 01.05.01 - Mood context visualization</li>
 * </ul>
 */

public class MoodEventDetailsActivity extends AppCompatActivity {

    private TextView moodStateTextView, reasonTextView, socialSituationTextView, dateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_event_details);

        moodStateTextView = findViewById(R.id.moodStateTextView);
        reasonTextView = findViewById(R.id.reasonTextView);
        socialSituationTextView = findViewById(R.id.socialSituationTextView);
        dateTextView = findViewById(R.id.dateTextView);

        Intent intent = getIntent();
        String moodEventId = intent.getStringExtra("moodEventId");

        loadMoodEventDetails(moodEventId);
    }
    /**
     * Fetches and displays complete mood event data from Firestore
     * @param moodEventId Document ID of the mood event to display
     *
     * <p>Retrieves:
     * <ul>
     *   <li>Emotional state classification</li>
     *   <li>Social context metadata</li>
     *   <li>Timestamped occurrence data</li>
     * </ul>
     */

    private void loadMoodEventDetails(String moodEventId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("moodEvents").document(moodEventId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String moodState = documentSnapshot.getString("moodState");
                String reason = documentSnapshot.getString("reason");
                String socialSituation = documentSnapshot.getString("socialSituation");
                String date = documentSnapshot.getDate("date").toString();

                moodStateTextView.setText(moodState);
                reasonTextView.setText(reason);
                socialSituationTextView.setText(socialSituation);
                dateTextView.setText(date);
            }
        });
    }
}
