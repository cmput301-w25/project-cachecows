package com.example.feelink.controller;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages a list of pending mood event IDs that need to be synced with Firestore.
 * This class uses SharedPreferences to store and retrieve the list of pending IDs.
 *
 * <p>When the app is offline, mood events are saved locally, and their document IDs
 * are added to the pending sync list. When connectivity is restored, these IDs are used
 * to sync the events with Firestore.</p>
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *  <li>US 07.01.01.01</li>
 *  <li>US 07.01.01.02</li>
 * </ul>
 * @see FirestoreManager
 * @see ConnectivityReceiver
 */
public class PendingSyncManager {
    private static final String PREFS_NAME = "pending_sync_prefs";  // Name of the SharedPreferences file
    private static final String KEY_PENDING_IDS = "pending_ids";  // Key for storing the set of pending IDs in SharedPreferences
    private SharedPreferences prefs;

    /**
     * Constructor for the PendingSyncManager.
     * @param context The context used to access SharedPreferences.
     */
    public PendingSyncManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Retrieves the set of pending mood event IDs that need to be synced..
     * @return A set of document IDs representing pending mood events.
     */
    public Set<String> getPendingIds() {
        return prefs.getStringSet(KEY_PENDING_IDS, new HashSet<String>());
    }

    /**
     * Adds a pending mood event ID to the list.
     * @param id the document ID of the mood event to be added to the pending list.
     */
    public void addPendingId(String id) {
        Set<String> pendingIds = new HashSet<>(getPendingIds());
        pendingIds.add(id);
        prefs.edit().putStringSet(KEY_PENDING_IDS, pendingIds).apply();
    }

    /**
     * Removes a document ID from the list of pending mood events.
     *
     * @param id The document ID of the mood event to be removed from the pending list.
     */
    public void removePendingId(String id) {
        Set<String> pendingIds = new HashSet<>(getPendingIds());
        if (pendingIds.contains(id)) {
            pendingIds.remove(id);
            prefs.edit().putStringSet(KEY_PENDING_IDS, pendingIds).apply();
        }
    }
}
