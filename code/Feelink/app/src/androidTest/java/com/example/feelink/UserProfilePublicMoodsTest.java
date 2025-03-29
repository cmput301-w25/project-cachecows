package com.example.feelink;

import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.WriteBatch;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class UserProfilePublicMoodsTest {
    private static final String TAG = "PublicMoodsTest";
    private static final String TEST_USER_ID = "test_user_id";

    @Before
    public void setup() {

        // Configure Firestore to use the emulator
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setHost("10.0.2.2:8080") // Emulator host
                .setSslEnabled(false)
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        // Seed test data
        seedTestMoods();
    }

    private void seedTestMoods() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create test user document
        Map<String, Object> user = new HashMap<>();
        user.put("username", "test_user");
        user.put("followers", 0);
        user.put("following", 0);
        db.collection("users").document(TEST_USER_ID).set(user);

        // Public Mood 1
        Map<String, Object> publicMood1 = new HashMap<>();
        publicMood1.put("userId", TEST_USER_ID);
        publicMood1.put("emotionalState", "Happy");
        publicMood1.put("timestamp", new Date());
        publicMood1.put("isPublic", true);
        db.collection("mood_events").document("publicMood1").set(publicMood1);

        // Public Mood 2
        Map<String, Object> publicMood2 = new HashMap<>();
        publicMood2.put("userId", TEST_USER_ID);
        publicMood2.put("emotionalState", "Sad");
        publicMood2.put("timestamp", new Date());
        publicMood2.put("isPublic", true);
        db.collection("mood_events").document("publicMood2").set(publicMood2);

        // Private Mood (should not appear)
        Map<String, Object> privateMood = new HashMap<>();
        privateMood.put("userId", TEST_USER_ID);
        privateMood.put("emotionalState", "Angry");
        privateMood.put("timestamp", new Date());
        privateMood.put("isPublic", false);
        db.collection("mood_events").document("privateMood").set(privateMood);

        Log.d(TAG, "Test data seeded");
    }

    @Test
    public void verifyPublicMoodsDisplayed() {
        // Bypass authentication
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = true;

        // Launch activity in test mode
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileActivity.class);
        intent.putExtra("TEST_MODE", true); // Filter test user's moods
        ActivityScenario.launch(intent);

        // Wait for Firestore data to load (adjust as needed)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify RecyclerView shows exactly 2 public moods
        onView(withId(R.id.recyclerMoodEvents))
                .check(matches(hasChildCount(2)));
    }

    @After
    public void cleanup() {
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = false;
        deleteTestData();
    }
    private void deleteTestData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        // Delete user document
        DocumentReference userRef = db.collection("users").document(TEST_USER_ID);
        batch.delete(userRef);

        // Delete mood documents
        String[] moodIds = {"publicMood1", "publicMood2", "privateMood"};
        for (String moodId : moodIds) {
            DocumentReference moodRef = db.collection("mood_events").document(moodId);
            batch.delete(moodRef);
        }

        try {
            // Commit and wait for batch deletion
            Tasks.await(batch.commit());
            Log.d(TAG, "Test data cleaned up successfully");
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error cleaning up test data", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}