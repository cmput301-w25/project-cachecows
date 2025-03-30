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
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class AddMoodEventTest {

    @Before
    public void seedDatabase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersRef = db.collection("users");

        // Add a seed user to Firestore
        Map<String, Object> user = new HashMap<>();
        user.put("uid", "testUserId123");
        user.put("username", "testUser");
        usersRef.document("testUserId123").set(user);

        // Optionally, create a Firebase Authentication user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInWithEmailAndPassword("testuser@example.com", "P@ssw0rd");
    }

    @Test
    public void testAddMoodEvent() {
        // Skip authentication for testing
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = true;

        // Launch UserProfileActivity
        ActivityScenario.launch(UserProfileActivity.class);

        // Click on the FloatingActionButton to navigate to AddMoodEventActivity
        onView(withId(R.id.fabAddMood)).perform(click());

        // Verify AddMoodEventActivity is displayed
        onView(withId(R.id.btnAddMood)).check(matches(isDisplayed()));

        // Fill in mood details and save the mood event
        onView(withId(R.id.moodHappy)).perform(click()); // Select "Happy" mood
        onView(withId(R.id.etReason)).perform(replaceText("Feeling great!"));
        onView(withId(R.id.btnAddMood)).perform(click());

        // Wait for Firestore to sync (adjust delay if necessary)
        SystemClock.sleep(5000);

        // Verify navigation back to UserProfileActivity
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));

        // Verify the newly added mood event is displayed in the RecyclerView
        onView(withText("Feeling great!")).check(matches(isDisplayed()));
    }
}
