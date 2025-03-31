package com.example.feelink.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.feelink.R;
import com.example.feelink.controller.FirestoreManager;
import com.example.feelink.model.Notification;
import com.example.feelink.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 * Manages display and interaction with notifications (follow requests/comments). Handles
 * profile image caching and request resolution.
 * <p>
 * Key user stories:
 * <ul>
 *   <li>US 05.02.02 (Follow request handling)</li>
 *   <li>US 05.07.01 (Comment notification display)</li>
 * </ul>
 */

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notifications;
    private Context context;
    private FirebaseFirestore db;
    private Map<String, User> userCache = new HashMap<>();

    // Update the constructor to accept a Context
    public NotificationAdapter(Context context, List<Notification> notifications) {
        this.notifications = notifications;
        this.context = context; // Use the passed context
        this.db = FirebaseFirestore.getInstance();
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
    /**
     * Configures notification items with action buttons and user details
     * @param holder ViewHolder to populate
     * @param position Item position in list
     *
     * Implements: US 03.03.01 (Profile image resolution via Glide)
     */

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.message.setText(notification.getMessage());
        holder.profileImage.setImageResource(R.drawable.ic_nav_profile); // Use your default profile icon


        final String senderId = notification.getSenderId();
        holder.itemView.setTag(senderId);

        if (userCache.containsKey(senderId)) {
            User user = userCache.get(senderId);
            if (user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_nav_profile)
                        .into(holder.profileImage);
            }
        } else {
            new FirestoreManager(senderId).getUserInfo(senderId, new FirestoreManager.OnUserInfoListener() {
                @Override
                public void onSuccess(User user) {
                    userCache.put(senderId, user);
                    // Check if the view is still attached to the window.
                    if (holder.getAdapterPosition() != RecyclerView.NO_POSITION && holder.itemView.isAttachedToWindow()) {
                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            Glide.with(holder.itemView.getContext())
                                    .load(user.getProfileImageUrl())
                                    .placeholder(R.drawable.ic_nav_profile)
                                    .into(holder.profileImage);
                        }
                    }
                }
                @Override
                public void onFailure(String error) {}
            });
        }

        // Handle different notification types
        if (notification.getType() == Notification.Type.FOLLOW_REQUEST) {
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.denyButton.setVisibility(View.VISIBLE);

            holder.acceptButton.setOnClickListener(v -> handleFollowRequest(notification, true));
            holder.denyButton.setOnClickListener(v -> handleFollowRequest(notification, false));
        } else {
            holder.acceptButton.setVisibility(View.GONE);
            holder.denyButton.setVisibility(View.GONE);
        }

        // Inside the COMMENT type block in onBindViewHolder
        if (notification.getType() == Notification.Type.COMMENT) {
            holder.acceptButton.setVisibility(View.GONE);
            holder.denyButton.setVisibility(View.GONE);

            FirestoreManager firestoreManager = new FirestoreManager(notification.getSenderId());
            firestoreManager.getUsernameById(notification.getSenderId(), new FirestoreManager.OnUsernameListener() {
                @Override
                public void onSuccess(String username) {
                    Date date = new Date(notification.getTimestamp());
                    SimpleDateFormat sdf = new SimpleDateFormat("h:mm a - MMM dd, yyyy", Locale.getDefault());
                    String formattedDate = sdf.format(date);
                    holder.message.setText(username + " commented at " + formattedDate + "\n" + notification.getMessage());
                }

                @Override
                public void onFailure(String fallbackName) {
                    Date date = new Date(notification.getTimestamp());
                    SimpleDateFormat sdf = new SimpleDateFormat("h:mm a - MMM dd, yyyy", Locale.getDefault());
                    String formattedDate = sdf.format(date);
                    holder.message.setText(fallbackName + " commented at " + formattedDate + "\n" + notification.getMessage());
                }
            });
        }
    }

    private void handleFollowRequest(Notification notification, boolean accepted) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("follow_requests")
                .document(notification.getId()) // Make sure this is the correct document ID
                .update("status", accepted ? "accepted" : "denied")
                .addOnSuccessListener(aVoid -> {
                    if (accepted) {
                        addFollowerRelationship(notification.getSenderId());
                    }
                    removeNotification(notification);
                })
                .addOnFailureListener(e -> {
                    Log.e("Notification", "Error updating request", e);
                });
    }

    // Replace addFollowerRelationship()
    private void addFollowerRelationship(String senderId) {
        String receiverId = FirebaseAuth.getInstance().getUid(); // Current user (User B)

        // Get User B's username (receiver's username)
        FirestoreManager receiverManager = new FirestoreManager(receiverId);
        receiverManager.getUsernameById(receiverId, new FirestoreManager.OnUsernameListener() {
            @Override
            public void onSuccess(String receiverUsername) {
                // User A (sender) should follow User B (receiver)
                FirestoreManager senderManager = new FirestoreManager(senderId);
                senderManager.createFollowRelationship(
                        receiverId,  // Target user (User B's ID)
                        receiverUsername,  // User B's username
                        new FirestoreManager.OnFollowRequestListener() {
                            @Override
                            public void onSuccess() {
                                Log.d("Notification", "Follow relationship created");
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e("Notification", "Error: " + error);
                            }
                        }
                );
            }

            @Override
            public void onFailure(String fallbackId) {
                // Fallback to using receiverId as username
                FirestoreManager senderManager = new FirestoreManager(senderId);
                senderManager.createFollowRelationship(
                        receiverId,
                        receiverId, // Fallback to ID as username
                        new FirestoreManager.OnFollowRequestListener() {
                            @Override
                            public void onSuccess() {
                                Log.d("Notification", "Fallback relationship created");
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e("Notification", "Fallback error: " + error);
                            }
                        }
                );
            }
        });
    }

    private void removeNotification(Notification notification) {
        int position = notifications.indexOf(notification);
        notifications.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void clearNotifications() {
        this.notifications.clear();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        de.hdodenhof.circleimageview.CircleImageView profileImage;
        TextView message;
        Button acceptButton, denyButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            message = itemView.findViewById(R.id.message);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            denyButton = itemView.findViewById(R.id.denyButton);
        }
    }
}