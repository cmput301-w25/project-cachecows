package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FeedManagerActivityTest {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_UID = "test_user_id";

    // Enable test mode in FeedManagerActivity
    static {
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = true;
    }

    @Rule
    public ActivityScenarioRule<FeedManagerActivity> activityRule =
            new ActivityScenarioRule<>(FeedManagerActivity.class);

    @Before
    public void setUp() {
        Intents.init();

        // Initialize Firebase if needed
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }

        // Get Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Seed test data
        seedTestData();
    }

    @After
    public void tearDown() {
        Intents.release();
        // Clean up test data
        cleanupTestData();
    }

    private void seedTestData() {
        // Create test user in usernames collection
        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", TEST_UID);
        usernameData.put("email", TEST_EMAIL);
        db.collection("usernames").document(TEST_USERNAME).set(usernameData);

        // Create test mood events for "My Mood" tab
        for (int i = 0; i < 3; i++) {
            Map<String, Object> myMoodEvent = new HashMap<>();
            myMoodEvent.put("mood", "Happy");
            myMoodEvent.put("description", "Test mood " + i);
            myMoodEvent.put("timestamp", System.currentTimeMillis());
            myMoodEvent.put("userId", TEST_UID);
            db.collection("moodEvents").document("myMood" + i).set(myMoodEvent);
        }

        // Create test mood events for "Their Mood" tab
        for (int i = 0; i < 3; i++) {
            Map<String, Object> theirMoodEvent = new HashMap<>();
            theirMoodEvent.put("mood", "Excited");
            theirMoodEvent.put("description", "Friend's mood " + i);
            theirMoodEvent.put("timestamp", System.currentTimeMillis());
            theirMoodEvent.put("userId", "other_user_id");
            theirMoodEvent.put("shared", true);
            db.collection("moodEvents").document("theirMood" + i).set(theirMoodEvent);
        }
    }

    private void cleanupTestData() {
        // Remove test user
        db.collection("usernames").document(TEST_USERNAME).delete();

        // Remove test mood events
        for (int i = 0; i < 3; i++) {
            db.collection("moodEvents").document("myMood" + i).delete();
            db.collection("moodEvents").document("theirMood" + i).delete();
        }
    }

    @Test
    public void testSwitchToMyMoodTab() {
        // Click on My Mood tab
        onView(withId(R.id.btnMyMood)).perform(click());

        // Verify My Mood tab is displayed
        onView(withText("My Mood")).check(matches(isDisplayed()));

        // Verify recycler view is displayed with mood events
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));
    }

    @Test
    public void testSwitchToTheirMoodTab() {
        // Click on Their Mood tab
        onView(withId(R.id.btnTheirMood)).perform(click());

        // Verify Their Mood tab is displayed
        onView(withText("Their Mood")).check(matches(isDisplayed()));

        // Verify recycler view is displayed with mood events
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));
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

        // Click on filter button
        onView(withId(R.id.btnFilter)).perform(click());

        // Additional assertions would be added here to verify filter dialog appears
    }

    @Test
    public void testAddMoodButtonNavigation() {
        // Click on the Add Mood FAB
        onView(withId(R.id.fabAddMood)).perform(click());

        // Verify navigation to AddMoodEventActivity
        // This would require additional Espresso intent verification
    }
}
