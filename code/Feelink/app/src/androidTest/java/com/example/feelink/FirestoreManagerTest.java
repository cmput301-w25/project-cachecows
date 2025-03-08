package com.example.feelink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

@RunWith(JUnit4.class)
public class FirestoreManagerTest {

    private FirestoreManager firestoreManager;

    @BeforeClass
    public static void setup() {
        // Specific address for emulated device to access our localHost
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }

    @Test
    public void testAddMoodEvent() {
        MoodEvent moodEvent = new MoodEvent("Happy", "Good weather", "With friends", "Feeling great!");
        firestoreManager.addMoodEvent(moodEvent, new FirestoreManager.OnMoodEventListener() {
            @Override
            public void onSuccess(MoodEvent moodEvent) {
                assertNotNull(moodEvent.getId());
            }

            @Override
            public void onFailure(String errorMessage) {
                assertEquals("Failed to add mood event", errorMessage);
            }
        });
    }

    @Test
    public void testGetMoodEvents() {
        firestoreManager.getMoodEvents(new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                assertNotNull(moodEvents);
            }

            @Override
            public void onFailure(String errorMessage) {
                assertEquals("Failed to retrieve mood events", errorMessage);
            }
        });
    }

    @Test
    public void testDeleteMoodEvent() {
        // Create a mood event to delete
        MoodEvent moodEvent = new MoodEvent("Happy", "Good weather", "With friends", "Feeling great!");
        firestoreManager.addMoodEvent(moodEvent, new FirestoreManager.OnMoodEventListener() {
            @Override
            public void onSuccess(MoodEvent addedMoodEvent) {
                // Delete the mood event
                firestoreManager.deleteMoodEvent(addedMoodEvent.getId(), new FirestoreManager.OnDeleteListener() {
                    @Override
                    public void onSuccess() {
                        // Verify that the mood event was deleted
                        firestoreManager.getMoodEvents(new FirestoreManager.OnMoodEventsListener() {
                            @Override
                            public void onSuccess(List<MoodEvent> moodEvents) {
                                // Ensure the deleted mood event is not in the list
                                for (MoodEvent event : moodEvents) {
                                    if (event.getId() == addedMoodEvent.getId()) {
                                        assertEquals("Mood event should not exist after deletion", false);
                                    }
                                }
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                assertEquals("Failed to retrieve mood events", errorMessage);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        assertEquals("Failed to delete mood event", errorMessage);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                assertEquals("Failed to add mood event for deletion", errorMessage);
            }
        });
    }
}