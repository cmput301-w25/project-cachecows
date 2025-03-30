package com.example.feelink;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RecyclerView adapter for displaying mood events with visual customization and CRUD operations
 *
 * <h3>User Stories Implemented:</h3>
 * <ul>
 *   <li>US 1.03.01.02 - Integrated centralized assets into UI views</li>
 *   <li>US 1.04.01.01 - Mood event detail UI implementation</li>
 *   <li>US 1.05.01.01 - Edit interface integration</li>
 *   <li>US 1.06.01.01 - Delete UI with confirmation dialog</li>
 *   <li>US 02.02.01.03 - Photograph display integration</li>
 * </ul>
 *
 * @see MoodEvent
 * @see FirestoreManager
 */
public class MoodEventAdapter extends RecyclerView.Adapter<MoodEventAdapter.MoodEventViewHolder> {

    private List<MoodEvent> moodEvents;
    private Context context;
    private Map<String, User> userCache = new HashMap<>();

    private boolean isMyMoodSection = false;
    private boolean isPublicFeed = false;


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

    /**
     * Toggles UI controls for personal vs shared mood events
     * @param isMyMood True to show edit/delete buttons
     */
    public void setMyMoodSection(boolean isMyMood) {
        this.isMyMoodSection = isMyMood;
    }

    /**
     * Initializes adapter with mood event data and context
     * @param moodEvents List of MoodEvent objects to display
     * @param context Hosting activity context
     */
    public MoodEventAdapter(List<MoodEvent> moodEvents, Context context) {
        this.moodEvents = moodEvents;
        this.context = context;
    }

