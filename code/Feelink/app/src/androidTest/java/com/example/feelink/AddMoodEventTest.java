package com.example.feelink;

import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.feelink.view.UserProfileActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class AddMoodEventTest {

    private static final String TAG = "AddMoodEventTest";
    private static final String TEST_USER_ID = "test_user_id";

    @BeforeClass
    public static void setupForTesting() {
        try {
            // Set SKIP_AUTH_FOR_TESTING flag for UserProfileActivity
            Field userProfileField = UserProfileActivity.class.getDeclaredField("SKIP_AUTH_FOR_TESTING");
            userProfileField.setAccessible(true);
            userProfileField.set(null, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setup() {
        try {
            // Configure Firestore emulator
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.useEmulator("10.0.2.2", 8080);
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(false)
                    .build();
            db.setFirestoreSettings(settings);

            // Configure Auth emulator
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.useEmulator("10.0.2.2", 9099);

            // Seed test data
            seedTestUser();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void seedTestUser() throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a test user document
        Map<String, Object> user = new HashMap<>();
        user.put("username", "test_user");
        user.put("followers", 0);
        user.put("following", 0);
        Tasks.await(db.collection("users").document(TEST_USER_ID).set(user));

        Log.d(TAG, "Test user seeded");
    }

    @Test
    public void testAddMoodEvent() throws InterruptedException {
        // Launch UserProfileActivity in test mode
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileActivity.class);
        intent.putExtra("TEST_MODE", true); // Add any required extras for testing
        ActivityScenario.launch(intent);

        // Wait for initial data load
        Thread.sleep(2000);

        // Click on the FAB to navigate to AddMoodEventActivity
        onView(withId(R.id.fabAddMood)).perform(click());

        // Verify AddMoodEventActivity is displayed
        onView(withId(R.id.btnAddMood)).check(matches(isDisplayed()));

        // Fill in mood details and save the mood event
        onView(withId(R.id.moodHappy)).perform(click()); // Select "Happy" mood
        onView(withId(R.id.etReason)).perform(replaceText("Feeling great!"));
        onView(withId(R.id.btnAddMood)).perform(click());

        // Wait for Firestore to sync (adjust delay if necessary)
        Thread.sleep(3000);

        // Verify navigation back to UserProfileActivity
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));

        // Verify the newly added mood event is displayed in the RecyclerView
        onView(withText("Feeling great!")).check(matches(isDisplayed()));
    }

//    @After
//    public void cleanup() {
//        try {
//            deleteTestData();
//            Log.d(TAG, "Test data cleaned up");
//        } catch (Exception e) {
//            Log.e(TAG, "Error during cleanup", e);
//        }
//    }

//    private void deleteTestData() throws ExecutionException, InterruptedException {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        WriteBatch batch = db.batch();
//
//        // Delete user document
//        batch.delete(db.collection("users").document(TEST_USER_ID));
//
//        // Delete mood events associated with the test user (if any exist)
//        batch.delete(db.collection("mood_events").document(TEST_USER_ID + "_mood"));
//
//        // Commit batch deletion
//        Tasks.await(batch.commit());
//    }
}
