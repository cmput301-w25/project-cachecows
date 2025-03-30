package com.example.feelink;

import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

@RunWith(AndroidJUnit4.class)
public class UserProfilePrivateMoodsTest {
    private static final String TAG = "PrivateMoodsTest";
    private static final String TEST_USER_ID = "test_user_id";

    @Before
    public void setup() {
        // Configure Firestore emulator
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setHost("10.0.2.2:8080")
                .setSslEnabled(false)
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

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

        // Private Mood 1
        Map<String, Object> privateMood1 = new HashMap<>();
        privateMood1.put("userId", TEST_USER_ID);
        privateMood1.put("emotionalState", "Confused");
        privateMood1.put("timestamp", new Date());
        privateMood1.put("isPublic", false);
        db.collection("mood_events").document("privateMood1").set(privateMood1);

        // Private Mood 2
        Map<String, Object> privateMood2 = new HashMap<>();
        privateMood2.put("userId", TEST_USER_ID);
        privateMood2.put("emotionalState", "Anxious");
        privateMood2.put("timestamp", new Date());
        privateMood2.put("isPublic", false);
        db.collection("mood_events").document("privateMood2").set(privateMood2);

        // Public Mood (should not appear in private view)
        Map<String, Object> publicMood = new HashMap<>();
        publicMood.put("userId", TEST_USER_ID);
        publicMood.put("emotionalState", "Happy");
        publicMood.put("timestamp", new Date());
        publicMood.put("isPublic", true);
        db.collection("mood_events").document("publicMood").set(publicMood);

        Log.d(TAG, "Test data seeded");
    }

    @Test
    public void verifyPrivateMoodsDisplayed() {
        // Bypass authentication
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = true;

        // Launch activity in test mode
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileActivity.class);
        intent.putExtra("TEST_MODE", true);
        ActivityScenario.launch(intent);

        // Wait for initial data load
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Toggle to private mode
        onView(withId(R.id.togglePrivacy))
                .perform(ViewActions.click());

        // Wait for private data load
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Verify RecyclerView shows exactly 2 private moods
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
        String[] moodIds = {"privateMood1", "privateMood2", "publicMood"};
        for (String moodId : moodIds) {
            DocumentReference moodRef = db.collection("mood_events").document(moodId);
            batch.delete(moodRef);
        }

        try {
            Tasks.await(batch.commit());
            Log.d(TAG, "Test data cleaned up");
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Cleanup error", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}