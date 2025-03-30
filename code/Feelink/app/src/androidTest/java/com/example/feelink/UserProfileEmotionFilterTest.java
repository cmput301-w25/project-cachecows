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
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class UserProfileEmotionFilterTest {  //tests run, just give permission for maps
    private static final String TAG = "EmotionFilterTest";
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

        // Happy moods
        Map<String, Object> happyMood1 = createMood("Happy", new Date());
        db.collection("mood_events").document("happyMood1").set(happyMood1);

        Map<String, Object> happyMood2 = createMood("Happy", new Date());
        db.collection("mood_events").document("happyMood2").set(happyMood2);

        // Sad moods
        Map<String, Object> sadMood1 = createMood("Sad", new Date());
        db.collection("mood_events").document("sadMood1").set(sadMood1);

        // Angry moods
        Map<String, Object> angryMood1 = createMood("Angry", new Date());
        db.collection("mood_events").document("angryMood1").set(angryMood1);

        Map<String, Object> angryMood2 = createMood("Angry", new Date());
        db.collection("mood_events").document("angryMood2").set(angryMood2);

        Log.d(TAG, "Test data seeded");
    }

    private Map<String, Object> createMood(String emotion, Date timestamp) {
        Map<String, Object> mood = new HashMap<>();
        mood.put("userId", TEST_USER_ID);
        mood.put("emotionalState", emotion);
        mood.put("timestamp", timestamp);
        mood.put("isPublic", true);
        return mood;
    }

    @Test
    public void testHappyFilter() {
        runEmotionFilterTest("Happy", 2);
    }

    @Test
    public void testSadFilter() {
        runEmotionFilterTest("Sad", 1);
    }

    @Test
    public void testAngryFilter() {
        runEmotionFilterTest("Angry", 2);
    }

    private void runEmotionFilterTest(String emotion, int expectedCount) {
        // Bypass authentication
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = true;

        // Launch activity in test mode
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileActivity.class);
        intent.putExtra("TEST_MODE", true);
        ActivityScenario.launch(intent);

        // Wait for initial data load
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Open filter menu and navigate to emotion submenu
        onView(withId(R.id.filterButton)).perform(ViewActions.click());
        onView(withText("Emotional States")).perform(ViewActions.click());

        // Select the target emotion
        onView(withText(emotion)).perform(ViewActions.click());

        // Wait for filtered data load
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Verify expected count
        onView(withId(R.id.recyclerMoodEvents))
                .check(matches(hasChildCount(expectedCount)));
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
        String[] moodIds = {"happyMood1", "happyMood2", "sadMood1", "angryMood1", "angryMood2"};
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