package com.example.feelink;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class AddMoodEventActivityTest {

    @Rule
    public ActivityScenarioRule<AddMoodEventActivity> scenario =
            new ActivityScenarioRule<>(AddMoodEventActivity.class);

    @Test
    public void testMoodSelection() {
        onView(withId(R.id.moodHappy)).perform(click());
        onView(withId(R.id.moodHappy)).check(matches(ViewMatchers.isSelected()));
        onView(withId(R.id.moodSad)).perform(click());
        onView(withId(R.id.moodSad)).check(matches(ViewMatchers.isSelected()));
        onView(withId(R.id.moodHappy)).check(matches(ViewMatchers.isNotSelected()));
    }

    @Test
    public void testAddMoodEventWithoutSelection() {
        onView(withId(R.id.btnAddMood)).perform(click());
        onView(withText("Please select a mood")).check(matches(isDisplayed()));
    }

    @Test
    public void testAddMoodEventWithSelection() {
        onView(withId(R.id.moodHappy)).perform(click());
        onView(withId(R.id.etReason)).perform(typeText("Test reason"));
        onView(withId(R.id.etTrigger)).perform(typeText("Test trigger"));
        onView(withId(R.id.etSocialSituation)).perform(typeText("Test situation"));

        Espresso.closeSoftKeyboard();

        onView(withId(R.id.btnAddMood)).perform(click());

        // You might want to check for a success message or navigation to another screen here
        // For example:
        // onView(withText("Mood added successfully")).check(matches(isDisplayed()));
    }
}
