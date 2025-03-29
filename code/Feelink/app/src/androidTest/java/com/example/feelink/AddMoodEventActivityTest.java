package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddMoodEventActivityTest {

    @Rule
    public ActivityScenarioRule<UserProfileActivity> activityRule =
            new ActivityScenarioRule<>(UserProfileActivity.class);

    // This will run once before any test methods, ensuring the Firestore emulator is configured early.
    @BeforeClass
    public static void setUpBeforeClass() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.useEmulator("10.0.2.2", 8100);
    }

    @Before
    public void setup() throws InterruptedException {
        // Removed the useEmulator() call from here because it must be done only once before instance initialization.
        // Enable test mode to skip auth checks
        AddMoodEventActivity.enableTestMode(true);
        UserProfileActivity.enableTestMode(true);
        // Wait for initial setup
        Thread.sleep(1000);
    }

    @Test
    public void testAddMoodWithoutAuthIssues() throws InterruptedException {
        // Wait for UserProfileActivity to load
        Thread.sleep(2000);

        // Open AddMoodEventActivity via FAB
        onView(withId(R.id.fabAddMood)).perform(click());

        // Select mood
        onView(withId(R.id.moodHappy)).perform(click());

        // Enter reason
        onView(withId(R.id.etReason))
                .perform(typeText("Test Mood"), closeSoftKeyboard());

        // Submit form
        onView(withId(R.id.btnAddMood)).perform(click());

        // Wait for Firestore write and activity transition
        Thread.sleep(3000);

        // Verify in RecyclerView
        onView(withId(R.id.recyclerMoodEvents))
                .check(matches(hasDescendant(withText("Test Mood"))));
    }

    @After
    public void cleanup() {
        // Delete test data
        FirebaseFirestore.getInstance().collection("mood_events")
                .whereEqualTo("reason", "Test Mood")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().delete();
                        }
                    }
                });

        // Reset test mode
        AddMoodEventActivity.enableTestMode(false);
        UserProfileActivity.enableTestMode(false);
    }
}
