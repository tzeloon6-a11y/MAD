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

        // 1. GET THE ROLE
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userRole = prefs.getString("role", "student");

        // ---------------------------------------------------------
        // ✅ NEW CODE: HIDE "SAVED" TAB IF USER IS A RECRUITER
        // ---------------------------------------------------------
        if (userRole.equalsIgnoreCase("recruiter")) {
            bottomNavigationView.getMenu().findItem(R.id.nav_saved).setVisible(false);
        }
        // ---------------------------------------------------------

        // 2. SET THE DEFAULT FRAGMENT
        if (savedInstanceState == null) {
            if (userRole.equalsIgnoreCase("recruiter")) {
                loadFragment(new RecruiterHomeFragment());
            } else {
                loadFragment(new StudentHomeFragment());
            }
        }

        // 3. HANDLE CLICKS (Paste the updated listener we wrote earlier)
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                if (userRole.equalsIgnoreCase("recruiter")) {
                    selectedFragment = new RecruiterHomeFragment();
                } else {
                    selectedFragment = new StudentHomeFragment();
                }
            }
            else if (id == R.id.nav_saved) {
                selectedFragment = new SavedJobsFragment();
            }
            else if (id == R.id.nav_chat) {
                selectedFragment = ChatFragment.newInstance();
            }
            else if (id == R.id.nav_post) {
                selectedFragment = PostFragment.newInstance();
            }
            else if (id == R.id.nav_profile) {
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