package com.example.mad;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    public ProfileFragment() { }

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

        // Set initial data
        refreshProfileData();
        loadExperiencePostsFromSupabase();

        // Button Listeners
        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditProfileActivity.class));
        });

        btnLogout.setOnClickListener(v -> handleLogout());

        return view;
    }

    private void refreshProfileData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(
                LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);

        // Retrieve data using the same keys used in EditProfileActivity
        String name = prefs.getString(LoginActivity.KEY_NAME, "User");
        String role = prefs.getString(LoginActivity.KEY_ROLE, "student");
        String phone = prefs.getString("user_phone", "No Phone");
        String bio = prefs.getString("user_bio", "No bio added yet.");

        tvName.setText(name);
        tvRole.setText("Role: " + role);
        tvPhone.setText("Phone: " + phone);
        tvBio.setText(bio);

        // Student-specific UI
        if ("student".equals(role)) {
            String resume = prefs.getString("resume_path", null);
            tvResumeStatus.setText(resume == null ? "Resume: Not uploaded" : "Resume: Uploaded");
            tvResumeStatus.setVisibility(View.VISIBLE);
        } else {
            tvResumeStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // This ensures the page updates immediately after you finish editing
        refreshProfileData();
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

        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/experience_posts?user_id=eq." + userId + "&select=*";

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
                        adapter.updateData(list);
                    } catch (JSONException e) { e.printStackTrace(); }
                },
                error -> Toast.makeText(getContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() { return ApiClient.getHeaders(); }
        };
        ApiClient.getRequestQueue(requireContext()).add(request);
    }
}