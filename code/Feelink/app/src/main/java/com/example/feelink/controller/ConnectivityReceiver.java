package com.example.feelink.controller;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.content.BroadcastReceiver;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.feelink.R;
import com.example.feelink.model.MoodEvent;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * A BroadcastReceiver that listens for changes in network connectivity.
 * When the device's network connection is restored, it checks for pending mood events
 * that need to be synced with Firestore and attempts to sync them.
 *
 * <p>This class is used to handle offline behavior in the app. When the app is offline,
 * mood events are saved locally and marked as pending sync. When connectivity is restored,
 * this receiver triggers the synchronization process.</p>
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *  <li>US 07.01.01.01</li>
 *  <li>US 07.01.01.02</li>
 * </ul>
 * @see PendingSyncManager
 */
public class ConnectivityReceiver extends BroadcastReceiver {
    /**
     * Interface to notify the listener when the network connection changes.
     */
    public interface ConnectivityReceiverListener {
        /**
         * Called when the network connection changes.
         * @param isConnected True if the device is connected to a network, false otherwise.
         */
        void onNetworkConnectionChanged(boolean isConnected);
    }

    private ConnectivityReceiverListener listener;
    private static boolean wasOffline = false;  //to check if the uses was offline at least once

    /**
     * Constructor for the ConnectivityReceiver.
     * @param listener The listener to notify when the network connection changes.
     */
    public ConnectivityReceiver(ConnectivityReceiverListener listener) {
        this.listener = listener;
    }

    /**
     * Called when the BroadcastReceiver receives an Intent broadcast.
     * This method checks if the device is connected to a network and, if so,
     * attempts to sync any pending mood events with Firestore.
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = isNetworkAvailable(context);
        if (isConnected) {
            // Notify listener online.
            if (listener != null) {
                listener.onNetworkConnectionChanged(true);
            }
            // Connectivity restored – check for pending syncs.
            PendingSyncManager pendingSyncManager = new PendingSyncManager(context);
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : "default_user";
            FirestoreManager firestoreManager = new FirestoreManager(uid);

            for (String documentId : pendingSyncManager.getPendingIds()) {
                firestoreManager.syncPendingMoodEvent(documentId, new FirestoreManager.OnMoodEventListener() {
                    @Override
                    public void onSuccess(MoodEvent moodEvent) {
                        // If location update is pending
                        boolean needsLocationUpdate = (moodEvent.getLocationName() == null ||
                                moodEvent.getLocationName().equals("Pending Location"));
                        if (needsLocationUpdate) {
                            Double lat = moodEvent.getLatitude();
                            Double lon = moodEvent.getLongitude();
                            if (lat != null && lon != null) {
                                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                                    assert addresses != null;
                                    if (!addresses.isEmpty()) {
                                        Address address = addresses.get(0);
                                        moodEvent.setLocationName(address.getAddressLine(0));
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {} // lat/lon are null
                        }
                        // If photo upload is pending
                        boolean needsPhotoUpload = (moodEvent.getImageUrl() == null &&
                                moodEvent.getTempLocalImagePath() != null);

                        if (needsPhotoUpload) {
                            firestoreManager.uploadLocalFileThenUpdateDocument(
                                    moodEvent.getTempLocalImagePath(),
                                    moodEvent.getDocumentId(),
                                    new FirestoreManager.OnImageUploadListener() {
                                        @Override
                                        public void onImageUploadSuccess(String newImageUrl) {
                                            moodEvent.setImageUrl(newImageUrl);
                                            moodEvent.setTempLocalImagePath(null);
                                            //Update the mood event with any changes (location and/or photo).
                                            firestoreManager.updateMoodEvent(moodEvent, moodEvent.getDocumentId(), new FirestoreManager.OnMoodEventListener() {
                                                @Override
                                                public void onSuccess(MoodEvent updated) {
                                                    pendingSyncManager.removePendingId(documentId);
                                                    Intent syncIntent = new Intent("MOOD_EVENT_SYNCED");
                                                    syncIntent.putExtra("DOCUMENT_ID", documentId);
                                                    LocalBroadcastManager.getInstance(context).sendBroadcast(syncIntent);
                                                }
                                                @Override
                                                public void onFailure(String errorMessage) {} //pending on failure
                                            });
                                        }
                                        @Override
                                        public void onImageUploadFailure(String error) {} //pending of failure
                                    }
                            );
                        } else { //if no photo upload is pending, update the mood event
                            firestoreManager.updateMoodEvent(moodEvent, moodEvent.getDocumentId(), new FirestoreManager.OnMoodEventListener() {
                                @Override
                                public void onSuccess(MoodEvent updated) {
                                    pendingSyncManager.removePendingId(documentId);
                                    Intent syncIntent = new Intent("MOOD_EVENT_SYNCED");
                                    syncIntent.putExtra("DOCUMENT_ID", documentId);
                                    LocalBroadcastManager.getInstance(context).sendBroadcast(syncIntent);
                                }
                                @Override
                                public void onFailure(String errorMessage) {}
                            });
                        }
                    }
                    @Override
                    public void onFailure(String errorMessage) {}
                });
            }
        } else {
            // Notify listener offline.
            if (listener != null) {
                listener.onNetworkConnectionChanged(false);
            }
        }
    }

    /**
     * Checks if the device is connected to a network.
     * Adapted from Stack Overflow answer by Yoshimitsu
     * <a href="https://stackoverflow.com/questions/57284582/networkinfo-has-been-deprecated-by-api-29">...</a>
     * @param context The Context in which the method is called.
     * @return True if the device is connected to a network, false otherwise.
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    public static void handleBanner(boolean isConnected, TextView tvOfflineIndicator, Context context) {
        if (tvOfflineIndicator == null) return;
        if (isConnected) {
            if (wasOffline){
                tvOfflineIndicator.setText(R.string.back_online);
                tvOfflineIndicator.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.online_indicator_background));
                tvOfflineIndicator.setVisibility(View.VISIBLE);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    tvOfflineIndicator.setText(R.string.you_are_currently_offline);
                    tvOfflineIndicator.setBackgroundColor(
                            ContextCompat.getColor(context, R.color.offline_indicator_background)
                    );
                    tvOfflineIndicator.setVisibility(View.GONE);
                }, 3000);
                wasOffline = false;
            } else {
                tvOfflineIndicator.setVisibility(View.GONE);
            }
        } else {
            wasOffline = true;
            tvOfflineIndicator.setText(R.string.you_are_currently_offline);
            tvOfflineIndicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.offline_indicator_background)
            );
            tvOfflineIndicator.setVisibility(View.VISIBLE);
        }
    }
}
