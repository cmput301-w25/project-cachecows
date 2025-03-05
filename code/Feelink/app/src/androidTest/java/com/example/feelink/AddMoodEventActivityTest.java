package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddMoodEventActivityTest {

    @Rule
    public ActivityScenarioRule<AddMoodEventActivity> scenario = new ActivityScenarioRule<>(AddMoodEventActivity.class);

    @BeforeClass
    public static void setup() {
        // Specific address for emulated device to access our localHost
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }

    @Test
    public void testAddMoodEventWithValidInput() {
        // Select a mood
        onView(withId(R.id.moodHappy)).perform(click());

        // Input reason, trigger, and social situation
        onView(withId(R.id.etReason)).perform(typeText("Feeling great!"));
        onView(withId(R.id.etTrigger)).perform(typeText("Good weather"));
        onView(withId(R.id.etSocialSituation)).perform(typeText("With friends"));

        // Click on the add mood button
        onView(withId(R.id.btnAddMood)).perform(click());

        // Check if the mood event is added and the activity is closed
//        onView(withId(R.id.btnMyMood)).check(matches(isDisplayed()));
    }

    @Test
    public void testAddMoodEventWithoutSelectingMood() {
        // Click on the add mood button without selecting a mood
        onView(withId(R.id.btnAddMood)).perform(click());

        // Check if an error message is displayed
        onView(withText("Please select a mood")).check(matches(isDisplayed()));
    }

    @Test
    public void testAddMoodEventWithEmptyReason() {
        // Select a mood
        onView(withId(R.id.moodHappy)).perform(click());

        // Click on the add mood button without entering a reason
        onView(withId(R.id.btnAddMood)).perform(click());

        // Check if an error message is displayed
        onView(withText("Please enter a reason")).check(matches(isDisplayed()));
    }
}