package com.example.feelink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feelink.CommentLike;

import java.util.List;

public class CommentsLikesAdapter extends RecyclerView.Adapter<CommentsLikesAdapter.ViewHolder> {

    private final List<CommentLike> commentsLikes;

    public CommentsLikesAdapter(List<CommentLike> commentsLikes) {
        this.commentsLikes = commentsLikes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment_like, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommentLike item = commentsLikes.get(position);
        holder.tvActionText.setText(item.getActionText());
        holder.tvTime.setText(item.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return commentsLikes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvActionText;
        public final TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvActionText = itemView.findViewById(R.id.tvActionText);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}