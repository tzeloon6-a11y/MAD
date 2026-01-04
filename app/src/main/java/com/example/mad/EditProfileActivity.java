package com.example.mad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etEditName, etEditPhone, etEditBio;
    private Button btnSaveProfile, btnUploadResume;
    private TextView tvResumeStatus;
    private String currentUserId;
    private Uri resumeUri;
    private String resumeUrl;
    private static final int PICK_RESUME_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etEditName = findViewById(R.id.et_edit_name);
        etEditPhone = findViewById(R.id.et_edit_phone);
        etEditBio = findViewById(R.id.et_edit_bio);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        btnUploadResume = findViewById(R.id.btn_upload_resume);
        tvResumeStatus = findViewById(R.id.tv_resume_status);

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);

        // Pre-fill fields with current data
        etEditName.setText(prefs.getString(LoginActivity.KEY_NAME, ""));
        etEditPhone.setText(prefs.getString("user_phone", ""));
        etEditBio.setText(prefs.getString("user_bio", ""));
        resumeUrl = prefs.getString("user_resume_url", null);
        
        // Update resume status if already uploaded
        if (resumeUrl != null && !resumeUrl.isEmpty()) {
            tvResumeStatus.setVisibility(TextView.VISIBLE);
            tvResumeStatus.setText("Resume already uploaded");
            tvResumeStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
            btnUploadResume.setText("Change Resume");
        }

        btnSaveProfile.setOnClickListener(v -> updateFullProfile());
        btnUploadResume.setOnClickListener(v -> selectResumeFile());
    }
    
    private void selectResumeFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        // Specify MIME types
        String[] mimeTypes = {"application/pdf", "image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        startActivityForResult(Intent.createChooser(intent, "Select Resume"), PICK_RESUME_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_RESUME_REQUEST && resultCode == RESULT_OK && data != null) {
            resumeUri = data.getData();
            if (resumeUri != null) {
                // Additional validation: Check MIME type again after selection
                String mimeType = getContentResolver().getType(resumeUri);
                if (mimeType != null && 
                    !mimeType.equals("application/pdf") && 
                    !mimeType.equals("image/jpeg") && 
                    !mimeType.equals("image/png")) {
                    Toast.makeText(this, "Invalid file type. Please select a PDF, JPEG, or PNG file.", Toast.LENGTH_LONG).show();
                    return;
                }
                // Upload file
                uploadResume();
            }
        }
    }
    
    private void uploadResume() {
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnUploadResume.setEnabled(false);
        btnUploadResume.setText("Uploading...");
        tvResumeStatus.setVisibility(TextView.VISIBLE);
        tvResumeStatus.setText("Uploading resume...");
        
        FileUploadHelper.uploadResume(this, resumeUri, currentUserId, new FileUploadHelper.UploadCallback() {
            @Override
            public void onSuccess(String fileUrl) {
                runOnUiThread(() -> {
                    resumeUrl = fileUrl;
                    btnUploadResume.setEnabled(true);
                    btnUploadResume.setText("Resume Uploaded ✓");
                    tvResumeStatus.setText("Resume uploaded successfully!");
                    tvResumeStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnUploadResume.setEnabled(true);
                    btnUploadResume.setText("Upload Resume (PDF, JPEG, PNG)");
                    tvResumeStatus.setText("Upload failed: " + error);
                    tvResumeStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
                    Toast.makeText(EditProfileActivity.this, "Resume upload failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateFullProfile() {
        String name = etEditName.getText().toString().trim();
        String phone = etEditPhone.getText().toString().trim();
        String bio = etEditBio.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/users?id=eq." + currentUserId;

        StringRequest request = new StringRequest(
                Request.Method.PATCH,
                url,
                response -> {
                    SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(LoginActivity.KEY_NAME, name);
                    editor.putString("user_phone", phone);
                    editor.putString("user_bio", bio);
                    if (resumeUrl != null && !resumeUrl.isEmpty()) {
                        editor.putString("user_resume_url", resumeUrl);
                    }
                    editor.apply();

                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    // LOG THE ERROR DETAILS
                    String errorBody = "";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                    }
                    Log.e("EditProfile", "Update Failed: " + error.toString() + " | Body: " + errorBody);
                    Toast.makeText(this, "Update failed! Check if columns 'phone' and 'bio' exist in Supabase.", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = ApiClient.getHeaders();
                headers.put("Prefer", "return=minimal"); // Required for PATCH in Supabase
                return headers;
            }

            @Override
            public byte[] getBody() {
                try {
                    JSONObject json = new JSONObject();
                    json.put("name", name);
                    json.put("phone", phone);
                    json.put("bio", bio);
                    if (resumeUrl != null && !resumeUrl.isEmpty()) {
                        json.put("resume_url", resumeUrl);
                    }
                    return json.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return null;
                }
            }
        };

        ApiClient.getRequestQueue(this).add(request);
    }
}
