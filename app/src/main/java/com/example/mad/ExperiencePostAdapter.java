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

public class ExperiencePostAdapter
        extends RecyclerView.Adapter<ExperiencePostAdapter.PostViewHolder> {

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

        // ✅ Safe text binding
        holder.tvTitle.setText(item.getTitle());
        holder.tvDate.setText(item.getCreatedAt());
        holder.tvDescription.setText(item.getDescription());

        // ✅ Reset views (VERY IMPORTANT for RecyclerView)
        holder.ivImage.setVisibility(View.GONE);
        holder.vvVideo.setVisibility(View.GONE);
        holder.vvVideo.stopPlayback();

        String mediaUrl = item.getMediaUrl();
        String mediaType = item.getMediaType(); // ✅ image / video

        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            try {
                Uri uri = Uri.parse(mediaUrl);

                if ("video".equalsIgnoreCase(mediaType)) {
                    holder.vvVideo.setVisibility(View.VISIBLE);
                    holder.vvVideo.setVideoURI(uri);
                    holder.vvVideo.seekTo(1); // ✅ preview frame only
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

    // ✅ ViewHolder
    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvDate, tvDescription;
        ImageView ivImage;
        VideoView vvVideo;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tv_post_title);
            tvDate = itemView.findViewById(R.id.tv_post_date);
            tvDescription = itemView.findViewById(R.id.tv_post_description);
            ivImage = itemView.findViewById(R.id.iv_post_image);
            vvVideo = itemView.findViewById(R.id.vv_post_video);
        }
    }
}
