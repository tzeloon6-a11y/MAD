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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvRole, tvResumeStatus, tvPhone, tvBio;
    private RecyclerView rvExperience;
    private ExperiencePostAdapter adapter;
    private MaterialButton btnLogout, btnEditProfile;
    private String currentUserId;
    private String resumeUrl;

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    private ProfileFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = view.findViewById(R.id.tv_profile_name);
        tvRole = view.findViewById(R.id.tv_profile_role);
        tvPhone = view.findViewById(R.id.tv_profile_phone);
        tvBio = view.findViewById(R.id.tv_profile_bio);
        tvResumeStatus = view.findViewById(R.id.tv_resume_status);

        rvExperience = view.findViewById(R.id.rv_experience_posts);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        rvExperience.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExperiencePostAdapter(new ArrayList<>());
        rvExperience.setAdapter(adapter);

        SharedPreferences prefs = requireActivity().getSharedPreferences(
                LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);

        refreshProfileData();
        loadExperiencePostsFromSupabase();

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditProfileActivity.class));
        });

        btnLogout.setOnClickListener(v -> handleLogout());

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

        String name = prefs.getString(LoginActivity.KEY_NAME, "User");
        String role = prefs.getString(LoginActivity.KEY_ROLE, "student");
        
        tvName.setText(name);
        tvRole.setText("Role: " + role);
        
        if (currentUserId != null && !currentUserId.isEmpty()) {
            fetchFullProfileFromDatabase();
        } else {
            useSharedPreferencesFallback();
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
                                
                                String name = userObj.optString("name", "");
                                if (!name.isEmpty()) {
                                    tvName.setText(name);
                                    SharedPreferences prefs = requireActivity().getSharedPreferences(
                                            LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
                                    prefs.edit().putString(LoginActivity.KEY_NAME, name).apply();
                                }
                                
                                String phone = userObj.optString("phone", "");
                                String bio = userObj.optString("bio", "");
                                resumeUrl = userObj.optString("resume_url", "");
                                
                                tvPhone.setText("Phone: " + (phone.isEmpty() ? "Not set" : phone));
                                tvBio.setText(bio.isEmpty() ? "No bio added yet." : bio);
                                
                                SharedPreferences prefs = requireActivity().getSharedPreferences(
                                        LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("user_phone", phone);
                                editor.putString("user_bio", bio);
                                if (resumeUrl != null && !resumeUrl.isEmpty()) {
                                    editor.putString("user_resume_url", resumeUrl);
                                }
                                editor.apply();
                                
                                String role = prefs.getString(LoginActivity.KEY_ROLE, "student");
                                if ("student".equals(role)) {
                                    updateResumeStatus();
                                    tvResumeStatus.setVisibility(View.VISIBLE);
                                } else {
                                    tvResumeStatus.setVisibility(View.GONE);
                                }
                            } else {
                                useSharedPreferencesFallback();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            useSharedPreferencesFallback();
                        }
                    },
                    error -> {
                        error.printStackTrace();
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
    
    // ✅ MODIFIED: Switched to Google Docs Viewer
    private void viewResume(String resumeUrl) {
        try {
            String encodedUrl = URLEncoder.encode(resumeUrl, "UTF-8");
            String viewerUrl = "https://docs.google.com/gview?embedded=true&url=" + encodedUrl;
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(viewerUrl));
            startActivity(browserIntent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Could not open resume", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProfileData();
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
            String encodedUserId = java.net.URLEncoder.encode(userId, "UTF-8");
            String url = SupabaseConfig.SUPABASE_URL 
                    + "/rest/v1/experience_posts?user_id=eq." + encodedUserId 
                    + "&select=*&order=created_at.desc";

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
                            list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                            adapter.updateData(list);
                        } catch (JSONException e) { 
                            e.printStackTrace(); 
                            if(isAdded()) Toast.makeText(getContext(), "Error parsing posts", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        if(isAdded()) Toast.makeText(getContext(), "Failed to load posts", Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() { return ApiClient.getHeaders(); }
            };
            ApiClient.getRequestQueue(requireContext()).add(request);
        } catch (Exception e) {
            e.printStackTrace();
            if(isAdded()) Toast.makeText(getContext(), "Error loading posts", Toast.LENGTH_SHORT).show();
        }
    }
}
