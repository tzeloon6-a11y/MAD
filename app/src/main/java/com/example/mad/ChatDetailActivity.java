package com.example.mad;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private EditText etMessageInput;
    private Button btnSend;
    private TextView tvChatHeader;

    private MessageAdapter adapter;
    private String chatId;
    private String currentUserId;
    private Handler messagePollHandler;
    private Runnable messagePollRunnable;
    private static final long POLL_INTERVAL = 2000; // Poll every 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        // Get chatId from Intent
        chatId = getIntent().getStringExtra("chatId");
        if (chatId == null || chatId.isEmpty()) {
            Toast.makeText(this, "Invalid chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get current user ID
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        recyclerMessages = findViewById(R.id.recycler_messages);
        etMessageInput = findViewById(R.id.et_message_input);
        btnSend = findViewById(R.id.btn_send);
        tvChatHeader = findViewById(R.id.tv_chat_header);

        // Setup RecyclerView
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(new ArrayList<>(), currentUserId);
        recyclerMessages.setAdapter(adapter);

        // Load initial messages
        loadMessages();

        // Setup Send button
        btnSend.setOnClickListener(v -> sendMessage());

        // Setup real-time polling
        setupMessagePolling();
    }

    private void setupMessagePolling() {
        messagePollHandler = new Handler(Looper.getMainLooper());
        messagePollRunnable = new Runnable() {
            @Override
            public void run() {
                loadMessages();
                messagePollHandler.postDelayed(this, POLL_INTERVAL);
            }
        };
        messagePollHandler.postDelayed(messagePollRunnable, POLL_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop polling when activity is destroyed
        if (messagePollHandler != null && messagePollRunnable != null) {
            messagePollHandler.removeCallbacks(messagePollRunnable);
        }
    }

    private void loadMessages() {
        try {
            String url = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/messages?select=*"
                    + "&chat_id=eq." + chatId
                    + "&order=timestamp.asc";

            JsonArrayRequest request = new JsonArrayRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        try {
                            ArrayList<MessageModel> messageList = new ArrayList<>();

                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);

                                String messageId = obj.optString("message_id", "");
                                String senderId = obj.optString("sender_id", "");
                                String text = obj.optString("text", "");
                                String timestamp = obj.optString("timestamp", "");

                                messageList.add(new MessageModel(
                                        messageId,
                                        senderId,
                                        text,
                                        timestamp
                                ));
                            }

                            adapter.updateData(messageList);
                            
                            // Scroll to bottom when new messages arrive
                            if (messageList.size() > 0) {
                                recyclerMessages.post(() -> {
                                    recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
                                });
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        // Silent fail for polling - don't show toast on every poll
                        if (!isPolling()) {
                            Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                        }
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
        }
    }

    private boolean isPolling() {
        // Simple flag to distinguish between initial load and polling
        return adapter.getItemCount() > 0;
    }

    private void sendMessage() {
        String messageText = etMessageInput.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Send message to Supabase
        try {
            String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/messages";

            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    url,
                    response -> {
                        // Clear input
                        etMessageInput.setText("");
                        
                        // Update chat's lastMessage and timestamp
                        updateChatLastMessage(messageText, timestamp);
                        
                        // Reload messages to show the new one
                        loadMessages();
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
                        Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
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
                        messageJson.put("sender_id", currentUserId);
                        messageJson.put("text", messageText);
                        messageJson.put("timestamp", timestamp);
                        return messageJson.toString().getBytes(StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            };

            ApiClient.getRequestQueue(this).add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating message", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateChatLastMessage(String lastMessage, String timestamp) {
        try {
            String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/chats?chat_id=eq." + chatId;

            StringRequest request = new StringRequest(
                    Request.Method.PATCH,
                    url,
                    response -> {
                        // Success - chat updated
                    },
                    error -> {
                        // Silent fail - not critical if this fails
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
                        updateJson.put("last_message", lastMessage);
                        updateJson.put("timestamp", timestamp);
                        return updateJson.toString().getBytes(StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            };

            ApiClient.getRequestQueue(this).add(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
