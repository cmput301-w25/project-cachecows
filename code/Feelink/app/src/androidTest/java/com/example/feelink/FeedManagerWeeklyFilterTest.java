package com.example.feelink;

import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.feelink.view.FeedManagerActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.WriteBatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class FeedManagerWeeklyFilterTest {
    private static final String TAG = "FeedWeeklyFilterTest";
    private static final String TEST_USER_ID = "test_user_id";
    private static final String USER_A_ID = "user_a_id";
    private static final String USER_B_ID = "user_b_id";

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        // Configure Firestore emulator
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setHost("10.0.2.2:8080")
                .setSslEnabled(false)
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        seedTestData();
    }

    private void seedTestData() throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create test users
        createUser(db, TEST_USER_ID, "testUser");
        createUser(db, USER_A_ID, "userA");
        createUser(db, USER_B_ID, "userB");

        // Establish follows
        followUser(TEST_USER_ID, USER_A_ID);
        followUser(TEST_USER_ID, USER_B_ID);

        // Create staggered mood events
        Calendar cal = Calendar.getInstance();

        // Recent mood (2 days old)
        cal.add(Calendar.DAY_OF_YEAR, -2);
        createMood(db, USER_A_ID, "feedMood1", "Happy", cal.getTime());

        // Recent mood (6 days old)
        cal.add(Calendar.DAY_OF_YEAR, -4); // 2+4=6 days old
        createMood(db, USER_B_ID, "feedMood2", "Sad", cal.getTime());

        // Old mood (8 days old)
        cal.add(Calendar.DAY_OF_YEAR, -2); // 6+2=8 days old
        createMood(db, USER_A_ID, "feedMood3", "Angry", cal.getTime());

        Log.d(TAG, "Test data seeded");
    }

    private void createUser(FirebaseFirestore db, String userId, String username) throws ExecutionException, InterruptedException {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("followers", 0);
        user.put("following", 0);
        Tasks.await(db.collection("users").document(userId).set(user));
    }

    private void followUser(String followerId, String followedUserId) throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> followData = new HashMap<>();
        followData.put("uid", followedUserId);
        followData.put("timestamp", FieldValue.serverTimestamp());

        DocumentReference followingRef = db.collection("users")
                .document(followerId)
                .collection("following")
                .document(followedUserId);
        Tasks.await(followingRef.set(followData));
    }

    private void createMood(FirebaseFirestore db, String userId, String moodId, String emotion, Date timestamp) throws ExecutionException, InterruptedException {
        Map<String, Object> mood = new HashMap<>();
        mood.put("userId", userId);
        mood.put("emotionalState", emotion);
        mood.put("timestamp", timestamp);
        mood.put("isPublic", true);
        Tasks.await(db.collection("mood_events").document(moodId).set(mood));
    }

    @Test
    public void testWeeklyFilterInFollowingMoods() {
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = true;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), FeedManagerActivity.class);
        intent.putExtra("TEST_MODE", true);
        intent.putExtra("FORCE_USER_ID", TEST_USER_ID);
        ActivityScenario.launch(intent);

        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Navigate to Following Moods
        onView(withId(R.id.btnFollowingMoods)).perform(click());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Apply weekly filter
        onView(withId(R.id.btnFilter)).perform(click());
        onView(withText("Last 7 Days")).perform(click());

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Verify only 2 recent moods are shown
        onView(withId(R.id.recyclerMoodEvents))
                .check(matches(hasChildCount(2)));
    }

    @After
    public void cleanup() throws ExecutionException, InterruptedException {
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = false;
        deleteTestData();
    }

    private void deleteTestData() throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        // Delete users
        batch.delete(db.collection("users").document(TEST_USER_ID));
        batch.delete(db.collection("users").document(USER_A_ID));
        batch.delete(db.collection("users").document(USER_B_ID));

        // Delete moods
        String[] moodIds = {"feedMood1", "feedMood2", "feedMood3"};
        for (String moodId : moodIds) {
            batch.delete(db.collection("mood_events").document(moodId));
        }

        Tasks.await(batch.commit());
        Log.d(TAG, "Test data cleaned up");
    }
}