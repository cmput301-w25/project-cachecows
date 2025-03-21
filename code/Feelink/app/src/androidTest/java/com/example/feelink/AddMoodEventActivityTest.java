package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
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
        // Use reflection to set the testing flag for both activities
        try {
            // Set flag for FeedManagerActivity
            Field feedField = FeedManagerActivity.class.getDeclaredField("SKIP_AUTH_FOR_TESTING");
            feedField.setAccessible(true);
            feedField.set(null, true);

            // Set flag for AddMoodEventActivity
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

    @Test(timeout = 20000)
    public void testAddMoodEventWithInvalidReason() {
        // Select a mood
        onView(withId(R.id.moodHappy)).perform(click());

        // Enter an invalid reason (too long)
        onView(withId(R.id.etReason)).perform(typeText("This reason is way too long and should trigger an error"),
                closeSoftKeyboard());

        // Check that there's an error message on the reason field
        onView(withId(R.id.etReason)).check(matches(hasErrorText("Reason must be limited to 20 characters or 3 words")));

        // Check that the button is disabled
        onView(withId(R.id.btnAddMood)).check(matches(not(isEnabled())));

        // Clear the invalid reason and enter a valid one
        onView(withId(R.id.etReason)).perform(replaceText("Valid"), closeSoftKeyboard());

        // Ensure the button is now enabled
        onView(withId(R.id.btnAddMood)).check(matches(isEnabled()));
    }



    @Test(timeout = 10000)
    public void testMoodEventAppearsInFeed() {
        // Add a mood in AddMoodEventActivity
        onView(withId(R.id.moodHappy)).perform(click());
        onView(withId(R.id.etReason)).perform(typeText("Test mood"), closeSoftKeyboard());
        onView(withId(R.id.btnAddMood)).perform(click());

        // Wait for the UI thread to be idle after clicking the button
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Add a delay to allow Firestore operation to complete
        // This is a compromise since we can't modify FirestoreManager to expose its async state
        try {
            Thread.sleep(2000); // 2 seconds should be enough for the operation to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Launch FeedManagerActivity with clear task flags
        Intent feedIntent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                FeedManagerActivity.class);
        feedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ActivityScenario<FeedManagerActivity> feedScenario = ActivityScenario.launch(feedIntent);

        // Wait for the feed activity to load
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Switch to "My Mood" tab and ensure it's fully visible
        onView(withId(R.id.btnMyMood)).perform(click());

        // Wait for UI thread to be idle after tab switch
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Small delay to ensure RecyclerView has loaded data from Firestore
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify the mood appears in the RecyclerView
        onView(withId(R.id.recyclerMoodEvents))
                .check(matches(hasDescendant(withText("Test mood"))));
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