package com.example.feelink;

import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.CoreMatchers.containsString;

@RunWith(AndroidJUnit4.class)
public class CommentsActivityTest {
    private static final String TAG = "CommentsActivityTest";
    private static final String TEST_USER_ID = "test_user_id";
    private static final String MOOD_OWNER_ID = "mood_owner_id";
    private static final String NON_FOLLOWER_ID = "non_follower_id";
    private static final String TEST_MOOD_ID = "test_mood_id";

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
        createUser(db, MOOD_OWNER_ID, "moodOwner");
        createUser(db, NON_FOLLOWER_ID, "nonFollower");

        // Establish follow relationship (test user follows mood owner)
        followUser(TEST_USER_ID, MOOD_OWNER_ID);

        // Create a test mood event
        createTestMoodEvent(MOOD_OWNER_ID, TEST_MOOD_ID, "Happy", "Test mood for comments", new Date());

        Log.d(TAG, "Test data for comments seeded");
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
        followData.put("timestamp", FieldValue.serverTimestamp());

        DocumentReference followingRef = db.collection("users")
                .document(followerId)
                .collection("following")
                .document(followedUserId);
        Tasks.await(followingRef.set(followData));
    }

    private void createTestMoodEvent(String userId, String moodId, String emotion, String reason, Date timestamp) throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> mood = new HashMap<>();
        mood.put("userId", userId);
        mood.put("emotionalState", emotion);
        mood.put("timestamp", timestamp);
        mood.put("isPublic", true);
        mood.put("reason", reason);
        Tasks.await(db.collection("mood_events").document(moodId).set(mood));
    }


    @Test
    public void testCommentPostingAsOwner() {
        CommentsActivity.SKIP_AUTH_FOR_TESTING = true;
        CommentsActivity.FORCE_USER_ID = MOOD_OWNER_ID;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CommentsActivity.class);
        intent.putExtra("MOOD_EVENT_ID", TEST_MOOD_ID);
        intent.putExtra("MOOD_EVENT_OWNER_ID", MOOD_OWNER_ID);
        ActivityScenario.launch(intent);

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Post a comment
        String testComment = "Owner's comment";
        onView(withId(R.id.etComment)).perform(
                typeText(testComment),
                ViewActions.closeSoftKeyboard()
        );
        onView(withId(R.id.btnSend)).perform(click());

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Verify comment appears with username
        onView(withText(containsString(testComment))).check(matches(isDisplayed()));
        onView(withText(containsString("moodOwner"))).check(matches(isDisplayed()));
    }

    @Test
    public void testCommentNotificationSent() throws ExecutionException, InterruptedException {
        CommentsActivity.SKIP_AUTH_FOR_TESTING = true;
        CommentsActivity.FORCE_USER_ID = TEST_USER_ID;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CommentsActivity.class);
        intent.putExtra("MOOD_EVENT_ID", TEST_MOOD_ID);
        intent.putExtra("MOOD_EVENT_OWNER_ID", MOOD_OWNER_ID);
        ActivityScenario.launch(intent);

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Post a comment
        String testComment = "This should trigger a notification";
        onView(withId(R.id.etComment)).perform(
                typeText(testComment),
                ViewActions.closeSoftKeyboard()
        );
        onView(withId(R.id.btnSend)).perform(click());

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Verify notification was created in Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        QuerySnapshot snapshot = Tasks.await(db.collection("notifications")
                .whereEqualTo("receiverId", MOOD_OWNER_ID)
                .whereEqualTo("type", "COMMENT")
                .whereEqualTo("moodEventId", TEST_MOOD_ID)
                .get());

        assert(snapshot.size() > 0);
        assert(snapshot.getDocuments().get(0).getString("text").equals(testComment));
    }

    @After
    public void cleanup() throws ExecutionException, InterruptedException {
        CommentsActivity.resetTestFlags();
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