package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
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
public class FeedManagerActivityTest {

    @Rule
    public ActivityScenarioRule<FeedManagerActivity> scenario = new ActivityScenarioRule<>(FeedManagerActivity.class);

    @BeforeClass
    public static void setup() {
        // Specific address for emulated device to access our localHost
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }

    @Test
    public void testLoadTheirMoodEvents() {
        // Check if the "Their Mood" tab is displayed by default
        onView(withId(R.id.btnTheirMood)).check(matches(isDisplayed()));

        // Check if mood events are loaded
        onView(withText("Sample Mood Event")).check(matches(isDisplayed()));
    }

    @Test
    public void testSwitchToMyMoodEvents() {
        // Click on the "My Mood" tab
        onView(withId(R.id.btnMyMood)).perform(click());

        // Check if the "My Mood" tab is displayed
        onView(withId(R.id.btnMyMood)).check(matches(isDisplayed()));

        // Check gitif mood events are loaded
        onView(withText("My Mood Event")).check(matches(isDisplayed()));
    }

    @Test
    public void testAddMoodEvent() {
        // Click on the add mood event button
        onView(withId(R.id.fabAddMood)).perform(click());

        // Check if the AddMoodEventActivity is displayed
        onView(withId(R.id.tvGreeting)).check(matches(isDisplayed()));
    }
}