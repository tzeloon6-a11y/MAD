package com.example.mad;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class JobDetailActivity extends AppCompatActivity {

    private TextView tvJobTitle, tvJobDescription, tvRecruiterName, tvApplicantsHeader;
    private Button btnApply;
    private RecyclerView recyclerApplicants;
    private ApplicantAdapter applicantAdapter;
    
    private String currentJobId;
    private String currentUserId;
    private String recruiterId;
    private String jobTitle;
    private String userRole;
    private boolean hasApplied = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        // Get job details from Intent
        currentJobId = getIntent().getStringExtra("jobId");
        jobTitle = getIntent().getStringExtra("jobTitle");
        String jobDescription = getIntent().getStringExtra("jobDescription");
        recruiterId = getIntent().getStringExtra("recruiterId");
        String recruiterName = getIntent().getStringExtra("recruiterName");

        // Get current user ID
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);
        userRole = prefs.getString(LoginActivity.KEY_ROLE, null);

        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        tvJobTitle = findViewById(R.id.tv_job_title);
        tvJobDescription = findViewById(R.id.tv_job_description);
        tvRecruiterName = findViewById(R.id.tv_recruiter_name);
        tvApplicantsHeader = findViewById(R.id.tv_applicants_header);
        btnApply = findViewById(R.id.btn_apply);
        recyclerApplicants = findViewById(R.id.recycler_applicants);

        // Set job details
        if (jobTitle != null) tvJobTitle.setText(jobTitle);
        if (jobDescription != null) tvJobDescription.setText(jobDescription);
        if (recruiterName != null) tvRecruiterName.setText("Posted by: " + recruiterName);

        // Role-based UI setup
        if ("student".equalsIgnoreCase(userRole)) {
            // Student view: Show apply button, hide applicants
            btnApply.setVisibility(View.VISIBLE);
            tvApplicantsHeader.setVisibility(View.GONE);
            recyclerApplicants.setVisibility(View.GONE);
            btnApply.setOnClickListener(v -> onApplyButtonClicked());
            checkIfAlreadyApplied();
        } else if ("recruiter".equalsIgnoreCase(userRole)) {
            // Recruiter view: Hide apply button, show applicants
            btnApply.setVisibility(View.GONE);
            tvApplicantsHeader.setVisibility(View.VISIBLE);
            recyclerApplicants.setVisibility(View.VISIBLE);
            
            // Setup RecyclerView for applicants
            recyclerApplicants.setLayoutManager(new LinearLayoutManager(this));
            applicantAdapter = new ApplicantAdapter(new ArrayList<>(), this::startChat);
            recyclerApplicants.setAdapter(applicantAdapter);
            
            // Load applicants
            loadApplicants();
        }
    }

    /**
     * Interface for application check callback
     */
    interface ApplicationCheckCallback {
        void onResult(boolean hasApplied, String error);
    }

    private void onApplyButtonClicked() {
        // Critical: Check for duplicate application BEFORE showing dialog
        checkForDuplicateApplication(() -> {
            // This callback runs if no duplicate exists
            showPitchDialog();
        });
    }

    private void checkForDuplicateApplication(Runnable onNoDuplicate) {
        if (currentJobId == null || currentUserId == null) {
            Toast.makeText(this, "Invalid job or user data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use snake_case column names: student_id and job_id
        checkIfApplied(currentUserId, currentJobId, (hasApplied, error) -> {
            if (error != null) {
                // Log error but allow user to proceed
                android.util.Log.e("JobDetailActivity", "Error checking application: " + error);
                Toast.makeText(this, "Could not verify application status. You can still apply.", Toast.LENGTH_SHORT).show();
                onNoDuplicate.run();
                return;
            }
            
            if (hasApplied) {
                // Already applied - disable button
                runOnUiThread(() -> {
                    Toast.makeText(this, "You have already applied to this job", Toast.LENGTH_SHORT).show();
                    btnApply.setText("Applied");
                    btnApply.setEnabled(false);
                    hasApplied = true;
                });
            } else {
                // No duplicate - proceed with dialog
                onNoDuplicate.run();
            }
        });
    }

    private void showPitchDialog() {
        // Create dialog with EditText for pitch
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Apply to Job");
        builder.setMessage("Enter a short message/pitch:");

        // Create EditText for pitch input
        final EditText input = new EditText(this);
        input.setHint("Why are you interested in this position?");
        input.setMinLines(3);
        input.setMaxLines(5);
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String pitch = input.getText().toString().trim();
            if (TextUtils.isEmpty(pitch)) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            } else {
                saveApplication(pitch);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveApplication(String initialMessage) {
        if (currentJobId == null || currentUserId == null || recruiterId == null) {
            Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get student name from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        String studentName = prefs.getString(LoginActivity.KEY_NAME, "");

        // Get current timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Save to Applications collection
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/applications";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    // Success - update UI
                    Toast.makeText(this, "Application submitted successfully!", Toast.LENGTH_SHORT).show();
                    btnApply.setText("Applied");
                    btnApply.setEnabled(false);
                    hasApplied = true;
                },
                error -> {
                    String responseBody = null;
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(this, "Failed to submit application", Toast.LENGTH_SHORT).show();
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
                    JSONObject applicationJson = new JSONObject();
                    applicationJson.put("student_id", currentUserId);
                    applicationJson.put("student_name", studentName); // ✅ Save student name directly
                    applicationJson.put("recruiter_id", recruiterId);
                    applicationJson.put("job_id", currentJobId);
                    applicationJson.put("status", "PENDING");
                    applicationJson.put("initial_message", initialMessage);
                    applicationJson.put("timestamp", timestamp);
                    return applicationJson.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        ApiClient.getRequestQueue(this).add(request);
    }

    /**
     * Check if user has already applied to this job (called on activity start)
     * Refactored to use checkIfApplied helper function
     */
    private void checkIfAlreadyApplied() {
        // Check on activity start to set button state correctly
        if (currentJobId == null || currentUserId == null) {
            return;
        }

        // Use snake_case column names: student_id and job_id
        checkIfApplied(currentUserId, currentJobId, (hasApplied, error) -> {
            if (error != null) {
                // Log error but don't show toast on initial check
                android.util.Log.d("JobDetailActivity", "Initial check failed: " + error);
                return;
            }
            
            if (hasApplied) {
                // Already applied - update UI
                runOnUiThread(() -> {
                    btnApply.setText("Applied");
                    btnApply.setEnabled(false);
                    hasApplied = true;
                });
            }
        });
    }

    /**
     * Helper function to check if application exists
     * Uses snake_case column names: student_id and job_id
     * 
     * CRITICAL FIXES:
     * - Uses snake_case for column names: student_id and job_id
     * - Handles response carefully: If data is not null and data.size() > 0, it means Already Applied
     * - If error is not null, logs the error message
     */
    private void checkIfApplied(String studentId, String jobId, ApplicationCheckCallback callback) {
        try {
            // URL encode the IDs to handle special characters
            String encodedUserId = java.net.URLEncoder.encode(studentId, "UTF-8");
            String encodedJobId = java.net.URLEncoder.encode(jobId, "UTF-8");
            
            // Query Applications collection using snake_case column names
            // CRITICAL: Use snake_case - student_id and job_id
            String checkUrl = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/applications?student_id=eq." + encodedUserId
                    + "&job_id=eq." + encodedJobId
                    + "&select=application_id"
                    + "&limit=1";

            android.util.Log.d("JobDetailActivity", "Checking application: " + checkUrl);
            android.util.Log.d("JobDetailActivity", "student_id: " + studentId + ", job_id: " + jobId);

            JsonArrayRequest checkRequest = new JsonArrayRequest(
                    Request.Method.GET,
                    checkUrl,
                    null,
                    response -> {
                        try {
                            // CRITICAL: Handle response carefully
                            // If data is not null and data.size() > 0, it means Already Applied
                            if (response != null && response.length() > 0) {
                                // Application exists - user has already applied
                                android.util.Log.d("JobDetailActivity", "Application found - user already applied");
                                callback.onResult(true, null);
                            } else {
                                // No application found - user can apply
                                android.util.Log.d("JobDetailActivity", "No application found - user can apply");
                                callback.onResult(false, null);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            android.util.Log.e("JobDetailActivity", "Error parsing response", e);
                            // On parsing error, return error in callback
                            callback.onResult(false, "Error parsing response: " + e.getMessage());
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        
                        // CRITICAL: If error is not null, log the error message
                        String errorMessage = null;
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            errorMessage = "HTTP " + statusCode;
                            
                            if (error.networkResponse.data != null) {
                                try {
                                    String errorBody = new String(error.networkResponse.data, java.nio.charset.StandardCharsets.UTF_8);
                                    errorMessage += ": " + errorBody;
                                    android.util.Log.e("JobDetailActivity", "Supabase Error Response: " + errorBody);
                                    
                                    // If table doesn't exist (404), provide specific message
                                    if (statusCode == 404) {
                                        errorMessage = "Applications table not found (404). Please create the table in Supabase.";
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            errorMessage = "Network error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error");
                            android.util.Log.e("JobDetailActivity", "Network Error: " + errorMessage);
                        }
                        
                        // Return error in callback
                        callback.onResult(false, errorMessage);
                    }
            ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        ApiClient.getRequestQueue(this).add(checkRequest);
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("JobDetailActivity", "Exception in checkIfApplied", e);
            callback.onResult(false, "Exception: " + e.getMessage());
        }
    }

    // Load applicants for recruiter view
    private void loadApplicants() {
        if (currentJobId == null) {
            return;
        }

        // Query Applications collection filtered by jobId and status PENDING
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/applications?job_id=eq." + currentJobId
                + "&status=eq.PENDING"
                + "&select=*"
                + "&order=timestamp.desc";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        ArrayList<ApplicationModel> applicantList = new ArrayList<>();

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            String applicationId = obj.optString("application_id", "");
                            String studentId = obj.optString("student_id", "");
                            String studentName = obj.optString("student_name", "");
                            String recruiterId = obj.optString("recruiter_id", "");
                            String jobId = obj.optString("job_id", "");
                            String status = obj.optString("status", "");
                            String initialMessage = obj.optString("initial_message", "");
                            String timestamp = obj.optString("timestamp", "");

                            applicantList.add(new ApplicationModel(
                                    applicationId,
                                    studentId,
                                    studentName,
                                    recruiterId,
                                    jobId,
                                    status,
                                    initialMessage,
                                    timestamp
                            ));
                        }

                        applicantAdapter.updateData(applicantList);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing applicants", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Failed to load applicants", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        ApiClient.getRequestQueue(this).add(request);
    }

    // Start Chat callback from adapter
    private void startChat(ApplicationModel application) {
        ApplicantAdapter.startChat(this, application, jobTitle, recruiterId != null ? recruiterId : currentUserId);
    }
}

