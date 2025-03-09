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

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

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
    public ActivityScenarioRule<AddMoodEventActivity> activityRule = new ActivityScenarioRule<>(AddMoodEventActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }
    @BeforeClass
    public static void disableAnimations() {
        // Disable animations for all tests
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("settings put global window_animation_scale 0");
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("settings put global transition_animation_scale 0");
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("settings put global animator_duration_scale 0");
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testAddMoodEventWithValidData() {
        onView(withId(R.id.moodHappy)).perform(click());
        onView(withId(R.id.etReason)).perform(typeText("Feeling great!"));
        onView(withId(R.id.btnAddMood)).perform(click());
        onView(withId(R.id.tvGreeting)).check(matches(isDisplayed()));
    } // working

    @Test
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
    } // working, now only checks if btnAddMood is enabled once all valid entries are in


    @Test
    public void testMoodEventAppearsInFeed() {
        // 1. Add a mood in AddMoodEventActivity
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

        // 3. Launch FeedManagerActivity with clear task flags
        Intent feedIntent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                FeedManagerActivity.class);
        feedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ActivityScenario<FeedManagerActivity> feedScenario = ActivityScenario.launch(feedIntent);

        // Wait for the feed activity to load
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // 4. Switch to "My Mood" tab and ensure it's fully visible
        onView(withId(R.id.btnMyMood)).perform(click());

        // Wait for UI thread to be idle after tab switch
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Small delay to ensure RecyclerView has loaded data from Firestore
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 5. Verify the mood appears in the RecyclerView
        onView(withId(R.id.recyclerMoodEvents))
                .check(matches(hasDescendant(withText("Test mood"))));
    }

//    @Test
//    public void testMoodEventAppearsInFeed() {
//        // 1. Add a mood in AddMoodEventActivity
//        onView(withId(R.id.moodHappy)).perform(click());
//        onView(withId(R.id.etReason)).perform(typeText("Test mood"), closeSoftKeyboard());
//        onView(withId(R.id.btnAddMood)).perform(click());
//
//        // 2. Wait for the operation to complete and activity to finish
//        // You might need to use idling resources for this
//
//        // 3. Launch FeedManagerActivity
//        Intent feedIntent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
//                FeedManagerActivity.class);
//        ActivityScenario<FeedManagerActivity> feedScenario = ActivityScenario.launch(feedIntent);
//
//        // 4. Switch to "My Mood" tab
//        onView(withId(R.id.btnMyMood)).perform(click());
//
////        Thread.sleep(2000); // Waits for 2 seconds
//
//
//        // 5. Verify the mood appears in the RecyclerView
//        onView(withId(R.id.recyclerMoodEvents))
//                .check(matches(hasDescendant(withText("Test mood"))));
//    }
}