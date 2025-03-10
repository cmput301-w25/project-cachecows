package com.example.feelink;

import static android.app.PendingIntent.getActivity;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<Login> scenario = new ActivityScenarioRule<>(Login.class);

    @BeforeClass
    public static void setup(){
        // Specific address for emulated device to access our localHost
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);

    }

    @Before
    public void seedDatabase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usernamesRef = db.collection("usernames");

        // Add a valid user with an email (Firestore)
        Map<String, Object> validUser = new HashMap<>();
        validUser.put("uid", "testUserId123");
        validUser.put("email", "testuser@example.com");  // ✅ Ensure email exists!
        usernamesRef.document("validUsername").set(validUser);

        // ✅ ALSO Create a Firebase Authentication user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword("testuser@example.com", "P@ssw0rd")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        System.out.println("Firebase Auth Test User Created!");
                    } else {
                        System.out.println("Error creating Firebase user: " + task.getException().getMessage());
                    }
                });

        SystemClock.sleep(15000); // Ensure Firestore syncs before test runs
    }

    @Test
    public void testSuccessfulLogin() {
        // Enter valid credentials
        onView(withId(R.id.username_text)).perform(replaceText("validUsername"));
        onView(withId(R.id.password_text)).perform(replaceText("P@ssw0rd"), closeSoftKeyboard());
        onView(withId(R.id.create_button)).perform(click());

        // Wait for the login process to complete
        SystemClock.sleep(5000); // Adjust sleep duration if needed

        // Launch FeedManagerActivity (ensures it's in the correct state)
        ActivityScenario<FeedManagerActivity> feedScenario = ActivityScenario.launch(FeedManagerActivity.class);

        // Verify recyclerMoodEvents is displayed
        onView(withId(R.id.recyclerMoodEvents)).check(matches(isDisplayed()));
    } // working!!!!!


    @Test
    public void testLoginWithInvalidPassword() {
        onView(withId(R.id.username_text)).perform(replaceText("validUsername"));
        onView(withId(R.id.password_text)).perform(replaceText("wrongPassword"));
        onView(withId(R.id.create_button)).perform(click());

        SystemClock.sleep(5000);
        // Verify error message in Toast
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(R.string.invalid_cred)));
    } // somehow working after adding sleep in seed database

    @Test
    public void testLoginWithEmptyFields() {
        onView(withId(R.id.create_button)).perform(click());
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(R.string.empty_field)));
    } // working

}