package com.example.feelink;

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

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private Context context;

    public UserAdapter(List<User> users, Context context) {
        this.users = users;
        this.context = context;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);

        holder.usernameTextView.setText(user.getUsername());

        if (user.getBio() != null && !user.getBio().isEmpty()) {
            holder.bioTextView.setText(user.getBio());
            holder.bioTextView.setVisibility(View.VISIBLE);
        } else {
            holder.bioTextView.setVisibility(View.GONE);
        }

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
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView usernameTextView;
        TextView bioTextView;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.userProfileImage);
            usernameTextView = itemView.findViewById(R.id.userUsername);
            bioTextView = itemView.findViewById(R.id.userBio);
        }
    }

}
