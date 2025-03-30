package com.example.feelink;

import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.endsWith;

@RunWith(AndroidJUnit4.class)
public class FeedManagerReasonSearchTest {
    private static final String TAG = "FeedReasonSearchTest";
    private static final String TEST_USER_ID = "test_user_id";
    private static final String USER_A_ID = "user_a_id";
    private static final String USER_B_ID = "user_b_id";
    private static final String USER_C_ID = "user_c_id"; // Non-followed user

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
        createUser(db, USER_C_ID, "userC"); // Non-followed user

        // Establish follows
        followUser(TEST_USER_ID, USER_A_ID);
        followUser(TEST_USER_ID, USER_B_ID);

        // Create mood events with reasons
        // Followed users' moods
        createMoodWithReason(USER_A_ID, "mood1", "Happy", "Great coffee shop find", new Date());
        createMoodWithReason(USER_B_ID, "mood2", "Sad", "Missed morning meeting", new Date());
        createMoodWithReason(USER_A_ID, "mood3", "Angry", "Traffic jam ruined plans", new Date());
        createMoodWithReason(USER_B_ID, "mood4", "Happy", "Coffee with colleagues", new Date());

        // Non-followed user's mood (should not appear)
        createMoodWithReason(USER_C_ID, "mood5", "Neutral", "Casual coffee drink", new Date());

        Log.d(TAG, "Test data with reasons seeded");
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

    private void createMoodWithReason(String userId, String moodId, String emotion, String reason, Date timestamp) throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> mood = new HashMap<>();
        mood.put("userId", userId);
        mood.put("emotionalState", emotion);
        mood.put("timestamp", timestamp);
        mood.put("isPublic", true);
        mood.put("reason", reason);
        Tasks.await(db.collection("mood_events").document(moodId).set(mood));
    }

    @Test
    public void testFollowingMoodsReasonSearch() {
        runReasonSearchTest("coffee", 2); // Should find mood1 and mood4
    }

    @Test
    public void testNoResultsFromNonFollowed() {
        runReasonSearchTest("casual", 0); // mood5 from userC shouldn't appear
    }

    private void runReasonSearchTest(String query, int expectedCount) {
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = true;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), FeedManagerActivity.class);
        intent.putExtra("TEST_MODE", true);
        intent.putExtra("FORCE_USER_ID", TEST_USER_ID);
        ActivityScenario.launch(intent);

        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Navigate to Following Moods section
        onView(withId(R.id.btnFollowingMoods)).perform(click());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Open filter and initiate search
        onView(withId(R.id.btnFilter)).perform(click());
        onView(withText("Search by Reason")).perform(click());

        // Input search query
        onView(allOf(
                withClassName(endsWith("SearchView$SearchAutoComplete")),
                isDisplayed()
        )).perform(
                click(),
                typeText(query),
                ViewActions.closeSoftKeyboard()
        );

        // Wait for search results
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Verify results count
        onView(withId(R.id.recyclerMoodEvents))
                .check(matches(hasChildCount(expectedCount)));
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
        batch.delete(db.collection("users").document(USER_C_ID));

        // Delete moods
        String[] moodIds = {"mood1", "mood2", "mood3", "mood4", "mood5"};
        for (String moodId : moodIds) {
            batch.delete(db.collection("mood_events").document(moodId));
        }

        Tasks.await(batch.commit());
        Log.d(TAG, "Test data cleaned up");
    }
}