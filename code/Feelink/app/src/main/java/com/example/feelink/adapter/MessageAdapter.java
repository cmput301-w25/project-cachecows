package com.example.feelink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feelink.R;
import com.example.feelink.model.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/**
 * Displays chat messages with sender differentiation and timestamps. Part of the app's direct
 * messaging "Wow" factor.
 * <p>
 * Implements:
 * <ul>
 *   <li>Message bubbles styling for sent/received messages</li>
 *   <li>Timestamp formatting for recent interactions (US 05.03.01)</li>
 * </ul>
 */


public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    public List<Message> messages;
    private String currentUserId;

    /**
     * Initializes adapter with message history and current user context
     * @param messages List of chat messages to display
     * @param currentUserId Authenticated user's ID for message alignment
     */

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    /**
     * Updates message list and refreshes UI
     * @param newMessages Updated list of messages
     *
     * Enables real-time chat updates for US 05.03.01 (Recent social interactions)
     */

    public void updateMessages(List<Message> newMessages) {
        messages = newMessages != null ? newMessages : new ArrayList<>();
        notifyDataSetChanged();
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.tvMessage.setText(message.getText());

        if (message.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String formattedTime = sdf.format(message.getTimestamp());
            holder.tvTime.setText(formattedTime);
        } else {
            holder.tvTime.setText("--:--"); // Fallback text
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTime;
        public ViewHolder(View view) {
            super(view);
            tvMessage = view.findViewById(R.id.tvMessage);
            tvTime = view.findViewById(R.id.tvTime);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isSent(currentUserId) ? 0 : 1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = viewType == 0 ? R.layout.item_message_sent : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }
}