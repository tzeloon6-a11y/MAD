package com.example.campuslink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class CardStackAdapter extends RecyclerView.Adapter<CardStackAdapter.ViewHolder> {

    private final List<Job> items;
    private OnButtonClickListener buttonClickListener;

    /**
     * Interface to handle button clicks on the card.
     */
    public interface OnButtonClickListener {
        void onNotNowClicked();
        void onInterestedClicked();
        void onSaveClicked(); // For the new "Save for Later" button
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        this.buttonClickListener = listener;
    }

    public CardStackAdapter(List<Job> items) {
        this.items = items;
    }

    public List<Job> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_job_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setData(items.get(position));

        // Listener for the "Not Now" button
        holder.notNowButton.setOnClickListener(v -> {
            if (buttonClickListener != null) {
                buttonClickListener.onNotNowClicked();
            }
        });

        // Listener for the "Interested" button
        holder.interestedButton.setOnClickListener(v -> {
            if (buttonClickListener != null) {
                buttonClickListener.onInterestedClicked();
            }
        });

        // Listener for the new "Save for Later" button
        holder.saveButton.setOnClickListener(v -> {
            if (buttonClickListener != null) {
                buttonClickListener.onSaveClicked();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView jobTitle, companyName, wage, location, jobDescription;
        MaterialButton notNowButton, interestedButton;
        MaterialButton saveButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            jobTitle = itemView.findViewById(R.id.job_title);
            companyName = itemView.findViewById(R.id.company_name);
            wage = itemView.findViewById(R.id.wage);
            location = itemView.findViewById(R.id.location);
            jobDescription = itemView.findViewById(R.id.job_description);
            notNowButton = itemView.findViewById(R.id.not_now_button);
            interestedButton = itemView.findViewById(R.id.interested_button);
            saveButton = itemView.findViewById(R.id.save_button); // Find the "Save" button
        }
        void setData(Job data) {
            jobTitle.setText(data.getTitle());
            companyName.setText(data.getCompanyName());
            wage.setText(data.getWage());
            location.setText(data.getLocation());
            jobDescription.setText(data.getDescription());
        }
    }
}






