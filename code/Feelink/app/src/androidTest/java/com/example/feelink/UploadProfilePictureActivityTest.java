package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;


import com.example.feelink.view.FeedManagerActivity;
import com.example.feelink.view.UploadImageActivity;
import com.example.feelink.view.UploadProfilePictureActivity;
import com.example.feelink.view.UserProfileActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class UploadProfilePictureActivityTest {

    @Before
    public void setUp() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setHost("10.0.2.2:8080")
                .setSslEnabled(false)
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        // Bypass real auth in the activity
        UploadProfilePictureActivity.SKIP_AUTH_FOR_TESTING = true;
        UploadProfilePictureActivity.FORCE_USER_ID = "test_user_id";
    }

    @After
    public void tearDown() throws Exception {
        // Reset flags
        UploadProfilePictureActivity.SKIP_AUTH_FOR_TESTING = false;
        UploadProfilePictureActivity.FORCE_USER_ID = null;
    }

    @Test
    public void testUploadPictureButtonSuccess() {
        Intents.init();

        // Launch the UploadProfilePictureActivity with an intent
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                UploadProfilePictureActivity.class
        );

        ActivityScenario.launch(intent);

        // Stub the result from UploadImageActivity
        Intent stubData = new Intent();
        stubData.putExtra("imageUrl", "http://example.com/image.png");
        Instrumentation.ActivityResult stubResult =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, stubData);

        // When we see an intent that matches UploadImageActivity, return stubResult
        intending(hasComponent(UploadImageActivity.class.getName())).respondWith(stubResult);

        onView(withId(R.id.btnUploadPicture)).perform(click());
        SystemClock.sleep(3000);
        intended(hasComponent(UserProfileActivity.class.getName()));  // verify that UserProfileActivity is launched

        Intents.release();
    }

    @Test
    public void testSkipForNowButton() {
        Intents.init();
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                UploadProfilePictureActivity.class
        );

        ActivityScenario.launch(intent);


        onView(withId(R.id.btnSkipForNow)).perform(click());
        SystemClock.sleep(2000);
        intended(hasComponent(FeedManagerActivity.class.getName()));

        Intents.release();
    }

}
