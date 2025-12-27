package com.example.mad;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatHelper {

    /**
     * Starts a chat with a student by creating a new Chat record and adding the initial message.
     * This function should be called when a Recruiter clicks "Start Chat" button.
     * 
     * @param context The Activity context (for navigation and SharedPreferences)
     * @param studentId The ID of the student to chat with
     * @param jobId The ID of the job posting
     * @param initialMessage The student's initial pitch/message
     */
    public static void startChatWithStudent(Context context, String studentId, String jobId, String initialMessage) {
        // Get current recruiter ID from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String recruiterId = prefs.getString(LoginActivity.KEY_USER_ID, null);

        if (recruiterId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // First, fetch the job title from the jobId
        fetchJobTitleAndCreateChat(context, recruiterId, studentId, jobId, initialMessage);
    }

    private static void fetchJobTitleAndCreateChat(Context context, String recruiterId, 
                                                   String studentId, String jobId, String initialMessage) {
        // Fetch job title from job_posts table
        String jobUrl = SupabaseConfig.SUPABASE_URL + "/rest/v1/job_posts?id=eq." + jobId + "&select=title";

        JsonArrayRequest jobRequest = new JsonArrayRequest(
                Request.Method.GET,
                jobUrl,
                null,
                response -> {
                    try {
                        String jobTitle = "";
                        // Supabase returns an array, get first element
                        if (response.length() > 0) {
                            JSONObject job = response.getJSONObject(0);
                            jobTitle = job.optString("title", "");
                        }

                        // Now create the chat with the job title
                        createChat(context, recruiterId, studentId, jobId, jobTitle, initialMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Continue with empty job title if fetch fails
                        createChat(context, recruiterId, studentId, jobId, "", initialMessage);
                    }
                },
                error -> {
                    // Continue with empty job title if fetch fails
                    createChat(context, recruiterId, studentId, jobId, "", initialMessage);
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

        ApiClient.getRequestQueue(context).add(jobRequest);
    }

    private static void createChat(Context context, String recruiterId, String studentId, 
                                   String jobId, String jobTitle, String initialMessage) {
        // First, check if a chat already exists for this student, recruiter, and job
        checkExistingChatAndCreate(context, recruiterId, studentId, jobId, jobTitle, initialMessage);
    }

    private static void checkExistingChatAndCreate(Context context, String recruiterId, String studentId,
                                                    String jobId, String jobTitle, String initialMessage) {
        // Query for existing chat
        String checkUrl = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/chats?recruiter_id=eq." + recruiterId
                + "&student_id=eq." + studentId
                + "&job_id=eq." + jobId
                + "&select=chat_id"
                + "&limit=1";

        JsonArrayRequest checkRequest = new JsonArrayRequest(
                Request.Method.GET,
                checkUrl,
                null,
                response -> {
                    try {
                        // If chat exists, use it; otherwise create new one
                        if (response.length() > 0) {
                            JSONObject existingChat = response.getJSONObject(0);
                            String existingChatId = existingChat.optString("chat_id", "");
                            if (existingChatId != null && !existingChatId.isEmpty()) {
                                // Chat exists, just add the initial message and navigate
                                String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                        .format(new Date());
                                createInitialMessage(context, existingChatId, studentId, initialMessage, timestamp);
                                return;
                            }
                        }
                        // No existing chat found, create a new one
                        createNewChat(context, recruiterId, studentId, jobId, jobTitle, initialMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // On error, try to create new chat
                        createNewChat(context, recruiterId, studentId, jobId, jobTitle, initialMessage);
                    }
                },
                error -> {
                    // On error, try to create new chat
                    createNewChat(context, recruiterId, studentId, jobId, jobTitle, initialMessage);
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

    private static void createNewChat(Context context, String recruiterId, String studentId, 
                                   String jobId, String jobTitle, String initialMessage) {
        // Get current timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Create chat JSON
        String chatUrl = SupabaseConfig.SUPABASE_URL + "/rest/v1/chats";

        StringRequest chatRequest = new StringRequest(
                Request.Method.POST,
                chatUrl,
                response -> {
                    // Chat created successfully, now extract chatId and create the initial message
                    // Supabase returns the created record, but we need to get the chat_id
                    // Since we might not get it in response, we'll query for it
                    String newChatId = extractChatIdFromResponse(response);
                    if (newChatId == null || newChatId.isEmpty()) {
                        // If we can't extract from response, query for the most recent chat
                        queryChatIdAndCreateMessage(context, recruiterId, studentId, jobId, initialMessage, timestamp);
                    } else {
                        createInitialMessage(context, newChatId, studentId, initialMessage, timestamp);
                    }
                },
                error -> {
                    String responseBody = null;
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(context, "Failed to create chat", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                headers.put("Prefer", "return=representation"); // Return the created record
                return headers;
            }

            @Override
            public byte[] getBody() {
                try {
                    JSONObject chatJson = new JSONObject();
                    chatJson.put("student_id", studentId);
                    chatJson.put("recruiter_id", recruiterId);
                    chatJson.put("job_id", jobId);
                    chatJson.put("job_title", jobTitle);
                    chatJson.put("last_message", initialMessage);
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
            // Response might be an array with the created record
            if (response.startsWith("[")) {
                org.json.JSONArray array = new org.json.JSONArray(response);
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

    private static void queryChatIdAndCreateMessage(Context context, String recruiterId, 
                                                    String studentId, String jobId, 
                                                    String initialMessage, String timestamp) {
        // Query for the most recent chat between this recruiter and student for this job
        String queryUrl = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/chats?recruiter_id=eq." + recruiterId
                + "&student_id=eq." + studentId
                + "&job_id=eq." + jobId
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
                        // Supabase returns an array, get first element
                        if (response.length() > 0) {
                            JSONObject chat = response.getJSONObject(0);
                            chatId = chat.optString("chat_id", "");
                        }

                        if (chatId != null && !chatId.isEmpty()) {
                            createInitialMessage(context, chatId, studentId, initialMessage, timestamp);
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

    private static void createInitialMessage(Context context, String chatId, String studentId, 
                                             String initialMessage, String timestamp) {
        // Create the initial message in the Messages collection
        String messageUrl = SupabaseConfig.SUPABASE_URL + "/rest/v1/messages";

        StringRequest messageRequest = new StringRequest(
                Request.Method.POST,
                messageUrl,
                response -> {
                    // Message created successfully, navigate to ChatDetailActivity
                    Intent intent = new Intent(context, ChatDetailActivity.class);
                    intent.putExtra("chatId", chatId);
                    context.startActivity(intent);
                },
                error -> {
                    String responseBody = null;
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(context, "Chat created but failed to add initial message", Toast.LENGTH_SHORT).show();
                    // Still navigate to chat even if message creation fails
                    Intent intent = new Intent(context, ChatDetailActivity.class);
                    intent.putExtra("chatId", chatId);
                    context.startActivity(intent);
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
                    messageJson.put("sender_id", studentId); // Student sent the initial message
                    messageJson.put("text", initialMessage);
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
}

