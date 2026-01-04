package com.example.mad;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ViewApplicantsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ApplicantAdapter adapter;
    List<ApplicationModel> applicantList;

    TextView tvHeader;
    ProgressBar progressBar;
    String jobId;
    String jobTitle;
    String recruiterId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_applicants);

        // 1. Initialize UI Components
        tvHeader = findViewById(R.id.tvHeader);
        recyclerView = findViewById(R.id.rvApplicants);
        progressBar = findViewById(R.id.progressBar);

        // 2. Get Data passed from the Recruiter Job List
        jobId = getIntent().getStringExtra("job_id");
        jobTitle = getIntent().getStringExtra("job_title");

        // Get current recruiter ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        recruiterId = prefs.getString(LoginActivity.KEY_USER_ID, null);

        if (recruiterId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set the header text
        if (jobTitle != null) {
            tvHeader.setText("Applicants: " + jobTitle);
        }

        // 3. Setup RecyclerView with ApplicantAdapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        applicantList = new ArrayList<>();
        adapter = new ApplicantAdapter(applicantList, application -> {
            // Handle "Start Chat" button click
            ApplicantAdapter.startChat(this, application, jobTitle, recruiterId);
        });
        recyclerView.setAdapter(adapter);

        // 4. Load Data
        if (jobId != null) {
            fetchApplicants();
        } else {
            Toast.makeText(this, "Error: No Job ID found", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchApplicants() {
        progressBar.setVisibility(View.VISIBLE);

        // Query applications table with all necessary fields
        // Note: We're fetching all fields directly from applications table since student_name is stored there
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/applications?job_id=eq." + jobId
                + "&select=*"
                + "&order=timestamp.desc";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    applicantList.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject appObj = response.getJSONObject(i);

                            // Extract all application fields
                            String applicationId = appObj.optString("application_id", "");
                            String studentId = appObj.optString("student_id", "");
                            String studentName = appObj.optString("student_name", "Unknown Student");
                            String appRecruiterId = appObj.optString("recruiter_id", "");
                            String appJobId = appObj.optString("job_id", "");
                            String status = appObj.optString("status", "PENDING");
                            String initialMessage = appObj.optString("initial_message", "");
                            String timestamp = appObj.optString("timestamp", "");

                            // Create ApplicationModel object
                            ApplicationModel application = new ApplicationModel(
                                    applicationId,
                                    studentId,
                                    studentName,
                                    appRecruiterId,
                                    appJobId,
                                    status,
                                    initialMessage,
                                    timestamp
                            );

                            applicantList.add(application);
                        }

                        // Update adapter with new data
                        adapter.updateData(applicantList);

                        if (applicantList.isEmpty()) {
                            Toast.makeText(this, "No applicants found for this job.", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing applicant data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    // Check specifically for 400 or 404
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        Toast.makeText(this, "Table 'applications' not found", Toast.LENGTH_LONG).show();
                    } else if (error.networkResponse != null && error.networkResponse.statusCode == 400) {
                        Toast.makeText(this, "Error loading applicants", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
                    }
                    Log.e("Volley", error.toString());
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                return ApiClient.getHeaders();
            }
        };

        ApiClient.getRequestQueue(this).add(request);
    }
}