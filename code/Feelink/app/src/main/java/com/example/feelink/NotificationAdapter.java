package com.example.feelink;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notifications;
    private FirebaseFirestore db;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        // Load sender's profile picture using Glide
//        Glide.with(holder.itemView.getContext())
//                .load(notification.getSenderProfileUrl())
//                .into(holder.profileImage);

        // Set common elements
        holder.message.setText(notification.getMessage());
        holder.profileImage.setImageResource(R.drawable.ic_nav_profile); // Use your default profile icon



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