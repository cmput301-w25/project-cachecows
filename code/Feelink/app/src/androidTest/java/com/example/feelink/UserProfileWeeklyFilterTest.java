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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class UserProfileWeeklyFilterTest {
    private static final String TAG = "WeeklyFilterTest";
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

        // Get dates
        Calendar cal = Calendar.getInstance();
        Date now = new Date();

        // Mood within 7 days
        Map<String, Object> recentMood1 = new HashMap<>();
        recentMood1.put("userId", TEST_USER_ID);
        recentMood1.put("emotionalState", "Happy");
        recentMood1.put("timestamp", now);
        recentMood1.put("isPublic", true);
        db.collection("mood_events").document("recentMood1").set(recentMood1);

        // Mood within 7 days (2 days ago)
        cal.add(Calendar.DAY_OF_YEAR, -2);
        Map<String, Object> recentMood2 = new HashMap<>();
        recentMood2.put("userId", TEST_USER_ID);
        recentMood2.put("emotionalState", "Sad");
        recentMood2.put("timestamp", cal.getTime());
        recentMood2.put("isPublic", true);
        db.collection("mood_events").document("recentMood2").set(recentMood2);

        // Mood older than 7 days (8 days ago)
        cal.add(Calendar.DAY_OF_YEAR, -6); // 2+6=8 days old
        Map<String, Object> oldMood = new HashMap<>();
        oldMood.put("userId", TEST_USER_ID);
        oldMood.put("emotionalState", "Angry");
        oldMood.put("timestamp", cal.getTime());
        oldMood.put("isPublic", true);
        db.collection("mood_events").document("oldMood").set(oldMood);

        Log.d(TAG, "Test data seeded");
    }

    @Test
    public void verifyWeeklyFilter() {
        // Bypass authentication
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = true;

        // Launch activity in test mode
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileActivity.class);
        intent.putExtra("TEST_MODE", true);
        ActivityScenario.launch(intent);

        // Wait for initial data load
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Open filter menu and select week filter
        onView(withId(R.id.filterButton)).perform(ViewActions.click());
        onView(withText("Last 7 Days")).perform(ViewActions.click());

        // Wait for filtered data load
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Verify RecyclerView shows exactly 2 recent moods
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
        String[] moodIds = {"recentMood1", "recentMood2", "oldMood"};
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