package com.example.mad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class JobPostAdapter extends RecyclerView.Adapter<JobPostAdapter.JobPostViewHolder> {

    private List<JobPostItem> jobPosts;

    public JobPostAdapter(List<JobPostItem> jobPosts) {
        this.jobPosts = jobPosts;
    }

    public void updateData(List<JobPostItem> newList) {
        this.jobPosts = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JobPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_job_post, parent, false);
        return new JobPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobPostViewHolder holder, int position) {
        JobPostItem item = jobPosts.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDesc.setText(item.getDescription());
        holder.tvRecruiter.setText("By: " + item.getRecruiterName());
        holder.tvCreatedAt.setText(item.getCreatedAt());
    }

    @Override
    public int getItemCount() {
        return jobPosts != null ? jobPosts.size() : 0;
    }

    static class JobPostViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvRecruiter, tvCreatedAt;

        public JobPostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_job_title);
            tvDesc = itemView.findViewById(R.id.tv_job_description);
            tvRecruiter = itemView.findViewById(R.id.tv_job_recruiter);
            tvCreatedAt = itemView.findViewById(R.id.tv_job_created_at);
        }
    }
}
