package com.example.feelink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";
    private RecyclerView searchResultsRecyclerView;
    private TextView noResultsTextView;
    private UserAdapter adapter;
    private List<User> searchResults;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        EditText searchEditText = findViewById(R.id.searchEditText);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        noResultsTextView = findViewById(R.id.noResultsTextView);
        ImageView backButton = findViewById(R.id.backButton);

        // Set up RecyclerView
        searchResults = new ArrayList<>();
        adapter = new UserAdapter(searchResults, this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(adapter);
        ImageView navHome = findViewById(R.id.navMap);
        ImageView navChats = findViewById(R.id.navChats);
        ImageView navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, FeedManagerActivity.class));
            finish();
        });

        navChats.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            finish();
        });

        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, UserProfileActivity.class));
            finish();
        });

        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Set up search text listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().trim();
                if (!searchText.isEmpty()) {
                    performSearch(searchText);
                } else {
                    // Clear results when search field is empty
                    searchResults.clear();
                    adapter.notifyDataSetChanged();
                    noResultsTextView.setVisibility(View.GONE);
                    searchResultsRecyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String searchText) {
        // Search for users with username starting with the search text
        db.collection("users")
                .orderBy("username")
                .startAt(searchText)
                .endAt(searchText + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        searchResults.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            user.setId(document.getId());
                            searchResults.add(user);
                        }

                        adapter.notifyDataSetChanged();

                        if (searchResults.isEmpty()) {
                            noResultsTextView.setVisibility(View.VISIBLE);
                            searchResultsRecyclerView.setVisibility(View.GONE);
                        } else {
                            noResultsTextView.setVisibility(View.GONE);
                            searchResultsRecyclerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.e(TAG, "Error getting search results: ", task.getException());
                        Toast.makeText(SearchActivity.this,
                                "Error performing search",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
