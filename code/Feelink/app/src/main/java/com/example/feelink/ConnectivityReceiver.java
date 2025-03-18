package com.example.feelink;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.BroadcastReceiver;
public class ConnectivityReceiver extends BroadcastReceiver {

    public interface ConnectivityReceiverListener {
        void onNetworkConnectionChanged(boolean isConnected);
    }

    private ConnectivityReceiverListener listener;

    public ConnectivityReceiver(ConnectivityReceiverListener listener) {
        this.listener = listener;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = isNetworkAvailable(context);
        if (isConnected) {
            // Connectivity is restored, check for pending syncs
            PendingSyncManager pendingSyncManager = new PendingSyncManager(context);
            FirestoreManager firestoreManager = new FirestoreManager("current_user_id"); // Replace with actual user ID

            for (String documentId : pendingSyncManager.getPendingIds()) {
                // Attempt to sync the pending mood event
                firestoreManager.syncPendingMoodEvent(documentId, new FirestoreManager.OnMoodEventListener() {
                    @Override
                    public void onSuccess(MoodEvent moodEvent) {
                        // Sync successful so remove from pending list
                        pendingSyncManager.removePendingId(documentId);
                        if (listener != null) {
                            listener.onNetworkConnectionChanged(true);
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {}
                });
            }
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }


}
