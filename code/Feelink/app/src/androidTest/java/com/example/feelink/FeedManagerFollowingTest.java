package com.example.feelink;

import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.feelink.view.CommentsActivity;
import com.example.feelink.view.FeedManagerActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.WriteBatch;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;

//Also has tests for Comments

@RunWith(AndroidJUnit4.class)
public class FeedManagerFollowingTest {
    private static final String TAG = "FeedManagerFollowingTest";
    private static final String TEST_USER_ID = "test_user_id";
    private static final String USER_A_ID = "user_a_id";
    private static final String USER_B_ID = "user_b_id";
    private static final String USER_C_ID = "user_c_id";

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
        createUser(db, USER_A_ID, "userA");
        createUser(db, USER_B_ID, "userB");
        createUser(db, USER_C_ID, "userC");

        // Test user follows userA and userB
        followUser(TEST_USER_ID, USER_A_ID);
        followUser(TEST_USER_ID, USER_B_ID);

        // Create public moods for each user
        createMood(db, USER_A_ID, "moodA", "Happy");
        createMood(db, USER_B_ID, "moodB", "Sad");
        createMood(db, USER_C_ID, "moodC", "Angry");

        createComment(db, "moodB", USER_A_ID, "Seeded comment");

        Log.d(TAG, "Test data seeded");
    }

    private void createComment(FirebaseFirestore db, String moodId, String userId, String text)
            throws ExecutionException, InterruptedException {

        Map<String, Object> comment = new HashMap<>();
        comment.put("userId", userId);
        comment.put("text", text);
        comment.put("timestamp", FieldValue.serverTimestamp());

        Tasks.await(db.collection("mood_events")
                .document(moodId)
                .collection("comments")
                .document()
                .set(comment));
    }

    private void createUser(FirebaseFirestore db, String userId, String username) throws ExecutionException, InterruptedException {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("followers", 0);
        user.put("following", 0);
        Tasks.await(db.collection("users").document(userId).set(user));
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

    private void createMood(FirebaseFirestore db, String userId, String moodId, String emotion) throws ExecutionException, InterruptedException {
        Map<String, Object> mood = new HashMap<>();
        mood.put("userId", userId);
        mood.put("emotionalState", emotion);
        mood.put("timestamp", new Date());
        mood.put("isPublic", true);
        Tasks.await(db.collection("mood_events").document(moodId).set(mood));
    }


    public static class RecyclerViewMatcher {
        private final int recyclerViewId;

        public RecyclerViewMatcher(int recyclerViewId) {
            this.recyclerViewId = recyclerViewId;
        }

        public Matcher<View> atPositionOnView(final int position, final int targetViewId) {
            return new TypeSafeMatcher<View>() {
                Resources resources = null;
                View childView;

                @Override
                public void describeTo(Description description) {
                    String idDescription = Integer.toString(recyclerViewId);
                    if (this.resources != null) {
                        try {
                            idDescription = resources.getResourceName(recyclerViewId);
                        } catch (Resources.NotFoundException e) {
                            idDescription = String.format("%s (resource name not found)", recyclerViewId);
                        }
                    }
                    description.appendText("RecyclerView with id: " + idDescription + " at position: " + position);
                }

                @Override
                protected boolean matchesSafely(View view) {
                    this.resources = view.getResources();
                    if (childView == null) {
                        RecyclerView recyclerView = view.getRootView().findViewById(recyclerViewId);
                        if (recyclerView != null && recyclerView.getId() == recyclerViewId) {
                            RecyclerView.Adapter adapter = recyclerView.getAdapter();
                            if (adapter != null && adapter.getItemCount() > position) {
                                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
                                if (viewHolder == null) {
                                    recyclerView.measure(0, 0);
                                    recyclerView.layout(0, 0, 1000, 1000);
                                    viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
                                }
                                if (viewHolder != null) {
                                    childView = viewHolder.itemView;
                                }
                            }
                        }
                    }
                    if (childView == null) {
                        return false;
                    }
                    View targetView = childView.findViewById(targetViewId);
                    return targetView != null && view == targetView;
                }
            };
        }
    }

    public static RecyclerViewMatcher withRecyclerView(int recyclerViewId) {
        return new RecyclerViewMatcher(recyclerViewId);
    }



    @Test
    public void testFollowingAndAllMoodsSections() {
        // Bypass authentication
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = true;

        // Launch activity with test user
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), FeedManagerActivity.class);
        intent.putExtra("TEST_MODE", true);
        intent.putExtra("FORCE_USER_ID", TEST_USER_ID);
        ActivityScenario.launch(intent);

        // Wait for initial data load
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Check Following Moods section
        onView(withId(R.id.btnFollowingMoods)).perform(click());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        onView(withId(R.id.recyclerMoodEvents)).check(matches(hasChildCount(2)));

        // Check All Moods section
        onView(withId(R.id.btnAllMoods)).perform(click());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        onView(withId(R.id.recyclerMoodEvents)).check(matches(hasChildCount(3)));
    }


    @Test
    public void checkCommentExistsInFeedManager(){
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = true;

        // Launch activity with test user
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), FeedManagerActivity.class);
        intent.putExtra("TEST_MODE", true);
        intent.putExtra("FORCE_USER_ID", TEST_USER_ID);
        ActivityScenario.launch(intent);

        onView(withId(R.id.btnFollowingMoods)).perform(click());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        onView(withRecyclerView(R.id.recyclerMoodEvents).atPositionOnView(0, R.id.btnComment))
                .check(matches(isDisplayed()));
        onView(withId(R.id.btnAllMoods)).perform(click());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        onView(withRecyclerView(R.id.recyclerMoodEvents).atPositionOnView(0, R.id.btnComment))
                .check(matches(not(isDisplayed())));

    }


    @Test
    public void testCommentButtonLaunchesCommentsActivity() {
        // Initialize Espresso Intents
        Intents.init();

        // Configure auth bypass for both activities
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = true;
        CommentsActivity.SKIP_AUTH_FOR_TESTING = true;
        CommentsActivity.FORCE_USER_ID = TEST_USER_ID;

        // Launch FeedManagerActivity with test user
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), FeedManagerActivity.class);
        intent.putExtra("TEST_MODE", true);
        intent.putExtra("FORCE_USER_ID", TEST_USER_ID);
        ActivityScenario.launch(intent);

        // Wait for data load
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Switch to Following Moods section where comment button exists
        onView(withId(R.id.btnFollowingMoods)).perform(click());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Click comment button on first item
        onView(withRecyclerView(R.id.recyclerMoodEvents).atPositionOnView(0, R.id.btnComment))
                .perform(click());

        // Verify CommentsActivity is launched with correct intent
        intended(hasComponent(CommentsActivity.class.getName()));
        intended(hasExtraWithKey("MOOD_EVENT_ID"));
        intended(hasExtraWithKey("MOOD_EVENT_OWNER_ID"));

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Verify the user ID is properly set in CommentsActivity
        onView(withId(R.id.btnSend)).check(matches(isDisplayed()));

        // Cleanup
        Intents.release();
    }


    @Test
    public void testUserCanAddAndViewComment() throws InterruptedException {
        // Configure test environment
        Intents.init();
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = true;
        CommentsActivity.SKIP_AUTH_FOR_TESTING = true;
        CommentsActivity.FORCE_USER_ID = TEST_USER_ID;

        // Launch activity as User A
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), FeedManagerActivity.class);
        intent.putExtra("TEST_MODE", true);
        intent.putExtra("FORCE_USER_ID", TEST_USER_ID);
        ActivityScenario.launch(intent);

        // Navigate to Following Moods
        onView(withId(R.id.btnFollowingMoods)).perform(click());
        Thread.sleep(2000); // Wait for data load

        // Open comments for first mood (User B's mood)
        onView(withRecyclerView(R.id.recyclerMoodEvents).atPositionOnView(0, R.id.btnComment))
                .perform(click());
        Thread.sleep(1000); // Wait for CommentsActivity

        // Verify empty state and post comment
        String testComment = "Test comment";
        onView(withId(R.id.etComment)).perform(typeText(testComment), closeSoftKeyboard());
        onView(withId(R.id.btnSend)).perform(click());
        Thread.sleep(2000); // Wait for comment submission

        // Verify comment appears
        onView(withRecyclerView(R.id.recyclerComments).atPositionOnView(1, R.id.tvComment))
                .check(matches(withText(testComment)));

        // Cleanup
        Intents.release();
    }

    @Test
    public void testExistingCommentDisplay() throws InterruptedException {
        // Bypass authentication
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = true;
        CommentsActivity.SKIP_AUTH_FOR_TESTING = true;

        // Launch activity with test user
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), FeedManagerActivity.class);
        intent.putExtra("TEST_MODE", true);
        intent.putExtra("FORCE_USER_ID", TEST_USER_ID);
        ActivityScenario.launch(intent);

        // Wait for data load
        Thread.sleep(3000);

        // Switch to Following Moods section
        onView(withId(R.id.btnFollowingMoods)).perform(click());
        Thread.sleep(2000);

        // Open comments for User B's mood (which has seeded comment)
        onView(withRecyclerView(R.id.recyclerMoodEvents).atPositionOnView(0, R.id.btnComment))
                .perform(click());
        Thread.sleep(2000);

        // Verify seeded comment exists
        onView(withRecyclerView(R.id.recyclerComments)
                .atPositionOnView(0, R.id.tvComment))
                .check(matches(withText("Seeded comment")));
    }



    @After
    public void cleanup() throws ExecutionException, InterruptedException {
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = false;
        CommentsActivity.resetTestFlags();
        deleteTestData();
    }

    private void deleteTestData() throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        // Delete users
        batch.delete(db.collection("users").document(TEST_USER_ID));
        batch.delete(db.collection("users").document(USER_A_ID));
        batch.delete(db.collection("users").document(USER_B_ID));
        batch.delete(db.collection("users").document(USER_C_ID));

        // Delete moods and their comments
        String[] moodIds = {"moodA", "moodB", "moodC"};
        for (String moodId : moodIds) {
            // Delete mood event
            batch.delete(db.collection("mood_events").document(moodId));

            // Delete all comments in subcollection
            Tasks.await(db.collection("mood_events").document(moodId)
                    .collection("comments")
                    .get()).forEach(queryDocumentSnapshot ->
                    batch.delete(queryDocumentSnapshot.getReference()));
        }

        Tasks.await(batch.commit());
        Log.d(TAG, "Test data cleaned up");
    }
}