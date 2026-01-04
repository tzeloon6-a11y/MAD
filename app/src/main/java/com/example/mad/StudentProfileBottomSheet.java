package com.example.mad;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class StudentProfileBottomSheet extends BottomSheetDialogFragment {

    private String studentId;
    private TextView tvProfileName, tvProfileEmail, tvProfilePhone, tvProfileBio;
    private Button btnViewResume;

    public static StudentProfileBottomSheet newInstance(String studentId) {
        StudentProfileBottomSheet fragment = new StudentProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString("studentId", studentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentId = getArguments().getString("studentId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_student_profile, container, false);

        // Initialize views
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        tvProfilePhone = view.findViewById(R.id.tv_profile_phone);
        tvProfileBio = view.findViewById(R.id.tv_profile_bio);
        btnViewResume = view.findViewById(R.id.btn_view_resume);

        // Load student profile data
        if (studentId != null && !studentId.isEmpty()) {
            loadStudentProfile(studentId);
        } else {
            Toast.makeText(getContext(), "Invalid student ID", Toast.LENGTH_SHORT).show();
            dismiss();
        }

        return view;
    }

    private void loadStudentProfile(String userId) {
        try {
            String encodedUserId = java.net.URLEncoder.encode(userId, "UTF-8");
            String url = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/users?select=id,name,email,phone,bio,resume_url&id=eq." + encodedUserId
                    + "&limit=1";

            JsonArrayRequest request = new JsonArrayRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        try {
                            if (response.length() > 0) {
                                JSONObject userObj = response.getJSONObject(0);

                                // Extract user data
                                String name = userObj.optString("name", "Unknown");
                                String email = userObj.optString("email", "Not provided");
                                String phone = userObj.optString("phone", "Not provided");
                                String bio = userObj.optString("bio", "No bio available");
                                String resumeUrl = userObj.optString("resume_url", "");

                                // Populate TextViews
                                tvProfileName.setText(name);
                                tvProfileEmail.setText(email);
                                tvProfilePhone.setText(phone);
                                tvProfileBio.setText(bio.isEmpty() ? "No bio available" : bio);

                                // Handle resume button
                                if (resumeUrl != null && !resumeUrl.isEmpty()) {
                                    btnViewResume.setVisibility(View.VISIBLE);
                                    btnViewResume.setOnClickListener(v -> viewResume(resumeUrl));
                                } else {
                                    btnViewResume.setVisibility(View.GONE);
                                }
                            } else {
                                Toast.makeText(getContext(), "Student profile not found", Toast.LENGTH_SHORT).show();
                                dismiss();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error parsing profile data", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(getContext(), "Failed to load student profile", Toast.LENGTH_SHORT).show();
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

            ApiClient.getRequestQueue(requireContext()).add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewResume(String resumeUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(resumeUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Verify that there's an app to handle this intent
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "No app available to view resume", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error opening resume", Toast.LENGTH_SHORT).show();
        }
    }
}

