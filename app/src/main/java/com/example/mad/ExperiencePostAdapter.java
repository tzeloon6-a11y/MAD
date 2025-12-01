package com.example.mad;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExperiencePostAdapter extends RecyclerView.Adapter<ExperiencePostAdapter.PostViewHolder> {

    private List<ExperiencePostItem> posts;

    public ExperiencePostAdapter(List<ExperiencePostItem> posts) {
        this.posts = posts;
    }

    public void updateData(List<ExperiencePostItem> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_experience_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        ExperiencePostItem item = posts.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvDate.setText(item.createdAt);
        holder.tvDescription.setText(item.description);

        // Reset visibility
        holder.ivImage.setVisibility(View.GONE);
        holder.vvVideo.setVisibility(View.GONE);

        if (item.mediaUrl != null && !item.mediaUrl.isEmpty()) {
            // Since ExperiencePostItem might not have a type field, we'll try to guess or default to Image
            // If you have a type field in ExperiencePostItem, use it here.
            try {
                Uri uri = Uri.parse(item.mediaUrl);
                // Basic check based on extension or just assume image for legacy items
                if (item.mediaUrl.contains(".mp4") || item.mediaUrl.contains("video")) {
                    holder.vvVideo.setVisibility(View.VISIBLE);
                    holder.vvVideo.setVideoURI(uri);
                    holder.vvVideo.seekTo(1);
                } else {
                    holder.ivImage.setVisibility(View.VISIBLE);
                    holder.ivImage.setImageURI(uri);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getItemCount() {
        return posts != null ? posts.size() : 0;
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvDescription;
        ImageView ivImage;
        VideoView vvVideo;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_post_title);
            tvDate = itemView.findViewById(R.id.tv_post_date);
            tvDescription = itemView.findViewById(R.id.tv_post_description);
            
            // Updated to use the new views in the layout
            ivImage = itemView.findViewById(R.id.iv_post_image);
            vvVideo = itemView.findViewById(R.id.vv_post_video);
        }
    }
}
