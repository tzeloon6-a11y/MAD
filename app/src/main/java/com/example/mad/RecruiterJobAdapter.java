package com.example.mad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecruiterJobAdapter extends RecyclerView.Adapter<RecruiterJobAdapter.ViewHolder> {

    private List<JobModel> jobList;

    public RecruiterJobAdapter(List<JobModel> jobList) {
        this.jobList = jobList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JobModel job = jobList.get(position);
        holder.tvTitle.setText(job.getTitle());
        holder.tvDesc.setText(job.getDescription());
    }

    @Override
    public int getItemCount() { return jobList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvJobTitle);
            tvDesc = itemView.findViewById(R.id.tvJobDesc);
        }
    }
}