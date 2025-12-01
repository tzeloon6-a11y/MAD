package com.example.mad;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExperienceAdapter extends RecyclerView.Adapter<ExperienceAdapter.ViewHolder> {

    private List<ExperiencePost> posts;

    public ExperienceAdapter(List<ExperiencePost> posts) {
        this.posts = posts;
    }

    public void updateData(List<ExperiencePost> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExperienceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_experience_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExperienceAdapter.ViewHolder holder, int position) {
        ExperiencePost post = posts.get(position);
        holder.tvTitle.setText(post.getTitle());
        holder.tvDescription.setText(post.getContent());
        holder.tvDate.setText(android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", post.getCreatedAt()));

        // Reset visibility
        holder.ivImage.setVisibility(View.GONE);
        holder.vvVideo.setVisibility(View.GONE);

        String uriStr = post.getMediaUri();
        String type = post.getMediaType();

        if (uriStr != null && type != null) {
            try {
                Uri uri = Uri.parse(uriStr);
                if ("image".equals(type)) {
                    holder.ivImage.setVisibility(View.VISIBLE);
                    holder.ivImage.setImageURI(uri);
                } else if ("video".equals(type)) {
                    holder.vvVideo.setVisibility(View.VISIBLE);
                    holder.vvVideo.setVideoURI(uri);
                    
                    // Add media controller for playback
                    MediaController mediaController = new MediaController(holder.itemView.getContext());
                    mediaController.setAnchorView(holder.vvVideo);
                    holder.vvVideo.setMediaController(mediaController);
                    
                    holder.vvVideo.seekTo(1); // show preview
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDate;
        ImageView ivImage;
        VideoView vvVideo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_post_title);
            tvDescription = itemView.findViewById(R.id.tv_post_description);
            tvDate = itemView.findViewById(R.id.tv_post_date);
            
            // Map to the new views in item_experience_post.xml
            ivImage = itemView.findViewById(R.id.iv_post_image);
            vvVideo = itemView.findViewById(R.id.vv_post_video);
        }
    }
}
