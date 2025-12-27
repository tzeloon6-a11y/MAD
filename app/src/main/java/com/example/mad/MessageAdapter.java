package com.example.mad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_MESSAGE_LEFT = 1;
    private static final int VIEW_TYPE_MESSAGE_RIGHT = 2;

    private List<MessageModel> messageList;
    private String currentUserId;

    public MessageAdapter(List<MessageModel> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    public void updateData(List<MessageModel> newList) {
        this.messageList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messageList.get(position);
        // If senderId matches current user, show right layout (my message)
        // Otherwise, show left layout (other person's message)
        if (currentUserId != null && currentUserId.equals(message.getSenderId())) {
            return VIEW_TYPE_MESSAGE_RIGHT;
        } else {
            return VIEW_TYPE_MESSAGE_LEFT;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_RIGHT) {
            // My message - use right layout
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_right, parent, false);
        } else {
            // Other person's message - use left layout
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_left, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageModel message = messageList.get(position);
        
        // Set message text
        holder.tvMessageText.setText(message.getText());
        
        // Set formatted timestamp
        holder.tvMessageTimestamp.setText(formatTimestamp(message.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "";
        }
        
        try {
            // Parse ISO format timestamp (e.g., "2025-01-15T12:30:00")
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            
            if (date != null) {
                // Format as HH:MM
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            // If parsing fails, try to extract time portion manually
            if (timestamp.contains("T")) {
                String timePart = timestamp.split("T")[1];
                if (timePart.contains(".")) {
                    timePart = timePart.split("\\.")[0];
                }
                if (timePart.length() >= 5) {
                    return timePart.substring(0, 5); // HH:MM
                }
            }
        }
        
        return timestamp;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText;
        TextView tvMessageTimestamp;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tv_message_text);
            tvMessageTimestamp = itemView.findViewById(R.id.tv_message_timestamp);
        }
    }
}

