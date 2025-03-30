package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class MoodEventFlowTest {

    private static final String TEST_USER_ID = "testUserId123";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "testuser@example.com";
    private static final String TEST_PASSWORD = "P@ssw0rd";

    @Rule
    public ActivityScenarioRule<UserProfileActivity> scenario =
            new ActivityScenarioRule<>(UserProfileActivity.class);

    @Before
    public void setup() {
        // Enable test mode for auth skipping
        AddMoodEventActivity.SKIP_AUTH_FOR_TESTING = true;
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = true;

        // Seed database with test user and mood event
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // 1. Create user in Authentication
        auth.createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD);

        // 2. Create user document in Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", TEST_USERNAME);
        userData.put("email", TEST_EMAIL);
        userData.put("followers", 0);
        userData.put("following", 0);
        db.collection("users").document(TEST_USER_ID).set(userData);

        // 3. Create username mapping
        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", TEST_USER_ID);
        usernameData.put("email", TEST_EMAIL);
        db.collection("usernames").document(TEST_USERNAME).set(usernameData);

        SystemClock.sleep(3000); // Wait for Firestore to sync
    }

    @Test
    public void testCompleteMoodEventFlow() {
        // Start at UserProfileActivity (auth is skipped)

        // 1. Click the add mood button
        onView(withId(R.id.fabAddMood)).perform(click());

        // Wait for AddMoodEventActivity to load
        SystemClock.sleep(1000);

        // 2. Select a mood (Happy)
        onView(withId(R.id.moodHappy)).perform(click());

        // 3. Enter a reason
        onView(withId(R.id.etReason))
                .perform(replaceText("Feeling great today!"));

        // 4. Select social situation
        onView(withId(R.id.socialSituationSpinner))
                .perform(click());
        onView(withText("With one other person"))
                .perform(click());

        // 5. Save the mood event
        onView(withId(R.id.btnAddMood)).perform(click());

        // Wait for the mood to be saved and return to UserProfileActivity
        SystemClock.sleep(3000);

        // 6. Verify the mood appears in the list
        onView(withText("Feeling great today!"))
                .check(matches(isDisplayed()));

        // 7. Verify the mood count increased
        onView(withId(R.id.moodPosts))
                .check(matches(withText("1")));
    }

    @After
    public void cleanup() {
        // Disable test mode
        AddMoodEventActivity.SKIP_AUTH_FOR_TESTING = false;
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = false;

        // Clear test data
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(TEST_USER_ID).delete();
        db.collection("usernames").document(TEST_USERNAME).delete();

        // Delete auth user
        FirebaseAuth.getInstance().signOut();
        FirebaseAuth.getInstance().getCurrentUser().delete();
    }
}
