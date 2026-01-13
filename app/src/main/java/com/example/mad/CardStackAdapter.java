package com.example.mad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;


public class CardStackAdapter extends RecyclerView.Adapter<CardStackAdapter.ViewHolder> {
    private final List<Job> items;
    private OnButtonClickListener buttonClickListener;

    public interface OnButtonClickListener {
        void onNotNowClicked();
        void onInterestedClicked();
        void onSaveClicked();
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
        // "R" might be red here until we do Step 4 (XML files). Ignore for now.
        View view = inflater.inflate(R.layout.item_job_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setData(items.get(position));

        holder.notNowButton.setOnClickListener(v -> {
            if (buttonClickListener != null) {
                buttonClickListener.onNotNowClicked();
            }
        });

        holder.interestedButton.setOnClickListener(v -> {
            if (buttonClickListener != null) {
                buttonClickListener.onInterestedClicked();
            }
        });

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
        TextView jobTitle, companyName, jobDescription;
        MaterialButton notNowButton, interestedButton, saveButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            jobTitle = itemView.findViewById(R.id.job_title);
            companyName = itemView.findViewById(R.id.company_name);
            jobDescription = itemView.findViewById(R.id.job_description);
            notNowButton = itemView.findViewById(R.id.not_now_button);
            interestedButton = itemView.findViewById(R.id.interested_button);
            saveButton = itemView.findViewById(R.id.save_button);
        }
        void setData(Job data) {
            jobTitle.setText(data.getTitle());
            companyName.setText(data.getCompanyName());
            jobDescription.setText(data.getDescription());
        }
    }
}
