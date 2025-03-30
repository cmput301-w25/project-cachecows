package com.example.feelink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class PendingSyncManagerTest {
    private static final String PREFS_NAME = "pending_sync_prefs";
    private static final String KEY_PENDING_IDS = "pending_ids";

    @Mock
    Context mockContext;

    @Mock
    SharedPreferences mockSharedPrefs;

    @Mock
    SharedPreferences.Editor mockEditor;

    private PendingSyncManager pendingSyncManager;

    @Before
    public void setUp() {
        when(mockContext.getSharedPreferences(eq(PREFS_NAME), anyInt()))
                .thenReturn(mockSharedPrefs);
        when(mockSharedPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putStringSet(eq(KEY_PENDING_IDS), anySet())).thenReturn(mockEditor);

        pendingSyncManager = new PendingSyncManager(mockContext);
    }

    @Test
    public void testGetPendingIds_emptyByDefault() {
        // If no data in SharedPreferences, getStringSet an empty set
        when(mockSharedPrefs.getStringSet(KEY_PENDING_IDS, new HashSet<>()))
                .thenReturn(new HashSet<>());

        Set<String> pendingIds = pendingSyncManager.getPendingIds();
        assertTrue(pendingIds.isEmpty());
    }

    @Test
    public void testGetPendingIds_nonEmpty() {
        // say SharedPreferences already has 2 IDs stored
        Set<String> stored = new HashSet<>();
        stored.add("id1");
        stored.add("id2");

        when(mockSharedPrefs.getStringSet(KEY_PENDING_IDS, new HashSet<>()))
                .thenReturn(stored);

        Set<String> pendingIds = pendingSyncManager.getPendingIds();

        assertEquals(2, pendingIds.size());
        assertTrue(pendingIds.contains("id1"));
        assertTrue(pendingIds.contains("id2"));
    }

    @Test
    public void testAddPendingId_savesToPrefs() {
        Set<String> existing = new HashSet<>();
        existing.add("id1");
        when(mockSharedPrefs.getStringSet(eq(KEY_PENDING_IDS), anySet()))
                .thenReturn(existing);

        pendingSyncManager.addPendingId("newId");

        // Mockito ArgumentCaptor was used here to capture the Set that was passed to putStringSet()
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(mockEditor).putStringSet(eq(KEY_PENDING_IDS), captor.capture());
        verify(mockEditor).apply();  //Ensures the changes were actually saved (not just staged)

        Set<String> putSet = captor.getValue();
        assertEquals(2, putSet.size());
        assertTrue(putSet.contains("id1"));
        assertTrue(putSet.contains("newId"));
    }

    @Test
    public void testRemovePendingId_existingIdIsRemoved() {
        Set<String> existing = new HashSet<>();
        existing.add("id1");
        existing.add("removeMe");
        existing.add("id2");
        when(mockSharedPrefs.getStringSet(eq(KEY_PENDING_IDS), anySet()))
                .thenReturn(existing);

        pendingSyncManager.removePendingId("removeMe");

        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(mockEditor).putStringSet(eq(KEY_PENDING_IDS), captor.capture());
        verify(mockEditor).apply();

        Set<String> newSet = captor.getValue();
        assertEquals(2, newSet.size());
        assertTrue(newSet.contains("id1"));
        assertTrue(newSet.contains("id2"));
        assertFalse(newSet.contains("removeMe"));
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }
}
