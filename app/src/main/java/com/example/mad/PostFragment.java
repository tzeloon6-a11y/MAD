package com.example.mad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PostFragment extends Fragment {

    private TextView tvHeader;
    private RadioGroup rgPostType;
    private EditText etTitle, etContent;
    private Button btnSubmit, btnPickMedia;
    private ImageView ivPreview;
    private VideoView vvPreview;

    private Uri selectedMediaUri;
    private String selectedMediaType; // "image" or "video"

    public PostFragment() { /* Required empty constructor */ }

    // Launcher for picking media
    private final ActivityResultLauncher<Intent> mediaPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        handleMediaSelection(uri);
                    }
                }
            }
    );
    private void sendJobPostToServer(int userId, String title, String content, String mediaUriString) {
        String url = ApiClient.BASE_URL + "save_job_post.php";

        // For now, just send mediaUriString as media_url (later you can implement upload)
        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    Toast.makeText(getActivity(), "Job post created!", Toast.LENGTH_SHORT).show();
                    clearInputs();
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(getActivity(), "Failed to create job post", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("title", title);
                params.put("description", content);
                params.put("media_url", mediaUriString != null ? mediaUriString : "");
                return params;
            }
        };

        ApiClient.getRequestQueue(requireContext()).add(request);
    }

    private void sendExperiencePostToServer(int userId, String title, String content, String mediaUriString) {
        String url = ApiClient.BASE_URL + "save_experience_post.php";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    Toast.makeText(getActivity(), "Experience post created!", Toast.LENGTH_SHORT).show();
                    clearInputs();
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(getActivity(), "Failed to create experience post", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("title", title);
                params.put("description", content);
                params.put("media_url", mediaUriString != null ? mediaUriString : "");
                return params;
            }
        };

        ApiClient.getRequestQueue(requireContext()).add(request);
    }

    private void clearInputs() {
        etTitle.setText("");
        etContent.setText("");
        selectedMediaUri = null;
        selectedMediaType = null;
        ivPreview.setVisibility(View.GONE);
        vvPreview.setVisibility(View.GONE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post, container, false);

        tvHeader = view.findViewById(R.id.tv_post_header);
        rgPostType = view.findViewById(R.id.rg_post_type);
        etTitle = view.findViewById(R.id.et_post_title);
        etContent = view.findViewById(R.id.et_post_content);
        btnSubmit = view.findViewById(R.id.btn_submit_post);
        btnPickMedia = view.findViewById(R.id.btn_pick_media);
        ivPreview = view.findViewById(R.id.iv_preview);
        vvPreview = view.findViewById(R.id.vv_preview);

        // Detect role from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences(
                LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String role = prefs.getString(LoginActivity.KEY_ROLE, "student");

        if ("recruiter".equalsIgnoreCase(role)) {
            tvHeader.setText("Create Post (Recruiter)");
            rgPostType.setVisibility(View.VISIBLE);
        } else {
            tvHeader.setText("Create Experience Post");
            rgPostType.setVisibility(View.GONE);
        }

        btnPickMedia.setOnClickListener(v -> pickMedia());

        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
                Toast.makeText(getActivity(), "Please fill in title and content", Toast.LENGTH_SHORT).show();
                return;
            }

            int userId = prefs.getInt(LoginActivity.KEY_USER_ID, -1);

            if (userId == -1) {
                Toast.makeText(getActivity(), "User ID not found. Please login again.", Toast.LENGTH_SHORT).show();
                return;
            }

            String type = ExperiencePost.TYPE_EXPERIENCE; // default
            if ("recruiter".equalsIgnoreCase(role)) {
                int checkedId = rgPostType.getCheckedRadioButtonId();
                if (checkedId == R.id.rb_job_post) {
                    type = ExperiencePost.TYPE_JOB;
                } else {
                    type = ExperiencePost.TYPE_EXPERIENCE;
                }
            }

            // Media URL – for now, we only store local URI or empty (you can upgrade later)
            String uriString = (selectedMediaUri != null) ? selectedMediaUri.toString() : null;

            if (ExperiencePost.TYPE_JOB.equals(type)) {
                // Recruiter Job Post -> call save_job_post.php
                sendJobPostToServer(userId, title, content, uriString);
            } else {
                // Experience Post -> call save_experience_post.php (like earlier)
                sendExperiencePostToServer(userId, title, content, uriString);
            }
        });


        return view;
    }

    private void pickMedia() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"image/*", "video/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        mediaPickerLauncher.launch(intent);
    }

    private void handleMediaSelection(Uri uri) {
        selectedMediaUri = uri;
        String type = requireContext().getContentResolver().getType(uri);

        ivPreview.setVisibility(View.GONE);
        vvPreview.setVisibility(View.GONE);

        if (type != null && type.startsWith("image")) {
            selectedMediaType = "image";
            ivPreview.setVisibility(View.VISIBLE);
            ivPreview.setImageURI(uri);
        } else if (type != null && type.startsWith("video")) {
            selectedMediaType = "video";
            vvPreview.setVisibility(View.VISIBLE);
            vvPreview.setVideoURI(uri);
            vvPreview.start(); // Auto-play preview
        } else {
            Toast.makeText(getContext(), "Selected file is not an image or video", Toast.LENGTH_SHORT).show();
            selectedMediaUri = null;
            selectedMediaType = null;
        }

        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            // ignore
        }
    }

    // 🔹 NEW: send experience post to PHP backend
    private void submitExperiencePostToServer(int userId, String title, String content, String mediaUrl) {
        String url = ApiClient.BASE_URL + "create_experience_post.php";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.optBoolean("success", false);
                        String message = json.optString("message", "No message");

                        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

                        if (success) {
                            // Clear fields on success
                            etTitle.setText("");
                            etContent.setText("");
                            selectedMediaUri = null;
                            selectedMediaType = null;
                            ivPreview.setVisibility(View.GONE);
                            vvPreview.setVisibility(View.GONE);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "Response parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    String msg = (error.getMessage() != null) ? error.getMessage() : "Unknown error";
                    Toast.makeText(getActivity(), "Network error: " + msg, Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("title", title);
                params.put("description", content);
                params.put("media_url", mediaUrl); // Just a string for now
                return params;
            }
        };

        ApiClient.getRequestQueue(requireContext()).add(request);
    }
}
