package com.example.mad;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.SharedPreferences; // Import this!
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private String userRole; // Variable to store the role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 1. GET THE ROLE FROM STORAGE (Saved by Ivan's Login)
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        // Default to "student" if nothing is found
        userRole = prefs.getString("role", "student");

        // 2. SET THE DEFAULT FRAGMENT (When app opens)
        if (savedInstanceState == null) {
            if (userRole.equalsIgnoreCase("recruiter")) {
                loadFragment(new RecruiterHomeFragment()); // Show YOUR Fragment
            } else {
                loadFragment(new HomeFragment()); // Show Student Fragment
            }
        }

        // 3. HANDLE NAVIGATION CLICKS
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Check role again when clicking Home
                if (userRole.equalsIgnoreCase("recruiter")) {
                    selectedFragment = new RecruiterHomeFragment();
                } else {
                    selectedFragment = new HomeFragment();
                }
            } else if (id == R.id.nav_chat) {
                selectedFragment = new ChatFragment();
            } else if (id == R.id.nav_post) {
                selectedFragment = new PostFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}