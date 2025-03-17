package com.example.feelink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

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
