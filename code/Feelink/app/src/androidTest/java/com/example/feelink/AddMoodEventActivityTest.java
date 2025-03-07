package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.not;

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
    }

    @Test
    public void testAddMoodEventWithInvalidReason() throws InterruptedException {
        // Select a mood
        onView(withId(R.id.moodHappy)).perform(click());

        // Enter an invalid reason (too long)
        onView(withId(R.id.etReason)).perform(typeText("This reason is way too long and should trigger an error"));

        // Wait for the button to be disabled
        Thread.sleep(1000); // Adjust the delay as needed

        // Check that the button is disabled
        onView(withId(R.id.btnAddMood)).check(matches(not(isEnabled())));

        // Clear the invalid reason and enter a valid one
        onView(withId(R.id.etReason)).perform(replaceText("Valid reason"));

        // Wait for the button to be enabled
        Thread.sleep(1000); // Adjust the delay as needed

        // Ensure the button is now enabled
        onView(withId(R.id.btnAddMood)).check(matches(isEnabled()));

        // Perform the click action
        onView(withId(R.id.btnAddMood)).perform(click());

        // Verify the result (e.g., check if the mood event is added)
        onView(withText("Mood added successfully!")).check(matches(isDisplayed()));
    }
}