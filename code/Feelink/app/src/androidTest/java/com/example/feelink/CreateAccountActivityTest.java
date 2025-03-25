package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.util.Log;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreateAccountActivityTest {

    @Rule
    public ActivityScenarioRule<CreateAccount> scenario = new ActivityScenarioRule<>(CreateAccount.class);

    @BeforeClass
    public static void setup() {
        // Specific address for emulated device to access localhost
        String androidLocalhost = "10.0.2.2";

        // Configure BOTH emulators
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, 8080);
        FirebaseAuth.getInstance().useEmulator(androidLocalhost, 9099);

        // Disable persistence to avoid caching issues
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        // Set the SKIP_AUTH flag in BOTH activities
        try {
            // For CreateAccount
            Class<?> createAccountClass = Class.forName("com.example.feelink.CreateAccount");
            java.lang.reflect.Field skipAuthFieldCA = createAccountClass.getDeclaredField("SKIP_AUTH_FOR_TESTING_CREATE_ACCOUNT");
            skipAuthFieldCA.setAccessible(true);
            skipAuthFieldCA.set(null, true);

            // ALSO for FeedManagerActivity - notice the different flag name
            Class<?> feedManagerClass = Class.forName("com.example.feelink.FeedManagerActivity");
            java.lang.reflect.Field skipAuthFieldFM = feedManagerClass.getDeclaredField("SKIP_AUTH_FOR_TESTING_CREATE_ACCOUNT");
            skipAuthFieldFM.setAccessible(true);
            skipAuthFieldFM.set(null, true);

            Log.d("Test", "Successfully set testing flags for both activities");
        } catch (Exception e) {
            Log.e("Test", "Failed to set testing flags", e);
            e.printStackTrace(); // Print the stack trace for detailed error info
        }
    }

    @Before
    public void initIntents() {
        // Sign out any existing user
        FirebaseAuth.getInstance().signOut();

        // Initialize intents
        Intents.init();
    }

    @Test
    public void testCreateAccountWithValidInput() throws InterruptedException {
        // Generate unique values to prevent conflicts
        String uniqueId = String.valueOf(System.currentTimeMillis());
        String username = "testuser" + uniqueId;
        String email = "test" + uniqueId + "@example.com";

        // Input valid user details with keyboard closing
        onView(withId(R.id.create_name_text)).perform(typeText("Test User"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.create_username_text)).perform(typeText(username), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.create_date_of_birth_text)).perform(typeText("01/01/1990"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.create_email_text)).perform(typeText(email), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.create_user_password_text)).perform(typeText("TestPass123"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.repeat_user_password_text)).perform(typeText("TestPass123"), ViewActions.closeSoftKeyboard());

        // Wait for username validation to complete
        Thread.sleep(2000);

        // Click create account button
        onView(withId(R.id.create_button)).perform(click());

        // After clicking create button
        Log.d("TEST", "Clicked create button, waiting for navigation...");

        Thread.sleep(10000);

        // Verify navigation to FeedManagerActivity
        intended(hasComponent(FeedManagerActivity.class.getName()));
        onView(withId(R.id.btnAllMoods)).check(matches(isDisplayed()));
    }


    @Test
    public void testCreateAccountWithInvalidUsername() {
        // Input invalid username
        onView(withId(R.id.create_username_text)).perform(typeText("jd"));

        // Check if an error message is displayed
        onView(withText("Invalid username! Use 3-25 characters (letters, numbers, underscores)")).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAccountWithMismatchedPasswords() throws InterruptedException {
        onView(withId(R.id.create_name_text)).perform(typeText("John Doe"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.create_username_text)).perform(typeText("johndoes" + System.currentTimeMillis()), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.create_date_of_birth_text)).perform(typeText("01/01/1990"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.create_email_text)).perform(typeText("johndoes" + System.currentTimeMillis() + "@example.com"), ViewActions.closeSoftKeyboard());

        // Input mismatched passwords
        onView(withId(R.id.create_user_password_text)).perform(typeText("P@ssword123"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.repeat_user_password_text)).perform(typeText("P@ssword456"), ViewActions.closeSoftKeyboard());

        // Wait for any validation to complete
        Thread.sleep(1000);

        // Click on the create account button
        onView(withId(R.id.create_button)).perform(click());

        // Need a small delay for Snackbar to appear
        Thread.sleep(1000);

        // Check if error message is displayed
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(R.string.password_no_match)));
    }


    @After
    public void cleanup() {
        Intents.release(); // Release Espresso Intents
        FirebaseAuth.getInstance().signOut();

        // Clear any cached data
        try {
            FirebaseFirestore.getInstance().clearPersistence();
        } catch (Exception e) {
            Log.e("Test", "Error clearing persistence", e);
        }
    }
    @After
    public void ClearEmulators() {
        String projectId = "feelink-database-test";
        URL url = null;
        try {
            url = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
        } catch (MalformedURLException exception) {
            Log.e("URL Error", Objects.requireNonNull(exception.getMessage()));
        }
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");
            int response = urlConnection.getResponseCode();
            Log.i("Response Code", "Response Code: " + response);
        } catch (IOException exception) {
            Log.e("IO Error", Objects.requireNonNull(exception.getMessage()));
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

}