package com.example.feelink.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feelink.adapter.CommentAdapter;
import com.example.feelink.controller.FirestoreManager;
import com.example.feelink.R;
import com.example.feelink.model.Comment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
/**
 * Enables commenting on mood events with real-time updates. Manages comment permissions and notifications.
 * <p>
 * Core implementation for:
 * <ul>
 *   <li>US 05.07.01 (Post comments on mood events)</li>
 *   <li>US 05.07.02 (View comment history)</li>
 * </ul>
 */

public class CommentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private List<Comment> comments = new ArrayList<>();
    private FirestoreManager firestoreManager;
    private String moodEventId;
    private EditText etComment;
    private ImageButton btnSend;

    public static boolean SKIP_AUTH_FOR_TESTING = false;
    public static String FORCE_USER_ID = null;

    /**
     * Configures comment interface and handles authentication checks
     * @param savedInstanceState Previous state data
     *
     * Enforces: US 05.02.01 (Follow permissions for commenting)
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        // Get intent extras
        moodEventId = getIntent().getStringExtra("MOOD_EVENT_ID");
        String moodEventOwnerId = getIntent().getStringExtra("MOOD_EVENT_OWNER_ID");
        String currentUserId;
        if (SKIP_AUTH_FOR_TESTING) {
            currentUserId = FORCE_USER_ID != null ? FORCE_USER_ID : "default_test_user";
        } else {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                currentUserId = currentUser.getUid();
            } else {
                Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

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
    /**
     * Resets testing configuration flags
     */
    public static void resetTestFlags() {
        SKIP_AUTH_FOR_TESTING = false;
        FORCE_USER_ID = null;
    }

    private void postComment(String commentText, String userId) {
        Comment comment = new Comment(commentText, userId);
        firestoreManager.addComment(moodEventId, comment, new FirestoreManager.OnCommentListener() {
            @Override
            public void onSuccess(Comment addedComment) {
                // Add username resolution
                String moodOwnerId = getIntent().getStringExtra("MOOD_EVENT_OWNER_ID");
                if (!userId.equals(moodOwnerId)) {
                    createCommentNotification(moodOwnerId, userId, commentText);
                }
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

    private void createCommentNotification(String receiverId, String senderId, String commentText) {
        FirestoreManager notificationManager = new FirestoreManager(senderId);
        notificationManager.createCommentNotification(receiverId, moodEventId, commentText,
                new FirestoreManager.OnNotificationListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("Notifications", "Comment notification created");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("Notifications", "Error creating notification: " + error);
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