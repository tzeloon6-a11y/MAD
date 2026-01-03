package com.example.mad;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
        View view = inflater.inflate(R.layout.item_job_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setData(items.get(position));

        // 1. RESET STATE (Default: Hollow White Outline)
        // Reset to the outline icon every time the card rebinds
        holder.saveButton.setIconResource(R.drawable.ic_save_for_later);
        // Force it to be WHITE so it is visible on the blue background
        holder.saveButton.setIconTint(ColorStateList.valueOf(Color.WHITE));

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

        // 2. CLICK LISTENER (Toggle to Solid Blue)
        holder.saveButton.setOnClickListener(v -> {
            // A. Change image to the "Filled White" vector (the shape is all we need)
            holder.saveButton.setIconResource(R.drawable.ic_bookmark_filled_white);

            // B. APPLY BLUE TINT 🎨
            // We get the "colorPrimary" (the blue background color) from the current theme.
            android.util.TypedValue typedValue = new android.util.TypedValue();
            holder.itemView.getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            int primaryColor = typedValue.data;

            // Set the tint to that blue color.
            holder.saveButton.setIconTint(ColorStateList.valueOf(primaryColor));

            // C. Trigger the database save
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
        MaterialButton notNowButton, interestedButton, saveButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            jobTitle = itemView.findViewById(R.id.job_title);
            companyName = itemView.findViewById(R.id.company_name);
            wage = itemView.findViewById(R.id.wage);
            location = itemView.findViewById(R.id.location);
            jobDescription = itemView.findViewById(R.id.job_description);
            notNowButton = itemView.findViewById(R.id.not_now_button);
            interestedButton = itemView.findViewById(R.id.interested_button);
            saveButton = itemView.findViewById(R.id.save_button);
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