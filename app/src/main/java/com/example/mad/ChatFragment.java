package com.example.mad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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

public class ChatFragment extends Fragment {

    private RecyclerView recyclerChatList;
    private ChatListAdapter adapter;
    private String currentUserId;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Get current user ID
        SharedPreferences prefs = requireActivity().getSharedPreferences(LoginActivity.PREFS_NAME, requireContext().MODE_PRIVATE);
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);

        if (currentUserId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize RecyclerView
        recyclerChatList = view.findViewById(R.id.recycler_chat_list);
        recyclerChatList.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ChatListAdapter(new ArrayList<>(), chatId -> {
            // Open ChatDetailActivity with chatId
            Intent intent = new Intent(getActivity(), ChatDetailActivity.class);
            intent.putExtra("chatId", chatId);
            startActivity(intent);
        }, currentUserId);

        recyclerChatList.setAdapter(adapter);

        // Load chats
        loadChatsFromSupabase();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUserId != null) {
            loadChatsFromSupabase();
        }
    }

    private void loadChatsFromSupabase() {
        try {
            // Query chats where current user is either student or recruiter
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
                            Toast.makeText(getContext(), "Error parsing chats", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(getContext(), "Failed to load chats", Toast.LENGTH_SHORT).show();
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

            ApiClient.getRequestQueue(requireContext()).add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error loading chats", Toast.LENGTH_SHORT).show();
        }
    }
}
