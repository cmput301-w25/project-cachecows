package com.example.feelink;

import android.os.IBinder;
import android.view.WindowManager;

import androidx.test.espresso.Root;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ToastMatcher extends TypeSafeMatcher<Root> {

    @Override
    protected boolean matchesSafely(Root root) {
        int type = root.getWindowLayoutParams().get().type;
        IBinder token = root.getDecorView().getWindowToken();
        IBinder windowToken = root.getDecorView().getApplicationWindowToken();
        return type == WindowManager.LayoutParams.TYPE_TOAST && token == windowToken;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is a Toast");
    }

    @Override
    protected void describeMismatchSafely(Root item, Description mismatchDescription) {
        mismatchDescription.appendText("was not a Toast");
    }
}
