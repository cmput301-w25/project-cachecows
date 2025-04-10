package com.example.feelink;

import android.content.Intent;
import android.location.Location;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.example.feelink.view.LocationPickerActivity;
import com.example.feelink.view.MoodMapActivity;
import com.example.feelink.view.UserProfileActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.WriteBatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MapFunctionalityTest {
    private static final String TAG = "MapFunctionalityTest";
    private static final String TEST_USER_ID = "test_user_id";
    private static final String FOLLOWED_USER_ID = "followed_user_id";
    private static final String NON_FOLLOWED_USER_ID = "non_followed_user_id";
    private static final String TEST_MOOD_ID = "test_mood_id";
    private static final String FOLLOWED_MOOD_ID = "followed_mood_id";
    private static final String PRIVATE_MOOD_ID = "private_mood_id";

    @Rule
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            );

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        // Configure Firestore emulator
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setHost("10.0.2.2:8080")
                .setSslEnabled(false)
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        seedTestData();
    }

    private void seedTestData() throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create test users
        createUser(db, TEST_USER_ID, "testUser");
        createUser(db, FOLLOWED_USER_ID, "followedUser");
        createUser(db, NON_FOLLOWED_USER_ID, "nonFollowedUser");

        // Establish follow relationship (test user follows followed user)
        followUser(TEST_USER_ID, FOLLOWED_USER_ID);

        // Create test mood events with locations
        createTestMoodEvent(TEST_USER_ID, TEST_MOOD_ID, "Happy", "Test mood",
                new Date(), 53.5461, -113.4937, "Edmonton", true);
        createTestMoodEvent(FOLLOWED_USER_ID, FOLLOWED_MOOD_ID, "Sad", "Followed mood",
                new Date(), 53.5409, -113.4938, "Nearby", true);
        createTestMoodEvent(NON_FOLLOWED_USER_ID, PRIVATE_MOOD_ID, "Angry", "Private mood",
                new Date(), 53.5410, -113.4939, "Private", false);

        Log.d(TAG, "Test data for map tests seeded");
    }

    private void createUser(FirebaseFirestore db, String userId, String username) throws ExecutionException, InterruptedException {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("followers", 0);
        user.put("following", 0);
        Tasks.await(db.collection("users").document(userId).set(user));

        // Also add to usernames collection for username resolution
        Map<String, Object> usernameEntry = new HashMap<>();
        usernameEntry.put("uid", userId);
        Tasks.await(db.collection("usernames").document(username).set(usernameEntry));
    }

    private void followUser(String followerId, String followedUserId) throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> followData = new HashMap<>();
        followData.put("uid", followedUserId);
        followData.put("username", "followedUser");
        followData.put("timestamp", FieldValue.serverTimestamp());

        DocumentReference followingRef = db.collection("users")
                .document(followerId)
                .collection("following")
                .document(followedUserId);
        Tasks.await(followingRef.set(followData));

        // Update follower counts
        WriteBatch batch = db.batch();
        batch.update(db.collection("users").document(followerId), "following", FieldValue.increment(1));
        batch.update(db.collection("users").document(followedUserId), "followers", FieldValue.increment(1));
        Tasks.await(batch.commit());
    }

    private void createTestMoodEvent(String userId, String moodId, String emotion, String reason,
                                     Date timestamp, double lat, double lng, String locationName,
                                     boolean isPublic) throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> mood = new HashMap<>();
        mood.put("userId", userId);
        mood.put("emotionalState", emotion);
        mood.put("timestamp", timestamp);
        mood.put("isPublic", isPublic);
        mood.put("reason", reason);
        mood.put("latitude", lat);
        mood.put("longitude", lng);
        mood.put("locationName", locationName);
        Tasks.await(db.collection("mood_events").document(moodId).set(mood));
    }

    @Test
    public void testUserProfileMapShowsOwnMoods() {
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = true;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), UserProfileActivity.class);
        intent.putExtra("TEST_MODE", true);
        ActivityScenario<UserProfileActivity> scenario = ActivityScenario.launch(intent);

        // Wait for map to load
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        scenario.onActivity(activity -> {
            // Verify map is initialized
            assertNotNull(activity.getGoogleMap());

            // Verify markers are displayed (should be 1 - the test user's mood)
            assertEquals(1, activity.getCurrentMarkers().size());

            // Verify marker has correct info
            Marker marker = activity.getCurrentMarkers().get(0);
            assertEquals("Happy", marker.getTitle());
            assertEquals("Edmonton", marker.getSnippet());
        });
    }


    @Test
    public void testMoodMapActivityShowsFollowingMoods() {
        MoodMapActivity.SKIP_AUTH_FOR_TESTING = true;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MoodMapActivity.class);
        intent.putExtra("userId", TEST_USER_ID);
        intent.putExtra("showMyMoods", false);
        ActivityScenario<MoodMapActivity> scenario = ActivityScenario.launch(intent);

        // Wait for map to load
        try { Thread.sleep(10000); } catch (InterruptedException e) {}

        scenario.onActivity(activity -> {
            // Verify map is initialized
            assertNotNull(activity.getGoogleMap());

            // Should show moods from followed users (1 in test data)
            assertEquals(1, activity.getCurrentMarkers().size());

            // Verify marker info
            Marker marker = activity.getCurrentMarkers().get(0);
            assertEquals("Sad", marker.getTitle());
            assertTrue(marker.getSnippet().contains("followedUser"));
        });
    }//working


    @Test
    public void testLocationPickerSelection() throws ExecutionException, InterruptedException {
        // Create test location data
        LatLng testLocation = new LatLng(53.5461, -113.4937);
        String testLocationName = "Edmonton Test Location";
        String testMoodId = "location_test_mood_id";

        // Create a mood event with the test location
        createTestMoodEvent(TEST_USER_ID, testMoodId, "Happy", "Testing location",
                new Date(), testLocation.latitude, testLocation.longitude, testLocationName, true);

        // Create the intent with test location data
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), LocationPickerActivity.class);
        ActivityScenario<LocationPickerActivity> scenario = ActivityScenario.launch(intent);

        // Wait for activity to initialize
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        scenario.onActivity(activity -> {
            // Set the location in the result
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", testLocation.latitude);
            resultIntent.putExtra("longitude", testLocation.longitude);
            resultIntent.putExtra("locationName", testLocationName);
            activity.setResult(android.app.Activity.RESULT_OK, resultIntent);

            // Verify the confirm button exists
            assertNotNull("Confirm button should exist", activity.findViewById(R.id.btnConfirmLocation));

            // Click confirm button
            activity.findViewById(R.id.btnConfirmLocation).performClick();

            // Wait for any animations or transitions
            try { Thread.sleep(1000); } catch (InterruptedException e) {}

            // Verify the activity finished
            assertTrue(activity.isFinishing());
        });

        // Wait for database operations to complete
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Verify the location data in the database
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> moodData = Tasks.await(db.collection("mood_events")
                .document(testMoodId)
                .get())
                .getData();

        assertNotNull("Mood data should exist in database", moodData);
        assertEquals("Latitude should match", testLocation.latitude, (Double) moodData.get("latitude"), 0.0001);
        assertEquals("Longitude should match", testLocation.longitude, (Double) moodData.get("longitude"), 0.0001);
        assertEquals("Location name should match", testLocationName, moodData.get("locationName"));
    }

    @Test
    public void testMapFilteringByEmotion() throws ExecutionException, InterruptedException {
        // Add another mood event with different emotion
        createTestMoodEvent(FOLLOWED_USER_ID, "angry_mood", "Angry", "Angry mood",
                new Date(), 53.5408, -113.4940, "Angry place", true);

        MoodMapActivity.SKIP_AUTH_FOR_TESTING = true;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MoodMapActivity.class);
        intent.putExtra("userId", TEST_USER_ID);
        intent.putExtra("showMyMoods", false);
        intent.putExtra("selectedEmotion", "Sad");
        ActivityScenario<MoodMapActivity> scenario = ActivityScenario.launch(intent);

        // Wait for map to load
        try { Thread.sleep(10000); } catch (InterruptedException e) {}

        scenario.onActivity(activity -> {
            // Should show only "Sad" moods (1 in test data)
            assertEquals(1, activity.getCurrentMarkers().size());
            assertEquals("Sad", activity.getCurrentMarkers().get(0).getTitle());
        });
    }//working

    @After
    public void cleanup() throws ExecutionException, InterruptedException {
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = false;
        MoodMapActivity.SKIP_AUTH_FOR_TESTING = false;
        ClearEmulators();
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