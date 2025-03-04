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
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> scenario = new ActivityScenarioRule<>(MainActivity.class);

    @BeforeClass
    public static void setup() {
        // Specific address for emulated device to access our localHost
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }

    @Test
    public void testLoginButtonNavigatesToLoginActivity() {
        // Click on the login button
        onView(withId(R.id.button_login)).perform(click());

        // Check if the Login activity is displayed
        onView(withId(R.id.username_text)).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountButtonNavigatesToCreateAccountActivity() {
        // Click on the create account button
        onView(withId(R.id.button_create_account)).perform(click());

        // Check if the CreateAccount activity is displayed
        onView(withId(R.id.create_name_text)).check(matches(isDisplayed()));
    }
}