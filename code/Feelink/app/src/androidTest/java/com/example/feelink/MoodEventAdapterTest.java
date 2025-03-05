package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
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
public class MoodEventAdapterTest {

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
    public void testMoodEventDisplay() {
        // Check if the mood event is displayed in the RecyclerView
        onView(withText("Sample Mood Event")).check(matches(isDisplayed()));
    }

    @Test
    public void testMoodEventColor() {
        // Check if the mood event color is displayed correctly
        onView(withId(R.id.cardView)).check(matches(isDisplayed()));
    }

    @Test
    public void testMoodEventIcon() {
        // Check if the mood event icon is displayed correctly
        onView(withId(R.id.ivMoodIcon)).check(matches(isDisplayed()));
    }
}