package com.example.mad;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private RecyclerView rvJobPosts;
    private JobPostAdapter adapter;

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvJobPosts = view.findViewById(R.id.rv_job_posts);
        rvJobPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new JobPostAdapter(new ArrayList<>());
        rvJobPosts.setAdapter(adapter);

        loadJobPostsFromSupabase();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadJobPostsFromSupabase();
    }

    // ✅ LOAD JOB POSTS FROM SUPABASE
    private void loadJobPostsFromSupabase() {

        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/job_posts?select=*"
                + "&order=created_at.desc";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        ArrayList<JobPostItem> list = new ArrayList<>();

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            int id = obj.optInt("id", 0); // optional if exists
                            String userId = obj.optString("user_id", ""); // ✅ UUID STRING
                            String title = obj.optString("title", "");
                            // Try "description" first (as saved by PostFragment), fallback to "content"
                            String desc = obj.optString("description", obj.optString("content", ""));
                            String mediaUrl = obj.isNull("media_url") ? "" : obj.getString("media_url");
                            String createdAt = obj.optString("created_at", "");

                            // Recruiter info (optional if not in table)
                            String recruiterName = obj.optString("recruiter_name", "");
                            String recruiterEmail = obj.optString("recruiter_email", "");

                            list.add(new JobPostItem(
                                    id,
                                    userId,         // ✅ STRING
                                    title,
                                    desc,
                                    mediaUrl,
                                    recruiterName,
                                    recruiterEmail,
                                    createdAt
                            ));
                        }

                        adapter.updateData(list);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(getContext(), "Failed to load job posts", Toast.LENGTH_SHORT).show();
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


