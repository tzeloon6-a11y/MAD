package com.example.mad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PostFragment extends Fragment {

    private TextView tvHeader;
    private RadioGroup rgPostType;
    private EditText etTitle, etContent;
    private Button btnSubmit, btnPickMedia;
    private ImageView ivPreview;
    private VideoView vvPreview;

    private Uri selectedMediaUri;
    private String selectedMediaType; // "image" or "video"

    public PostFragment() {}

    private final ActivityResultLauncher<Intent> mediaPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) handleMediaSelection(uri);
                }
            });

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

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);

        String role = prefs.getString(LoginActivity.KEY_ROLE, "student");

        if ("recruiter".equalsIgnoreCase(role)) {
            tvHeader.setText("Create Job / Experience Post");
            rgPostType.setVisibility(View.VISIBLE);
        } else {
            tvHeader.setText("Create Experience Post");
            rgPostType.setVisibility(View.GONE);
        }

        btnPickMedia.setOnClickListener(v -> pickMedia());

        btnSubmit.setOnClickListener(v -> submitPost());

        return view;
    }

    private void submitPost() {

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);

        String userId = prefs.getString(LoginActivity.KEY_USER_ID, null);
        String role = prefs.getString(LoginActivity.KEY_ROLE, "student");

        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            Toast.makeText(getActivity(), "Title & Content required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userId == null) {
            Toast.makeText(getActivity(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Decide table based on role & radio
        String table = "experience_posts";
        if ("recruiter".equalsIgnoreCase(role)) {
            int checkedId = rgPostType.getCheckedRadioButtonId();
            if (checkedId == R.id.rb_job_post) {
                table = "job_posts";
            }
        }

        String mediaUrl = (selectedMediaUri != null) ? selectedMediaUri.toString() : null;

        sendToSupabase(table, userId, title, content, mediaUrl, selectedMediaType);
    }

    // ✅ SUPABASE INSERT
    private void sendToSupabase(String table, String userId, String title,
                                String content, String mediaUrl, String mediaType) {

        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/" + table;

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    Log.d("PostFragment", "Success: " + response);
                    Toast.makeText(getActivity(), "Post published!", Toast.LENGTH_SHORT).show();
                    clearInputs();
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
                    Log.e("PostFragment", "Error: " + error.toString() + " | Body: " + responseBody);
                    Toast.makeText(getActivity(), "Post failed! Check Logcat for details.", Toast.LENGTH_LONG).show();
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
                    json.put("user_id", userId);
                    json.put("title", title);
                    // ✅ FIXED: Changed "content" to "description" to match database schema
                    json.put("description", content);

                    if (mediaUrl != null) {
                        json.put("media_url", mediaUrl);
                    }
                    if (mediaType != null) {
                        json.put("media_type", mediaType);
                    }

                    return json.toString().getBytes(StandardCharsets.UTF_8);

                } catch (Exception e) {
                    Log.e("PostFragment", "JSON Error", e);
                    return null;
                }
            }
        };

        ApiClient.getRequestQueue(requireContext()).add(request);
    }

    private void pickMedia() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"image/*", "video/*"});
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
            vvPreview.start();

        } else {
            Toast.makeText(getContext(), "Unsupported file type", Toast.LENGTH_SHORT).show();
            selectedMediaUri = null;
            selectedMediaType = null;
        }
    }

    private void clearInputs() {
        etTitle.setText("");
        etContent.setText("");
        selectedMediaUri = null;
        selectedMediaType = null;
        ivPreview.setVisibility(View.GONE);
        vvPreview.setVisibility(View.GONE);
    }
}
