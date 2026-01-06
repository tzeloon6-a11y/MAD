package com.example.mad;

import android.content.Context;
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

    // Use static factory method instead of public constructor
    public static ChatFragment newInstance() {
        return new ChatFragment();
    }

    private ChatFragment() {
        // Required empty private constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Get current user ID
        SharedPreferences prefs = requireActivity().getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);

        if (currentUserId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize RecyclerView
        recyclerChatList = view.findViewById(R.id.recycler_chat_list);
        recyclerChatList.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ChatListAdapter(new ArrayList<>(), chatId -> {
            // Check if this is an example chat (starts with "example-")
            if (chatId != null && chatId.startsWith("example-")) {
                Toast.makeText(getContext(), "This is an example chat. Create a real chat by applying to a job or starting a chat from an application.", Toast.LENGTH_LONG).show();
                return;
            }
            
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
            // First, test basic connection with a simple query
            // Query chats where current user is either student or recruiter
            String encodedUserId = URLEncoder.encode(currentUserId, "UTF-8");
            
            // Build query - we'll fetch user data separately for simplicity
            // Supabase foreign key joins can be complex, so we'll do a simpler approach
            String url = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/chats?select=*"
                    + "&or=(student_id.eq." + encodedUserId + ",recruiter_id.eq." + encodedUserId + ")"
                    + "&order=timestamp.desc";
            
            android.util.Log.d("ChatFragment", "Loading chats from: " + url);
            android.util.Log.d("ChatFragment", "Current User ID: " + currentUserId);

            JsonArrayRequest request = new JsonArrayRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        try {
                            ArrayList<ChatModel> chatList = new ArrayList<>();

                            // First pass: collect all chat data and user IDs
                            ArrayList<ChatModel> tempChatList = new ArrayList<>();
                            ArrayList<String> studentIds = new ArrayList<>();
                            ArrayList<String> recruiterIds = new ArrayList<>();
                            
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);

                                String chatId = obj.optString("chat_id", "");
                                String jobId = obj.optString("job_id", "");
                                String studentId = obj.optString("student_id", "");
                                String recruiterId = obj.optString("recruiter_id", "");
                                String jobTitle = obj.optString("job_title", "");
                                String lastMessage = obj.optString("last_message", "");
                                String timestamp = obj.optString("timestamp", "");

                                ChatModel chatModel = new ChatModel(
                                        chatId,
                                        jobId,
                                        studentId,
                                        recruiterId,
                                        jobTitle,
                                        lastMessage,
                                        timestamp
                                );
                                
                                tempChatList.add(chatModel);
                                
                                if (studentId != null && !studentId.isEmpty() && !studentIds.contains(studentId)) {
                                    studentIds.add(studentId);
                                }
                                if (recruiterId != null && !recruiterId.isEmpty() && !recruiterIds.contains(recruiterId)) {
                                    recruiterIds.add(recruiterId);
                                }
                            }
                            
                            // If no chats found, show example chats
                            if (tempChatList.isEmpty()) {
                                adapter.updateData(getExampleChats());
                            } else {
                                // Fetch user data and update chats
                                fetchUserDataAndUpdateChats(tempChatList, studentIds, recruiterIds);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error parsing chats. Showing examples...", Toast.LENGTH_SHORT).show();
                            // Show example chats on error
                            adapter.updateData(getExampleChats());
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        
                        // Log detailed error information
                        String errorMessage = "Failed to load chats";
                        if (error.networkResponse != null) {
                            errorMessage += " - Status: " + error.networkResponse.statusCode;
                            if (error.networkResponse.data != null) {
                                try {
                                    String errorBody = new String(error.networkResponse.data, "UTF-8");
                                    errorMessage += " - " + errorBody;
                                    android.util.Log.e("ChatFragment", "Supabase Error: " + errorBody);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            errorMessage += " - Network error: " + error.getMessage();
                            android.util.Log.e("ChatFragment", "Network Error: " + error.getMessage());
                        }
                        
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        // Show example chats when loading fails
                        adapter.updateData(getExampleChats());
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
            Toast.makeText(getContext(), "Error loading chats. Showing examples...", Toast.LENGTH_SHORT).show();
            // Show example chats on exception
            adapter.updateData(getExampleChats());
        }
    }

    /**
     * Creates example/mock chats for demonstration purposes
     * These will be shown when database query fails or returns empty
     */
    private ArrayList<ChatModel> getExampleChats() {
        ArrayList<ChatModel> exampleChats = new ArrayList<>();
        
        // Example Chat 1: Software Developer Position
        exampleChats.add(new ChatModel(
                "example-chat-1",
                "example-job-1",
                "example-student-1",
                currentUserId != null ? currentUserId : "example-recruiter-1",
                "Software Developer - Full Stack",
                "Hi! I'm very interested in this position. I have 3 years of experience...",
                "2025-01-15T14:30:00"
        ));
        
        // Example Chat 2: Marketing Intern
        exampleChats.add(new ChatModel(
                "example-chat-2",
                "example-job-2",
                "example-student-2",
                currentUserId != null ? currentUserId : "example-recruiter-2",
                "Marketing Intern - Summer 2025",
                "Thank you for considering my application. I'm excited about...",
                "2025-01-14T10:15:00"
        ));
        
        // Example Chat 3: Data Analyst
        exampleChats.add(new ChatModel(
                "example-chat-3",
                "example-job-3",
                currentUserId != null ? currentUserId : "example-student-3",
                "example-recruiter-3",
                "Junior Data Analyst Position",
                "I saw your job posting and I believe my skills match perfectly...",
                "2025-01-13T16:45:00"
        ));
        
        // Example Chat 4: UI/UX Designer
        exampleChats.add(new ChatModel(
                "example-chat-4",
                "example-job-4",
                "example-student-4",
                currentUserId != null ? currentUserId : "example-recruiter-4",
                "UI/UX Designer - Remote",
                "Hello! I'm a creative designer with a passion for user experience...",
                "2025-01-12T09:20:00"
        ));
        
        return exampleChats;
    }
    
    private void fetchUserDataAndUpdateChats(ArrayList<ChatModel> chatList, ArrayList<String> studentIds, ArrayList<String> recruiterIds) {
        // Create a map to store user data
        java.util.Map<String, UserInfo> userInfoMap = new java.util.HashMap<>();
        
        // Fetch all unique user IDs
        java.util.Set<String> allUserIds = new java.util.HashSet<>();
        allUserIds.addAll(studentIds);
        allUserIds.addAll(recruiterIds);
        
        if (allUserIds.isEmpty()) {
            // No users to fetch, just update adapter
            adapter.updateData(chatList);
            return;
        }
        
        // Build query to fetch all users at once
        try {
            StringBuilder userIdsQuery = new StringBuilder();
            for (String userId : allUserIds) {
                if (userIdsQuery.length() > 0) {
                    userIdsQuery.append(",");
                }
                userIdsQuery.append(URLEncoder.encode(userId, "UTF-8"));
            }
            
            String usersUrl = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/users?select=id,name,bio&id=in.(" + userIdsQuery.toString() + ")";
            
            JsonArrayRequest usersRequest = new JsonArrayRequest(
                    Request.Method.GET,
                    usersUrl,
                    null,
                    usersResponse -> {
                        try {
                            // Parse user data
                            for (int i = 0; i < usersResponse.length(); i++) {
                                JSONObject userObj = usersResponse.getJSONObject(i);
                                String userId = userObj.optString("id", "");
                                String name = userObj.optString("name", "");
                                String bio = userObj.optString("bio", "");
                                
                                userInfoMap.put(userId, new UserInfo(name, bio));
                            }
                            
                            // Update chat models with user data
                            for (ChatModel chat : chatList) {
                                UserInfo studentInfo = userInfoMap.get(chat.getStudentId());
                                if (studentInfo != null) {
                                    chat.setStudentName(studentInfo.name);
                                    chat.setStudentBio(studentInfo.bio);
                                } else {
                                    chat.setStudentName("Student");
                                    chat.setStudentBio("");
                                }
                                
                                UserInfo recruiterInfo = userInfoMap.get(chat.getRecruiterId());
                                if (recruiterInfo != null) {
                                    chat.setRecruiterName(recruiterInfo.name);
                                } else {
                                    chat.setRecruiterName("Recruiter");
                                }
                            }
                            
                            // Fetch unread counts for each chat
                            fetchUnreadCountsForChats(chatList);
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                            // On error, still show chats without user data
                            adapter.updateData(chatList);
                        }
                    },
                    error -> {
                        // On error, still show chats without user data
                        adapter.updateData(chatList);
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
            
            ApiClient.getRequestQueue(requireContext()).add(usersRequest);
            
        } catch (Exception e) {
            e.printStackTrace();
            // On error, still show chats without user data
            adapter.updateData(chatList);
        }
    }
    
    private void fetchUnreadCountsForChats(ArrayList<ChatModel> chatList) {
        if (chatList.isEmpty()) {
            adapter.updateData(chatList);
            return;
        }
        
        SharedPreferences prefs = requireActivity().getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        final int[] completedCount = {0};
        final int totalChats = chatList.size();
        
        for (ChatModel chat : chatList) {
            String chatId = chat.getChatId();
            String studentId = chat.getStudentId();
            String recruiterId = chat.getRecruiterId();
            String chatTimestamp = chat.getTimestamp(); // Last message timestamp from chat
            
            // Determine the other user (sender of messages we haven't read)
            String otherUserId = currentUserId.equals(studentId) ? recruiterId : studentId;
            
            // Get last viewed timestamp for this chat
            String lastViewedKey = "last_viewed_" + chatId;
            String lastViewedTime = prefs.getString(lastViewedKey, "");
            
            // If we've never viewed this chat, or if the chat's last message is newer than our last viewed time
            // then we have unread messages
            if (lastViewedTime == null || lastViewedTime.isEmpty()) {
                // Never viewed - check if there are any messages from the other user
                countUnreadMessagesForChat(chat, chatId, otherUserId, "", completedCount, totalChats, chatList);
            } else if (chatTimestamp != null && !chatTimestamp.isEmpty()) {
                // Compare chat's last message timestamp with our last viewed time
                // If chat timestamp is newer, we have unread messages
                if (chatTimestamp.compareTo(lastViewedTime) > 0) {
                    // Chat has new messages - count unread from other user after last viewed time
                    countUnreadMessagesForChat(chat, chatId, otherUserId, lastViewedTime, completedCount, totalChats, chatList);
                } else {
                    // No new messages - unread count is 0
                    chat.setUnreadCount(0);
                    completedCount[0]++;
                    if (completedCount[0] == totalChats) {
                        adapter.updateData(chatList);
                    }
                }
            } else {
                // Fallback: count messages from other user after last viewed time
                countUnreadMessagesForChat(chat, chatId, otherUserId, lastViewedTime, completedCount, totalChats, chatList);
            }
        }
    }
    
    private void countUnreadMessagesForChat(ChatModel chat, String chatId, String senderId, String lastViewedTime,
                                            int[] completedCount, int totalChats, ArrayList<ChatModel> chatList) {
        try {
            String encodedChatId = URLEncoder.encode(chatId, "UTF-8");
            String encodedSenderId = URLEncoder.encode(senderId, "UTF-8");
            
            StringBuilder url = new StringBuilder(SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/messages?select=message_id&chat_id=eq." + encodedChatId
                    + "&sender_id=eq." + encodedSenderId);
            
            // If we have a last viewed time, only count messages after that
            if (lastViewedTime != null && !lastViewedTime.isEmpty()) {
                url.append("&timestamp=gt.").append(URLEncoder.encode(lastViewedTime, "UTF-8"));
            }
            
            JsonArrayRequest request = new JsonArrayRequest(
                    Request.Method.GET,
                    url.toString(),
                    null,
                    response -> {
                        chat.setUnreadCount(response.length());
                        completedCount[0]++;
                        
                        // When all chats are processed, update adapter
                        if (completedCount[0] == totalChats) {
                            adapter.updateData(chatList);
                        }
                    },
                    error -> {
                        chat.setUnreadCount(0);
                        completedCount[0]++;
                        
                        if (completedCount[0] == totalChats) {
                            adapter.updateData(chatList);
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
            
            ApiClient.getRequestQueue(requireContext()).add(request);
            
        } catch (Exception e) {
            e.printStackTrace();
            chat.setUnreadCount(0);
            completedCount[0]++;
            
            if (completedCount[0] == totalChats) {
                adapter.updateData(chatList);
            }
        }
    }
    
    // Helper class to store user info
    private static class UserInfo {
        String name;
        String bio;
        
        UserInfo(String name, String bio) {
            this.name = name;
            this.bio = bio;
        }
    }
}