    /**
     * Creates view holder instances for RecyclerView
     * @param parent ViewGroup container
     * @param viewType View type identifier
     * @return Configured ViewHolder instance
     */
    @NonNull
    @Override
    public MoodEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mood_event, parent, false);
        return new MoodEventViewHolder(view);
    }

    /**
     * Binds mood event data to view holder components
     * <p>Handles:
     * <ul>
     *   <li>Color theming based on emotional state</li>
     *   <li>Image loading with Glide</li>
     *   <li>Edit/delete button visibility</li>
     * </ul>
     *
     * @param holder ViewHolder instance
     * @param position Item position in dataset
     */
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

        // Modify the comment button click listener in onBindViewHolder
        holder.btnComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("MOOD_EVENT_ID", moodEvent.getDocumentId());
            intent.putExtra("MOOD_EVENT_OWNER_ID", moodEvent.getUserId()); // Add this line
            context.startActivity(intent);
        });
        holder.cardView.setOnClickListener(v -> showDetailsDialog(moodEvent));

        // Handle delete button click
        holder.btnDelete.setOnClickListener(v -> {
            showDeleteConfirmationDialog(moodEvent);
        });

        int socialVisibility = isPublicFeed ? View.GONE : View.VISIBLE;
        holder.btnComment.setVisibility(socialVisibility);

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
            intent.putExtra("SOCIAL_SITUATION", moodEvent.getSocialSituation());
            intent.putExtra("IMAGE_URL", moodEvent.getImageUrl());
            intent.putExtra("IS_PUBLIC", moodEvent.isPublic());
            intent.putExtra("LOCATION_NAME", moodEvent.getLocationName());
            intent.putExtra("LATITUDE", moodEvent.getLatitude());
            intent.putExtra("LONGITUDE", moodEvent.getLongitude());
            context.startActivity(intent);
        });

        String imageUrl = moodEvent.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            holder.photoContainer.setVisibility(View.VISIBLE);
            holder.moodPostedImage.setVisibility(View.VISIBLE);
            holder.tvPhotoPlaceholder.setVisibility(View.GONE);

            Glide.with(context)
                    .load(imageUrl)
                    .fitCenter()
                    .into(holder.moodPostedImage);
        } else {
            String localPath = moodEvent.getTempLocalImagePath();
            if (localPath != null && !localPath.isEmpty()) {
                holder.photoContainer.setVisibility(View.VISIBLE);
                holder.moodPostedImage.setVisibility(View.VISIBLE);
                holder.tvPhotoPlaceholder.setVisibility(View.GONE);

                Glide.with(context)
                        .load(new File(localPath))
                        .fitCenter()
                        .into(holder.moodPostedImage);
            } else {
                //No image
                holder.photoContainer.setVisibility(View.GONE);
            }
        }


        String postUserId = moodEvent.getUserId();
        if (moodEvent.getUsername() != null && !moodEvent.getUsername().isEmpty()
                && moodEvent.getUserProfileImageUrl() != null) {
            holder.userUsername.setText(moodEvent.getUsername());
            Glide.with(context)
                    .load(moodEvent.getUserProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(holder.userProfileImage);
        } else {
            // Check cache first
            if (userCache.containsKey(postUserId)) {
                User user = userCache.get(postUserId);
                moodEvent.setUsername(user.getUsername());
                moodEvent.setUserProfileImageUrl(user.getProfileImageUrl());
                holder.userUsername.setText(user.getUsername());
                Glide.with(context)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(holder.userProfileImage);
            } else {
                // Fetch user info from Firestore
                new FirestoreManager(postUserId).getUserInfo(postUserId, new FirestoreManager.OnUserInfoListener() {
                    @Override
                    public void onSuccess(User user) {
                        // Cache the result
                        userCache.put(postUserId, user);
                        moodEvent.setUsername(user.getUsername());
                        moodEvent.setUserProfileImageUrl(user.getProfileImageUrl());
                        holder.userUsername.setText(user.getUsername());
                        Glide.with(context)
                                .load(user.getProfileImageUrl())
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .into(holder.userProfileImage);
                    }

                    @Override
                    public void onFailure(String error) {
                        holder.userUsername.setText("Unknown");
                        holder.userProfileImage.setImageResource(R.drawable.ic_profile_placeholder);
                    }
                });
            }
        }

        PendingSyncManager pendingSyncManager = new PendingSyncManager(context);
        if (pendingSyncManager.getPendingIds().contains(moodEvent.getDocumentId())) {
            holder.lottieSync.setVisibility(View.VISIBLE);
            holder.lottieSync.setAnimation("loading2.json");
            if (!holder.lottieSync.isAnimating()) {
                holder.lottieSync.playAnimation();
            }
        } else {
            holder.lottieSync.cancelAnimation();
            holder.lottieSync.setVisibility(View.GONE);
        }
    }

    /**
     * Displays confirmation dialog before deleting mood event
     *
     * <p>Implements US 1.06.01.01 requirements for delete confirmation UI.
     * Uses native AlertDialog with positive/negative action buttons.</p>
     *
     * @param moodEvent MoodEvent to potentially delete
     *
     * @see #deleteMoodEvent(MoodEvent)
     */
    private void showDeleteConfirmationDialog(MoodEvent moodEvent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Mood?");
        builder.setMessage("By Deleting this post, you won't be able to access it.?");
        builder.setPositiveButton("Yes, Delete", (dialog, which) -> {
            // User confirmed, delete the mood event
            deleteMoodEvent(moodEvent);
        });
        builder.setNegativeButton("No, Cancel", (dialog, which) -> {
            // User cancelled, do nothing
            dialog.dismiss();
        });
        builder.show();
    }

    /**
     * Handles mood event deletion with Firestore integration
     * @param moodEvent MoodEvent to delete
     */
    private void deleteMoodEvent(MoodEvent moodEvent) {
        FirestoreManager firestoreManager = new FirestoreManager(moodEvent.getUserId());

        if (!ConnectivityReceiver.isNetworkAvailable(context)) {
            // Offline: update UI immediately.
            moodEvents.remove(moodEvent);
            notifyDataSetChanged();
            Toast.makeText(context, "You are offline. Your changes have been saved locally!", Toast.LENGTH_SHORT).show();

            // Still call delete so Firestore queues it for when connectivity returns.
            firestoreManager.deleteMoodEvent(moodEvent.getId(), new FirestoreManager.OnDeleteListener() {
                @Override
                public void onSuccess() {}
                @Override
                public void onFailure(String errorMessage) {}
            });
        } else {
            // Online
            firestoreManager.deleteMoodEvent(moodEvent.getId(), new FirestoreManager.OnDeleteListener() {
                @Override
                public void onSuccess() {
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
    }

    /**
     * Displays detailed view dialog for mood event
     * @param moodEvent MoodEvent object to display
     *
     */
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
        TextView tvContent = dialogView.findViewById(R.id.tvContent);
        ImageView ivMoodIcon = dialogView.findViewById(R.id.ivMoodIcon);
        ImageView ivProfilePic = dialogView.findViewById(R.id.ivProfilePic);
        ImageView btnBack = dialogView.findViewById(R.id.btnBack);
        View cardViewBackground = dialogView.findViewById(R.id.cardViewBackground);
        // Add these in the "Get references to all views" section:
        ImageView ivMoodPhoto = dialogView.findViewById(R.id.ivMoodPhoto);
        TextView tvPhotoPlaceholderText = dialogView.findViewById(R.id.tvPhotoPlaceholderText);

        // Replace the existing photo handling code with:
        String imageUrl = moodEvent.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            ivMoodPhoto.setVisibility(View.VISIBLE);
            tvPhotoPlaceholderText.setVisibility(View.GONE);
            Glide.with(context)
                    .load(imageUrl)
                    .fitCenter()
                    .into(ivMoodPhoto);
        } else {
            ivMoodPhoto.setVisibility(View.GONE);
            tvPhotoPlaceholderText.setVisibility(View.VISIBLE);
        }

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

        // Add location name display
        String locationName = moodEvent.getLocationName();
        if (locationName != null && !locationName.isEmpty()) {
            TextView tvLocationName = dialogView.findViewById(R.id.tvLocationName);
            if (tvLocationName != null) {
                tvLocationName.setText(locationName);
                tvLocationName.setVisibility(View.VISIBLE);
            }
        }

        // Set the content/reason
        tvContent.setText(moodEvent.getReason());

        // Set profile picture placeholder
        ivProfilePic.setImageResource(R.drawable.ic_nav_profile);

        // Show or hide photo placeholder based on whether a photo exists


        // Setup back button
        btnBack.setOnClickListener(v -> dialog.dismiss());

        // Show the dialog
        dialog.show();
    }

    /**
     * Determines text color contrast based on background luminance
     * @param color Background color in integer format
     * @return True if dark text should be used
     *
     * @see <a href="https://www.w3.org/TR/AERT/#color-contrast">WCAG Contrast Guidelines</a>
     */
    private boolean isDarkColor(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    /**
     * Returns total number of mood events in the adapter
     *
     * <p>Implements safe null-checking to prevent RecyclerView crashes.
     * Required for proper RecyclerView item recycling.</p>
     *
     * @return Number of mood events (0 if null)
     */
    @Override
    public int getItemCount() {
        return moodEvents != null ? moodEvents.size() : 0;
    }

    /**
     * Updates dataset and refreshes UI
     * @param moodEvents New list of MoodEvent objects
     */
    public void updateMoodEvents(List<MoodEvent> moodEvents) {
        this.moodEvents = moodEvents;
        notifyDataSetChanged();
    }
    public int findPositionById(String documentId) {
        if (moodEvents == null) return -1;
        for (int i = 0; i < moodEvents.size(); i++) {
            if (moodEvents.get(i).getDocumentId().equals(documentId)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isPublicFeed() {
        return isPublicFeed;
    }

    public void setPublicFeed(boolean isPublicFeed) {
        this.isPublicFeed = isPublicFeed;
    }

    /**
     * @return The current list of mood events (filtered or unfiltered)
     */
    public List<MoodEvent> getCurrentMoodEvents() {
        return moodEvents;
    }

    /**
     * Returns the current list of mood events in the adapter
     * @return List<MoodEvent> The current list of mood events
     */
    public List<MoodEvent> getMoodEvents() {
        return moodEvents;
    }

    /**
     * ViewHolder implementation for mood event items
     *
     * <p>Manages view references and click handlers for:</p>
     * <ul>
     *   <li>Expand/collapse functionality</li>
     *   <li>Social interaction buttons</li>
     *   <li>Edit/delete operations</li>
     * </ul>
     */
    static class MoodEventViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout photoContainer;
        TextView tvMoodDescription, tvPhotoPlaceholder, userUsername;
        ImageView moodImage, moodPostedImage;
        View btnComment;
        CardView cardView;

        de.hdodenhof.circleimageview.CircleImageView userProfileImage;
        ImageButton btnEdit, btnDelete;

        LottieAnimationView lottieSync; // UI element for offline behavior

        /**
         * Initializes view references and click handlers
         * @param itemView Root view of item layout
         */

        public MoodEventViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize all views
            tvMoodDescription = itemView.findViewById(R.id.tvMoodDescription); // Critical fix
            moodImage = itemView.findViewById(R.id.ivMoodIcon); // Ensure this matches XML
            btnComment = itemView.findViewById(R.id.btnComment);
            cardView = itemView.findViewById(R.id.cardView);

            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            photoContainer = itemView.findViewById(R.id.photoContainer);
            moodPostedImage = itemView.findViewById(R.id.moodImage);
            tvPhotoPlaceholder = itemView.findViewById(R.id.tvPhotoPlaceholder);
            lottieSync = itemView.findViewById(R.id.lottieSync);
            userProfileImage = itemView.findViewById(R.id.userProfileImage);
            userUsername = itemView.findViewById(R.id.userUsername);
        }
    }

}