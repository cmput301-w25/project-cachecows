package com.example.feelink;

import androidx.test.espresso.IdlingResource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class FirestoreIdlingResource implements IdlingResource {

    private static final String RESOURCE = "Firestore";
    private ResourceCallback resourceCallback;
    private boolean isIdle = true;

    private FirestoreIdlingResource() {
        // Monitor Firestore operations
        FirebaseFirestore.getInstance().addSnapshotsInSyncListener(() -> {
            isIdle = true;
            if (resourceCallback != null) {
                resourceCallback.onTransitionToIdle();
            }
        });
    }

    @Override
    public String getName() {
        return RESOURCE;
    }

    @Override
    public boolean isIdleNow() {
        return isIdle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.resourceCallback = callback;
    }

    public static FirestoreIdlingResource create() {
        return new FirestoreIdlingResource();
    }
}