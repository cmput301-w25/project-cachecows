package com.example.feelink;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feelink.FollowRequest;
import com.example.feelink.NotificationsActivity;
import com.example.feelink.R;

import java.util.List;

public class FollowRequestsAdapter extends RecyclerView.Adapter<FollowRequestsAdapter.ViewHolder> {

    private final List<FollowRequest> followRequests;
    private final NotificationsActivity parentActivity;

    public FollowRequestsAdapter(List<FollowRequest> followRequests, NotificationsActivity context) {
        this.followRequests = followRequests;
        this.parentActivity = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_follow_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FollowRequest request = followRequests.get(position);
        holder.tvRequestText.setText(request.getRequestText());
        holder.tvTime.setText(request.getTimestamp());

        holder.itemView.setOnClickListener(v -> {
            if(position != RecyclerView.NO_POSITION) {
                parentActivity.showRequestDetail(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return followRequests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvRequestText;
        public final TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRequestText = itemView.findViewById(R.id.tvRequestText);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}