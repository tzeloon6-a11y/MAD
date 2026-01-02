package com.example.mad;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewApplicantsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    JobApplicantAdapter adapter; // ✅ Uses your renamed Adapter
    List<Map<String, String>> applicantList;

    TextView tvHeader;
    ProgressBar progressBar;
    String jobId;

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
        String jobTitle = getIntent().getStringExtra("job_title");

        // Set the header text
        if (jobTitle != null) {
            tvHeader.setText("Applicants: " + jobTitle);
        }

        // 3. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        applicantList = new ArrayList<>();
        adapter = new JobApplicantAdapter(applicantList);
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

        // ⚠️ QUERY EXPLANATION:
        // 1. Look in 'applications' table.
        // 2. Filter where job_id equals our ID.
        // 3. JOIN with 'users' table (users(*)) to get name/email.
        String url = ApiClient.BASE_URL + "applications?select=*,users(*)&job_id=eq." + jobId;

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

                            // 1. Get Application Status
                            String status = appObj.optString("status", "Applied");

                            // 2. Extract Student Details from the joined 'users' object
                            // NOTE: "users" must match the name of your Foreign Key relation in Supabase
                            if (!appObj.isNull("users")) {
                                JSONObject userObj = appObj.getJSONObject("users");

                                String name = userObj.optString("name", "Unknown Student");
                                String email = userObj.optString("email", "-");

                                // 3. Add to List
                                Map<String, String> student = new HashMap<>();
                                student.put("name", name);
                                student.put("email", email);
                                student.put("status", status);

                                applicantList.add(student);
                            }
                        }

                        adapter.notifyDataSetChanged();

                        if (applicantList.isEmpty()) {
                            Toast.makeText(this, "No applicants found for this job.", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing student data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    // Check specifically for 400 or 404
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        Toast.makeText(this, "Table 'applications' not found", Toast.LENGTH_LONG).show();
                    } else if (error.networkResponse != null && error.networkResponse.statusCode == 400) {
                        Toast.makeText(this, "Relationship error (Foreign Key missing)", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
                    }
                    Log.e("Volley", error.toString());
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return ApiClient.getHeaders();
            }
        };

        ApiClient.getRequestQueue(this).add(request);
    }
}