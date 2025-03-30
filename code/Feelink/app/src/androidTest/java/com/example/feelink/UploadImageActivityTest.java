package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;



import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.provider.MediaStore;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class UploadImageActivityTest {
    @Rule
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            );


    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testSelectImageFromCamera() {
        ActivityScenario<UploadImageActivity> scenario = ActivityScenario.launch(UploadImageActivity.class);

        //A test bitmap.
        Bitmap testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Intent stubCameraData = new Intent();
        stubCameraData.putExtra("data", testBitmap);
        Instrumentation.ActivityResult cameraResult =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, stubCameraData);

        // Stub camera intent result.
        intending(IntentMatchers.hasAction(MediaStore.ACTION_IMAGE_CAPTURE))
                .respondWith(cameraResult);

        onView(withId(R.id.btnUseCamera)).perform(click());
        SystemClock.sleep(1000);


        onView(withId(R.id.ivPreview)).check(matches(isDisplayed()));
        onView(withId(R.id.btnConfirmUpload)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCancelUpload)).check(matches(isDisplayed()));

        scenario.close();
    }
}
