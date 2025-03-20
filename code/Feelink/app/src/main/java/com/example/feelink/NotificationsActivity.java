package com.example.feelink;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView commentsLikesRecycler, followRequestsRecycler;
    private Button btnCommentsLikes, btnFollowRequests, btnAccept, btnDeny;
    private LinearLayout detailView;
    private TextView tvRequestDetail;
    private List<FollowRequest> followRequests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize views
        commentsLikesRecycler = findViewById(R.id.recyclerCommentsLikes);
        followRequestsRecycler = findViewById(R.id.recyclerFollowRequests);
        btnCommentsLikes = findViewById(R.id.btnCommentsLikes);
        btnFollowRequests = findViewById(R.id.btnFollowRequests);
        detailView = findViewById(R.id.detailView);
        tvRequestDetail = findViewById(R.id.tvRequestDetail);
        btnAccept = findViewById(R.id.btnAccept);
        btnDeny = findViewById(R.id.btnDeny);

        // Setup sample data
        followRequests.add(new FollowRequest("Dai men has requested to follow your mood event", "20 minutes ago"));
        followRequests.add(new FollowRequest("Than trung wants to follow your recent post", "1 hour ago"));



        // Setup RecyclerViews
        setupRecyclers();

        // Set initial state
        showCommentsLikesSection();

        // Set click listeners
        btnCommentsLikes.setOnClickListener(v -> showCommentsLikesSection());
        btnFollowRequests.setOnClickListener(v -> showFollowRequestsSection());
    }

    private void setupRecyclers() {
        List<CommentLike> sampleCommentsLikes = new ArrayList<>();
        sampleCommentsLikes.add(new CommentLike("Edeln Vindain reacted to your comment", "just now"));
        sampleCommentsLikes.add(new CommentLike("Than trung liked your mood post", "2 hours ago"));
        // Comments/Likes RecyclerView
        commentsLikesRecycler.setLayoutManager(new LinearLayoutManager(this));
        commentsLikesRecycler.setAdapter(new CommentsLikesAdapter(sampleCommentsLikes)); // Use the sample data

        // Follow Requests RecyclerView
        followRequestsRecycler.setLayoutManager(new LinearLayoutManager(this));
        followRequestsRecycler.setAdapter(new FollowRequestsAdapter(followRequests, this));
    }
    public void showRequestDetail(int position) {
        FollowRequest request = followRequests.get(position);
        detailView.setVisibility(View.VISIBLE);
        followRequestsRecycler.setVisibility(View.GONE);

        tvRequestDetail.setText(request.getRequestText() + "\n" + request.getTimestamp());

        btnAccept.setOnClickListener(v -> handleRequest(position, true));
        btnDeny.setOnClickListener(v -> handleRequest(position, false));
    }

    private void handleRequest(int position, boolean accepted) {
        if(accepted) {
            // Update follower counts here
        }
        followRequests.remove(position);
        followRequestsRecycler.getAdapter().notifyItemRemoved(position);
        returnToListView();
    }

    private void returnToListView() {
        detailView.setVisibility(View.GONE);
        followRequestsRecycler.setVisibility(View.VISIBLE);
    }

    private void showCommentsLikesSection() {
        commentsLikesRecycler.setVisibility(View.VISIBLE);
        followRequestsRecycler.setVisibility(View.GONE);
        btnCommentsLikes.setBackgroundTintList(getResources().getColorStateList(R.color.selected_tab_color));
        btnFollowRequests.setBackgroundTintList(getResources().getColorStateList(R.color.unselected_tab_color));
    }

    private void showFollowRequestsSection() {
        commentsLikesRecycler.setVisibility(View.GONE);
        followRequestsRecycler.setVisibility(View.VISIBLE);
        btnFollowRequests.setBackgroundTintList(getResources().getColorStateList(R.color.selected_tab_color));
        btnCommentsLikes.setBackgroundTintList(getResources().getColorStateList(R.color.unselected_tab_color));
    }
}