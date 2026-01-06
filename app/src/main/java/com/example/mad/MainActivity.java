package com.example.mad;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private String userRole;
    private String currentUserId;
    private Handler unreadCheckHandler;
    private Runnable unreadCheckRunnable;
    private static final long UNREAD_CHECK_INTERVAL = 3000; // Check every 3 seconds
    private int lastUnreadCount = 0;
    private View notificationBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        notificationBanner = findViewById(R.id.notification_banner);

        // 1. GET THE ROLE AND USER ID
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        userRole = prefs.getString(LoginActivity.KEY_ROLE, "student");
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);

        // ---------------------------------------------------------
        // ✅ NEW CODE: HIDE "SAVED" TAB IF USER IS A RECRUITER
        // ---------------------------------------------------------
        if (userRole.equalsIgnoreCase("recruiter")) {
            bottomNavigationView.getMenu().findItem(R.id.nav_saved).setVisible(false);
        }
        // ---------------------------------------------------------

        // 2. SET THE DEFAULT FRAGMENT
        if (savedInstanceState == null) {
            if (userRole.equalsIgnoreCase("recruiter")) {
                loadFragment(new RecruiterHomeFragment());
            } else {
                loadFragment(new StudentHomeFragment());
            }
        }

        // 3. HANDLE CLICKS (Paste the updated listener we wrote earlier)
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                if (userRole.equalsIgnoreCase("recruiter")) {
                    selectedFragment = new RecruiterHomeFragment();
                } else {
                    selectedFragment = new StudentHomeFragment();
                }
            }
            else if (id == R.id.nav_saved) {
                selectedFragment = new SavedJobsFragment();
            }
            else if (id == R.id.nav_chat) {
                selectedFragment = ChatFragment.newInstance();
            }
            else if (id == R.id.nav_post) {
                selectedFragment = PostFragment.newInstance();
            }
            else if (id == R.id.nav_profile) {
                selectedFragment = ProfileFragment.newInstance();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
        
        // Start checking for unread messages
        if (currentUserId != null) {
            startUnreadMessagePolling();
            checkUnreadMessages(); // Initial check
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh unread count when returning to MainActivity
        if (currentUserId != null) {
            checkUnreadMessages();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopUnreadMessagePolling();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopUnreadMessagePolling();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
    
    private void startUnreadMessagePolling() {
        unreadCheckHandler = new Handler(Looper.getMainLooper());
        unreadCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkUnreadMessages();
                unreadCheckHandler.postDelayed(this, UNREAD_CHECK_INTERVAL);
            }
        };
        unreadCheckHandler.postDelayed(unreadCheckRunnable, UNREAD_CHECK_INTERVAL);
    }
    
    private void stopUnreadMessagePolling() {
        if (unreadCheckHandler != null && unreadCheckRunnable != null) {
            unreadCheckHandler.removeCallbacks(unreadCheckRunnable);
        }
    }
    
    private void checkUnreadMessages() {
        if (currentUserId == null) return;
        
        try {
            // First, get all chats where current user is involved
            String encodedUserId = URLEncoder.encode(currentUserId, "UTF-8");
            String chatsUrl = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/chats?select=chat_id,student_id,recruiter_id"
                    + "&or=(student_id.eq." + encodedUserId + ",recruiter_id.eq." + encodedUserId + ")";
            
            JsonArrayRequest chatsRequest = new JsonArrayRequest(
                    Request.Method.GET,
                    chatsUrl,
                    null,
                    response -> {
                        try {
                            if (response.length() == 0) {
                                updateChatBadge(0);
                                lastUnreadCount = 0;
                                return;
                            }
                            
                            SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
                            final int[] completedCount = {0};
                            final int[] totalUnread = {0};
                            final int totalChats = response.length();
                            
                            // For each chat, count unread messages asynchronously
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject chatObj = response.getJSONObject(i);
                                String chatId = chatObj.optString("chat_id", "");
                                String studentId = chatObj.optString("student_id", "");
                                String recruiterId = chatObj.optString("recruiter_id", "");
                                
                                // Determine the other user (sender of messages we haven't read)
                                String otherUserId = currentUserId.equals(studentId) ? recruiterId : studentId;
                                
                                // Get last viewed timestamp for this chat
                                String lastViewedKey = "last_viewed_" + chatId;
                                String lastViewedTime = prefs.getString(lastViewedKey, "");
                                
                                // Count unread messages asynchronously
                                countUnreadMessagesAsync(chatId, otherUserId, lastViewedTime, unreadCount -> {
                                    synchronized (totalUnread) {
                                        totalUnread[0] += unreadCount;
                                        completedCount[0]++;
                                        
                                        // When all chats are processed, update UI
                                        if (completedCount[0] == totalChats) {
                                            runOnUiThread(() -> {
                                                updateChatBadge(totalUnread[0]);
                                                
                                                // Show banner if new unread messages appeared
                                                if (totalUnread[0] > lastUnreadCount && totalUnread[0] > 0) {
                                                    showNotificationBanner(totalUnread[0]);
                                                }
                                                
                                                lastUnreadCount = totalUnread[0];
                                            });
                                        }
                                    }
                                });
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
                    return ApiClient.getHeaders();
                }
            };
            
            ApiClient.getRequestQueue(this).add(chatsRequest);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void countUnreadMessagesAsync(String chatId, String senderId, String lastViewedTime, UnreadCountCallback callback) {
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
                        callback.onCount(response.length());
                    },
                    error -> {
                        callback.onCount(0);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return ApiClient.getHeaders();
                }
            };
            
            ApiClient.getRequestQueue(this).add(request);
            
        } catch (Exception e) {
            e.printStackTrace();
            callback.onCount(0);
        }
    }
    
    private interface UnreadCountCallback {
        void onCount(int count);
    }
    
    private void updateChatBadge(int unreadCount) {
        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_chat);
        
        if (unreadCount > 0) {
            badge.setVisible(true);
            badge.setNumber(unreadCount);
        } else {
            badge.clearNumber();
            badge.setVisible(false);
        }
    }
    
    private void showNotificationBanner(int unreadCount) {
        if (notificationBanner != null) {
            String message = unreadCount == 1 
                    ? "You have 1 unread message" 
                    : "You have " + unreadCount + " unread messages";
            NotificationHelper.showNotificationBanner(notificationBanner, message);
        }
    }
}