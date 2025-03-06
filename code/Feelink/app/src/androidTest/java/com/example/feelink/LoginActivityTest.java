package com.example.feelink;

import static android.app.PendingIntent.getActivity;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<Login> scenario = new ActivityScenarioRule<>(Login.class);

    @Before
    public void seedDatabase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usernamesRef = db.collection("usernames");

        // Add a valid user for testing
        Map<String, Object> validUser = new HashMap<>();
        validUser.put("uid", "testUserId123"); // Replace with a valid UID
        validUser.put("email", "testuser@example.com"); // Replace with a valid email
        usernamesRef.document("validUsername").set(validUser); // Replace with a valid username

        // Add an invalid user for testing
        Map<String, Object> invalidUser = new HashMap<>();
        invalidUser.put("uid", "invalidUserId123"); // Replace with an invalid UID
        invalidUser.put("email", "invaliduser@example.com"); // Replace with an invalid email
        usernamesRef.document("invalidUsername").set(invalidUser); // Replace with an invalid username
    }

    @Test
    public void testSuccessfulLogin() {
        // Enter valid username and password
        onView(withId(R.id.username_text)).perform(replaceText("testUserId123"));
        onView(withId(R.id.password_text)).perform(replaceText("P@assw0rd"));
        onView(withId(R.id.create_button)).perform(click());
        ActivityScenario<FeedManagerActivity> scenario = ActivityScenario.launch(FeedManagerActivity.class);

        // Verify navigation to FeedManagerActivity
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));
    }

    @Test
    public void testLoginWithInvalidPassword() {
        onView(withId(R.id.username_text)).perform(replaceText("validUser"));
        onView(withId(R.id.password_text)).perform(replaceText("wrongPassword"));
        onView(withId(R.id.create_button)).perform(click());

        // Verify error message in Toast
        onView(withText("Login failed"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLoginWithEmptyFields() {
        ActivityScenario<Login> scenario = ActivityScenario.launch(Login.class);

        scenario.onActivity(activity -> {
            onView(withId(R.id.create_button)).perform(click());

            SystemClock.sleep(2000); // Give time for Toast to appear

            onView(withText("Please fill all fields!"))
                    .inRoot(withDecorView(not(is(activity.getWindow().getDecorView()))))
                    .check(matches(isDisplayed()));
        });
    }
}
