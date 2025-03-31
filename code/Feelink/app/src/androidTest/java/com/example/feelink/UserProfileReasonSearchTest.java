package com.example.feelink;

import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.feelink.view.UserProfileActivity;
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

//-----------------------------------------
//To run these tests, allow map permissions
//-----------------------------------------


@RunWith(AndroidJUnit4.class)
public class UserProfileReasonSearchTest {
    private static final String TAG = "ReasonSearchTest";
    private static final String TEST_USER_ID = "test_user_id";

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

        seedTestMoodsWithReasons();
    }

    private void seedTestMoodsWithReasons() throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create test user document synchronously
        Map<String, Object> user = new HashMap<>();
        user.put("username", "test_user");
        user.put("followers", 0);
        user.put("following", 0);
        Tasks.await(db.collection("users").document(TEST_USER_ID).set(user));

        // Create moods with synchronous writes
        Tasks.await(db.collection("mood_events").document("mood1")
                .set(createMoodWithReason("Happy", "Enjoying morning coffee", new Date())));
        Tasks.await(db.collection("mood_events").document("mood2")
                .set(createMoodWithReason("Sad", "Missed important meeting", new Date())));
        Tasks.await(db.collection("mood_events").document("mood3")
                .set(createMoodWithReason("Angry", "Traffic jam made me late", new Date())));
        Tasks.await(db.collection("mood_events").document("mood4")
                .set(createMoodWithReason("Happy", "Coffee with friends", new Date())));

        Log.d(TAG, "Test data with reasons seeded");
    }

    private Map<String, Object> createMoodWithReason(String emotion, String reason, Date timestamp) {
        Map<String, Object> mood = new HashMap<>();
        mood.put("userId", TEST_USER_ID);
        mood.put("emotionalState", emotion);
        mood.put("timestamp", timestamp);
        mood.put("isPublic", true);
        mood.put("reason", reason);
        return mood;
    }

    @Test
    public void testExactMatchSearch() {
        runReasonSearchTest("coffee", 2);
    }

    @Test
    public void testPartialMatchNoResults() {
        runReasonSearchTest("jamm", 0);
    }

    @Test
    public void testCaseInsensitiveSearch() {
        runReasonSearchTest("MEETING", 1);
    }

    private void runReasonSearchTest(String query, int expectedCount) {
        // Bypass authentication and force test user ID
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = true;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileActivity.class);
        intent.putExtra("TEST_MODE", true);
        intent.putExtra("FORCE_USER_ID", TEST_USER_ID);
        ActivityScenario.launch(intent);

        // Wait for initial data load
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Open filter menu and select search
        onView(withId(R.id.filterButton)).perform(click());
        onView(withText("Search by Reason")).perform(click());

        // Target the internal SearchView text field
        onView(allOf(
                withClassName(endsWith("SearchView$SearchAutoComplete")),
                isDisplayed()
        )).perform(
                click(),
                typeText(query),
                ViewActions.closeSoftKeyboard()
        );

        // Extended wait for search processing
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Verify results
        onView(withId(R.id.recyclerMoodEvents))
                .check(matches(hasChildCount(expectedCount)));
    }


    @After
    public void cleanup() throws ExecutionException, InterruptedException {
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = false;
        deleteTestData();
    }

    private void deleteTestData() throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        batch.delete(db.collection("users").document(TEST_USER_ID));
        String[] moodIds = {"mood1", "mood2", "mood3", "mood4"};
        for (String moodId : moodIds) {
            batch.delete(db.collection("mood_events").document(moodId));
        }

        Tasks.await(batch.commit());
        Log.d(TAG, "Test data cleaned up");
    }
}