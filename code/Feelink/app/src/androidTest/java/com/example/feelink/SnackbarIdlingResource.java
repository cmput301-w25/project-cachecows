package com.example.feelink;

import android.app.Activity;
import android.view.View;

import androidx.test.espresso.IdlingResource;

public class SnackbarIdlingResource implements IdlingResource {
    private ResourceCallback resourceCallback;
    private boolean isIdle = true;

    @Override
    public String getName() {
        return "SnackbarIdlingResource";
    }

    @Override
    public boolean isIdleNow() {
        if (isIdle) {
            // Check if the Snackbar is visible
            Activity activity = getActivity();
            if (activity != null) {
                View snackbarView = activity.findViewById(com.google.android.material.R.id.snackbar_text);
                if (snackbarView != null && snackbarView.isShown()) {
                    isIdle = false;
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.resourceCallback = callback;
    }

    private Activity getActivity() {
        // You need to get the current activity here
        // This can be done using ActivityScenario or similar
        // For simplicity, assume you have a method to get the activity
        return getActivityFromScenario();
    }

    private Activity getActivityFromScenario() {
        // Implement this method to get the current activity from your ActivityScenario
        // This might involve using ActivityScenarioRule or similar
        // For demonstration purposes, assume it's implemented correctly
        return null; // Implement this method
    }
}

