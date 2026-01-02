package com.example.mad;

import android.content.Context;
import android.content.Intent;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recruiter_job, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JobModel job = jobList.get(position);

        // Safety check to prevent crash if view is missing
        if (holder.tvTitle != null) {
            holder.tvTitle.setText(job.getTitle());
        }

        if (holder.tvDesc != null) {
            holder.tvDesc.setText(job.getDescription());
        }

        holder.tvViewApplicants.setOnClickListener(v -> {
            Context context = v.getContext();

            // Create the Intent to go to the ViewApplicantsActivity
            Intent intent = new Intent(context, ViewApplicantsActivity.class);

            // Pass the data so the next screen knows WHICH job to load
            intent.putExtra("job_id", job.getId());
            intent.putExtra("job_title", job.getTitle());

            // Launch the screen
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() { return jobList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvViewApplicants;
        TextView tvTitle, tvDesc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvJobTitle);
            tvDesc = itemView.findViewById(R.id.tvJobDescription);
            tvViewApplicants = itemView.findViewById(R.id.tvViewApplicants);        }
    }
}