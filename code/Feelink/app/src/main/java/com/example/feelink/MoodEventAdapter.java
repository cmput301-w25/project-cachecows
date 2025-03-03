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
    // Map mood types to drawable resources (matching add_mood_event.xml)
    private final Map<String, Integer> moodIconMap = new HashMap<String, Integer>() {{
        put("Happy", R.drawable.ic_mood_happy);
        put("Sad", R.drawable.ic_mood_sad);
        put("Angry", R.drawable.ic_mood_angry);
        put("Surprised", R.drawable.ic_mood_surprised);
        put("Confused", R.drawable.ic_mood_confused);
        put("Disgusted", R.drawable.ic_mood_disgusted);
        put("Fear", R.drawable.ic_mood_fear);
        put("Shame", R.drawable.ic_mood_shame);
    }};

    private final Map<String, String> moodColorMap = new HashMap<String, String>() {{
        put("Happy", "#FFD700");
        put("Sad", "#1E90FF");
        put("Angry", "#DC143C");
        put("Surprised", "#FFA500");
        put("Confused", "#800080");
        put("Disgusted", "#556B2F");
        put("Fear", "#2F4F4F");
        put("Shame", "#CD5C5C");
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

        // Get mood icon from map
        Integer moodIcon = moodIconMap.get(moodEvent.getEmotionalState());

        if (moodIcon != null) {
            holder.moodImage.setVisibility(View.VISIBLE);
            holder.moodImage.setImageResource(moodIcon);
        } else {
            holder.moodImage.setVisibility(View.GONE);
        }

        // Handle like & comment actions
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
        ImageView moodImage;  // Updated to hold mood icon
        View btnLike;
        View btnComment;

        public MoodEventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMoodDescription = itemView.findViewById(R.id.tvMoodDescription);
            moodImage = itemView.findViewById(R.id.ivMoodIcon);  // Use ivMoodIcon
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
        }
    }

}