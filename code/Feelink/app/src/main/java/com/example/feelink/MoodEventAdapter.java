package com.example.feelink;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoodEventAdapter extends RecyclerView.Adapter<MoodEventAdapter.MoodEventViewHolder> {

    private List<MoodEvent> moodEvents;
    private Context context;

    // Map mood types to emoji icons
    private final Map<String, String> moodEmojiMap = new HashMap<String, String>() {{
        put("Happy", "😊");
        put("Sad", "😢");
        put("Angry", "😠");
        put("Surprised", "😲");
        put("Confused", "😕");
        put("Disgusted", "🤢");
        put("Fear", "😨");
        put("Shame", "😳");
    }};

    public MoodEventAdapter(List<MoodEvent> moodEvents, Context context) {
        this.moodEvents = moodEvents;
        this.context = context;
    }

    @NonNull
    @Override
    public MoodEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mood_event, parent, false);
        return new MoodEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodEventViewHolder holder, int position) {
        MoodEvent moodEvent = moodEvents.get(position);

        // Set reason/description
        holder.tvMoodDescription.setText(moodEvent.getReason());

        // Set emoji based on mood type
        String emoji = moodEmojiMap.getOrDefault(moodEvent.getEmotionalState(), "😐");
        holder.tvMoodEmoji.setText(emoji);

        // Handle photo if available (not implemented in current MoodEvent class)
        // If you add photo support later, uncomment this
        /*
        if (moodEvent.hasPhoto()) {
            holder.moodImage.setVisibility(View.VISIBLE);
            // Load photo using your preferred image loading library
            // Glide.with(context).load(moodEvent.getPhotoUrl()).into(holder.moodImage);
        } else {
            holder.moodImage.setVisibility(View.GONE);
        }
        */

        // Set up click listeners for reactions
        holder.btnLike.setOnClickListener(v -> {
            // Handle like action
        });

        holder.btnComment.setOnClickListener(v -> {
            // Handle comment action
        });
    }

    @Override
    public int getItemCount() {
        return moodEvents != null ? moodEvents.size() : 0;
    }

    public void updateMoodEvents(List<MoodEvent> moodEvents) {
        this.moodEvents = moodEvents;
        notifyDataSetChanged();
    }

    static class MoodEventViewHolder extends RecyclerView.ViewHolder {
        TextView tvMoodDescription;
        TextView tvMoodEmoji;
        ImageView moodImage;
        View btnLike;
        View btnComment;

        public MoodEventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMoodDescription = itemView.findViewById(R.id.tvMoodDescription);
            tvMoodEmoji = itemView.findViewById(R.id.tvMoodEmoji);
            moodImage = itemView.findViewById(R.id.moodImage);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
        }
    }
}