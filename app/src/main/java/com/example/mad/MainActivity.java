package com.example.mad;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 1. GET THE ROLE FROM STORAGE
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userRole = prefs.getString("role", "student");

        // 2. SET THE DEFAULT FRAGMENT
        if (savedInstanceState == null) {
            if (userRole.equalsIgnoreCase("recruiter")) {
                loadFragment(new RecruiterHomeFragment());
            } else {
                // CHANGED: Use the new StudentHomeFragment we created
                loadFragment(new StudentHomeFragment());
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
                    // CHANGED: Use the new StudentHomeFragment we created
                    selectedFragment = new StudentHomeFragment();
                }
            } else if (id == R.id.nav_chat) {
                selectedFragment = ChatFragment.newInstance();
            } else if (id == R.id.nav_post) {
                selectedFragment = PostFragment.newInstance();
            } else if (id == R.id.nav_profile) {
                selectedFragment = ProfileFragment.newInstance();
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