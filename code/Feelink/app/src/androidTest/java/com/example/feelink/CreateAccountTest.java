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
public class CreateAccountTest {

    @Rule
    public ActivityScenarioRule<CreateAccount> scenario =
            new ActivityScenarioRule<>(CreateAccount.class);


    @Test
    public void testInvalidUsername() {
        onView(withId(R.id.create_username_text)).perform(typeText("in"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.create_username_feedback))
                .check(matches(withText("Invalid username! Use 3-25 characters (letters, numbers, underscores)")));
    }

    @Test
    public void testValidUsername() {
        onView(withId(R.id.create_username_text)).perform(typeText("validUsername"));
        Espresso.closeSoftKeyboard();
        // Note: This might not work as expected due to asynchronous Firebase call
        // You might need to use IdlingResource or similar mechanism to wait for the Firebase response
        onView(withId(R.id.create_username_feedback)).check(matches(withText("Available")));
    }

    @Test
    public void testCreateAccountWithEmptyFields() {
        onView(withId(R.id.create_button)).perform(click());
        onView(withText("Please fill all fields!")).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithInvalidEmail() {
        onView(withId(R.id.create_name_text)).perform(typeText("Test Name"));
        onView(withId(R.id.create_username_text)).perform(typeText("TestUsername"));
        onView(withId(R.id.create_date_of_birth_text)).perform(typeText("01/01/2000"));
        onView(withId(R.id.create_email_text)).perform(typeText("invalid-email"));
        onView(withId(R.id.create_user_password_text)).perform(typeText("Password123!"));
        onView(withId(R.id.repeat_user_password_text)).perform(typeText("Password123!"));

        Espresso.closeSoftKeyboard();

        onView(withId(R.id.create_button)).perform(click());
        onView(withText("Invalid email format!")).check(matches(isDisplayed()));
    }
}

