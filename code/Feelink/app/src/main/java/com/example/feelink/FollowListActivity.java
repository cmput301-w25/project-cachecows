package com.example.feelink;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
/**
 * Displays either followers or followed users with profile details. Handles empty states and
 * real-time updates. Central to follow management features.
 * <p>
 * Key user stories:
 * <ul>
 *   <li>US 05.02.02 (View follow requests)</li>
 *   <li>US 03.03.01 (View user profiles)</li>
 * </ul>
 */

public class FollowListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private FirestoreManager firestoreManager;
    private TextView emptyStateText;

    private TextView titleText;
    /**
     * Initializes the activity with dynamic header and list based on follow type
     *
     * @param savedInstanceState Preserved state if available
     *
     * Handles: US 05.02.01 (Follow management), US 05.03.01 (Recent mood visibility)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_list);

        String userId = getIntent().getStringExtra("userId");
        String type = getIntent().getStringExtra("type");

        titleText = findViewById(R.id.titleText);
        String headerText = type.equals("following")
                ? "Your Following List"
                : "Your Followers List";
        titleText.setText(headerText);

        recyclerView = findViewById(R.id.recyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        firestoreManager = new FirestoreManager(userId);
        fetchData(type);
    }

    private void fetchData(String type) {
        firestoreManager.getFollowRelations(type, new FirestoreManager.OnFollowListListener() {
            @Override
            public void onSuccess(List<User> users) {
                adapter = new UserAdapter(users, FollowListActivity.this);
                recyclerView.setAdapter(adapter);
                emptyStateText.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(FollowListActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}