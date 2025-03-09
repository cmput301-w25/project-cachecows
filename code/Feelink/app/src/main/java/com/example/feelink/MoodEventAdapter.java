package com.example.feelink;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoodEventAdapter extends RecyclerView.Adapter<MoodEventAdapter.MoodEventViewHolder> {

    private List<MoodEvent> moodEvents;
    private Context context;

    private boolean isMyMoodSection = false;

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


    public void setMyMoodSection(boolean isMyMood) {
        this.isMyMoodSection = isMyMood;
    }


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
        holder.btnExpand.setOnClickListener(v -> {
            showDetailsDialog(moodEvent);
        });
        // Handle delete button click
        holder.btnDelete.setOnClickListener(v -> {
            showDeleteConfirmationDialog(moodEvent);
        });

        if (isMyMoodSection) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Handle edit button click
        holder.btnEdit.setOnClickListener(v -> {
            // Navigate to AddMoodEventActivity with the mood event's data
            Intent intent = new Intent(context, AddMoodEventActivity.class);
            intent.putExtra("EDIT_MODE", true);
            intent.putExtra("MOOD_EVENT_ID", moodEvent.getId());
            intent.putExtra("DOCUMENT_ID", moodEvent.getDocumentId());
            intent.putExtra("EMOTIONAL_STATE", moodEvent.getEmotionalState());
            intent.putExtra("REASON", moodEvent.getReason());
            intent.putExtra("TRIGGER", moodEvent.getTrigger());
            intent.putExtra("SOCIAL_SITUATION", moodEvent.getSocialSituation());
            intent.putExtra("IMAGE_URL", moodEvent.getImageUrl());
            context.startActivity(intent);
        });

        String imageUrl = moodEvent.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()){
            holder.photoContainer.setVisibility(View.VISIBLE);
            holder.moodPostedImage.setVisibility(View.VISIBLE);
            holder.tvPhotoPlaceholder.setVisibility(View.GONE);

            Glide.with(context)
                    .load(imageUrl)
                    .fitCenter()
                    .into(holder.moodPostedImage);
        } else {
            //No image
            holder.photoContainer.setVisibility(View.GONE);
        }
    }

    private void showDeleteConfirmationDialog(MoodEvent moodEvent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Mood Event");
        builder.setMessage("Are you sure you want to delete this mood event?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // User confirmed, delete the mood event
            deleteMoodEvent(moodEvent);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // User cancelled, do nothing
            dialog.dismiss();
        });
        builder.show();
    }

    private void deleteMoodEvent(MoodEvent moodEvent) {
        FirestoreManager firestoreManager = new FirestoreManager(moodEvent.getUserId());
        firestoreManager.deleteMoodEvent(moodEvent.getId(), new FirestoreManager.OnDeleteListener() {
            @Override
            public void onSuccess() {
                // Remove the mood event from the list and notify the adapter
                moodEvents.remove(moodEvent);
                notifyDataSetChanged();
                Toast.makeText(context, "Mood event deleted successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(context, "Failed to delete mood event: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void showDetailsDialog(MoodEvent moodEvent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_mood_details, null);
        builder.setView(dialogView);

        // Get references to all views
        TextView tvEmotionalState = dialogView.findViewById(R.id.tvEmotionalState);
        TextView tvTimestamp = dialogView.findViewById(R.id.tvTimestamp);
        TextView tvUsername = dialogView.findViewById(R.id.tvUsername); // New username TextView
        TextView tvLocation = dialogView.findViewById(R.id.tvLocation);
        TextView tvTrigger = dialogView.findViewById(R.id.tvTrigger);
        TextView tvContent = dialogView.findViewById(R.id.tvContent);
        ImageView ivMoodIcon = dialogView.findViewById(R.id.ivMoodIcon);
        ImageView ivProfilePic = dialogView.findViewById(R.id.ivProfilePic);
        ImageView btnBack = dialogView.findViewById(R.id.btnBack);
        View cardViewBackground = dialogView.findViewById(R.id.cardViewBackground);
        View photoPlaceholder = dialogView.findViewById(R.id.photoPlaceholder);

        // Set background color based on mood
        String emotionalState = moodEvent.getEmotionalState();
        int colorRes = moodColorMap.getOrDefault(emotionalState, R.color.white);
        cardViewBackground.setBackgroundColor(ContextCompat.getColor(context, colorRes));

        // Set mood icon
        Integer moodIcon = moodIconMap.get(emotionalState);
        if (moodIcon != null) {
            ivMoodIcon.setVisibility(View.VISIBLE);
            ivMoodIcon.setImageResource(moodIcon);
        } else {
            ivMoodIcon.setVisibility(View.GONE);
        }

        // Set emotional state text
        tvEmotionalState.setText(emotionalState + "!");

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault());
        String formattedDate = sdf.format(moodEvent.getTimestamp());
        tvTimestamp.setText(formattedDate);

        // Get and set username
        String userId = moodEvent.getUserId();
        FirestoreManager firestoreManager = new FirestoreManager(userId); // Create temporary instance for query

        // Create and show dialog first so we can update it later when the username is fetched
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Fetch username from Firestore
        firestoreManager.getUsernameById(userId, new FirestoreManager.OnUsernameListener() {
            @Override
            public void onSuccess(String username) {
                tvUsername.setText(username);
            }

            @Override
            public void onFailure(String fallbackName) {
                // Use userId as fallback
                tvUsername.setText(fallbackName);
            }
        });

        // Set location (if available, otherwise hide)
        if (moodEvent.getSocialSituation() != null && !moodEvent.getSocialSituation().isEmpty()) {
            tvLocation.setText(moodEvent.getSocialSituation());
            tvLocation.setVisibility(View.VISIBLE);
        } else {
            tvLocation.setText("None");
        }

        // Set trigger (if available)
        if (moodEvent.getTrigger() != null && !moodEvent.getTrigger().isEmpty()) {
            tvTrigger.setText(moodEvent.getTrigger());
        } else {
            tvTrigger.setText("None");
        }

        // Set the content/reason
        tvContent.setText(moodEvent.getReason());

        // Set profile picture placeholder
        ivProfilePic.setImageResource(R.drawable.ic_nav_profile);

        // Show or hide photo placeholder based on whether a photo exists
        photoPlaceholder.setVisibility(View.VISIBLE);

        // Setup back button
        btnBack.setOnClickListener(v -> dialog.dismiss());

        // Show the dialog
        dialog.show();
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
        ConstraintLayout photoContainer;
        TextView tvMoodDescription, tvPhotoPlaceholder;
        ImageView moodImage, moodPostedImage;
        View btnLike;
        View btnComment;
        CardView cardView;
        View btnExpand;

        ImageButton btnEdit, btnDelete;


        public MoodEventViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize all views
            tvMoodDescription = itemView.findViewById(R.id.tvMoodDescription); // Critical fix
            moodImage = itemView.findViewById(R.id.ivMoodIcon); // Ensure this matches XML
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            cardView = itemView.findViewById(R.id.cardView);
            btnExpand = itemView.findViewById(R.id.btnExpand);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            photoContainer = itemView.findViewById(R.id.photoContainer);
            moodPostedImage = itemView.findViewById(R.id.moodImage);
            tvPhotoPlaceholder = itemView.findViewById(R.id.tvPhotoPlaceholder);
        }
    }

}