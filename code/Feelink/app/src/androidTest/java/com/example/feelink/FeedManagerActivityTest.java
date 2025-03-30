package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
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

    // Test User IDs
    private static final String TEST_USER_UID = "test_user";
    private static final String USER_A_UID = "user_a";
    private static final String USER_B_UID = "user_b";
    private static final String USER_C_UID = "user_c";

    // Test User Emails (for auth)
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String USER_A_EMAIL = "user_a@example.com";
    private static final String USER_B_EMAIL = "user_b@example.com";
    private static final String USER_C_EMAIL = "user_c@example.com";
    private static final String TEST_PASSWORD = "Test@123";

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

//         Initialize Firebase
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



    @Test
    public void testSwitchToFollowingMoodsTab() {
        // Click on My Mood tab
        onView(withId(R.id.btnFollowingMoods)).perform(click());

        // Verify My Mood tab is displayed
        onView(withText("Following Moods")).check(matches(isDisplayed()));

        // Verify recycler view is displayed
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));

    }

    @Test
    public void testSwitchToAllMoodsTab() {
        // Click on Their Mood tab
        onView(withId(R.id.btnAllMoods)).perform(click());

        // Verify Their Mood tab is displayed
        onView(withText("All Moods")).check(matches(isDisplayed()));

        // Verify recycler view is displayed
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));

    }

    @Test
    public void testAddMoodButtonIsDisplayed() {
        // Verify the FAB for adding mood is displayed
        onView(withId(R.id.fabAddMood)).check(matches(isDisplayed()));
    }
    @Test
    public void testChatMoodButtonIsDisplayed() {
        onView(withId(R.id.btnChat)).check(matches(isDisplayed()));
    }

    @Test
    public void testFilterButtonIsDisplayed() {
        // Switch to Following Moods tab where filter button is visible
        onView(withId(R.id.btnFollowingMoods)).perform(click());
        // Verify the filter button is displayed
        onView(withId(R.id.btnFilter)).check(matches(isDisplayed()));
    }

    @Test
    public void testFilterButtonClick() {
        // Switch to Following Moods tab where filter button is visible
        onView(withId(R.id.btnFollowingMoods)).perform(click());
        // Click on filter button
        onView(withId(R.id.btnFilter)).perform(click());

        // Since showFilterOptions() is not implemented yet, we can't test the dialog
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
        onView(withId(R.id.btnFollowingMoods)).perform(click());

        // Then switch to Their Mood tab
        onView(withId(R.id.btnAllMoods)).perform(click());

        // Verify the activity is still displayed
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));
    }

    @Test
    public void testTabSelectionVisualFeedback() {
        // Click on My Mood tab
        onView(withId(R.id.btnFollowingMoods)).perform(click());

        // Verify My Mood tab is displayed
        onView(withText("Following Moods")).check(matches(isDisplayed()));

        // Click on Their Mood tab
        onView(withId(R.id.btnAllMoods)).perform(click());

        // Verify Their Mood tab is displayed
        onView(withText("All Moods")).check(matches(isDisplayed()));

    }


    private void seedTestDatabase() {
        CountDownLatch latch = new CountDownLatch(1);

        // 1. Create users with mutual follows
        Map<String, Object> testUser = new HashMap<>();
        testUser.put("following", Arrays.asList(USER_A_UID, USER_B_UID, USER_C_UID)); // Test user follows all

        Map<String, Object> userA = new HashMap<>();
        userA.put("following", Arrays.asList(TEST_USER_UID, USER_B_UID, USER_C_UID)); // User A follows others

        Map<String, Object> userB = new HashMap<>();
        userB.put("following", Arrays.asList(TEST_USER_UID, USER_A_UID, USER_C_UID)); // User B follows others

        Map<String, Object> userC = new HashMap<>();
        userC.put("following", Arrays.asList(TEST_USER_UID, USER_A_UID, USER_B_UID)); // User C follows others

        // 2. Create mood events for all users
        List<Map<String, Object>> testEvents = new ArrayList<>();

        // Test user's events
        addEvent(testEvents, TEST_USER_UID, "Happy", "Test user event 1", System.currentTimeMillis() - 10000);
        addEvent(testEvents, TEST_USER_UID, "Sad", "Test user event 2", System.currentTimeMillis() - 20000);

        // User A's events
        addEvent(testEvents, USER_A_UID, "Angry", "User A event 1", System.currentTimeMillis() - 5000);
        addEvent(testEvents, USER_A_UID, "Sad", "User A event 2", System.currentTimeMillis() - 15000);

        // User B's events
        addEvent(testEvents, USER_B_UID, "Happy", "User B event 1", System.currentTimeMillis() - 8000);
        addEvent(testEvents, USER_B_UID, "Sad", "User B event 2", System.currentTimeMillis() - 18000);

        // User C's events
        addEvent(testEvents, USER_C_UID, "Sad", "User C event 1", System.currentTimeMillis() - 12000);
        addEvent(testEvents, USER_C_UID, "Happy", "User C event 2", System.currentTimeMillis() - 25000);

        FirebaseFirestore.getInstance().runBatch(batch -> {
            // Create users
            batch.set(db.collection("users").document(TEST_USER_UID), testUser);
            batch.set(db.collection("users").document(USER_A_UID), userA);
            batch.set(db.collection("users").document(USER_B_UID), userB);
            batch.set(db.collection("users").document(USER_C_UID), userC);

            // Create mood events
            for (int i = 0; i < testEvents.size(); i++) {
                batch.set(db.collection("moodEvents").document("test_event_" + i), testEvents.get(i));
            }
        }).addOnCompleteListener(task -> latch.countDown());

        try { latch.await(10, TimeUnit.SECONDS); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }



    private void addEvent(List<Map<String, Object>> events, String userId, String mood, String reason, long timestamp) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("mood", mood);
        event.put("reason", reason);
        event.put("timestamp", timestamp);
        event.put("shared", true);
        events.add(event);
    }


    private void cleanupTestDatabase() {
        CountDownLatch latch = new CountDownLatch(12); // 8 events + 4 users

        // Delete users
        deleteDocument("users", TEST_USER_UID, latch);
        deleteDocument("users", USER_A_UID, latch);
        deleteDocument("users", USER_B_UID, latch);
        deleteDocument("users", USER_C_UID, latch);

        // Delete events
        for (int i = 0; i < 8; i++) {
            deleteDocument("moodEvents", "test_event_" + i, latch);
        }

        try { latch.await(10, TimeUnit.SECONDS); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }

    private void deleteDocument(String collection, String docId, CountDownLatch latch) {
        db.collection(collection).document(docId)
                .delete()
                .addOnCompleteListener(task -> latch.countDown());
    }


}