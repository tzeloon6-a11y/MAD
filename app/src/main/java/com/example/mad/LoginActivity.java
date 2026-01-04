package com.example.mad;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoRegister;

    // SharedPreferences keys
    public static final String PREFS_NAME = "UserPrefs";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_NAME = "name";
    public static final String KEY_ROLE = "role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_login);

        // Status bar - setStatusBarColor is still valid, only the flags were deprecated
        Window window = getWindow();
        window.setStatusBarColor(Color.BLACK);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(false);
        }

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoRegister = findViewById(R.id.tv_go_register);

        checkIfAlreadyLoggedIn();

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void checkIfAlreadyLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // ✅ CHANGED: Read as String
        String savedUserId = prefs.getString(KEY_USER_ID, null);
        String savedEmail = prefs.getString(KEY_EMAIL, null);
        String savedRole = prefs.getString(KEY_ROLE, null);

        if (savedUserId != null && savedEmail != null && savedRole != null) {
            goToMainActivity();
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Encode params
            String url = ApiClient.BASE_URL +
                    "users?email=eq." + URLEncoder.encode(email, "UTF-8") +
                    "&password=eq." + URLEncoder.encode(password, "UTF-8") +
                    "&select=*";

            StringRequest request = new StringRequest(
                    Request.Method.GET,
                    url,
                    response -> {
                        try {
                            if (response.equals("[]")) {
                                Toast.makeText(this, "Invalid login", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Remove brackets if array
                            String jsonStr = response;
                            if (jsonStr.startsWith("[") && jsonStr.endsWith("]")) {
                                jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
                            }
                            JSONObject user = new JSONObject(jsonStr);

                            String userId = user.getString("id"); // ✅ UUID STRING
                            String name = user.getString("name");
                            String role = user.getString("role");

                            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(KEY_USER_ID, userId); // ✅ STRING NOW
                            editor.putString(KEY_NAME, name);
                            editor.putString(KEY_EMAIL, email);
                            editor.putString(KEY_ROLE, role);
                            editor.apply();

                            Toast.makeText(this, "Login success", Toast.LENGTH_SHORT).show();
                            goToMainActivity();

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Parsing error", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                    headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                    return headers;
                }
            };

            ApiClient.getRequestQueue(this).add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
