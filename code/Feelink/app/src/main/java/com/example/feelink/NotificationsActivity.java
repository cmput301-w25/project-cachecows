package com.example.feelink;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feelink.NotificationAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private LinearLayout emptyState;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize views
        emptyState = findViewById(R.id.emptyState);
        recyclerView = findViewById(R.id.notificationsRecycler);
        tabLayout = findViewById(R.id.tabLayout);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        setupTabLayout();
        checkEmptyState();
    }

    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Follow Requests"));
        tabLayout.addTab(tabLayout.newTab().setText("Interactions"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateNotifications(tab.getPosition());
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateNotifications(int tabPosition) {
        // This will be replaced with actual data loading later
        List<Notification> notifications = new ArrayList<>();
        adapter.updateNotifications(notifications);
        checkEmptyState();
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