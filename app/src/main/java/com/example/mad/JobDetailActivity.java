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

        // Query Applications collection for existing application
        String checkUrl = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/applications?student_id=eq." + currentUserId
                + "&job_id=eq." + currentJobId
                + "&select=application_id"
                + "&limit=1";

        JsonArrayRequest checkRequest = new JsonArrayRequest(
                Request.Method.GET,
                checkUrl,
                null,
                response -> {
                    try {
                        // If response has any items, application already exists
                        if (response.length() > 0) {
                            // Duplicate found - show message and disable button
                            Toast.makeText(this, "You have already applied to this job", Toast.LENGTH_SHORT).show();
                            btnApply.setText("Applied");
                            btnApply.setEnabled(false);
                            hasApplied = true;
                        } else {
                            // No duplicate - proceed with dialog
                            onNoDuplicate.run();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error checking application status", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Failed to check application status", Toast.LENGTH_SHORT).show();
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

    private void checkIfAlreadyApplied() {
        // Check on activity start to set button state correctly
        if (currentJobId == null || currentUserId == null) {
            return;
        }

        String checkUrl = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/applications?student_id=eq." + currentUserId
                + "&job_id=eq." + currentJobId
                + "&select=application_id"
                + "&limit=1";

        JsonArrayRequest checkRequest = new JsonArrayRequest(
                Request.Method.GET,
                checkUrl,
                null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            // Already applied
                            btnApply.setText("Applied");
                            btnApply.setEnabled(false);
                            hasApplied = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Silent fail on initial check
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

