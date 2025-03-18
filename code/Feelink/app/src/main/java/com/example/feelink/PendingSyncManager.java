package com.example.feelink;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class PendingSyncManager {
    private static final String PREFS_NAME = "pending_sync_prefs";
    private static final String KEY_PENDING_IDS = "pending_ids";
    private SharedPreferences prefs;

    public PendingSyncManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public Set<String> getPendingIds() {
        return prefs.getStringSet(KEY_PENDING_IDS, new HashSet<String>());
    }

    public void addPendingId(String id) {
        Set<String> pendingIds = new HashSet<>(getPendingIds());
        pendingIds.add(id);
        prefs.edit().putStringSet(KEY_PENDING_IDS, pendingIds).apply();
    }

    public void removePendingId(String id) {
        Set<String> pendingIds = new HashSet<>(getPendingIds());
        if (pendingIds.contains(id)) {
            pendingIds.remove(id);
            prefs.edit().putStringSet(KEY_PENDING_IDS, pendingIds).apply();
        }
    }
}
