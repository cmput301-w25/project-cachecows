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
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<Login> scenario = new ActivityScenarioRule<>(Login.class);

    @BeforeClass
    public static void setup() {
        // Specific address for emulated device to access our localHost
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }

    @Test
    public void testLoginWithValidCredentials() {
        // Input valid username and password
        onView(withId(R.id.username_text)).perform(typeText("validUsername"));
        onView(withId(R.id.password_text)).perform(typeText("validPassword"));

        // Click on the login button
        onView(withId(R.id.create_button)).perform(click());

        // Check if the FeedManagerActivity is displayed
        onView(withId(R.id.btnTheirMood)).check(matches(isDisplayed()));
    }

    @Test
    public void testLoginWithInvalidCredentials() {
        // Input invalid username and password
        onView(withId(R.id.username_text)).perform(typeText("invalidUsername"));
        onView(withId(R.id.password_text)).perform(typeText("invalidPassword"));

        // Click on the login button
        onView(withId(R.id.create_button)).perform(click());

        // Check if an error message is displayed
        onView(withText("Invalid username")).check(matches(isDisplayed()));
    }

    @Test
    public void testLoginWithEmptyFields() {
        // Click on the login button without entering any credentials
        onView(withId(R.id.create_button)).perform(click());

        // Check if an error message is displayed
        onView(withText("Please fill all fields!")).check(matches(isDisplayed()));
    }
}