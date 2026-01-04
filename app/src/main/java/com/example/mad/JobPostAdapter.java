package com.example.mad;

import android.content.Intent;
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
        
        // Add click listener to navigate to JobDetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), JobDetailActivity.class);
            // Convert job ID to string
            intent.putExtra("jobId", String.valueOf(item.getId()));
            intent.putExtra("jobTitle", item.getTitle());
            intent.putExtra("jobDescription", item.getDescription());
            intent.putExtra("recruiterId", item.getUserId());
            intent.putExtra("recruiterName", item.getRecruiterName());
            v.getContext().startActivity(intent);
        });
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
            // ✅ FIXED: Uncommented these lines to initialize the TextViews
            tvRecruiter = itemView.findViewById(R.id.tv_recruiter_name);
            tvCreatedAt = itemView.findViewById(R.id.tv_job_created_at);
        }
    }
}
