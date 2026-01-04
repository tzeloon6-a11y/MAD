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
        adapter.setContext(this); // Set context for checking existing chats
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

                        // Fetch user personal information for all applicants
                        fetchUserPersonalInfo(applicantList);

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
    
    private void fetchUserPersonalInfo(List<ApplicationModel> applications) {
        if (applications == null || applications.isEmpty()) {
            adapter.updateData(applications);
            adapter.refreshChatStatuses();
            return;
        }
        
        // Collect all unique student IDs
        java.util.Set<String> studentIds = new java.util.HashSet<>();
        for (ApplicationModel app : applications) {
            if (app.getStudentId() != null && !app.getStudentId().isEmpty()) {
                studentIds.add(app.getStudentId());
            }
        }
        
        if (studentIds.isEmpty()) {
            adapter.updateData(applications);
            adapter.refreshChatStatuses();
            return;
        }
        
        // Build query to fetch all user data at once
        try {
            StringBuilder userIdsQuery = new StringBuilder();
            for (String userId : studentIds) {
                if (userIdsQuery.length() > 0) {
                    userIdsQuery.append(",");
                }
                userIdsQuery.append(java.net.URLEncoder.encode(userId, "UTF-8"));
            }
            
            String usersUrl = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/users?select=id,name,email,phone,bio,resume_url&id=in.(" + userIdsQuery.toString() + ")";
            
            JsonArrayRequest usersRequest = new JsonArrayRequest(
                    Request.Method.GET,
                    usersUrl,
                    null,
                    response -> {
                        try {
                            // Create map of user data
                            java.util.Map<String, UserPersonalInfo> userInfoMap = new java.util.HashMap<>();
                            
                            for (int i = 0; i < response.length(); i++) {
                                org.json.JSONObject userObj = response.getJSONObject(i);
                                String userId = userObj.optString("id", "");
                                String email = userObj.optString("email", "");
                                String phone = userObj.optString("phone", "");
                                String bio = userObj.optString("bio", "");
                                String resumeUrl = userObj.optString("resume_url", "");
                                
                                userInfoMap.put(userId, new UserPersonalInfo(email, phone, bio, resumeUrl));
                            }
                            
                            // Update applications with user personal info
                            for (ApplicationModel app : applications) {
                                UserPersonalInfo info = userInfoMap.get(app.getStudentId());
                                if (info != null) {
                                    app.setStudentEmail(info.email);
                                    app.setStudentPhone(info.phone);
                                    app.setStudentBio(info.bio);
                                    app.setStudentResumeUrl(info.resumeUrl);
                                }
                            }
                            
                            // Update adapter with complete data
                            adapter.updateData(applications);
                            adapter.refreshChatStatuses();
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                            // On error, still show applicants without personal info
                            adapter.updateData(applications);
                            adapter.refreshChatStatuses();
                        }
                    },
                    error -> {
                        // On error, still show applicants without personal info
                        adapter.updateData(applications);
                        adapter.refreshChatStatuses();
                    }
            ) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    return ApiClient.getHeaders();
                }
            };
            
            ApiClient.getRequestQueue(this).add(usersRequest);
            
        } catch (Exception e) {
            e.printStackTrace();
            // On error, still show applicants without personal info
            adapter.updateData(applications);
            adapter.refreshChatStatuses();
        }
    }
    
    // Helper class to store user personal info
    private static class UserPersonalInfo {
        String email;
        String phone;
        String bio;
        String resumeUrl;
        
        UserPersonalInfo(String email, String phone, String bio, String resumeUrl) {
            this.email = email;
            this.phone = phone;
            this.bio = bio;
            this.resumeUrl = resumeUrl;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh chat statuses when returning to this activity (e.g., after creating a chat)
        if (adapter != null) {
            adapter.refreshChatStatuses();
        }
    }
}