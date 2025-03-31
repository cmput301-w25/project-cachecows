package com.example.feelink.view;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feelink.controller.FirestoreManager;
import com.example.feelink.adapter.NotificationAdapter;
import com.example.feelink.R;
import com.example.feelink.model.FollowRequest;
import com.example.feelink.model.Notification;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
/**
 * Manages follow requests and interaction notifications
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 05.01.01 - Follow request notifications</li>
 *   <li>US 05.02.01 - Comment interaction alerts</li>
 * </ul>
 */

public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private LinearLayout emptyState;
    private TabLayout tabLayout;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize views
        emptyState = findViewById(R.id.emptyState);
        recyclerView = findViewById(R.id.notificationsRecycler);
        tabLayout = findViewById(R.id.tabLayout);
        firestoreManager = new FirestoreManager(FirebaseAuth.getInstance().getCurrentUser().getUid());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NotificationAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        setupTabLayout();
        checkEmptyState();

        // Inside NotificationsActivity's onCreate() after initializing views
        ImageView navSearch = findViewById(R.id.navSearch);
        ImageView navHome = findViewById(R.id.navHome);
        ImageView navProfile = findViewById(R.id.navProfile);
        ImageView navMap = findViewById(R.id.navMap);


        navSearch.setOnClickListener(v -> {Log.d("NAVIGATION", "Search icon clicked"); startActivity(new Intent(this, SearchActivity.class));});
        navHome.setOnClickListener(v -> {Log.d("NAVIGATION", "Search icon clicked"); startActivity(new Intent(this, FeedManagerActivity.class));});
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, UserProfileActivity.class)));
        navMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, MoodMapActivity.class);
            startActivity(intent);
        });
    }
    /**
     * Updates notification list based on selected tab
     * @param tabPosition 0 for follow requests, 1 for interactions
     */

    private void updateNotifications(int tabPosition) {
        if (tabPosition == 0) { // Follow Requests tab
            firestoreManager.getFollowRequests(new FirestoreManager.OnFollowRequestsListener() {
                @Override
                public void onSuccess(List<FollowRequest> requests) {
                    List<Notification> notifications = convertRequestsToNotifications(requests);
                    adapter.updateNotifications(notifications);
                    checkEmptyState();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(NotificationsActivity.this,
                            "Error loading requests: " + error, Toast.LENGTH_SHORT).show();
                    Log.e("Error loading requests",error);
                }
            });
        } else { // Interactions tab
            firestoreManager.getCommentNotifications(new FirestoreManager.OnNotificationsListener() {
                @Override
                public void onSuccess(List<Notification> notifications) {
                    adapter.updateNotifications(notifications);
                    checkEmptyState();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(NotificationsActivity.this,
                            "Error loading interactions: " + error, Toast.LENGTH_SHORT).show();
                    Log.e("Error loading interactions",error);
                }
            });
        }

    }
    private List<Notification> convertRequestsToNotifications(List<FollowRequest> requests) {
        List<Notification> notifications = new ArrayList<>();
        for (FollowRequest request : requests) {
            Notification notification = new Notification();
            notification.setId(request.getId());
            notification.setType(Notification.Type.FOLLOW_REQUEST);
            notification.setSenderId(request.getSenderId());
            notification.setMessage(request.getSenderName()); // Use senderName
            // Convert Date to long
            if (request.getTimestamp() != null) {
                notification.setTimestamp(request.getTimestamp().getTime());
            } else {
                notification.setTimestamp(System.currentTimeMillis()); // Fallback
            }
            notifications.add(notification);
        }
        return notifications;
    }

    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Follow Requests"));
        tabLayout.addTab(tabLayout.newTab().setText("Interactions"));

        updateNotifications(0); // Load follow requests immediately

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateNotifications(tab.getPosition());
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        tabLayout.post(this::applyCustomFontToTabs);
    }



    private void checkEmptyState() {
        if (adapter.getItemCount() == 0) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void applyCustomFontToTabs() {
        Typeface typeface = ResourcesCompat.getFont(this, R.font.poppins_regular);

        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                ViewGroup vg = (ViewGroup) tab.view;
                for (int j = 0; j < vg.getChildCount(); j++) {
                    View tabViewChild = vg.getChildAt(j);
                    if (tabViewChild instanceof TextView) {
                        ((TextView) tabViewChild).setTypeface(typeface);
                    }
                }
            }
        }
    }

}