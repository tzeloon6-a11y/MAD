package com.example.mad;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvRole, tvResumeStatus, tvPhone, tvBio;
    private RecyclerView rvExperience;
    private ExperiencePostAdapter adapter;
    private MaterialButton btnLogout, btnEditProfile;
    private String currentUserId;
    private String resumeUrl;

    // Use static factory method instead of public constructor
    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    private ProfileFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize all TextViews including new ones
        tvName = view.findViewById(R.id.tv_profile_name);
        tvRole = view.findViewById(R.id.tv_profile_role);
        tvPhone = view.findViewById(R.id.tv_profile_phone);
        tvBio = view.findViewById(R.id.tv_profile_bio);
        tvResumeStatus = view.findViewById(R.id.tv_resume_status);

        rvExperience = view.findViewById(R.id.rv_experience_posts);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        // Setup RecyclerView
        rvExperience.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExperiencePostAdapter(new ArrayList<>());
        rvExperience.setAdapter(adapter);

        // Get current user ID
        SharedPreferences prefs = requireActivity().getSharedPreferences(
                LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);

        // Set initial data
        refreshProfileData();
        loadExperiencePostsFromSupabase();

        // Button Listeners
        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditProfileActivity.class));
        });

        btnLogout.setOnClickListener(v -> handleLogout());

        // Make resume status clickable (will be enabled/disabled in updateResumeStatus)
        tvResumeStatus.setOnClickListener(v -> {
            if (resumeUrl != null && !resumeUrl.isEmpty()) {
                viewResume(resumeUrl);
            } else {
                Toast.makeText(getContext(), "No resume uploaded yet", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void refreshProfileData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(
                LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);

        // Retrieve basic data from SharedPreferences
        String name = prefs.getString(LoginActivity.KEY_NAME, "User");
        String role = prefs.getString(LoginActivity.KEY_ROLE, "student");
        
        // Set basic info immediately
        tvName.setText(name);
        tvRole.setText("Role: " + role);
        
        // Fetch latest profile data from database (phone, bio, resume_url)
        // This ensures we always have the most up-to-date information
        if (currentUserId != null && !currentUserId.isEmpty()) {
            fetchFullProfileFromDatabase();
        } else {
            // Fallback to SharedPreferences if no user ID
            String phone = prefs.getString("user_phone", "No Phone");
            String bio = prefs.getString("user_bio", "No bio added yet.");
            tvPhone.setText("Phone: " + phone);
            tvBio.setText(bio);
            
            // Student-specific UI
            if ("student".equals(role)) {
                resumeUrl = prefs.getString("user_resume_url", null);
                updateResumeStatus();
                tvResumeStatus.setVisibility(View.VISIBLE);
            } else {
                tvResumeStatus.setVisibility(View.GONE);
            }
        }
    }
    
    private void fetchFullProfileFromDatabase() {
        try {
            String encodedUserId = java.net.URLEncoder.encode(currentUserId, "UTF-8");
            String url = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/users?select=id,name,phone,bio,resume_url&id=eq." + encodedUserId
                    + "&limit=1";

            JsonArrayRequest request = new JsonArrayRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        try {
                            if (response.length() > 0) {
                                JSONObject userObj = response.getJSONObject(0);
                                
                                // Update name (in case it changed)
                                String name = userObj.optString("name", "");
                                if (!name.isEmpty()) {
                                    tvName.setText(name);
                                    // Update SharedPreferences
                                    SharedPreferences prefs = requireActivity().getSharedPreferences(
                                            LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
                                    prefs.edit().putString(LoginActivity.KEY_NAME, name).apply();
                                }
                                
                                // Get phone and bio
                                String phone = userObj.optString("phone", "");
                                String bio = userObj.optString("bio", "");
                                resumeUrl = userObj.optString("resume_url", "");
                                
                                // Update UI
                                tvPhone.setText("Phone: " + (phone.isEmpty() ? "Not set" : phone));
                                tvBio.setText(bio.isEmpty() ? "No bio added yet." : bio);
                                
                                // Update SharedPreferences for future use
                                SharedPreferences prefs = requireActivity().getSharedPreferences(
                                        LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("user_phone", phone);
                                editor.putString("user_bio", bio);
                                if (resumeUrl != null && !resumeUrl.isEmpty()) {
                                    editor.putString("user_resume_url", resumeUrl);
                                }
                                editor.apply();
                                
                                // Handle resume status for students
                                String role = prefs.getString(LoginActivity.KEY_ROLE, "student");
                                if ("student".equals(role)) {
                                    updateResumeStatus();
                                    tvResumeStatus.setVisibility(View.VISIBLE);
                                } else {
                                    tvResumeStatus.setVisibility(View.GONE);
                                }
                            } else {
                                // User not found - use SharedPreferences fallback
                                useSharedPreferencesFallback();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // On error, use SharedPreferences fallback
                            useSharedPreferencesFallback();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        // On error, use SharedPreferences fallback
                        useSharedPreferencesFallback();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return ApiClient.getHeaders();
                }
            };

            ApiClient.getRequestQueue(requireContext()).add(request);

        } catch (Exception e) {
            e.printStackTrace();
            useSharedPreferencesFallback();
        }
    }
    
    private void useSharedPreferencesFallback() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(
                LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String role = prefs.getString(LoginActivity.KEY_ROLE, "student");
        String phone = prefs.getString("user_phone", "No Phone");
        String bio = prefs.getString("user_bio", "No bio added yet.");
        
        tvPhone.setText("Phone: " + phone);
        tvBio.setText(bio);
        
        if ("student".equals(role)) {
            resumeUrl = prefs.getString("user_resume_url", null);
            updateResumeStatus();
            tvResumeStatus.setVisibility(View.VISIBLE);
        } else {
            tvResumeStatus.setVisibility(View.GONE);
        }
    }
    
    
    private void updateResumeStatus() {
        if (resumeUrl != null && !resumeUrl.isEmpty()) {
            tvResumeStatus.setText("Resume: Uploaded (Tap to view)");
            tvResumeStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
            tvResumeStatus.setClickable(true);
            tvResumeStatus.setFocusable(true);
        } else {
            tvResumeStatus.setText("Resume: Not uploaded");
            tvResumeStatus.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
            tvResumeStatus.setClickable(false);
            tvResumeStatus.setFocusable(false);
        }
    }
    
    private void viewResume(String resumeUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(resumeUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Verify that there's an app to handle this intent
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "No app available to view resume", Toast.LENGTH_SHORT).show();
            }
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No app available to view resume", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error opening resume", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // This ensures the page updates immediately after you finish editing
        refreshProfileData();
        // Reload experience posts to show latest posts
        loadExperiencePostsFromSupabase();
    }

    private void handleLogout() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(
                LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void loadExperiencePostsFromSupabase() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(
                LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(LoginActivity.KEY_USER_ID, null);
        if (userId == null) return;

        try {
            // URL encode userId to handle special characters
            String encodedUserId = java.net.URLEncoder.encode(userId, "UTF-8");
            
            // Order by created_at descending to show newest posts first
            String url = SupabaseConfig.SUPABASE_URL 
                    + "/rest/v1/experience_posts?user_id=eq." + encodedUserId 
                    + "&select=*"
                    + "&order=created_at.desc";

            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            ArrayList<ExperiencePostItem> list = new ArrayList<>();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);
                                list.add(new ExperiencePostItem(
                                        obj.getString("id"), obj.getString("user_id"),
                                        obj.getString("title"), obj.optString("description", ""),
                                        obj.optString("media_url", ""), obj.optString("media_type", ""),
                                        obj.getString("created_at")
                                ));
                            }
                            
                            // Fallback: Sort by created_at descending if API ordering didn't work
                            // This ensures newest posts are always on top
                            list.sort((a, b) -> {
                                String dateA = a.getCreatedAt();
                                String dateB = b.getCreatedAt();
                                if (dateA == null || dateB == null) return 0;
                                // Compare dates (newest first = descending order)
                                return dateB.compareTo(dateA);
                            });
                            
                            adapter.updateData(list);
                        } catch (JSONException e) { 
                            e.printStackTrace(); 
                            Toast.makeText(getContext(), "Error parsing posts", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(getContext(), "Failed to load posts", Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() { return ApiClient.getHeaders(); }
            };
            ApiClient.getRequestQueue(requireContext()).add(request);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error loading posts", Toast.LENGTH_SHORT).show();
        }
    }
}