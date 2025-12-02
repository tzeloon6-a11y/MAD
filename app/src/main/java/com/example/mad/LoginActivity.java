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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

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
    public static final String KEY_ROLE = "role"; // "student" or "recruiter"
    public static final String KEY_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Force status bar to be black with light (white) icons
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.BLACK);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            // false = dark background, light icons
            windowInsetsController.setAppearanceLightStatusBars(false);
        }

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoRegister = findViewById(R.id.tv_go_register);

        // If already logged in, skip login page
        checkIfAlreadyLoggedIn();

        // Login button
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Go to Register screen
        tvGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void checkIfAlreadyLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedUserId = prefs.getInt(KEY_USER_ID, -1);
        String savedEmail = prefs.getString(KEY_EMAIL, null);
        String savedRole = prefs.getString(KEY_ROLE, null);

        if (savedUserId != -1 && savedEmail != null && savedRole != null) {
            goToMainActivity();
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = ApiClient.BASE_URL + "login.php";

        // Using StringRequest (POST Form Data) instead of JsonObjectRequest
        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    try {
                        // Parse the JSON response from server
                        JSONObject json = new JSONObject(response);
                        boolean success = json.optBoolean("success", false);
                        String message = json.optString("message", "No message");

                        if (!success) {
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONObject userObj = json.getJSONObject("user");
                        int userId = userObj.getInt("id");
                        String name = userObj.optString("name", "");
                        String userEmail = userObj.getString("email");
                        String role = userObj.getString("role");

                        // Save to SharedPreferences
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(KEY_USER_ID, userId);
                        editor.putString(KEY_NAME, name);
                        editor.putString(KEY_EMAIL, userEmail);
                        editor.putString(KEY_ROLE, role);
                        editor.apply();

                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        goToMainActivity();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Response parse error: " + response, Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    android.util.Log.e("LoginError", "Volley error: " + error.getMessage());
                    Toast.makeText(LoginActivity.this, "Network error. Check connection.", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                params.put("password", password);
                return params;
            }
        };

        ApiClient.getRequestQueue(this).add(request);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
