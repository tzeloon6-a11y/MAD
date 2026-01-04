package com.example.mad;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ApplicantAdapter extends RecyclerView.Adapter<ApplicantAdapter.ApplicantViewHolder> {

    private List<ApplicationModel> applicantList;
    private OnStartChatClickListener clickListener;
    private java.util.Map<String, String> applicationToChatIdMap = new java.util.HashMap<>(); // Maps application_id to chat_id
    private Context context;

    public interface OnStartChatClickListener {
        void onStartChat(ApplicationModel application);
    }

    public ApplicantAdapter(List<ApplicationModel> applicantList, OnStartChatClickListener clickListener) {
        this.applicantList = applicantList;
        this.clickListener = clickListener;
    }
    
    public void setContext(Context context) {
        this.context = context;
    }

    public void updateData(List<ApplicationModel> newList) {
        this.applicantList = newList;
        // Clear and refresh chat ID map
        applicationToChatIdMap.clear();
        notifyDataSetChanged();
    }
    
    public void refreshChatStatuses() {
        // Refresh all chat statuses for current applications
        if (context != null && applicantList != null) {
            for (int i = 0; i < applicantList.size(); i++) {
                ApplicationModel application = applicantList.get(i);
                String applicationId = application.getApplicationId();
                if (applicationId != null && !applicationId.isEmpty() && !applicationToChatIdMap.containsKey(applicationId)) {
                    checkChatExistsForApplication(applicationId, i);
                }
            }
        }
    }
    
    private void checkChatExistsForApplication(String applicationId, int position) {
        String checkUrl = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/chats?application_id=eq." + applicationId
                + "&select=chat_id"
                + "&limit=1";

        JsonArrayRequest checkRequest = new JsonArrayRequest(
                Request.Method.GET,
                checkUrl,
                null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            JSONObject chat = response.getJSONObject(0);
                            String chatId = chat.optString("chat_id", "");
                            if (chatId != null && !chatId.isEmpty()) {
                                applicationToChatIdMap.put(applicationId, chatId);
                                // Notify item changed to update button
                                if (context instanceof android.app.Activity) {
                                    ((android.app.Activity) context).runOnUiThread(() -> {
                                        notifyItemChanged(position);
                                    });
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Silent fail
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        if (context != null) {
            ApiClient.getRequestQueue(context).add(checkRequest);
        }
    }

    @NonNull
    @Override
    public ApplicantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_applicant, parent, false);
        return new ApplicantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicantViewHolder holder, int position) {
        ApplicationModel application = applicantList.get(position);
        
        // Set student name
        holder.tvStudentName.setText(application.getStudentName());
        
        // Set personal information
        if (holder.tvStudentEmail != null) {
            String email = application.getStudentEmail();
            if (email != null && !email.isEmpty()) {
                holder.tvStudentEmail.setText("Email: " + email);
                holder.tvStudentEmail.setVisibility(View.VISIBLE);
            } else {
                holder.tvStudentEmail.setVisibility(View.GONE);
            }
        }
        
        if (holder.tvStudentPhone != null) {
            String phone = application.getStudentPhone();
            if (phone != null && !phone.isEmpty()) {
                holder.tvStudentPhone.setText("Phone: " + phone);
                holder.tvStudentPhone.setVisibility(View.VISIBLE);
            } else {
                holder.tvStudentPhone.setVisibility(View.GONE);
            }
        }
        
        if (holder.tvStudentBio != null) {
            String bio = application.getStudentBio();
            if (bio != null && !bio.isEmpty()) {
                holder.tvStudentBio.setText("Bio: " + bio);
                holder.tvStudentBio.setVisibility(View.VISIBLE);
            } else {
                holder.tvStudentBio.setVisibility(View.GONE);
            }
        }
        
        // Set initial message (pitch)
        String message = application.getInitialMessage();
        if (message != null && message.length() > 100) {
            message = message.substring(0, 97) + "...";
        }
        holder.tvInitialMessage.setText(message != null ? message : "No message provided");
        
        // Handle View Resume button
        if (holder.btnViewResume != null) {
            String resumeUrl = application.getStudentResumeUrl();
            // Check if resume URL exists and is valid
            boolean hasResume = resumeUrl != null 
                    && !resumeUrl.trim().isEmpty() 
                    && !resumeUrl.equals("null")
                    && !resumeUrl.equalsIgnoreCase("null");
            
            if (hasResume) {
                holder.btnViewResume.setVisibility(View.VISIBLE);
                holder.btnViewResume.setOnClickListener(v -> viewResume(holder.itemView.getContext(), resumeUrl));
            } else {
                holder.btnViewResume.setVisibility(View.GONE);
                holder.btnViewResume.setOnClickListener(null);
            }
        }
        
        // Check if chat exists for this application
        String applicationId = application.getApplicationId();
        String existingChatId = applicationToChatIdMap.get(applicationId);
        
        if (existingChatId != null && !existingChatId.isEmpty()) {
            // Chat exists - show "Go Chat" button
            holder.btnStartChat.setText("Go Chat");
            holder.btnStartChat.setOnClickListener(v -> {
                // Navigate directly to existing chat
                navigateToChat(holder.itemView.getContext(), existingChatId);
            });
        } else {
            // No chat exists - show "Start Chat" button and check for existing chat
            holder.btnStartChat.setText("Start Chat");
            holder.btnStartChat.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onStartChat(application);
                }
            });
            
            // Check if chat exists in background (async)
            if (context != null && applicationId != null && !applicationId.isEmpty()) {
                checkChatExists(applicationId, holder, position);
            }
        }
    }
    
    private void checkChatExists(String applicationId, ApplicantViewHolder holder, int position) {
        String checkUrl = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/chats?application_id=eq." + applicationId
                + "&select=chat_id"
                + "&limit=1";

        JsonArrayRequest checkRequest = new JsonArrayRequest(
                Request.Method.GET,
                checkUrl,
                null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            JSONObject chat = response.getJSONObject(0);
                            String chatId = chat.optString("chat_id", "");
                            if (chatId != null && !chatId.isEmpty()) {
                                // Store chat ID in map
                                applicationToChatIdMap.put(applicationId, chatId);
                                
                                // Update button on main thread
                                if (holder.itemView.getContext() instanceof android.app.Activity) {
                                    ((android.app.Activity) holder.itemView.getContext()).runOnUiThread(() -> {
                                        holder.btnStartChat.setText("Go Chat");
                                        holder.btnStartChat.setOnClickListener(v -> {
                                            navigateToChat(holder.itemView.getContext(), chatId);
                                        });
                                    });
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Silent fail - button stays as "Start Chat"
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        if (context != null) {
            ApiClient.getRequestQueue(context).add(checkRequest);
        }
    }

    @Override
    public int getItemCount() {
        return applicantList != null ? applicantList.size() : 0;
    }

    /**
     * Starts a chat with a student by creating a chat room and moving the student's pitch into it.
     * This is the bridge between Application table and Chat table.
     */
    public static void startChat(Context context, ApplicationModel application, String jobTitle, String recruiterId) {
        // Step 1: Check if chat already exists for this applicationId
        checkExistingChat(context, application, jobTitle, recruiterId);
    }

    private static void checkExistingChat(Context context, ApplicationModel application, String jobTitle, String recruiterId) {
        // Query Chats collection to see if a chat already exists for this applicationId
        String checkUrl = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/chats?application_id=eq." + application.getApplicationId()
                + "&select=chat_id"
                + "&limit=1";

        JsonArrayRequest checkRequest = new JsonArrayRequest(
                Request.Method.GET,
                checkUrl,
                null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            // Chat already exists - navigate to it
                            JSONObject chat = response.getJSONObject(0);
                            String existingChatId = chat.optString("chat_id", "");
                            if (existingChatId != null && !existingChatId.isEmpty()) {
                                navigateToChat(context, existingChatId);
                            } else {
                                createNewChat(context, application, jobTitle, recruiterId);
                            }
                        } else {
                            // No chat exists - create new one
                            createNewChat(context, application, jobTitle, recruiterId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error checking chat", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // On error, try to create new chat
                    createNewChat(context, application, jobTitle, recruiterId);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        ApiClient.getRequestQueue(context).add(checkRequest);
    }

    private static void createNewChat(Context context, ApplicationModel application, String jobTitle, String recruiterId) {
        // Get current timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Create new Chat document
        String chatUrl = SupabaseConfig.SUPABASE_URL + "/rest/v1/chats";

        StringRequest chatRequest = new StringRequest(
                Request.Method.POST,
                chatUrl,
                response -> {
                    // Chat created - extract chatId and proceed
                    String newChatId = extractChatIdFromResponse(response);
                    if (newChatId == null || newChatId.isEmpty()) {
                        // If we can't extract, query for it
                        queryChatIdAndContinue(context, application, recruiterId, jobTitle, timestamp);
                    } else {
                        // Transfer message and update status
                        transferMessageAndUpdateStatus(context, application, newChatId, timestamp);
                    }
                },
                error -> {
                    Toast.makeText(context, "Failed to create chat", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                headers.put("Prefer", "return=representation");
                return headers;
            }

            @Override
            public byte[] getBody() {
                try {
                    JSONObject chatJson = new JSONObject();
                    chatJson.put("student_id", application.getStudentId());
                    chatJson.put("recruiter_id", recruiterId);
                    chatJson.put("job_id", application.getJobId());
                    chatJson.put("job_title", jobTitle != null ? jobTitle : "");
                    chatJson.put("application_id", application.getApplicationId());
                    chatJson.put("last_message", "Chat started");
                    chatJson.put("timestamp", timestamp);
                    return chatJson.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        ApiClient.getRequestQueue(context).add(chatRequest);
    }

    private static String extractChatIdFromResponse(String response) {
        try {
            if (response.startsWith("[")) {
                JSONArray array = new JSONArray(response);
                if (array.length() > 0) {
                    JSONObject chat = array.getJSONObject(0);
                    return chat.optString("chat_id", "");
                }
            } else {
                JSONObject chat = new JSONObject(response);
                return chat.optString("chat_id", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void queryChatIdAndContinue(Context context, ApplicationModel application,
                                               String recruiterId, String jobTitle, String timestamp) {
        // Query for the most recent chat
        String queryUrl = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/chats?application_id=eq." + application.getApplicationId()
                + "&order=timestamp.desc"
                + "&limit=1"
                + "&select=chat_id";

        JsonArrayRequest queryRequest = new JsonArrayRequest(
                Request.Method.GET,
                queryUrl,
                null,
                response -> {
                    try {
                        String chatId = "";
                        if (response.length() > 0) {
                            JSONObject chat = response.getJSONObject(0);
                            chatId = chat.optString("chat_id", "");
                        }

                        if (chatId != null && !chatId.isEmpty()) {
                            transferMessageAndUpdateStatus(context, application, chatId, timestamp);
                        } else {
                            Toast.makeText(context, "Failed to retrieve chat ID", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error retrieving chat", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(context, "Failed to query chat", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        ApiClient.getRequestQueue(context).add(queryRequest);
    }

    private static void transferMessageAndUpdateStatus(Context context, ApplicationModel application,
                                                       String chatId, String timestamp) {
        // Step 1: Transfer initial message to Messages collection
        String messageUrl = SupabaseConfig.SUPABASE_URL + "/rest/v1/messages";

        StringRequest messageRequest = new StringRequest(
                Request.Method.POST,
                messageUrl,
                response -> {
                    // Message transferred - now update application status
                    updateApplicationStatus(context, application, chatId);
                },
                error -> {
                    Toast.makeText(context, "Failed to transfer message", Toast.LENGTH_SHORT).show();
                    // Still try to update status and navigate
                    updateApplicationStatus(context, application, chatId);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                headers.put("Prefer", "return=minimal");
                return headers;
            }

            @Override
            public byte[] getBody() {
                try {
                    JSONObject messageJson = new JSONObject();
                    messageJson.put("chat_id", chatId);
                    messageJson.put("sender_id", application.getStudentId()); // Student sent the initial message
                    messageJson.put("text", application.getInitialMessage());
                    messageJson.put("timestamp", timestamp);
                    return messageJson.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        ApiClient.getRequestQueue(context).add(messageRequest);
    }

    private static void updateApplicationStatus(Context context, ApplicationModel application, String chatId) {
        // Update Application status to ACCEPTED
        String updateUrl = SupabaseConfig.SUPABASE_URL + "/rest/v1/applications?application_id=eq." + application.getApplicationId();

        StringRequest updateRequest = new StringRequest(
                Request.Method.PATCH,
                updateUrl,
                response -> {
                    // Success - navigate to chat
                    navigateToChat(context, chatId);
                },
                error -> {
                    Toast.makeText(context, "Chat created but failed to update application status", Toast.LENGTH_SHORT).show();
                    // Still navigate to chat
                    navigateToChat(context, chatId);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                headers.put("Prefer", "return=minimal");
                return headers;
            }

            @Override
            public byte[] getBody() {
                try {
                    JSONObject updateJson = new JSONObject();
                    updateJson.put("status", "ACCEPTED");
                    return updateJson.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        ApiClient.getRequestQueue(context).add(updateRequest);
    }

    public static void navigateToChat(Context context, String chatId) {
        Intent intent = new Intent(context, ChatDetailActivity.class);
        intent.putExtra("chatId", chatId);
        context.startActivity(intent);
    }
    
    private void viewResume(Context context, String resumeUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(resumeUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Verify that there's an app to handle this intent
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "No app available to view resume", Toast.LENGTH_SHORT).show();
            }
        } catch (android.content.ActivityNotFoundException e) {
            // Handle case where no app can handle the intent
            Toast.makeText(context, "No app available to view resume", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error opening resume", Toast.LENGTH_SHORT).show();
        }
    }

    static class ApplicantViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName;
        TextView tvStudentEmail;
        TextView tvStudentPhone;
        TextView tvStudentBio;
        TextView tvInitialMessage;
        Button btnStartChat;
        Button btnViewResume;

        public ApplicantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name);
            tvStudentEmail = itemView.findViewById(R.id.tv_student_email);
            tvStudentPhone = itemView.findViewById(R.id.tv_student_phone);
            tvStudentBio = itemView.findViewById(R.id.tv_student_bio);
            tvInitialMessage = itemView.findViewById(R.id.tv_initial_message);
            btnStartChat = itemView.findViewById(R.id.btn_start_chat);
            btnViewResume = itemView.findViewById(R.id.btn_view_resume);
        }
    }
}


