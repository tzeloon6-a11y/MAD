package com.example.mad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerChatList;
    private ChatListAdapter adapter;
    private String currentUserId;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // Get current user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);
        currentUserRole = prefs.getString(LoginActivity.KEY_ROLE, null);

        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerChatList = findViewById(R.id.recycler_chat_list);
        recyclerChatList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChatListAdapter(new ArrayList<>(), chatId -> {
            // Open ChatDetailActivity with chatId
            Intent intent = new Intent(ChatListActivity.this, ChatDetailActivity.class);
            intent.putExtra("chatId", chatId);
            startActivity(intent);
        }, currentUserId);

        recyclerChatList.setAdapter(adapter);

        loadChatsFromSupabase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatsFromSupabase();
    }

    private void loadChatsFromSupabase() {
        try {
            // Query chats where current user is either student or recruiter
            // Using Supabase OR filter: (student_id=eq.userId OR recruiter_id=eq.userId)
            String encodedUserId = URLEncoder.encode(currentUserId, "UTF-8");
            String url = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/chats?select=*"
                    + "&or=(student_id.eq." + encodedUserId + ",recruiter_id.eq." + encodedUserId + ")"
                    + "&order=timestamp.desc";

            JsonArrayRequest request = new JsonArrayRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        try {
                            ArrayList<ChatModel> chatList = new ArrayList<>();

                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);

                                String chatId = obj.optString("chat_id", "");
                                String jobId = obj.optString("job_id", "");
                                String studentId = obj.optString("student_id", "");
                                String recruiterId = obj.optString("recruiter_id", "");
                                String jobTitle = obj.optString("job_title", "");
                                String lastMessage = obj.optString("last_message", "");
                                String timestamp = obj.optString("timestamp", "");

                                // Determine the other user's name
                                // For now, we'll use a placeholder - you may want to fetch user names separately
                                String otherUserName = getOtherUserName(studentId, recruiterId);

                                chatList.add(new ChatModel(
                                        chatId,
                                        jobId,
                                        studentId,
                                        recruiterId,
                                        jobTitle,
                                        lastMessage,
                                        timestamp
                                ));
                            }

                            adapter.updateData(chatList);

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error parsing chats", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(this, "Failed to load chats", Toast.LENGTH_SHORT).show();
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

            ApiClient.getRequestQueue(this).add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading chats", Toast.LENGTH_SHORT).show();
        }
    }

    private String getOtherUserName(String studentId, String recruiterId) {
        // Determine which user is the "other" user
        if (currentUserId.equals(studentId)) {
            // Current user is student, so other user is recruiter
            // You may want to fetch recruiter name from users table
            return "Recruiter";
        } else {
            // Current user is recruiter, so other user is student
            // You may want to fetch student name from users table
            return "Student";
        }
    }
}

