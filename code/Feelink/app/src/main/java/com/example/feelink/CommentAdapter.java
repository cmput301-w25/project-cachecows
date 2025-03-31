package com.example.feelink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Displays comments with user profiles and timestamps. Automatically resolves usernames from IDs.
 * <p>
 * Directly supports:
 * <ul>
 *   <li>US 05.07.02 (View comments on mood events)</li>
 *   <li>US 03.01.01/03.03.01 (User profile integration)</li>
 * </ul>
 */
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

    /**
     * Populates comment items with user data fetched from Firestore
     * @param holder ViewHolder to configure
     * @param position Comment position in list
     *
     * Implements: US 03.03.01 (Profile viewing via Glide-loaded images)
     */


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