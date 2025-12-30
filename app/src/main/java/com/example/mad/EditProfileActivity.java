package com.example.mad;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etEditName, etEditPhone, etEditBio;
    private Button btnSaveProfile;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etEditName = findViewById(R.id.et_edit_name);
        etEditPhone = findViewById(R.id.et_edit_phone);
        etEditBio = findViewById(R.id.et_edit_bio);
        btnSaveProfile = findViewById(R.id.btn_save_profile);

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);

        // Pre-fill fields with current data
        etEditName.setText(prefs.getString(LoginActivity.KEY_NAME, ""));
        etEditPhone.setText(prefs.getString("user_phone", ""));
        etEditBio.setText(prefs.getString("user_bio", ""));

        btnSaveProfile.setOnClickListener(v -> updateFullProfile());
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
                    return json.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return null;
                }
            }
        };

        ApiClient.getRequestQueue(this).add(request);
    }
}
