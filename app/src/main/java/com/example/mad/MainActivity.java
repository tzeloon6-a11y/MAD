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
            // Count distinct chats that have at least one unread message
            // This gives us the number of CHATS with unread, not total messages
            String encodedReceiverId = URLEncoder.encode(currentUserId, "UTF-8");
            String url = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/messages?select=chat_id"
                    + "&receiver_id=eq." + encodedReceiverId
                    + "&is_read=eq.false";
            
            JsonArrayRequest request = new JsonArrayRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        try {
                            // Count distinct chat_ids
                            java.util.Set<String> unreadChatIds = new java.util.HashSet<>();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject msgObj = response.getJSONObject(i);
                                String chatId = msgObj.optString("chat_id", "");
                                if (chatId != null && !chatId.isEmpty()) {
                                    unreadChatIds.add(chatId);
                                }
                            }
                            
                            int unreadChatCount = unreadChatIds.size();
                            
                            runOnUiThread(() -> {
                                updateChatBadge(unreadChatCount);
                                
                                // Show banner if new unread chats appeared
                                if (unreadChatCount > lastUnreadCount && unreadChatCount > 0) {
                                    showNotificationBanner(unreadChatCount);
                                }
                                
                                lastUnreadCount = unreadChatCount;
                            });
                            
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
            
            ApiClient.getRequestQueue(this).add(request);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void updateChatBadge(int unreadChatCount) {
        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_chat);
        
        if (unreadChatCount > 0) {
            badge.setVisible(true);
            badge.setNumber(unreadChatCount);
        } else {
            badge.clearNumber();
            badge.setVisible(false);
        }
    }
    
    private void showNotificationBanner(int unreadChatCount) {
        if (notificationBanner != null) {
            String message = unreadChatCount == 1 
                    ? "You have 1 unread chat" 
                    : "You have " + unreadChatCount + " unread chats";
            NotificationHelper.showNotificationBanner(notificationBanner, message);
        }
    }
}