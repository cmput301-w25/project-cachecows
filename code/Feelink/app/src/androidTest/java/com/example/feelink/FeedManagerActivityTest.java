package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FeedManagerActivityTest {

    @Rule
    public ActivityScenarioRule<FeedManagerActivity> activityRule = new ActivityScenarioRule<>(FeedManagerActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testSwitchToMyMoodTab() {
        onView(withId(R.id.btnMyMood)).perform(click());
        onView(withText("My Mood")).check(matches(isDisplayed()));
    }

    @Test
    public void testSwitchToTheirMoodTab() {
        onView(withId(R.id.btnTheirMood)).perform(click());
        onView(withText("Their Mood")).check(matches(isDisplayed()));
    }
}