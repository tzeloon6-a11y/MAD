package com.example.mad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {

    private List<ChatModel> chatList;
    private OnChatClickListener clickListener;
    private String currentUserId;

    public interface OnChatClickListener {
        void onChatClick(String chatId);
    }

    public ChatListAdapter(List<ChatModel> chatList, OnChatClickListener clickListener, String currentUserId) {
        this.chatList = chatList;
        this.clickListener = clickListener;
        this.currentUserId = currentUserId;
    }

    public void updateData(List<ChatModel> newList) {
        this.chatList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_list, parent, false);
        return new ChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListViewHolder holder, int position) {
        ChatModel chat = chatList.get(position);
        
        // Set job title
        holder.tvJobTitle.setText(chat.getJobTitle());
        
        // Set last message (truncate if too long)
        String lastMessage = chat.getLastMessage();
        if (lastMessage != null && lastMessage.length() > 50) {
            lastMessage = lastMessage.substring(0, 47) + "...";
        }
        holder.tvLastMessage.setText(lastMessage != null ? lastMessage : "No messages yet");
        
        // Set timestamp (format if needed)
        holder.tvTimestamp.setText(formatTimestamp(chat.getTimestamp()));
        
        // Set other user's name and bio
        // If current user is recruiter, show student name and bio
        // If current user is student, show recruiter name
        String otherUserName = determineOtherUserName(chat);
        String otherUserBio = determineOtherUserBio(chat);
        
        holder.tvOtherUserName.setText(otherUserName);
        if (holder.tvOtherUserBio != null) {
            if (otherUserBio != null && !otherUserBio.isEmpty()) {
                holder.tvOtherUserBio.setText(otherUserBio);
                holder.tvOtherUserBio.setVisibility(View.VISIBLE);
            } else {
                holder.tvOtherUserBio.setVisibility(View.GONE);
            }
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onChatClick(chat.getChatId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    private String determineOtherUserName(ChatModel chat) {
        // Determine which user is the "other" user based on current user ID
        if (currentUserId != null) {
            if (currentUserId.equals(chat.getStudentId())) {
                // Current user is student, so other user is recruiter
                return chat.getRecruiterName() != null ? chat.getRecruiterName() : "Recruiter";
            } else if (currentUserId.equals(chat.getRecruiterId())) {
                // Current user is recruiter, so other user is student
                return chat.getStudentName() != null ? chat.getStudentName() : "Student";
            }
        }
        // Fallback if user ID doesn't match
        return "Chat Participant";
    }
    
    private String determineOtherUserBio(ChatModel chat) {
        // Return bio only if current user is recruiter (viewing student)
        if (currentUserId != null && currentUserId.equals(chat.getRecruiterId())) {
            return chat.getStudentBio();
        }
        return null; // Don't show bio for recruiter when student is viewing
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "";
        }
        // Simple formatting - you may want to use DateFormat for better formatting
        // For now, return as-is or extract time portion
        try {
            // If timestamp is in ISO format like "2025-01-15T12:30:00", extract time
            if (timestamp.contains("T")) {
                String timePart = timestamp.split("T")[1];
                if (timePart.contains(".")) {
                    timePart = timePart.split("\\.")[0];
                }
                return timePart.substring(0, Math.min(5, timePart.length())); // HH:MM
            }
            return timestamp;
        } catch (Exception e) {
            return timestamp;
        }
    }

    static class ChatListViewHolder extends RecyclerView.ViewHolder {
        TextView tvOtherUserName;
        TextView tvOtherUserBio;
        TextView tvJobTitle;
        TextView tvLastMessage;
        TextView tvTimestamp;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOtherUserName = itemView.findViewById(R.id.tv_other_user_name);
            tvOtherUserBio = itemView.findViewById(R.id.tv_other_user_bio);
            tvJobTitle = itemView.findViewById(R.id.tv_job_title);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
        }
    }
}

