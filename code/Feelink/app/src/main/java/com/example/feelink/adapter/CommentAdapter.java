package com.example.feelink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.feelink.R;
import com.example.feelink.controller.FirestoreManager;
import com.example.feelink.model.Comment;
import com.example.feelink.model.User;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments;

    public CommentAdapter(List<Comment> comments) {
        this.comments = comments;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(comment.getTimestamp());
        holder.tvTimestamp.setText(formattedDate);
        holder.tvComment.setText(comment.getText());

        // Add username resolution
        FirestoreManager firestoreManager = new FirestoreManager(comment.getUserId());
        firestoreManager.getUserInfo(comment.getUserId(), new FirestoreManager.OnUserInfoListener() {
            @Override
            public void onSuccess(User user) {
                // Set username
                holder.tvUsername.setText(user.getUsername());

                // Load profile image if it exists
                String profileUrl = user.getProfileImageUrl();
                if (profileUrl != null && !profileUrl.isEmpty()) {
                    // If using Glide
                    Glide.with(holder.itemView.getContext())
                            .load(profileUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(holder.ivCommentProfile);
                } else {
                    holder.ivCommentProfile.setImageResource(R.drawable.ic_profile_placeholder);
                }
            }
            @Override
            public void onFailure(String fallbackName) {
                holder.tvUsername.setText(fallbackName);
                holder.ivCommentProfile.setImageResource(R.drawable.ic_profile_placeholder);
            }
        });


    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvComment, tvTimestamp;
        CircleImageView ivCommentProfile;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            ivCommentProfile = itemView.findViewById(R.id.ivCommentProfile);
        }
    }
}