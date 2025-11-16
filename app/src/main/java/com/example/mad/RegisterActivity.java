package com.example.mad;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etRegEmail, etRegPassword;
    private RadioGroup rgRole;
    private RadioButton rbStudent, rbRecruiter;
    private Button btnRegister;
    private TextView tvGoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.et_name);
        etRegEmail = findViewById(R.id.et_reg_email);
        etRegPassword = findViewById(R.id.et_reg_password);
        rgRole = findViewById(R.id.rg_role);
        rbStudent = findViewById(R.id.rb_student);
        rbRecruiter = findViewById(R.id.rb_recruiter);
        btnRegister = findViewById(R.id.btn_register);
        tvGoLogin = findViewById(R.id.tv_go_login);

        btnRegister.setOnClickListener(v -> attemptRegister());

        tvGoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etRegEmail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedRoleId = rgRole.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        String role;
        if (selectedRoleId == R.id.rb_student) {
            role = "student";
        } else {
            role = "recruiter";
        }

        // Save "registered user" locally using SharedPreferences
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LoginActivity.KEY_EMAIL, email);
        editor.putString(LoginActivity.KEY_PASSWORD, password);
        editor.putString(LoginActivity.KEY_ROLE, role);
        editor.apply();

        Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_SHORT).show();

        // Go back to Login
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
