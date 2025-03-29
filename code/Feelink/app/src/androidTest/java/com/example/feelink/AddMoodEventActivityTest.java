package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.not;

import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddMoodEventActivityTest {

    @Rule
    public ActivityScenarioRule<AddMoodEventActivity> activityRule = new ActivityScenarioRule<>(AddMoodEventActivity.class);

    @Before
    public void setup() {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.useEmulator("10.0.2.2", 8080);

            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.useEmulator("10.0.2.2", 9099);

            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(false)
                    .build();
            db.setFirestoreSettings(settings);

            Thread.sleep(500); // Ensure connection is established
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void seedDatabase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Use Firestore emulator for testing
        db.useEmulator("10.0.2.2", 8080);

        // Seed user profile data
        String userId = "testUserId123";
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", "testuser");
        userData.put("email", "testuser@example.com");
        userData.put("profileImageUrl", "https://example.com/profile.jpg");

        db.collection("users").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> Log.d("SeedDatabase", "User profile seeded successfully"))
                .addOnFailureListener(e -> Log.e("SeedDatabase", "Error seeding user profile", e));

        // Optionally seed mood events for the user (if needed for tests)
        List<Map<String, Object>> moodEvents = Arrays.asList(
                new HashMap<String, Object>() {{
                    put("reason", "Feeling happy");
                    put("emotion", "Happy");
                    put("timestamp", FieldValue.serverTimestamp());
                }},
                new HashMap<String, Object>() {{
                    put("reason", "Feeling sad");
                    put("emotion", "Sad");
                    put("timestamp", FieldValue.serverTimestamp());
                }}
        );

        for (Map<String, Object> event : moodEvents) {
            db.collection("users").document(userId).collection("moodEvents").add(event)
                    .addOnSuccessListener(documentReference -> Log.d("SeedDatabase", "Mood event seeded successfully"))
                    .addOnFailureListener(e -> Log.e("SeedDatabase", "Error seeding mood event", e));
        }
    }



    @AfterClass
    public static void tearDown() {
        String projectId = "feelink-database-test";

        try {
            URL url = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");

            int responseCode = connection.getResponseCode();
            Log.i("TearDown", "Response Code: " + responseCode);

            connection.disconnect();

        } catch (IOException e) {
            Log.e("TearDown Error", Objects.requireNonNull(e.getMessage()));
            e.printStackTrace();
        }
    }


    @Test(timeout = 2000)
    public void testAddMoodEventWithInvalidReason() {
        // Select a mood
        onView(withId(R.id.moodHappy)).perform(click());

        // Enter an invalid reason (too long)
        onView(withId(R.id.etReason)).perform(typeText("This is an intentionally long string that exceeds 200 characters to test input validation. It contains multiple sentences and various punctuation marks to simulate realistic user input. The purpose is to verify that your application properly handles and validates lengthy text inputs, especially in fields like reason or description that might have character limits."),
                closeSoftKeyboard());

        // Check that there's an error message on the reason field
        onView(withId(R.id.etReason)).check(matches(hasErrorText("Reason must be limited to 200 characters")));

        // Check that the button is disabled
        onView(withId(R.id.btnAddMood)).check(matches(not(isEnabled())));

        // Clear the invalid reason and enter a valid one
        onView(withId(R.id.etReason)).perform(replaceText("Valid"), closeSoftKeyboard());

        // Ensure the button is now enabled
        onView(withId(R.id.btnAddMood)).check(matches(isEnabled()));
    }

    @Test(timeout = 2000)
    public void testMoodEventAppearsInProfile() {
        // Launch UserProfileActivity with dummy user
        Intent profileIntent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                UserProfileActivity.class);
        profileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ActivityScenario<UserProfileActivity> profileScenario = ActivityScenario.launch(profileIntent);

        // Click on FAB to navigate to AddMoodEventActivity
        onView(withId(R.id.fabAddMood)).perform(click());

        // Select a mood icon and add reason
        onView(withId(R.id.moodHappy)).perform(click());
        onView(withId(R.id.etReason)).perform(typeText("Test Mood"), closeSoftKeyboard());
        onView(withId(R.id.btnAddMood)).perform(click());

        // Wait for Firestore operation to complete
        try {
            Thread.sleep(2000); // Adjust delay if needed
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Return to UserProfileActivity and verify mood event appears in RecyclerView
        profileScenario.close(); // Close AddMoodEventActivity scenario

        ActivityScenario<UserProfileActivity> updatedProfileScenario = ActivityScenario.launch(profileIntent);

        try {
            Thread.sleep(5000); // Ensure RecyclerView has loaded data from Firestore
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.recyclerMoodEvents))
                .check(matches(hasDescendant(withText("Test Mood"))));
    }

    @AfterClass
    public static void ClearEmulator() {
        String projectId = "feelink-database-test";

        try {
            URL url = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");

            int responseCode = connection.getResponseCode();
            Log.i("TearDown", "Response Code: " + responseCode);

            connection.disconnect();

        } catch (IOException e) {
            Log.e("TearDown Error", Objects.requireNonNull(e.getMessage()));
            e.printStackTrace();
        }
    }

}