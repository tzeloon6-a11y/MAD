package com.example.mad;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvRole, tvResumeStatus;
    private RecyclerView rvExperience;
    private ExperiencePostAdapter adapter;
    private MaterialButton btnLogout;

    public ProfileFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = view.findViewById(R.id.tv_profile_name);
        tvRole = view.findViewById(R.id.tv_profile_role);
        tvResumeStatus = view.findViewById(R.id.tv_resume_status);
        rvExperience = view.findViewById(R.id.rv_experience_posts);
        btnLogout = view.findViewById(R.id.btn_logout);

        // Load user info
        SharedPreferences prefs = requireActivity().getSharedPreferences(
                LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String email = prefs.getString(LoginActivity.KEY_EMAIL, "No Email");
        String role = prefs.getString(LoginActivity.KEY_ROLE, "student");

        tvName.setText(email);
        tvRole.setText("Role: " + role);

        String resume = prefs.getString("resume_path", null);
        if ("student".equals(role)) {
            tvResumeStatus.setText(resume == null ? "Resume: Not uploaded" : "Resume: Uploaded");
            tvResumeStatus.setVisibility(View.VISIBLE);
        } else {
            tvResumeStatus.setVisibility(View.GONE);
        }

        // Setup RecyclerView
        rvExperience.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExperiencePostAdapter(new ArrayList<>());
        rvExperience.setAdapter(adapter);

        // Load posts from server
        loadExperiencePostsFromServer();

        // Logout logic
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();

                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                getActivity().finish();
            });
        }

        return view;
    }

    private void loadExperiencePostsFromServer() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(
                LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);

        int userId = prefs.getInt(LoginActivity.KEY_USER_ID, -1);

        if (userId == -1) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = ApiClient.BASE_URL + "get_experience_posts.php?user_id=" + userId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        boolean success = response.optBoolean("success", false);
                        if (!success) {
                            String msg = response.optString("message", "Failed to load posts");
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray data = response.getJSONArray("data");
                        ArrayList<ExperiencePostItem> list = new ArrayList<>();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            int id = obj.getInt("id");
                            int uid = obj.getInt("user_id");
                            String title = obj.getString("title");
                            String desc = obj.getString("description");
                            String mediaUrl = obj.isNull("media_url") ? "" : obj.getString("media_url");
                            String createdAt = obj.getString("created_at");

                            list.add(new ExperiencePostItem(id, uid, title, desc, mediaUrl, createdAt));
                        }

                        adapter.updateData(list);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
        );

        ApiClient.getRequestQueue(requireContext()).add(request);
    }
}
