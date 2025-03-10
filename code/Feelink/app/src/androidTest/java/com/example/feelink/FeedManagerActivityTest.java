package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.containsString;

import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
//import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FeedManagerActivityTest {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Test@123";
    private static final String TEST_UID = "test_user_id";

    // Set test mode flag to skip authentication
    static {
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = true;
    }

    @Rule
    public ActivityScenarioRule<FeedManagerActivity> activityRule =
            new ActivityScenarioRule<>(FeedManagerActivity.class);

    @Before
    public void setUp() {
        Intents.init();

        // Initialize Firebase
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Seed test data
        seedTestDatabase();
    }

    @After
    public void tearDown() {
        Intents.release();
        cleanupTestDatabase();
    }

    private void seedTestDatabase() {
        // Create a latch to wait for async operations
        CountDownLatch latch = new CountDownLatch(3);

        // Create test user in usernames collection
        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", TEST_UID);
        usernameData.put("email", TEST_EMAIL);
        db.collection("usernames").document(TEST_USERNAME)
                .set(usernameData)
                .addOnSuccessListener(aVoid -> latch.countDown())
                .addOnFailureListener(e -> latch.countDown());

        // Create test mood events for "My Mood" tab
        List<Map<String, Object>> myMoodEvents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> event = new HashMap<>();
            event.put("mood", "Happy");
            event.put("description", "Test mood " + i);
            event.put("timestamp", System.currentTimeMillis() - (i * 3600000)); // Different times
            event.put("userId", TEST_UID);
            event.put("shared", false);

            db.collection("moodEvents").document("myMood" + i)
                    .set(event)
                    .addOnSuccessListener(aVoid -> {})
                    .addOnFailureListener(e -> {});

            myMoodEvents.add(event);
        }
        latch.countDown();

        // Create test mood events for "Their Mood" tab
        List<Map<String, Object>> theirMoodEvents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> event = new HashMap<>();
            event.put("mood", "Excited");
            event.put("description", "Friend's mood " + i);
            event.put("timestamp", System.currentTimeMillis() - (i * 3600000)); // Different times
            event.put("userId", "other_user_id");
            event.put("shared", true);

            db.collection("moodEvents").document("theirMood" + i)
                    .set(event)
                    .addOnSuccessListener(aVoid -> {})
                    .addOnFailureListener(e -> {});

            theirMoodEvents.add(event);
        }
        latch.countDown();

        try {
            // Wait for database operations to complete
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void cleanupTestDatabase() {
        // Create a latch to wait for async operations
        CountDownLatch latch = new CountDownLatch(7);

        // Remove test user
        db.collection("usernames").document(TEST_USERNAME)
                .delete()
                .addOnSuccessListener(aVoid -> latch.countDown())
                .addOnFailureListener(e -> latch.countDown());

        // Remove test mood events
        for (int i = 0; i < 3; i++) {
            db.collection("moodEvents").document("myMood" + i)
                    .delete()
                    .addOnSuccessListener(aVoid -> latch.countDown())
                    .addOnFailureListener(e -> latch.countDown());

            db.collection("moodEvents").document("theirMood" + i)
                    .delete()
                    .addOnSuccessListener(aVoid -> latch.countDown())
                    .addOnFailureListener(e -> latch.countDown());
        }

        try {
            // Wait for database operations to complete
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSwitchToMyMoodTab() {
        // Click on My Mood tab
        onView(withId(R.id.btnMyMood)).perform(click());

        // Verify My Mood tab is displayed
        onView(withText("My Mood")).check(matches(isDisplayed()));

        // Verify recycler view is displayed
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));

        // Verify tab selection is updated
        // This would require a custom matcher to check background tint
    }

    @Test
    public void testSwitchToTheirMoodTab() {
        // Click on Their Mood tab
        onView(withId(R.id.btnTheirMood)).perform(click());

        // Verify Their Mood tab is displayed
        onView(withText("Their Mood")).check(matches(isDisplayed()));

        // Verify recycler view is displayed
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));

        // Verify tab selection is updated
        // This would require a custom matcher to check background tint
    }

    @Test
    public void testAddMoodButtonIsDisplayed() {
        // Verify the FAB for adding mood is displayed
        onView(withId(R.id.fabAddMood)).check(matches(isDisplayed()));
    }

    @Test
    public void testFilterButtonIsDisplayed() {
        // Verify the filter button is displayed
        onView(withId(R.id.btnFilter)).check(matches(isDisplayed()));
    }

    @Test
    public void testFilterButtonClick() {
        // Click on filter button
        onView(withId(R.id.btnFilter)).perform(click());

        // This would normally verify that a filter dialog appears
        // Since showFilterOptions() is not implemented yet, we can't test the dialog
        // But we can verify the button is clickable
    }

    @Test
    public void testAddMoodButtonNavigation() {
        // Click on the Add Mood FAB
        onView(withId(R.id.fabAddMood)).perform(click());

        // Verify navigation to AddMoodEventActivity
        Intents.intended(IntentMatchers.hasComponent(AddMoodEventActivity.class.getName()));
    }

    @Test
    public void testEmptyStateHandling() {
        // This test would verify that empty state is handled correctly
        // Since checkEmptyState() is not implemented yet, we can't fully test this
        // But we can ensure the activity loads without crashing

        // First switch to My Mood tab
        onView(withId(R.id.btnMyMood)).perform(click());

        // Then switch to Their Mood tab
        onView(withId(R.id.btnTheirMood)).perform(click());

        // Verify the activity is still displayed
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));
    }

    @Test
    public void testTabSelectionVisualFeedback() {
        // Click on My Mood tab
        onView(withId(R.id.btnMyMood)).perform(click());

        // Verify My Mood tab is displayed
        onView(withText("My Mood")).check(matches(isDisplayed()));

        // Click on Their Mood tab
        onView(withId(R.id.btnTheirMood)).perform(click());

        // Verify Their Mood tab is displayed
        onView(withText("Their Mood")).check(matches(isDisplayed()));

        // Note: Visual verification of color changes would require custom matchers
    }

}
