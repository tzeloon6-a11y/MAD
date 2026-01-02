package com.example.mad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class JobApplicantAdapter extends RecyclerView.Adapter<JobApplicantAdapter.ViewHolder> {

    // We store data as a Map (Key=Name/Email, Value=String)
    private List<Map<String, String>> applicantList;

    public JobApplicantAdapter(List<Map<String, String>> applicantList) {
        this.applicantList = applicantList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_job_applicant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> applicant = applicantList.get(position);

        // Safety check to handle potential null values
        String name = applicant.get("name");
        String email = applicant.get("email");
        String status = applicant.get("status");

        holder.tvName.setText(name != null ? name : "Unknown Student");
        holder.tvEmail.setText(email != null ? email : "-");

        if (holder.tvStatus != null) {
            holder.tvStatus.setText("Status: " + (status != null ? status : "Applied"));
        }
    }

    @Override
    public int getItemCount() { return applicantList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs match 'item_job_applicant.xml'
            tvName = itemView.findViewById(R.id.tvApplicantName);
            tvEmail = itemView.findViewById(R.id.tvApplicantEmail);
            tvStatus = itemView.findViewById(R.id.tvApplicationStatus);
        }
    }
}