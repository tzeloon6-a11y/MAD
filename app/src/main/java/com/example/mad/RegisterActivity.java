package com.example.mad;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // TODO: CHANGE THIS IP ADDRESS TO YOUR SERVER IP
    // Emulator: http://10.0.2.2/mad_api/register.php
    // Real Device: http://192.168.x.x/mad_api/register.php
    private static final String REGISTER_URL = "http://10.0.2.2/mad/register.php";

    private EditText etName, etEmail, etPassword;
    private RadioGroup rgRole;
    private Button btnRegister;
    private TextView tvGoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Disable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        setContentView(R.layout.activity_register);

        // 2. Force Black Background and White Icons
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
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
        tvGoLogin = findViewById(R.id.tv_go_login);

        // Pre-select student
        rgRole.check(R.id.rb_student);

        btnRegister.setOnClickListener(v -> attemptRegister());

        tvGoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void attemptRegister() {
        final String name = etName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedRole = "student";
        int selectedId = rgRole.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_recruiter) {
            selectedRole = "recruiter";
        }
        final String role = selectedRole;

        // Disable button to prevent double clicks
        btnRegister.setEnabled(false);
        btnRegister.setText("Registering...");

        // --- VOLLEY REQUEST TO DATABASE ---
        StringRequest stringRequest = new StringRequest(Request.Method.POST, REGISTER_URL,
                response -> {
                    // Response from PHP
                    Log.d("Register", "Response: " + response);
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Register");

                    if (response.trim().equalsIgnoreCase("success")) {
                        // Registration successful in DB
                        Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_LONG).show();
                        
                        // Also save locally for convenience (optional, can rely on DB login)
                        saveLocally(name, email, password, role);

                        // Go to Login
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Error from PHP (e.g., "Email already exists")
                        Toast.makeText(RegisterActivity.this, "Server: " + response, Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    // Network Error
                    Log.e("Register", "Error: " + error.toString());
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Register");
                    Toast.makeText(RegisterActivity.this, "Network Error! Check your connection.", Toast.LENGTH_LONG).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                // Send data to PHP
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("email", email);
                params.put("password", password);
                params.put("role", role);
                return params;
            }
        };

        // Add request to queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private void saveLocally(String name, String email, String password, String role) {
        // Optional: save some basic info locally.
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LoginActivity.KEY_NAME, name);
        editor.putString(LoginActivity.KEY_EMAIL, email);
        editor.putString(LoginActivity.KEY_ROLE, role);
        editor.apply();
    }
}
