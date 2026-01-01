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
        // 1. Construct the URL
        // We want to select ALL columns (*) where recruiter_id equals 123
        // Note: Change "jobs" if your table name is different (e.g., "job_posts")
        String url = ApiClient.BASE_URL + "jobs?select=*&recruiter_id=eq.123";

        // 2. Create the Request
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        jobList.clear();
                        try {
                            // 3. Parse the Data (Manually read the JSON)
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jobObject = response.getJSONObject(i);

                                // Get the text from the curly braces
                                // Make sure these match your Supabase column names exactly!
                                String id = String.valueOf(jobObject.getInt("id")); // or getString("id")
                                String title = jobObject.getString("title");
                                String description = jobObject.getString("description");
                                String recruiterId = jobObject.getString("recruiter_id");

                                // Add to our list
                                jobList.add(new JobModel(id, title, description, recruiterId));
                            }

                            // 4. Update the Screen
                            adapter.notifyDataSetChanged();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Parsing Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getContext(), "Network Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Volley", error.toString());
                    }
                }
        ) {
            // 5. Attach the Headers (API Key) from your teammate's file
            @Override
            public Map<String, String> getHeaders() {
                return ApiClient.getHeaders();
            }
        };

        // 6. Add the request to the queue (Send it!)
        ApiClient.getRequestQueue(getContext()).add(request);
    }
}