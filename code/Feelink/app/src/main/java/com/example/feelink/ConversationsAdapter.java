package com.example.feelink;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder> {
    private List<Conversation> conversations;
    private OnConversationClickListener listener;

    public ConversationsAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        holder.bind(conversation);
        String otherUserId = getOtherUserId(conversation.getParticipants());

        // Fetch username from Firestore
        FirestoreManager firestoreManager = new FirestoreManager(FirebaseAuth.getInstance().getCurrentUser().getUid());
        firestoreManager.getUserInfo(otherUserId, new FirestoreManager.OnUserInfoListener() {
            @Override
            public void onSuccess(User user) {
                holder.tvUsername.setText(user.getUsername());

                String imageUrl = user.getProfileImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_nav_profile)
                            .into(holder.profileImage);
                } else {
                    holder.profileImage.setImageResource(R.drawable.ic_nav_profile);
                }
            }

            @Override
            public void onFailure(String error) {
                holder.tvUsername.setText("Unknown User");
                holder.profileImage.setImageResource(R.drawable.ic_nav_profile);
            }
        });
        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
    }

    private String getOtherUserId(List<String> participantIds) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return participantIds.get(0).equals(currentUserId) ?
                participantIds.get(1) : participantIds.get(0);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvLastMessage, tvTimestamp;
        CircleImageView profileImage;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.username);
            tvLastMessage = itemView.findViewById(R.id.lastMessage);
            tvTimestamp = itemView.findViewById(R.id.timestamp);
            profileImage = itemView.findViewById(R.id.profileImage);
        }

        public void bind(Conversation conversation) {
            tvLastMessage.setText(conversation.getLastMessage());
            tvTimestamp.setText(DateUtils.formatDateTime(itemView.getContext(),
                    conversation.getTimestamp().getTime(), DateUtils.FORMAT_SHOW_TIME));
        }
    }

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }
}