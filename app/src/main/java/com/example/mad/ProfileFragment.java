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
import java.util.HashMap;
import java.util.Map;

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

        // ✅ RecyclerView setup
        rvExperience.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExperiencePostAdapter(new ArrayList<>());
        rvExperience.setAdapter(adapter);

        // ✅ Load Experience from Supabase
        loadExperiencePostsFromSupabase();

        // ✅ Logout
        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    // ✅ ✅ ✅ LOAD EXPERIENCE POSTS FROM SUPABASE
    private void loadExperiencePostsFromSupabase() {

        SharedPreferences prefs = requireActivity().getSharedPreferences(
                LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);

        String userId = prefs.getString(LoginActivity.KEY_USER_ID, null);

        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query 'experience_posts' table
        String url = SupabaseConfig.SUPABASE_URL +
                "/rest/v1/experience_posts?user_id=eq." + userId + "&select=*";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        ArrayList<ExperiencePostItem> list = new ArrayList<>();

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            String id = obj.getString("id"); // ✅ CHANGED TO STRING (UUID)
                            String uid = obj.getString("user_id");
                            String title = obj.getString("title");
                            
                            // Read 'description' as per schema update in PostFragment
                            String desc = obj.optString("description", obj.optString("content", ""));
                            
                            String mediaUrl = obj.isNull("media_url") ? "" : obj.getString("media_url");
                            String mediaType = obj.isNull("media_type") ? "" : obj.getString("media_type");
                            String createdAt = obj.getString("created_at");

                            list.add(new ExperiencePostItem(
                                    id, uid, title, desc, mediaUrl, mediaType, createdAt
                            ));
                        }

                        adapter.updateData(list);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(getContext(), "Failed to load experience posts", Toast.LENGTH_SHORT).show();
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

        ApiClient.getRequestQueue(requireContext()).add(request);
    }
}
