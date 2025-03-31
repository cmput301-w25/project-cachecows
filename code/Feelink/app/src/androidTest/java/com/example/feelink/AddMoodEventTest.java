package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.example.feelink.model.MoodEvent;
import com.example.feelink.view.AddMoodEventActivity;
import com.example.feelink.view.UserProfileActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import static androidx.test.espresso.assertion.ViewAssertions.matches;



import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class AddMoodEventTest {

    private static final String TAG = "AddMoodEventTest";
    private static final String TEST_USER_ID = "test_user_id";
    private static final String TEST_REASON = "TestReason_" + System.currentTimeMillis();

    // Grant location permissions to avoid the geolocation dialog interfering.
    @Rule
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            );

    @Before
    public void setUp() throws Exception {
        // Enable test mode
        AddMoodEventActivity.enableTestMode(true);
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = true;

        // Setup Firestore emulator
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.useEmulator("10.0.2.2", 8080);
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        cleanupTestData();
        createTestUser();
    }

    private void createTestUser() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();
        user.put("username", "test_user");
        user.put("followers", 0);
        user.put("following", 0);
        Tasks.await(db.collection("users").document(TEST_USER_ID).set(user));
    }

    private void cleanupTestData() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query = db.collection("mood_events")
                .whereEqualTo("userId", TEST_USER_ID);
        QuerySnapshot snapshot = Tasks.await(query.get());
        WriteBatch batch = db.batch();
        snapshot.getDocuments().forEach(doc -> batch.delete(doc.getReference()));
        Tasks.await(batch.commit());
    }

    @Test
    public void testAddMoodEventAppearsInUserProfile() throws Exception {
        // Verify that Firestore is empty
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        QuerySnapshot preTestSnapshot = Tasks.await(db.collection("mood_events")
                .whereEqualTo("userId", TEST_USER_ID)
                .get());
        assertEquals(0, preTestSnapshot.size());

        // Create and add a test mood event
        MoodEvent testEvent = new MoodEvent("Happy", "", TEST_REASON);
        testEvent.setUserId(TEST_USER_ID);
        testEvent.setPublic(true);
        testEvent.setTimestamp(new Date());

        DocumentReference docRef = db.collection("mood_events").document();
        testEvent.setDocumentId(docRef.getId());
        Tasks.await(docRef.set(testEvent));
        Log.d(TAG, "Added test mood event to Firestore");

        QuerySnapshot postAddSnapshot = Tasks.await(db.collection("mood_events")
                .whereEqualTo("userId", TEST_USER_ID)
                .whereEqualTo("reason", TEST_REASON)
                .get());
        assertEquals(1, postAddSnapshot.size());
        Log.d(TAG, "Verified mood event exists in Firestore");


        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileActivity.class);
        intent.putExtra("TEST_MODE", true);
        ActivityScenario<UserProfileActivity> scenario = ActivityScenario.launch(intent);

        // verify that recycler displays the test mood event
        onView(withId(R.id.recyclerMoodEvents))
                .check(matches(hasDescendant(withText(TEST_REASON))));

        scenario.close();
    }

    @After
    public void tearDown() throws Exception {
        cleanupTestData();
    }
}

