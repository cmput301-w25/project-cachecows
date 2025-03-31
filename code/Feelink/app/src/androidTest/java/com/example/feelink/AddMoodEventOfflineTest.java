package com.example.feelink;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.content.Context;
import android.content.Intent;


import com.example.feelink.controller.PendingSyncManager;
import com.example.feelink.view.AddMoodEventActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;


@RunWith(AndroidJUnit4.class)
public class AddMoodEventOfflineTest {
    private Context context;
    private PendingSyncManager pendingSyncManager;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        pendingSyncManager = new PendingSyncManager(context);
        clearPendingSyncs();
    }

        private void clearPendingSyncs() {
            Set<String> pendingIds = pendingSyncManager.getPendingIds();
            for (String id : pendingIds) {
                pendingSyncManager.removePendingId(id);
            }
        }

    @Test
    public void testAddMoodEventInForcedOffline() {
        // forced offline flag from addMoodEventActivity
        Intent intent = new Intent(context, AddMoodEventActivity.class);
        intent.putExtra("FORCE_OFFLINE", true);

        try (ActivityScenario<AddMoodEventActivity> scenario = ActivityScenario.launch(intent)) {

            // Perform UI actions
            onView(withId(R.id.moodHappy)).perform(click());
            onView(withId(R.id.etReason)).perform(typeText("Offline test"), closeSoftKeyboard());
            onView(withId(R.id.btnAddMood)).perform(click());

            scenario.onActivity(activity -> {
                assertTrue(activity.isFinishing());
            });

            // Verify pending sync
            Set<String> pendingIds = pendingSyncManager.getPendingIds();
            assertFalse(pendingIds.isEmpty());

            // Verify the saved data
            String documentId = pendingIds.iterator().next();
            assertNotNull(documentId);
        }
    }
}
