package com.example.feelink;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private List<Comment> comments = new ArrayList<>();
    private FirestoreManager firestoreManager;
    private String moodEventId;
    private EditText etComment;
    private ImageButton btnSend;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        // Get intent extras
        moodEventId = getIntent().getStringExtra("MOOD_EVENT_ID");
        String moodEventOwnerId = getIntent().getStringExtra("MOOD_EVENT_OWNER_ID");
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestoreManager = new FirestoreManager(currentUserId);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerComments);
        etComment = findViewById(R.id.etComment);
        btnSend = findViewById(R.id.btnSend);

        // Setup RecyclerView
        adapter = new CommentAdapter(comments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load existing comments
        loadComments();

        // Add automatic scrolling when keyboard appears
        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) { // Keyboard opened
                recyclerView.postDelayed(() -> {
                    if (!comments.isEmpty()) {
                        recyclerView.smoothScrollToPosition(comments.size() - 1);
                    }
                }, 100);
            }
        });

        // Send comment button handler
        btnSend.setOnClickListener(v -> {
            String commentText = etComment.getText().toString().trim();
            if (!commentText.isEmpty()) {
                // Check if user is owner or follower
                firestoreManager.checkFollowStatus(moodEventOwnerId, new FirestoreManager.OnFollowCheckListener() {
                    @Override
                    public void onSuccess(boolean isFollowing) {
                        if (currentUserId.equals(moodEventOwnerId) || isFollowing) {
                            postComment(commentText, currentUserId);
                        } else {
                            Toast.makeText(CommentsActivity.this,
                                    "You must follow this user to comment",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(CommentsActivity.this,
                                "Error verifying permissions: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void postComment(String commentText, String userId) {
        Comment comment = new Comment(commentText, userId);
        firestoreManager.addComment(moodEventId, comment, new FirestoreManager.OnCommentListener() {
            @Override
            public void onSuccess(Comment addedComment) {
                // Add username resolution
                firestoreManager.getUsernameById(userId, new FirestoreManager.OnUsernameListener() {
                    @Override
                    public void onSuccess(String username) {
                        addedComment.setUsername(username);
                        comments.add(addedComment);
                        adapter.notifyItemInserted(comments.size() - 1);
                        recyclerView.scrollToPosition(comments.size() - 1);
                        etComment.setText("");
                    }

                    @Override
                    public void onFailure(String fallbackName) {
                        addedComment.setUsername(fallbackName);
                        comments.add(addedComment);
                        adapter.notifyItemInserted(comments.size() - 1);
                        recyclerView.scrollToPosition(comments.size() - 1);
                        etComment.setText("");
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(CommentsActivity.this,
                        "Failed to post comment: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadComments() {
        firestoreManager.getComments(moodEventId, new FirestoreManager.OnCommentsListener() {
            @Override
            public void onSuccess(List<Comment> commentList) {
                comments.clear();
                comments.addAll(commentList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(CommentsActivity.this, "Failed to load comments: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}