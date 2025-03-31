package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;



import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.feelink.controller.ConnectivityReceiver;
import com.example.feelink.view.FeedManagerActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FeedManagerOfflineBehaviorTest {

    private ActivityScenario<FeedManagerActivity> scenario;
    private OfflineBannerIdlingResource offlineBannerIdlingResource;

    @Before
    public void setUp() {
        FeedManagerActivity.SKIP_AUTH_FOR_TESTING = true;
        scenario = ActivityScenario.launch(FeedManagerActivity.class);
    }

    @After
    public void tearDown() {
        if (offlineBannerIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(offlineBannerIdlingResource);
        }
        scenario.close();
    }

    @Test
    public void testOfflineBannerBehavior() {
        // Simulate offline behavior
        scenario.onActivity(activity -> {
            TextView tvOfflineIndicator = activity.findViewById(R.id.tvOfflineIndicator);
            ConnectivityReceiver.handleBanner(false, tvOfflineIndicator, activity); //banner should show "You are currently offline"
        });

        // Verify the offline banner
        scenario.onActivity(activity -> {
            TextView tvOfflineIndicator = activity.findViewById(R.id.tvOfflineIndicator);
            String expectedOfflineText = activity.getString(R.string.you_are_currently_offline);
            assertEquals(View.VISIBLE, tvOfflineIndicator.getVisibility());
            assertEquals(expectedOfflineText, tvOfflineIndicator.getText().toString());
        });

        // Simulate coming back online
        scenario.onActivity(activity -> {
            TextView tvOfflineIndicator = activity.findViewById(R.id.tvOfflineIndicator);
            ConnectivityReceiver.handleBanner(true, tvOfflineIndicator, activity);  //banner should show "Back Online"
        });

        // Verify online banner
        scenario.onActivity(activity -> {
            TextView tvOfflineIndicator = activity.findViewById(R.id.tvOfflineIndicator);
            String expectedOnlineText = activity.getString(R.string.back_online);
            assertEquals(View.VISIBLE, tvOfflineIndicator.getVisibility());
            assertEquals(expectedOnlineText, tvOfflineIndicator.getText().toString());
        });

        // Use the idling resource to wait until the banner becomes hidden
        scenario.onActivity(activity -> {
            TextView tvOfflineIndicator = activity.findViewById(R.id.tvOfflineIndicator);
            offlineBannerIdlingResource = new OfflineBannerIdlingResource(tvOfflineIndicator);
            IdlingRegistry.getInstance().register(offlineBannerIdlingResource);
        });

        // Wait until the banner is gone and verify that it resets. it should go back to back offline (so its ready for next offline event)
        // and the visibility should be gone.
        onView(withId(R.id.tvOfflineIndicator))
                .check(matches(withEffectiveVisibility(GONE)));
        onView(withId(R.id.tvOfflineIndicator))
                .check(matches(withText(R.string.you_are_currently_offline)));
    }

    /**
     *  IdlingResource that waits until the given TextView's visibility is GONE.
     */
    public static class OfflineBannerIdlingResource implements IdlingResource {
        private final TextView tvOfflineIndicator;
        private ResourceCallback resourceCallback;

        public OfflineBannerIdlingResource(TextView tvOfflineIndicator) {
            this.tvOfflineIndicator = tvOfflineIndicator;
        }

        @Override
        public String getName() {
            return OfflineBannerIdlingResource.class.getName();
        }

        @Override
        public boolean isIdleNow() {
            boolean idle = tvOfflineIndicator.getVisibility() == View.GONE;
            if (idle && resourceCallback != null) {
                resourceCallback.onTransitionToIdle();
            }
            return idle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            this.resourceCallback = callback;
        }
    }
}
