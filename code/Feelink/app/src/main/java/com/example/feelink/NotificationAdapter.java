package com.example.feelink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notifications;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        // Set common elements
        holder.message.setText(notification.getMessage());

        // Handle different notification types
        if (notification.getType() == Notification.Type.FOLLOW_REQUEST) {
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.denyButton.setVisibility(View.VISIBLE);

            // Placeholder click listeners
            holder.acceptButton.setOnClickListener(v -> {
                // Will implement later
            });

            holder.denyButton.setOnClickListener(v -> {
                // Will implement later
            });
        } else {
            holder.acceptButton.setVisibility(View.GONE);
            holder.denyButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView message;
        Button acceptButton, denyButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.message);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            denyButton = itemView.findViewById(R.id.denyButton);
        }
    }
}