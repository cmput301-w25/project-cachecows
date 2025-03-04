package com.example.feelink;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
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

    private final Map<String, Integer> moodColorMap = new HashMap<String, Integer>() {{
        put("Happy", R.color.mood_happy);
        put("Sad", R.color.mood_sad);
        put("Angry", R.color.mood_angry);
        put("Surprised", R.color.mood_surprised);
        put("Confused", R.color.mood_confused);
        put("Disgusted", R.color.mood_disgusted);
        put("Fear", R.color.mood_fear);
        put("Shame", R.color.mood_shame);
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

        int colorRes = moodColorMap.getOrDefault(moodEvent.getEmotionalState(), R.color.white);
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorRes));

        // Replace the color check with:
        int color = ContextCompat.getColor(context, colorRes);
        if (isDarkColor(color)) {
            holder.tvMoodDescription.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.tvMoodDescription.setTextColor(ContextCompat.getColor(context, R.color.black));
        }

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

    // Helper method to check color brightness
    // Change the parameter to use color integers
    private boolean isDarkColor(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
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
        ImageView moodImage;
        View btnLike;
        View btnComment;
        CardView cardView;

        public MoodEventViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize all views
            tvMoodDescription = itemView.findViewById(R.id.tvMoodDescription); // Critical fix
            moodImage = itemView.findViewById(R.id.ivMoodIcon); // Ensure this matches XML
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }

}