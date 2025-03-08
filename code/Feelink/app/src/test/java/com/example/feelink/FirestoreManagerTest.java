package com.example.feelink;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class FirestoreManagerTest{

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private CollectionReference mockMoodCollection;
    @Mock private DocumentReference mockDocument;
    @Mock private Query mockQuery;
    @Mock private QuerySnapshot mockQuerySnapshot;

    private FirestoreManager firestoreManager;
    private final String testUserId = "test_user_123";

    @Before
    public void setUp() {// Initialize with mock Firestore
        firestoreManager = new FirestoreManager(testUserId, mockFirestore);

        // Mock collection chain
        when(mockFirestore.collection(anyString())).thenReturn(mockMoodCollection);
        when(mockMoodCollection.document(anyString())).thenReturn(mockDocument);
        when(mockMoodCollection.add(any(Map.class))).thenReturn(mock(Task.class));

        // Mock query chain
        when(mockMoodCollection.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.orderBy(anyString(), any(Query.Direction.class))).thenReturn(mockQuery);

        // Mock successful task
        Task<QuerySnapshot> successfulTask = Tasks.forResult(mock(QuerySnapshot.class));
        when(mockQuery.get()).thenReturn(successfulTask);
    }

    @Test
    public void addMoodEvent_Success() {
        // Arrange
        MoodEvent testEvent = new MoodEvent("happy", "work", "alone", "achievement");
        when(mockMoodCollection.add(any(Map.class))).thenReturn(Tasks.forResult(mockDocument));

        // Act
        firestoreManager.addMoodEvent(testEvent, new FirestoreManager.OnMoodEventListener() {
            @Override
            public void onSuccess(MoodEvent moodEvent) {
                // Assert
                assert moodEvent.getId() == mockDocument.getId().hashCode();
            }

            @Override
            public void onFailure(String errorMessage) {
                throw new AssertionError("Should not fail");
            }
        });

        // Verify
        verify(mockMoodCollection).add(any(Map.class));
    }

    @Test
    public void getMoodEvents_Success() {
        // Arrange
        QueryDocumentSnapshot mockSnapshot = mock(QueryDocumentSnapshot.class);
        when(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot));
        when(mockQuerySnapshot.iterator()).thenReturn(Collections.singletonList(mockSnapshot).iterator());

        // Mock document fields
        when(mockSnapshot.getId()).thenReturn("doc123");
        when(mockSnapshot.getString("emotionalState")).thenReturn("happy");
        when(mockSnapshot.getDate("timestamp")).thenReturn(new Date());

        // Act
        firestoreManager.getMoodEvents(new FirestoreManager.OnMoodEventsListener() {
            @Override
            public void onSuccess(List<MoodEvent> moodEvents) {
                // Assert
                assert !moodEvents.isEmpty();
                assert moodEvents.get(0).getEmotionalState().equals("happy");
            }

            @Override
            public void onFailure(String errorMessage) {
                throw new AssertionError("Should not fail");
            }
        });

        // Verify
        verify(mockQuery).get();
    }

    @Test
    public void deleteMoodEvent_Success() {
        // Arrange
        QueryDocumentSnapshot mockSnapshot = mock(QueryDocumentSnapshot.class);
        when(mockSnapshot.getId()).thenReturn("doc123");
        when(mockSnapshot.getReference()).thenReturn(mockDocument);

        QuerySnapshot mockSnapshotResult = mock(QuerySnapshot.class);
        when(mockSnapshotResult.iterator()).thenReturn(Collections.singletonList(mockSnapshot).iterator());
        Task<QuerySnapshot> successfulQueryTask = Tasks.forResult(mockSnapshotResult);
        when(mockQuery.get()).thenReturn(successfulQueryTask);

        // Mock successful delete task
        Task<Void> successfulDeleteTask = Tasks.forResult(null);
        when(mockDocument.delete()).thenReturn(successfulDeleteTask);

        // Act & Assert
        firestoreManager.deleteMoodEvent("doc123".hashCode(), new FirestoreManager.OnDeleteListener() {
            @Override
            public void onSuccess() { /* Test passes */ }

            @Override
            public void onFailure(String errorMessage) {
                throw new AssertionError("Should not fail");
            }
        });
    }
}