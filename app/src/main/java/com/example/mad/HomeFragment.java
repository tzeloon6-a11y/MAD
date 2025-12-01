package com.example.mad;

import android.content.Context;
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
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private RecyclerView rvJobPosts;
    private JobPostAdapter adapter;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvJobPosts = view.findViewById(R.id.rv_job_posts);
        rvJobPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new JobPostAdapter(new ArrayList<>());
        rvJobPosts.setAdapter(adapter);

        loadJobPostsFromServer();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadJobPostsFromServer(); // reload each time returning to Home
    }

    private void loadJobPostsFromServer() {
        String url = ApiClient.BASE_URL + "get_job_posts.php";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        boolean success = response.optBoolean("success", false);
                        if (!success) {
                            String msg = response.optString("message", "Failed to load job posts");
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray data = response.getJSONArray("data");
                        ArrayList<JobPostItem> list = new ArrayList<>();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);

                            int id = obj.getInt("id");
                            int userId = obj.getInt("user_id");
                            String title = obj.getString("title");
                            String desc = obj.getString("description");
                            String mediaUrl = obj.isNull("media_url") ? "" : obj.getString("media_url");
                            String recruiterName = obj.optString("recruiter_name", "");
                            String recruiterEmail = obj.optString("recruiter_email", "");
                            String createdAt = obj.optString("created_at", "");

                            list.add(new JobPostItem(id, userId, title, desc, mediaUrl,
                                    recruiterName, recruiterEmail, createdAt));
                        }

                        adapter.updateData(list);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(getContext(), "Network error loading job posts", Toast.LENGTH_SHORT).show();
                }
        );

        ApiClient.getRequestQueue(requireContext()).add(request);
    }
}

