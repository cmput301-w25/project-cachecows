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
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.feelink.view.AddMoodEventActivity;
import com.example.feelink.view.FeedManagerActivity;
import com.example.feelink.view.UserProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.junit.After;
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
import java.util.Objects;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddMoodEventActivityTest {

    @Rule
    public ActivityScenarioRule<AddMoodEventActivity> activityRule = new ActivityScenarioRule<>(AddMoodEventActivity.class);

    @Before
    public void setup() {
        try {
            // Connect to Firestore emulator
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.useEmulator("10.0.2.2", 8080);
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(false)
                    .build();
            db.setFirestoreSettings(settings);

            // Connect to Auth emulator if needed
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);

            // Wait briefly to ensure connection is established
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void setupForTesting() {
        try {
            // Add UserProfileActivity to testing flags
            Field userProfileField = UserProfileActivity.class.getDeclaredField("SKIP_AUTH_FOR_TESTING");
            userProfileField.setAccessible(true);
            userProfileField.set(null, true);

            // Existing FeedManager and AddMoodEventActivity flags
            Field feedField = FeedManagerActivity.class.getDeclaredField("SKIP_AUTH_FOR_TESTING");
            feedField.setAccessible(true);
            feedField.set(null, true);

            Field addMoodField = AddMoodEventActivity.class.getDeclaredField("SKIP_AUTH_FOR_TESTING");
            addMoodField.setAccessible(true);
            addMoodField.set(null, true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @After
    public void tearDown() {
        Intents.release();
    }

    @Test(timeout = 30000)
    public void testAddMoodEventWithInvalidReason() {
        // Select a mood
        onView(withId(R.id.moodHappy)).perform(click());

        // Create a 201-character string
        String invalidReason = new String(new char[201]).replace('\0', 'a');

        // Enter an invalid reason (exceeds 200 characters)
        onView(withId(R.id.etReason)).perform(
                typeText(invalidReason),
                closeSoftKeyboard()
        );

        // Check for new error message
        onView(withId(R.id.etReason)).check(matches(
                hasErrorText("Reason must be limited to 200 characters")
        ));

        // Verify button is disabled
        onView(withId(R.id.btnAddMood)).check(matches(not(isEnabled())));

        // Replace with valid 200-character reason
        String validReason = new String(new char[200]).replace('\0', 'a');
        onView(withId(R.id.etReason)).perform(
                replaceText(validReason),
                closeSoftKeyboard()
        );

        // Verify button becomes enabled
        onView(withId(R.id.btnAddMood)).check(matches(isEnabled()));
    }



    @Test(timeout = 10000)
    public void testMoodEventAppearsInFeed() {
        // Add a PUBLIC mood
        onView(withId(R.id.moodHappy)).perform(click());
        onView(withId(R.id.etReason)).perform(typeText("Test public mood"), closeSoftKeyboard());
        onView(withId(R.id.togglePrivacy)).perform(click()); // Set to public (toggle starts as public)
        onView(withId(R.id.btnAddMood)).perform(click());

        // Add PRIVATE mood
        onView(withId(R.id.moodHappy)).perform(click());
        onView(withId(R.id.etReason)).perform(typeText("Test private mood"), closeSoftKeyboard());
        onView(withId(R.id.togglePrivacy)).perform(click()); // Toggle to private
        onView(withId(R.id.btnAddMood)).perform(click());

        // Verify public mood appears in public feed
        verifyMoodVisibility(true, "Test public mood");

        // Verify private mood doesn't appear in public feed
        verifyMoodVisibility(false, "Test private mood");
    }


    private void verifyMoodVisibility(boolean shouldBeVisible, String moodText) {
        // Launch profile with test user context
        Intent profileIntent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                UserProfileActivity.class);
        profileIntent.putExtra("TEST_MODE", true);
        ActivityScenario<UserProfileActivity> profileScenario = ActivityScenario.launch(profileIntent);

        // Check visibility based on privacy setting
        if (shouldBeVisible) {
            onView(withId(R.id.recyclerMoodEvents))
                    .check(matches(hasDescendant(withText(moodText))));
        } else {
            onView(withId(R.id.recyclerMoodEvents))
                    .check(matches(not(hasDescendant(withText(moodText)))));
        }
    }

    @After
    public void ClearEmulators() {
        String projectId = "feelink-database-test";
        URL url = null;
        try {
            url = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
        } catch (MalformedURLException exception) {
            Log.e("URL Error", Objects.requireNonNull(exception.getMessage()));
        }
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");
            int response = urlConnection.getResponseCode();
            Log.i("Response Code", "Response Code: " + response);
        } catch (IOException exception) {
            Log.e("IO Error", Objects.requireNonNull(exception.getMessage()));
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

}