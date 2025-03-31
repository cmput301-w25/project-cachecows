package com.example.feelink.adapter;

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

import java.util.List;
/**
 * Displays user search results with profiles. Enables navigation to user profiles.
 * <p>
 * Directly implements:
 * <ul>
 *   <li>US 03.02.01 (User search functionality)</li>
 *   <li>US 03.03.01 (Profile previews in search results)</li>
 * </ul>
 */

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {
    private List<User> searchResults;
    private OnUserClickListener listener;

    public SearchAdapter(List<User> searchResults, OnUserClickListener listener) {
        this.searchResults = searchResults;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new SearchViewHolder(view);
    }
    /**
     * Binds user data to search result items
     * @param holder ViewHolder to configure
     * @param position Result position in list
     *
     * Uses Glide for US 03.03.01 profile image display
     */

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        User user = searchResults.get(position);
        holder.usernameTextView.setText(user.getUsername()); // Use getUsername()

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl()) // Use getProfileImageUrl()
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_nav_profile);
        }

        // Set click listener for the item
        holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView usernameTextView;

        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImage);
            usernameTextView = itemView.findViewById(R.id.username);
        }
    }

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

}