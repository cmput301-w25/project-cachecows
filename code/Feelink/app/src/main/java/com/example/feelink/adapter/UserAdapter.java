package com.example.feelink.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.feelink.R;
import com.example.feelink.model.User;
import com.example.feelink.view.OtherUserProfileActivity;
import com.example.feelink.view.UserProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
/**
 * Shows user profiles in lists with navigation to full profiles. Handles both current user
 * and other users' profiles.
 * <p>
 * Core implementation for:
 * <ul>
 *   <li>US 03.03.01 (View user profiles)</li>
 *   <li>Profile switching between own/others' views</li>
 * </ul>
 */

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private Context context;

    private String currentUserId;
    public UserAdapter(List<User> users, Context context) {
        this.users = users;
        this.context = context;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = user != null ? user.getUid() : null;
    }

    // New constructor for testing
    public UserAdapter(List<User> users, Context context, String userId) {
        this.users = users;
        this.context = context;
        this.currentUserId = userId;
    }



    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }
    /**
     * Configures user items with profile images and click handlers
     * @param holder ViewHolder to populate
     * @param position User position in list
     *
     * Implements profile navigation from US 03.03.01
     */

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);

        holder.usernameTextView.setText(user.getUsername());


        // Load profile image if available
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.add_profile)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.add_profile);
        }

        // Set click listener to open user profile
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OtherUserProfileActivity.class);
            intent.putExtra("userId", user.getId());
            context.startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            if (user.getId().equals(currentUserId)) {
                // Navigate to own profile
                Intent intent = new Intent(context, UserProfileActivity.class);
                context.startActivity(intent);
            } else {
                // Navigate to other user's profile
                Intent intent = new Intent(context, OtherUserProfileActivity.class);
                intent.putExtra("userId", user.getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView usernameTextView;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.userProfileImage);
            usernameTextView = itemView.findViewById(R.id.userUsername);
        }
    }

}
