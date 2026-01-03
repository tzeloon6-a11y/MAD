package com.example.mad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;
public class SavedJobAdapter extends RecyclerView.Adapter<SavedJobAdapter.ViewHolder> {
    private final List<Job> savedJobs;
    private OnButtonClickListener buttonClickListener;

    public interface OnButtonClickListener {
        void onInterestedClicked(int position);
        void onNotNowClicked(int position);
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        this.buttonClickListener = listener;
    }

    public SavedJobAdapter(List<Job> savedJobs) {
        this.savedJobs = savedJobs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_job_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Job job = savedJobs.get(position);
        holder.bind(job);

        holder.interestedButton.setOnClickListener(v -> {
            if (buttonClickListener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                buttonClickListener.onInterestedClicked(holder.getAdapterPosition());
            }
        });

        holder.notNowButton.setOnClickListener(v -> {
            if (buttonClickListener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                buttonClickListener.onNotNowClicked(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return savedJobs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView jobTitle, companyName, wage, location;
        MaterialButton interestedButton, notNowButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            jobTitle = itemView.findViewById(R.id.job_title);
            companyName = itemView.findViewById(R.id.company_name);
            wage = itemView.findViewById(R.id.wage);
            location = itemView.findViewById(R.id.location);
            interestedButton = itemView.findViewById(R.id.interested_button);
            notNowButton = itemView.findViewById(R.id.not_now_button);
        }

        void bind(Job job) {
            jobTitle.setText(job.getTitle());
            companyName.setText(job.getCompanyName());
            wage.setText(job.getWage());
            location.setText(job.getLocation());
        }
    }
}

