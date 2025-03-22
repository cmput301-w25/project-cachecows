package com.example.feelink;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feelink.NotificationAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

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
        adapter = new NotificationAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        setupTabLayout();
        checkEmptyState();
    }

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
        } else {
            // Load interactions
            adapter.updateNotifications(new ArrayList<>());
            checkEmptyState();
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
}