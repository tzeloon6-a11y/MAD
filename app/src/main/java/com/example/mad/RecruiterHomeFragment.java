package com.example.mad;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecruiterHomeFragment extends Fragment {

    RecyclerView recyclerView;
    RecruiterJobAdapter adapter;
    List<JobModel> jobList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recruiter_home, container, false);

        recyclerView = view.findViewById(R.id.rvJobs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        jobList = new ArrayList<>();
        adapter = new RecruiterJobAdapter(jobList);
        recyclerView.setAdapter(adapter);

        // Fetch data using the Volley method
        fetchJobsFromSupabase();

        return view;
    }

    private void fetchJobsFromSupabase() {
        // 1. Get the current User ID from local storage (Saved by Ivan during Login)
        android.content.SharedPreferences prefs = requireActivity()
                .getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);

        // "user_id" is the key Ivan must use. "user_001" is a default backup for testing.
        String currentUserId = prefs.getString("user_id", "user_001");

        // 2. Construct the URL using the REAL ID
        // Note: ensure "jobs" matches your table name exactly
        String url = ApiClient.BASE_URL + "jobs?select=*&recruiter_id=eq." + currentUserId;

        // 3. Create the Request
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        jobList.clear();
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jobObject = response.getJSONObject(i);

                                // Make sure these match your Supabase column names!
                                String id = String.valueOf(jobObject.getInt("id"));
                                String title = jobObject.getString("title");
                                String description = jobObject.getString("description");
                                String recruiterId = jobObject.getString("recruiter_id");

                                jobList.add(new JobModel(id, title, description, recruiterId));
                            }
                            adapter.notifyDataSetChanged();

                            // Optional: Show a message if list is empty
                            if (jobList.isEmpty()) {
                                Toast.makeText(getContext(), "No jobs found.", Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Parsing Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
                        Log.e("Volley", error.toString());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return ApiClient.getHeaders();
            }
        };

        ApiClient.getRequestQueue(getContext()).add(request);
    }



}