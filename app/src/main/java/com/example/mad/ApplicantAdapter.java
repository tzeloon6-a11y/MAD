package com.example.mad;

import android.content.Context;
import android.content.Intent;
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

    public interface OnStartChatClickListener {
        void onStartChat(ApplicationModel application);
    }

    public ApplicantAdapter(List<ApplicationModel> applicantList, OnStartChatClickListener clickListener) {
        this.applicantList = applicantList;
        this.clickListener = clickListener;
    }

    public void updateData(List<ApplicationModel> newList) {
        this.applicantList = newList;
        notifyDataSetChanged();
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
        
        // Set initial message (pitch)
        String message = application.getInitialMessage();
        if (message != null && message.length() > 100) {
            message = message.substring(0, 97) + "...";
        }
        holder.tvInitialMessage.setText(message != null ? message : "No message provided");
        
        // Set click listener for Start Chat button
        holder.btnStartChat.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onStartChat(application);
            }
        });
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
        android.util.Log.d("ChatDebug", "▶️ startChat called for applicationId=" + application.getApplicationId());
        android.util.Log.d("ChatDebug", "   studentId=" + application.getStudentId()
                + ", recruiterId=" + recruiterId
                + ", jobId=" + application.getJobId());
        checkExistingChat(context, application, jobTitle, recruiterId);
    }

    private static void checkExistingChat(Context context, ApplicationModel application, String jobTitle, String recruiterId) {
        // Query Chats collection to see if a chat already exists for this applicationId
        String checkUrl = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/chats?application_id=eq." + application.getApplicationId()
                + "&select=chat_id"
                + "&limit=1";

        android.util.Log.d("ChatDebug", "🔎 Checking existing chat: " + checkUrl);

        JsonArrayRequest checkRequest = new JsonArrayRequest(
                Request.Method.GET,
                checkUrl,
                null,
                response -> {
                    try {
                        android.util.Log.d("ChatDebug", "✅ checkExistingChat response: " + response);
                        if (response.length() > 0) {
                            // Chat already exists - navigate to it
                            JSONObject chat = response.getJSONObject(0);
                            String existingChatId = chat.optString("chat_id", "");
                            if (existingChatId != null && !existingChatId.isEmpty()) {
                                android.util.Log.d("ChatDebug", "➡️ Existing chat found. chatId=" + existingChatId);
                                navigateToChat(context, existingChatId);
                            } else {
                                android.util.Log.w("ChatDebug", "⚠️ Existing chat row has empty chat_id, creating new chat");
                                createNewChat(context, application, jobTitle, recruiterId);
                            }
                        } else {
                            // No chat exists - create new one
                            android.util.Log.d("ChatDebug", "ℹ️ No existing chat for this application, creating new chat");
                            createNewChat(context, application, jobTitle, recruiterId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        android.util.Log.e("ChatDebug", "❌ Exception while checking existing chat", e);
                        Toast.makeText(context, "Error checking chat", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // On error, try to create new chat
                    android.util.Log.e("ChatDebug", "❌ Network error while checking existing chat", error);
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
                    android.util.Log.d("ChatDebug", "✅ Chat create response: " + response);
                    String newChatId = extractChatIdFromResponse(response);
                    if (newChatId == null || newChatId.isEmpty()) {
                        android.util.Log.w("ChatDebug", "⚠️ Could not extract chatId from response, querying for latest chat instead");
                        // If we can't extract, query for it
                        queryChatIdAndContinue(context, application, recruiterId, jobTitle, timestamp);
                    } else {
                        android.util.Log.d("ChatDebug", "✅ Chat created! ID: " + newChatId);
                        // Transfer message and update status
                        transferMessageAndUpdateStatus(context, application, newChatId, timestamp);
                    }
                },
                error -> {
                    // Detailed error logging similar to your Retrofit example
                    String errorBody = null;
                    int statusCode = -1;
                    if (error.networkResponse != null) {
                        statusCode = error.networkResponse.statusCode;
                        if (error.networkResponse.data != null) {
                            try {
                                errorBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    android.util.Log.e("ChatDebug", "❌ Failed to create chat. HTTP code: " + statusCode);
                    android.util.Log.e("ChatDebug", "❌ Error Body: " + (errorBody != null ? errorBody : "null"));
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

    private static void navigateToChat(Context context, String chatId) {
        Intent intent = new Intent(context, ChatDetailActivity.class);
        intent.putExtra("chatId", chatId);
        context.startActivity(intent);
    }

    static class ApplicantViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName;
        TextView tvInitialMessage;
        Button btnStartChat;

        public ApplicantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name);
            tvInitialMessage = itemView.findViewById(R.id.tv_initial_message);
            btnStartChat = itemView.findViewById(R.id.btn_start_chat);
        }
    }
}


