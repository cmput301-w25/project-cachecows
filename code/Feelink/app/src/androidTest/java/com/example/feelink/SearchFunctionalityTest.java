package com.example.feelink;

import android.content.Intent;
import android.content.res.Resources;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class SearchFunctionalityTest {
    private static final String TAG = "SearchFunctionalityTest";
    private static final String USER_A_ID = "user_a_id";
    private static final String USER_B_ID = "user_b_id";
    private static final String USER_C_ID = "user_c_id";

    private static final String USER_D_ID = "user_d_id";

    private static final String USER_E_ID = "user_e_id";




    @Before
    public void setup() throws ExecutionException, InterruptedException {
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

        createUser(db, USER_A_ID, "userA");
        createUser(db, USER_B_ID, "userB");
        createUser(db, USER_C_ID, "userC");
        createUser(db, USER_D_ID, "a_test_user");
        createUser(db, USER_E_ID, "b_test_user");
        followUser(USER_A_ID, USER_B_ID);
    }

    private void createUser(FirebaseFirestore db, String userId, String username)
            throws ExecutionException, InterruptedException {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("username_lowercase", username.toLowerCase());
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

    @Test
    public void test01_SearchShowsAllMatchingUserswithLowerCase() throws InterruptedException {
        SearchActivity.SKIP_AUTH_FOR_TESTING = true;
        SearchActivity.FORCE_USER_ID = USER_A_ID;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SearchActivity.class);
        ActivityScenario.launch(intent);

        Thread.sleep(2000);


        onView(withId(R.id.searchEditText))
                .perform(click())
                .perform(typeText("user"));

        Thread.sleep(2000);

        onView(withId(R.id.searchResultsRecyclerView))
                .check(matches(hasChildCount(3)));

        onView(withRecyclerView(R.id.searchResultsRecyclerView).atPositionOnView(0, R.id.userUsername))
                .check(matches(withText("userA")));
        onView(withRecyclerView(R.id.searchResultsRecyclerView).atPositionOnView(1, R.id.userUsername))
                .check(matches(withText("userB")));
        onView(withRecyclerView(R.id.searchResultsRecyclerView).atPositionOnView(2, R.id.userUsername))
                .check(matches(withText("userC")));
    }

    @Test
    public void test02_SearchShowsAllMatchingUserswithUpperCase() throws InterruptedException {
        SearchActivity.SKIP_AUTH_FOR_TESTING = true;
        SearchActivity.FORCE_USER_ID = USER_A_ID;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SearchActivity.class);
        ActivityScenario.launch(intent);

        Thread.sleep(2000);

        onView(withId(R.id.searchEditText))
                .perform(click())
                .perform(typeText("USER"));

        Thread.sleep(2000);

        onView(withId(R.id.searchResultsRecyclerView))
                .check(matches(hasChildCount(0)));


    }

    @Test
    public void test03_ClickUserAOpensProfile() throws InterruptedException {
        SearchActivity.SKIP_AUTH_FOR_TESTING = true;
        SearchActivity.FORCE_USER_ID = USER_A_ID;
        UserProfileActivity.SKIP_AUTH_FOR_TESTING = true;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SearchActivity.class);
        ActivityScenario.launch(intent);

        Thread.sleep(2000);

        onView(withId(R.id.searchEditText))
                .perform(click())
                .perform(typeText("user"));

        Thread.sleep(2000);

        // Click on User A's username in the first item
        onView(withRecyclerView(R.id.searchResultsRecyclerView).atPositionOnView(0, R.id.userUsername))
                .perform(click());

        Thread.sleep(2000);

        // Verify if UserProfileActivity by checking for Edit Profile Button
        onView(withId(R.id.editProfileButton))
                .check(matches(isDisplayed()));
    }

    @Test
    public void test04_ClickUserCOpensOtherProfile() throws InterruptedException {
        // Bypass auth checks
        SearchActivity.SKIP_AUTH_FOR_TESTING = true;
        SearchActivity.FORCE_USER_ID = USER_A_ID;
        OtherUserProfileActivity.SKIP_AUTH_FOR_TESTING = true;
        OtherUserProfileActivity.FORCE_USER_ID = USER_A_ID;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SearchActivity.class);
        ActivityScenario.launch(intent);

        Thread.sleep(2000);

        onView(withId(R.id.searchEditText))
                .perform(click())
                .perform(typeText("user"));

        Thread.sleep(2000);

        // Click on User C (third item in RecyclerView)
        onView(withRecyclerView(R.id.searchResultsRecyclerView).atPositionOnView(2, R.id.userUsername))
                .perform(click());

        Thread.sleep(2000);

        // Verify follow button exists in OtherUserProfileActivity
        onView(withId(R.id.followButton))
                .check(matches(isDisplayed()));
    }


    @Test
    public void test05_ClickUserCCheckIfFollowButtonSaysFollow() throws InterruptedException {
        // Bypass auth checks
        SearchActivity.SKIP_AUTH_FOR_TESTING = true;
        SearchActivity.FORCE_USER_ID = USER_A_ID;
        OtherUserProfileActivity.SKIP_AUTH_FOR_TESTING = true;
        OtherUserProfileActivity.FORCE_USER_ID = USER_A_ID;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SearchActivity.class);
        ActivityScenario.launch(intent);

        Thread.sleep(2000);

        onView(withId(R.id.searchEditText))
                .perform(click())
                .perform(typeText("user"));

        Thread.sleep(2000);

        // Click on User C (third item in RecyclerView)
        onView(withRecyclerView(R.id.searchResultsRecyclerView).atPositionOnView(2, R.id.userUsername))
                .perform(click());

        Thread.sleep(2000);

        // Verify if follow button says follow because User A does not follow User C
        onView(withId(R.id.followButton))
                .check(matches(isDisplayed())) .check(matches(withText("Follow")));;
    }


    @Test
    public void test06_ClickUserBCheckIfFollowButtonSaysUnFollow() throws InterruptedException {
        // Bypass auth checks
        SearchActivity.SKIP_AUTH_FOR_TESTING = true;
        SearchActivity.FORCE_USER_ID = USER_A_ID;
        OtherUserProfileActivity.SKIP_AUTH_FOR_TESTING = true;
        OtherUserProfileActivity.FORCE_USER_ID = USER_A_ID;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SearchActivity.class);
        ActivityScenario.launch(intent);

        Thread.sleep(2000);

        onView(withId(R.id.searchEditText))
                .perform(click())
                .perform(typeText("user"));

        Thread.sleep(2000);

        // Click on User B (second item in RecyclerView)
        onView(withRecyclerView(R.id.searchResultsRecyclerView).atPositionOnView(1, R.id.userUsername))
                .perform(click());

        Thread.sleep(2000);

        // Verify if follow button says unfollow because User A does follow User B
        onView(withId(R.id.followButton))
                .check(matches(isDisplayed())).check(matches(withText("Unfollow")));;
    }


    @Test
    public void test07_UserASendsFollowRequestToUserC() throws InterruptedException {
        // Bypass auth checks
        SearchActivity.SKIP_AUTH_FOR_TESTING = true;
        SearchActivity.FORCE_USER_ID = USER_A_ID;
        OtherUserProfileActivity.SKIP_AUTH_FOR_TESTING = true;
        OtherUserProfileActivity.FORCE_USER_ID = USER_A_ID;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SearchActivity.class);
        ActivityScenario.launch(intent);

        Thread.sleep(2000);

        onView(withId(R.id.searchEditText))
                .perform(click())
                .perform(typeText("user"));

        Thread.sleep(2000);

        // Click on User C (third item in RecyclerView)
        onView(withRecyclerView(R.id.searchResultsRecyclerView).atPositionOnView(2, R.id.userUsername))
                .perform(click());

        Thread.sleep(2000);


        onView(withId(R.id.followButton)).perform(click());
        onView(withId(R.id.followButton)).check(matches(withText("Requested"))); // Check if button goes to requested
        Thread.sleep(500);
    }

    @Test
    public void test08_UserAUnfollowsUserB() throws InterruptedException {
        // Bypass auth checks
        SearchActivity.SKIP_AUTH_FOR_TESTING = true;
        SearchActivity.FORCE_USER_ID = USER_A_ID;
        OtherUserProfileActivity.SKIP_AUTH_FOR_TESTING = true;
        OtherUserProfileActivity.FORCE_USER_ID = USER_A_ID;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SearchActivity.class);
        ActivityScenario.launch(intent);

        Thread.sleep(2000);

        onView(withId(R.id.searchEditText))
                .perform(click())
                .perform(typeText("user"));

        Thread.sleep(2000);

        // Click on User B (second item in RecyclerView)
        onView(withRecyclerView(R.id.searchResultsRecyclerView).atPositionOnView(1, R.id.userUsername))
                .perform(click());

        Thread.sleep(2000);

        onView(withId(R.id.followButton)).perform(click());
        onView(withId(R.id.followButton)).check(matches(withText("Follow")));; // Check if button goes back to follow
        Thread.sleep(500);
    }

    @Test
    public void test09_SearchForUserD() throws InterruptedException {
        SearchActivity.SKIP_AUTH_FOR_TESTING = true;
        SearchActivity.FORCE_USER_ID = USER_A_ID;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SearchActivity.class);
        ActivityScenario.launch(intent);

        Thread.sleep(2000);


        onView(withId(R.id.searchEditText))
                .perform(click())
                .perform(typeText("a"));

        Thread.sleep(2000);

        onView(withId(R.id.searchResultsRecyclerView))
                .check(matches(hasChildCount(1)));

        onView(withRecyclerView(R.id.searchResultsRecyclerView).atPositionOnView(0, R.id.userUsername))
                .check(matches(withText("a_test_user")));
    }


    @Test
    public void test10_SearchForUserD() throws InterruptedException {
        SearchActivity.SKIP_AUTH_FOR_TESTING = true;
        SearchActivity.FORCE_USER_ID = USER_A_ID;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SearchActivity.class);
        ActivityScenario.launch(intent);

        Thread.sleep(2000);


        onView(withId(R.id.searchEditText))
                .perform(click())
                .perform(typeText("b"));

        Thread.sleep(2000);

        onView(withId(R.id.searchResultsRecyclerView))
                .check(matches(hasChildCount(1)));

        onView(withRecyclerView(R.id.searchResultsRecyclerView).atPositionOnView(0, R.id.userUsername))
                .check(matches(withText("b_test_user")));
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
                    if (childView == null) return false;
                    View targetView = childView.findViewById(targetViewId);
                    return targetView != null && view == targetView;
                }
            };
        }
    }

    public static RecyclerViewMatcher withRecyclerView(int recyclerViewId) {
        return new RecyclerViewMatcher(recyclerViewId);
    }
}