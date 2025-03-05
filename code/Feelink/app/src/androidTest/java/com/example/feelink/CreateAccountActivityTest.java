package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
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
public class CreateAccountActivityTest {

    @Rule
    public ActivityScenarioRule<CreateAccount> scenario = new ActivityScenarioRule<>(CreateAccount.class);

    @BeforeClass
    public static void setup() {
        // Specific address for emulated device to access our localHost
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }

    @Test
    public void testCreateAccountWithValidInput() {
        // Input valid account details
        onView(withId(R.id.create_name_text)).perform(typeText("John Doe"));
        onView(withId(R.id.create_username_text)).perform(typeText("johndoe123"));
        onView(withId(R.id.create_date_of_birth_text)).perform(typeText("01/01/1990"));
        onView(withId(R.id.create_email_text)).perform(typeText("johndoe12@example.com"));
        onView(withId(R.id.create_user_password_text)).perform(typeText("Password123"));
        onView(withId(R.id.repeat_user_password_text)).perform(typeText("Password123"));

        // Click on the create account button
        onView(withId(R.id.create_button)).perform(click());

        // Wait for the new activity
        ActivityScenario<FeedManagerActivity> scenario = ActivityScenario.launch(FeedManagerActivity.class);

        // Check if the new activity's UI is displayed
        onView(withId(R.id.btnTheirMood)).check(matches(isDisplayed()));
    }


    @Test
    public void testCreateAccountWithInvalidUsername() {
        // Input invalid username
        onView(withId(R.id.create_username_text)).perform(typeText("jd"));

        // Check if an error message is displayed
        onView(withText("Invalid username! Use 3-25 characters (letters, numbers, underscores)")).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithMismatchedPasswords() {
        // Input mismatched passwords
        onView(withId(R.id.create_user_password_text)).perform(typeText("P@ssword123"));
        onView(withId(R.id.repeat_user_password_text)).perform(typeText("P@ssword456"));

        // Click on the create account button
        onView(withId(R.id.create_button)).perform(click());

        // Check if an error message is displayed
        onView(withText("Passwords do not match!")).check(matches(isDisplayed()));
    }
}