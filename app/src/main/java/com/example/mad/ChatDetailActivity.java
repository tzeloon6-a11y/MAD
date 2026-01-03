package com.example.mad;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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

        // 1. Get Data from Intent (Fixed Keys to match ChatAdapter)
        chatId = getIntent().getStringExtra("chat_id"); // Changed from "chatId"
        String title = getIntent().getStringExtra("title"); // Optional title

        if (chatId == null || chatId.isEmpty()) {
            Toast.makeText(this, "Invalid chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Get current user ID
        // Using "UserPrefs" to match LoginActivity/StudentHomeFragment
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);

        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3. Initialize views
        recyclerMessages = findViewById(R.id.recycler_messages); // Ensure this ID matches your XML
        etMessageInput = findViewById(R.id.et_message_input);
        btnSend = findViewById(R.id.btn_send);
        tvChatHeader = findViewById(R.id.tv_chat_header);

        if (tvChatHeader != null && title != null) {
            tvChatHeader.setText(title);
        }

        // 4. Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Helper: Messages start from bottom
        recyclerMessages.setLayoutManager(layoutManager);

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
        if (messagePollHandler != null && messagePollRunnable != null) {
            messagePollHandler.removeCallbacks(messagePollRunnable);
        }
    }

    private void loadMessages() {
        try {
            // FIX: Using 'created_at' instead of 'timestamp'
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

                                String text = obj.optString("text", ""); // Change 'content' to 'text'
                                String timestamp = obj.optString("timestamp", "");

                                messageList.add(new MessageModel(
                                        messageId,
                                        senderId,
                                        text,
                                        timestamp
                                ));
                            }

                            adapter.updateData(messageList);

                            // Only scroll if we are at the bottom or it's the first load
                            if (messageList.size() > 0) {
                                // recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        // Silent fail for polling
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                    headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                    return headers;
                }
            };

            ApiClient.getRequestQueue(this).add(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String messageText = etMessageInput.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        // Clear input immediately for better UX
        etMessageInput.setText("");

        try {
            String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/messages";

            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    url,
                    response -> {
                        // 1. Update the chat list screen (Last Message)
                        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                        updateChatLastMessage(messageText, timestamp);

                        // 2. Reload this screen
                        loadMessages();
                    },
                    error -> {
                        Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show();
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
                        JSONObject json = new JSONObject();
                        json.put("chat_id", chatId);
                        json.put("sender_id", currentUserId);

                        // CHANGE 'content' TO 'text'
                        json.put("text", messageText);

                        // ADD TIMESTAMP (Supabase might need it if it's not auto-generated)
                        String timeNow = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                        json.put("timestamp", timeNow);

                        return json.toString().getBytes(StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        return null;
                    }
                }
            };

            ApiClient.getRequestQueue(this).add(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateChatLastMessage(String lastMessage, String timestamp) {
        try {
            String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/chats?chat_id=eq." + chatId;

            StringRequest request = new StringRequest(
                    Request.Method.PATCH,
                    url,
                    response -> { /* Success */ },
                    error -> { /* Fail silently */ }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                    headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }

                @Override
                public byte[] getBody() {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("last_message", lastMessage);
                        // Ensure your chats table has a 'timestamp' column, otherwise remove this line
                        json.put("timestamp", timestamp);
                        return json.toString().getBytes(StandardCharsets.UTF_8);
                    } catch (Exception e) {
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