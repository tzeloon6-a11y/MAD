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

        // 1. GET THE ROLE (Kept from your backup so it's ready for later)
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userRole = prefs.getString("role", "student");

        // 2. SET DEFAULT FRAGMENT
        if (savedInstanceState == null) {
            // SAFE MODE: Loading Student Home directly for now.
            // When Nicole merges her RecruiterHomeFragment, you can uncomment the 'if' logic below.

            /* if (userRole.equalsIgnoreCase("recruiter")) {
                loadFragment(new RecruiterHomeFragment()); // Uncomment this later!
            } else {
                loadFragment(new StudentHomeFragment());
            }
            */

            loadFragment(new StudentHomeFragment());
        }

        // 3. HANDLE NAVIGATION CLICKS
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // For now, we force Student Home so you can test YOUR feature.
                // Later, you can wrap this in the (if userRole == "recruiter") check again.
                selectedFragment = new StudentHomeFragment();

            } else if (id == R.id.nav_saved) {
                // ADDED: Your new Saved Jobs Tab
                selectedFragment = new SavedJobsFragment();

            } else if (id == R.id.nav_chat) {
                // Keep Steve's Chat
                selectedFragment = new ChatFragment();

            } else if (id == R.id.nav_post) {
                // Keep Steve's Post
                selectedFragment = new PostFragment();

            } else if (id == R.id.nav_profile) {
                // Keep Steve's Profile
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