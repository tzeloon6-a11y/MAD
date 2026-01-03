package com.example.mad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SavedJobsFragment extends Fragment {
    private RecyclerView recyclerView;
    private SavedJobAdapter adapter;
    private List<Job> savedJobsList;
    private TextView emptyView;
    private String currentUserId;
    private String studentName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_jobs, container, false);

        // Get User Info
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);
        studentName = prefs.getString("name", "Student");

        recyclerView = view.findViewById(R.id.saved_jobs_recyclerview);
        emptyView = view.findViewById(R.id.empty_view_saved_jobs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        savedJobsList = new ArrayList<>();
        adapter = new SavedJobAdapter(savedJobsList);
        recyclerView.setAdapter(adapter);

        // Handle Button Clicks from the Saved Card
        adapter.setOnButtonClickListener(new SavedJobAdapter.OnButtonClickListener() {
            @Override
            public void onInterestedClicked(int position) {
                // Logic: Apply (Pitch) -> Remove from Saved
                if (position >= 0 && position < savedJobsList.size()) {
                    Job job = savedJobsList.get(position);
                    showPitchDialog(job, position);
                }
            }

            @Override
            public void onNotNowClicked(int position) {
                // Logic: Remove from DB -> Remove from Screen
                if (position >= 0 && position < savedJobsList.size()) {
                    Job job = savedJobsList.get(position);
                    deleteSavedJobFromSupabase(job, position);
                }
            }
        });

        loadSavedJobsFromSupabase();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedJobsFromSupabase();
    }

    // ==========================================
    // 1. FETCH SAVED JOBS FROM SUPABASE
    // ==========================================
    private void loadSavedJobsFromSupabase() {
        if (currentUserId == null) return;

        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/saved_jobs?select=*&user_id=eq." + currentUserId;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        savedJobsList.clear();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            // 1. Get the IDs
                            String jobId = obj.optString("job_id");
                            String recruiterId = obj.optString("recruiter_id", ""); // ✅ CRITICAL FIX

                            // 2. Get Details (Use defaults if missing)
                            String title = obj.optString("job_title", "Job");
                            String company = obj.optString("company_name", "Company");
                            String wage = obj.optString("wage", "Negotiable");
                            String location = obj.optString("location", "On Campus");
                            String desc = obj.optString("description", "See details...");

                            // 3. Create Job Object (Now with correct recruiterId!)
                            Job job = new Job(jobId, title, company, wage, location, desc, recruiterId);

                            savedJobsList.add(job);
                        }
                        adapter.notifyDataSetChanged();
                        checkIfEmpty();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(getContext(), "Failed to load saved jobs", Toast.LENGTH_SHORT).show()
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

    // ==========================================
    // 2. APPLY LOGIC (Pitch Dialog)
    // ==========================================
    private void showPitchDialog(Job job, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Apply to " + job.getCompanyName());
        builder.setMessage("Send a message with your application:");

        final EditText input = new EditText(requireContext());
        input.setHint("I am interested because...");
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String pitch = input.getText().toString();
            submitApplication(job, pitch, position);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void submitApplication(Job job, String pitch, int position) {
        // Validation
        if (job.getRecruiterId() == null || job.getRecruiterId().isEmpty()) {
            Toast.makeText(getContext(), "Error: Missing Recruiter Info", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/applications";
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    // Success: Remove from Saved list because we applied!
                    deleteSavedJobFromSupabase(job, position);
                    Toast.makeText(getContext(), "Application Sent!", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    String errorMsg = "Failed to apply";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg += ": " + new String(error.networkResponse.data, StandardCharsets.UTF_8);
                    }
                    android.util.Log.e("Supabase", errorMsg);
                    Toast.makeText(getContext(), "Error sending application", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                headers.put("Prefer", "return=minimal");
                return headers;
            }

            @Override
            public byte[] getBody() {
                try {
                    JSONObject json = new JSONObject();
                    json.put("student_id", currentUserId);
                    json.put("student_name", studentName);
                    json.put("recruiter_id", job.getRecruiterId());
                    json.put("job_id", job.getJobId());
                    json.put("status", "PENDING");
                    json.put("initial_message", pitch);
                    json.put("timestamp", timestamp);
                    return json.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return null;
                }
            }
        };

        ApiClient.getRequestQueue(requireContext()).add(request);
    }

    // ==========================================
    // 3. DELETE LOGIC (Remove from Saved)
    // ==========================================
    private void deleteSavedJobFromSupabase(Job job, int position) {
        // Delete from 'saved_jobs' where user_id = current and job_id = job.getId()
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/saved_jobs?user_id=eq." + currentUserId
                + "&job_id=eq." + job.getJobId();

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    // Remove from UI list
                    if (position >= 0 && position < savedJobsList.size()) {
                        savedJobsList.remove(position);
                        adapter.notifyItemRemoved(position);
                        checkIfEmpty();
                    }
                },
                error -> Toast.makeText(getContext(), "Error removing job", Toast.LENGTH_SHORT).show()
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

    private void checkIfEmpty() {
        if (savedJobsList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}