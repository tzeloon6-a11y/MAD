package com.example.mad;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private RadioGroup rgRole;
    private Button btnRegister, btnUploadResume;
    private TextView tvGoLogin, tvResumeStatus;
    private Uri resumeUri;
    private String resumeUrl;
    private static final int PICK_RESUME_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_register);

        // Status bar - setStatusBarColor is still valid, only the flags were deprecated
        Window window = getWindow();
        window.setStatusBarColor(Color.BLACK);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(false);
        }

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_reg_email);
        etPassword = findViewById(R.id.et_reg_password);
        rgRole = findViewById(R.id.rg_role);
        btnRegister = findViewById(R.id.btn_register);
        btnUploadResume = findViewById(R.id.btn_upload_resume);
        tvGoLogin = findViewById(R.id.tv_go_login);
        tvResumeStatus = findViewById(R.id.tv_resume_status);

        rgRole.check(R.id.rb_student);

        btnRegister.setOnClickListener(v -> attemptRegister());
        
        btnUploadResume.setOnClickListener(v -> selectResumeFile());

        tvGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
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
        btnUploadResume.setEnabled(false);
        btnUploadResume.setText("Uploading...");
        tvResumeStatus.setVisibility(TextView.VISIBLE);
        tvResumeStatus.setText("Uploading resume...");
        
        // Generate a temporary user ID for file naming (will be replaced after registration)
        String tempUserId = "temp_" + System.currentTimeMillis();
        
        FileUploadHelper.uploadResume(this, resumeUri, tempUserId, new FileUploadHelper.UploadCallback() {
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
                    Toast.makeText(RegisterActivity.this, "Resume upload failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = rgRole.getCheckedRadioButtonId() == R.id.rb_recruiter
                ? "recruiter" : "student";

        btnRegister.setEnabled(false);
        btnRegister.setText("Registering...");

        new Thread(() -> {
            try {
                URL url = new URL(SupabaseConfig.SUPABASE_URL + "/rest/v1/users");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("name", name);
                json.put("email", email);
                json.put("password", password);
                json.put("role", role);
                
                // Add resume_url if it was uploaded
                if (resumeUrl != null && !resumeUrl.isEmpty()) {
                    json.put("resume_url", resumeUrl);
                    android.util.Log.d("RegisterActivity", "Including resume_url in registration: " + resumeUrl);
                } else {
                    android.util.Log.d("RegisterActivity", "No resume_url to include in registration");
                }

                String jsonString = json.toString();
                android.util.Log.d("RegisterActivity", "Registration JSON being sent: " + jsonString);
                
                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Register");

                    if (responseCode == 201) {
                        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_LONG).show();
                        saveLocally(name, email, role);
                        // Note: Resume URL is already saved in database during registration
                        // User will need to login to access their account
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Email already exists!", Toast.LENGTH_LONG).show();
                    }
                });

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Register");
                    Toast.makeText(this, "Network Error", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void saveLocally(String name, String email, String role) {
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LoginActivity.KEY_NAME, name);
        editor.putString(LoginActivity.KEY_EMAIL, email);
        editor.putString(LoginActivity.KEY_ROLE, role);
        editor.apply();
    }
}

