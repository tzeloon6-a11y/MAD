package com.example.mad;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
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
    
    // Helper class to sort jobs by created_at
    private static class JobWithDate {
        JobModel job;
        String createdAt;
        
        JobWithDate(JobModel job, String createdAt) {
            this.job = job;
            this.createdAt = createdAt;
        }
    }

    private RecyclerView recyclerView;
    private RecruiterJobAdapter adapter;
    private List<JobModel> jobList;
    private List<JobModel> filteredJobList; // Filtered jobs based on search
    private TextView tvEmptyState; // Optional: To show "No jobs found" text
    private EditText etSearch;
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Inflate the layout
        View view = inflater.inflate(R.layout.fragment_recruiter_home, container, false);

        // 2. Initialize RecyclerView
        recyclerView = view.findViewById(R.id.rvJobs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 3. Initialize Adapter
        jobList = new ArrayList<>();
        filteredJobList = new ArrayList<>();
        adapter = new RecruiterJobAdapter(filteredJobList);
        recyclerView.setAdapter(adapter);

        // 4. Setup Search
        etSearch = view.findViewById(R.id.et_search);
        ImageView ivSearchIcon = view.findViewById(R.id.iv_search_icon);
        setupSearch(ivSearchIcon);

        // 5. Fetch Data
        fetchJobsFromSupabase();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload data when coming back to this screen (e.g. after adding a post)
        fetchJobsFromSupabase();
    }

    private void fetchJobsFromSupabase() {
        // A. Get the current User ID from SharedPreferences
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        String currentUserId = prefs.getString("user_id", "user_001");

        // B. Construct the URL with ordering by created_at descending (newest first)
        String url = SupabaseConfig.SUPABASE_URL 
                + "/rest/v1/job_posts?select=*&user_id=eq." + currentUserId
                + "&order=created_at.desc";

        // C. Create the Request
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        jobList.clear();
                        try {
                            // Temporary list to store jobs with their created_at dates for sorting
                            List<JobWithDate> jobsWithDates = new ArrayList<>();
                            
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jobObject = response.getJSONObject(i);

                                // D. Safe Parsing (Matches HomeFragment logic)

                                // ID: Handle both Integer and String IDs safely
                                String id = String.valueOf(jobObject.optString("id"));
                                // Title
                                String title = jobObject.optString("title", "No Title");

                                // Description: Try 'description', fallback to 'content' if empty
                                String description = jobObject.optString("description",
                                        jobObject.optString("content", "No description available."));

                                // Recruiter ID (which is the user_id)
                                String recruiterId = jobObject.optString("user_id");
                                
                                // Get created_at for sorting
                                String createdAt = jobObject.optString("created_at", "");

                                // Add to temporary list with date
                                jobsWithDates.add(new JobWithDate(
                                        new JobModel(id, title, description, recruiterId),
                                        createdAt
                                ));
                            }
                            
                            // Sort by created_at descending (newest first) as fallback
                            jobsWithDates.sort((a, b) -> {
                                String dateA = a.createdAt;
                                String dateB = b.createdAt;
                                if (dateA == null || dateB == null) return 0;
                                // Compare dates (newest first = descending order)
                                return dateB.compareTo(dateA);
                            });
                            
                            // Extract sorted jobs
                            jobList.clear();
                            for (JobWithDate jobWithDate : jobsWithDates) {
                                jobList.add(jobWithDate.job);
                            }

                            // Apply search filter
                            applySearchFilter();
                            
                            // E. Refresh the List
                            adapter.notifyDataSetChanged();

                            // Optional: Show toast if empty
                            if (jobList.isEmpty()) {
                                Toast.makeText(getContext(), "You haven't posted any jobs yet.", Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Data Parsing Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getContext(), "Network Error. Check connection.", Toast.LENGTH_SHORT).show();
                        Log.e("Volley", error.toString());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                // Add Authorization Headers (Key)
                return ApiClient.getHeaders();
            }
        };

        // F. Add to Queue
        ApiClient.getRequestQueue(getContext()).add(request);
    }
    
    private void setupSearch(ImageView ivSearchIcon) {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                        requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                applySearchFilter();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        ivSearchIcon.setOnClickListener(v -> {
            // Clear search on icon click
            etSearch.setText("");
            currentSearchQuery = "";
            applySearchFilter();
        });
    }
    
    private void applySearchFilter() {
        filteredJobList.clear();
        
        if (currentSearchQuery.isEmpty()) {
            // No search query - show all jobs
            filteredJobList.addAll(jobList);
        } else {
            // Filter jobs by title or description
            for (JobModel job : jobList) {
                String title = job.getTitle().toLowerCase();
                String description = job.getDescription().toLowerCase();
                
                if (title.contains(currentSearchQuery) || description.contains(currentSearchQuery)) {
                    filteredJobList.add(job);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
    }
}