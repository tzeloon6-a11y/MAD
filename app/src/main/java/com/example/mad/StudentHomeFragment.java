package com.example.mad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.button.MaterialButton;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.Duration;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

public class StudentHomeFragment extends Fragment implements CardStackListener {
    
    // Helper class to sort jobs by created_at
    private static class JobWithDate {
        Job job;
        String createdAt;
        
        JobWithDate(Job job, String createdAt) {
            this.job = job;
            this.createdAt = createdAt;
        }
    }
    private CardStackLayoutManager layoutManager;
    private CardStackAdapter adapter;
    private CardStackView cardStackView;
    private LinearLayout emptyStateLayout;
    private View swipedCardView;
    private String currentUserId;
    private Map<String, Boolean> appliedJobs = new HashMap<>(); // Track which jobs have been applied to
    
    // Tab management
    private MaterialButton btnAllJobs, btnAppliedJobs;
    private boolean isShowingAppliedTab = false;
    private List<Job> allJobsList = new ArrayList<>(); // All jobs from database
    private List<Job> appliedJobsList = new ArrayList<>(); // Only applied jobs
    private List<Job> filteredAllJobsList = new ArrayList<>(); // Filtered all jobs
    private List<Job> filteredAppliedJobsList = new ArrayList<>(); // Filtered applied jobs
    
    // Search
    private EditText etSearch;
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We are using a UNIQUE layout name here to avoid conflicts
        View view = inflater.inflate(R.layout.fragment_student_home, container, false);

        cardStackView = view.findViewById(R.id.card_stack_view);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state);
        
        // Initialize tab buttons
        btnAllJobs = view.findViewById(R.id.btn_all_jobs);
        btnAppliedJobs = view.findViewById(R.id.btn_applied_jobs);
        
        // Initialize search
        etSearch = view.findViewById(R.id.et_search);
        ImageView ivSearchIcon = view.findViewById(R.id.iv_search_icon);
        
        // Get current user ID
        SharedPreferences prefs = requireActivity().getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);
        
        // Setup tab button listeners
        setupTabButtons();
        
        // Setup search functionality
        setupSearch(ivSearchIcon);
        
        setupCardStackView();
        loadJobsFromSupabase();
        return view;
    }
    
    private void setupTabButtons() {
        // Set initial state - "All Jobs" is selected
        updateTabButtonStates(true);
        
        btnAllJobs.setOnClickListener(v -> {
            if (!isShowingAppliedTab) return; // Already on All Jobs tab
            isShowingAppliedTab = false;
            updateTabButtonStates(true);
            showAllJobsTab();
        });
        
        btnAppliedJobs.setOnClickListener(v -> {
            if (isShowingAppliedTab) return; // Already on Applied tab
            isShowingAppliedTab = true;
            updateTabButtonStates(false);
            showAppliedJobsTab();
        });
    }
    
    private void updateTabButtonStates(boolean allJobsSelected) {
        if (allJobsSelected) {
            // All Jobs tab is selected
            btnAllJobs.setBackgroundColor(getResources().getColor(R.color.app_blue, null));
            btnAllJobs.setTextColor(getResources().getColor(android.R.color.white, null));
            
            btnAppliedJobs.setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
            btnAppliedJobs.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
        } else {
            // Applied tab is selected
            btnAppliedJobs.setBackgroundColor(getResources().getColor(R.color.app_blue, null));
            btnAppliedJobs.setTextColor(getResources().getColor(android.R.color.white, null));
            
            btnAllJobs.setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
            btnAllJobs.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
        }
    }
    
    private void setupSearch(ImageView ivSearchIcon) {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                applySearchFilter();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        ivSearchIcon.setOnClickListener(v -> {
            // Clear search on icon click
            etSearch.setText("");
            currentSearchQuery = "";
            applySearchFilter();
        });
    }
    
    private void applySearchFilter() {
        // Filter all jobs list
        filteredAllJobsList.clear();
        for (Job job : allJobsList) {
            String jobId = job.getJobId();
            // Skip applied jobs
            if (appliedJobs.containsKey(jobId) && appliedJobs.get(jobId)) {
                continue;
            }
            
            // Apply search filter
            if (currentSearchQuery.isEmpty() || 
                job.getTitle().toLowerCase().contains(currentSearchQuery) ||
                job.getDescription().toLowerCase().contains(currentSearchQuery)) {
                filteredAllJobsList.add(job);
            }
        }
        
        // Filter applied jobs list
        filteredAppliedJobsList.clear();
        for (Job job : appliedJobsList) {
            if (currentSearchQuery.isEmpty() ||
                job.getTitle().toLowerCase().contains(currentSearchQuery) ||
                job.getDescription().toLowerCase().contains(currentSearchQuery)) {
                filteredAppliedJobsList.add(job);
            }
        }
        
        // Refresh the current tab
        if (isShowingAppliedTab) {
            showAppliedJobsTab();
        } else {
            showAllJobsTab();
        }
    }
    
    private void showAllJobsTab() {
        // Use filtered list
        List<Job> availableJobs = new ArrayList<>(filteredAllJobsList);
        
        // Update adapter with filtered jobs
        adapter = new CardStackAdapter(availableJobs);
        cardStackView.setAdapter(adapter);
        setupButtonListeners();
        
        // Show/hide empty state
        if (availableJobs.isEmpty()) {
            cardStackView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            cardStackView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    private void showAppliedJobsTab() {
        // Use filtered list
        List<Job> availableJobs = new ArrayList<>(filteredAppliedJobsList);
        
        // Show only applied jobs
        adapter = new CardStackAdapter(availableJobs);
        cardStackView.setAdapter(adapter);
        setupButtonListeners();
        
        // Show/hide empty state
        if (availableJobs.isEmpty()) {
            cardStackView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            cardStackView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload jobs when returning to this fragment (e.g., after posting a new job)
        // Applied jobs will be loaded as part of loadJobsFromSupabase()
        loadJobsFromSupabase();
    }

    private void setupCardStackView() {
        layoutManager = new CardStackLayoutManager(getContext(), this);
        layoutManager.setStackFrom(StackFrom.None);
        layoutManager.setVisibleCount(3);
        layoutManager.setTranslationInterval(8.0f);
        layoutManager.setScaleInterval(0.95f);
        layoutManager.setSwipeThreshold(0.3f);
        layoutManager.setMaxDegree(20.0f);
        ArrayList<Direction> directions = new ArrayList<>();
        directions.add(Direction.Left);
        directions.add(Direction.Right);
        directions.add(Direction.Top);
        layoutManager.setDirections(directions);
        cardStackView.setLayoutManager(layoutManager);
    }

    private void loadJobsFromSupabase() {
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/job_posts?select=*"
                + "&order=created_at.desc";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        // Temporary list to store jobs with their created_at dates for sorting
                        List<JobWithDate> jobsWithDates = new ArrayList<>();

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            String id = String.valueOf(obj.optString("id"));
                            String title = obj.optString("title", "No Title");
                            String description = obj.optString("description", obj.optString("content", "No description available."));
                            String recruiterId = obj.optString("user_id", "");
                            String createdAt = obj.optString("created_at", "");
                            
                            // Map Supabase data to Job model
                            // Note: job_posts table might not have companyName, wage, location
                            // Using defaults or extracting from description if needed
                            String companyName = obj.optString("company_name", obj.optString("recruiter_name", "Company"));
                            String wage = obj.optString("wage", "Not specified");
                            String location = obj.optString("location", "Not specified");

                            Job job = new Job(id, title, companyName, wage, location, description, recruiterId);
                            jobsWithDates.add(new JobWithDate(job, createdAt));
                        }
                        
                        // Sort by created_at descending (newest first) as fallback
                        jobsWithDates.sort((a, b) -> {
                            String dateA = a.createdAt;
                            String dateB = b.createdAt;
                            if (dateA == null || dateB == null) return 0;
                            // Compare dates (newest first = descending order)
                            return dateB.compareTo(dateA);
                        });
                        
                        // Extract sorted jobs
                        allJobsList.clear();
                        for (JobWithDate jobWithDate : jobsWithDates) {
                            allJobsList.add(jobWithDate.job);
                        }

                        // Apply search filter if there's a query
                        applySearchFilter();
                        
                        // Load applied jobs first, then show the appropriate tab
                        loadAppliedJobsAndShowTab();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error loading jobs", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(getContext(), "Failed to load jobs", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        ApiClient.getRequestQueue(requireContext()).add(request);
    }

    private void setupButtonListeners() {
        if (adapter == null) return;
        
        adapter.setOnButtonClickListener(new CardStackAdapter.OnButtonClickListener() {
            @Override
            public void onNotNowClicked() {
                // "Not Now" should always work, even for applied jobs, so students can swipe away to see other jobs
                View topCard = layoutManager.getTopView();
                if (topCard != null) {
                    animateButton(topCard.findViewById(R.id.not_now_button));
                    topCard.postDelayed(() -> triggerSwipe(Direction.Left, Duration.Normal.duration), 150);
                }
            }

            @Override
            public void onInterestedClicked() {
                if (adapter == null || adapter.getItemCount() == 0) return;

                int topPosition = layoutManager.getTopPosition();
                if (topPosition < 0 || topPosition >= adapter.getItemCount()) return;

                Job currentJob = adapter.getItems().get(topPosition);
                String jobId = currentJob.getJobId();

                if (appliedJobs.containsKey(jobId) && appliedJobs.get(jobId)) {
                    Toast.makeText(requireContext(), "You have already applied to this job", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentUserId == null) {
                    Toast.makeText(requireContext(), "Please login to apply", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ✅ PASS FALSE: This is a button click, card is still there.
                showApplicationDialog(currentJob, false);
            }

            @Override
            public void onSaveClicked() {
                if (adapter == null || adapter.getItemCount() == 0) return;

                View topCard = layoutManager.getTopView();
                if (topCard == null) return;

                MaterialButton saveButton = topCard.findViewById(R.id.save_button);

                // 1. Swap to the SOLID icon we just fixed
                saveButton.setIconResource(R.drawable.ic_saved);

                // 2. Force tint to WHITE (This makes the solid block pure white)
                saveButton.setIconTint(ColorStateList.valueOf(Color.WHITE));

                saveButton.setEnabled(false);

                // 3. Fly away after a split second
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    triggerSwipe(Direction.Top, Duration.Slow.duration);
                }, 200);
            }
        });
    }

    private void animateButton(View button) {
        if (button != null) {
            button.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100)
                    .withEndAction(() -> button.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100)).start();
        }
    }

    private void triggerSwipe(Direction direction, int duration) {
        swipedCardView = layoutManager.getTopView();
        SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                .setDirection(direction)
                .setDuration(duration)
                .build();
        layoutManager.setSwipeAnimationSetting(setting);
        cardStackView.swipe();
    }

    private void showApplicationDialog(Job job, boolean isFromSwipe) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Apply to Job");
        builder.setMessage("Enter a short message/pitch:");

        // Create EditText for pitch input
        final EditText input = new EditText(requireContext());
        input.setHint("Why are you interested in this position?");
        input.setMinLines(3);
        input.setMaxLines(5);
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String pitch = input.getText().toString().trim();
            if (TextUtils.isEmpty(pitch)) {
                Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                // If they messed up the input on a swipe, bring the card back so they can try again
                if (isFromSwipe) cardStackView.rewind();
            } else {
                saveApplication(job, pitch);
            }
        });

        // ✅ FIX: If Cancel is clicked, bring the card back!
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            if (isFromSwipe) {
                cardStackView.rewind();
            }
        });

        // ✅ Handle clicking outside the box (dismiss)
        builder.setOnCancelListener(dialog -> {
            if (isFromSwipe) {
                cardStackView.rewind();
            }
        });

        builder.show();
    }

    private void saveApplication(Job job, String initialMessage) {
        if (currentUserId == null || job == null) {
            Toast.makeText(requireContext(), "Invalid data", Toast.LENGTH_SHORT).show();
            return;
        }

        String jobId = job.getJobId();
        String recruiterId = job.getRecruiterId();

        // Check if already applied
        if (appliedJobs.containsKey(jobId) && appliedJobs.get(jobId)) {
            Toast.makeText(requireContext(), "You have already applied to this job", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ FIX: Only update buttons if the visible card is the one we are applying to
        // (Prevents disabling buttons on the NEXT card if we applied via Swipe)
        int topPos = layoutManager.getTopPosition();
        if (adapter != null && topPos >= 0 && topPos < adapter.getItemCount()) {
            Job currentVisibleJob = adapter.getItems().get(topPos);

            // Only update UI if the visible card matches the job we are saving
            if (currentVisibleJob.getJobId().equals(jobId)) {
                View topCard = layoutManager.getTopView();
                if (topCard != null) {
                    MaterialButton interestedButton = topCard.findViewById(R.id.interested_button);
                    if (interestedButton != null) {
                        interestedButton.setText("Applied");
                        interestedButton.setEnabled(false);
                    }
                    MaterialButton saveButton = topCard.findViewById(R.id.save_button);
                    // Not Now stays enabled
                    if (saveButton != null) saveButton.setEnabled(false);
                }
            }
        }

        // Get student name from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String studentName = prefs.getString(LoginActivity.KEY_NAME, "");

        // Get current timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Save to Applications collection
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/applications";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    // Success - mark as applied
                    appliedJobs.put(jobId, true);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Application submitted successfully!", Toast.LENGTH_SHORT).show();
                        // Move applied job to end of stack and auto-advance to next job
                        moveJobToEndAndAdvance(job);
                    });
                },
                error -> {
                    String responseBody = null;
                    int statusCode = -1;
                    if (error.networkResponse != null) {
                        statusCode = error.networkResponse.statusCode;
                        if (error.networkResponse.data != null) {
                            try {
                                responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Check if error is due to duplicate (unique constraint violation)
                    if (statusCode == 409 || (responseBody != null && (responseBody.contains("duplicate") || responseBody.contains("unique")))) {
                        appliedJobs.put(jobId, true);
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "You have already applied to this job", Toast.LENGTH_SHORT).show();
                            // Move applied job to end of stack and auto-advance to next job
                            moveJobToEndAndAdvance(job);
                        });
                    } else {
                        // Re-enable button on error
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Failed to submit application. Please try again.", Toast.LENGTH_SHORT).show();

                            // Re-enable the button if it was the visible card
                            int currentTopPos = layoutManager.getTopPosition();
                            if (adapter != null && currentTopPos >= 0 && currentTopPos < adapter.getItemCount()) {
                                Job currentJob = adapter.getItems().get(currentTopPos);
                                if (currentJob.getJobId().equals(jobId)) {
                                    View topCardView = layoutManager.getTopView();
                                    if (topCardView != null) {
                                        MaterialButton interestedBtn = topCardView.findViewById(R.id.interested_button);
                                        if (interestedBtn != null) {
                                            interestedBtn.setText("Interested");
                                            interestedBtn.setEnabled(true);
                                        }
                                        MaterialButton saveBtn = topCardView.findViewById(R.id.save_button);
                                        if (saveBtn != null) saveBtn.setEnabled(true);
                                    }
                                }
                            }
                        });
                    }
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
                    JSONObject applicationJson = new JSONObject();
                    applicationJson.put("student_id", currentUserId);
                    applicationJson.put("student_name", studentName);
                    applicationJson.put("recruiter_id", recruiterId);
                    applicationJson.put("job_id", jobId);
                    applicationJson.put("status", "PENDING");
                    applicationJson.put("initial_message", initialMessage);
                    applicationJson.put("timestamp", timestamp);
                    return applicationJson.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        ApiClient.getRequestQueue(requireContext()).add(request);
    }
    
    private void loadAppliedJobsAndShowTab() {
        if (currentUserId == null || allJobsList.isEmpty()) {
            // No user or no jobs, just show the tab
            if (isShowingAppliedTab) {
                showAppliedJobsTab();
            } else {
                showAllJobsTab();
            }
            return;
        }
        
        // Load all applied jobs for this user in one query
        try {
            String encodedUserId = java.net.URLEncoder.encode(currentUserId, "UTF-8");
            String url = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/applications?student_id=eq." + encodedUserId
                    + "&select=job_id";
            
            JsonArrayRequest request = new JsonArrayRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        try {
                            // Clear and rebuild appliedJobs map
                            appliedJobs.clear();
                            appliedJobsList.clear();
                            
                            // Extract all applied job IDs
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);
                                String jobId = obj.optString("job_id", "");
                                if (!jobId.isEmpty()) {
                                    appliedJobs.put(jobId, true);
                                    
                                    // Find the job in allJobsList and add to appliedJobsList
                                    for (Job job : allJobsList) {
                                        if (job.getJobId().equals(jobId)) {
                                            appliedJobsList.add(job);
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            // Now show the appropriate tab with correct filtering
                            if (isShowingAppliedTab) {
                                showAppliedJobsTab();
                            } else {
                                showAllJobsTab();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // On error, still show the tab
                            if (isShowingAppliedTab) {
                                showAppliedJobsTab();
                            } else {
                                showAllJobsTab();
                            }
                        }
                    },
                    error -> {
                        // On error, still show the tab (appliedJobs map will be empty)
                        if (isShowingAppliedTab) {
                            showAppliedJobsTab();
                        } else {
                            showAllJobsTab();
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                    headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            
            ApiClient.getRequestQueue(requireContext()).add(request);
        } catch (Exception e) {
            e.printStackTrace();
            // On error, still show the tab
            if (isShowingAppliedTab) {
                showAppliedJobsTab();
            } else {
                showAllJobsTab();
            }
        }
    }
    
    private void moveJobToEndAndAdvance(Job appliedJob) {
        if (appliedJob == null) return;
        
        String jobId = appliedJob.getJobId();
        
        // Mark as applied in the map (this should already be done, but ensure it)
        appliedJobs.put(jobId, true);
        
        // Add to applied jobs list if not already there
        boolean alreadyInAppliedList = false;
        for (Job job : appliedJobsList) {
            if (job.getJobId().equals(jobId)) {
                alreadyInAppliedList = true;
                break;
            }
        }
        
        if (!alreadyInAppliedList) {
            appliedJobsList.add(appliedJob);
        }
        
        // If we're on the "All Jobs" tab, refresh it to remove the applied job and auto-advance
        if (!isShowingAppliedTab) {
            showAllJobsTab();
            // The CardStackView will automatically show the next job since we filtered out the applied one
        } else {
            // If we're on the "Applied" tab, refresh it to show the new applied job
            showAppliedJobsTab();
        }
    }
    
    private void updateAppliedJobsList() {
        // Update appliedJobsList based on appliedJobs map and allJobsList
        appliedJobsList.clear();
        for (Job job : allJobsList) {
            if (appliedJobs.containsKey(job.getJobId()) && appliedJobs.get(job.getJobId())) {
                appliedJobsList.add(job);
            }
        }
    }
    
    private void updateButtonStateForJob(String jobId) {
        // Update button state for the top card if it matches this job
        View topCard = layoutManager.getTopView();
        if (topCard == null) return;
        
        if (adapter != null) {
            int topPosition = layoutManager.getTopPosition();
            if (topPosition >= 0 && topPosition < adapter.getItemCount()) {
                Job topJob = adapter.getItems().get(topPosition);
                if (topJob.getJobId().equals(jobId)) {
                    // This is the top card - update its buttons
                    MaterialButton interestedButton = topCard.findViewById(R.id.interested_button);
                    MaterialButton notNowButton = topCard.findViewById(R.id.not_now_button);
                    MaterialButton saveButton = topCard.findViewById(R.id.save_button);
                    
                    if (interestedButton != null) {
                        interestedButton.setText("Applied");
                        interestedButton.setEnabled(false);
                    }
                    // Not Now button stays enabled so students can swipe away to see other jobs
                    if (notNowButton != null) {
                        notNowButton.setEnabled(true);
                    }
                    if (saveButton != null) {
                        saveButton.setEnabled(false);
                    }
                }
            }
        }
    }
    
    private void checkAppliedJobs() {
        if (currentUserId == null || allJobsList == null || allJobsList.isEmpty()) return;
        
        // Check each job in allJobsList to see if student has applied
        for (Job job : allJobsList) {
            String jobId = job.getJobId();
            if (appliedJobs.containsKey(jobId) && appliedJobs.get(jobId)) {
                continue; // Already checked
            }
            
            // Query to check if application exists
            try {
                String encodedUserId = java.net.URLEncoder.encode(currentUserId, "UTF-8");
                String encodedJobId = java.net.URLEncoder.encode(jobId, "UTF-8");
                
                String checkUrl = SupabaseConfig.SUPABASE_URL
                        + "/rest/v1/applications?student_id=eq." + encodedUserId
                        + "&job_id=eq." + encodedJobId
                        + "&select=application_id"
                        + "&limit=1";
                
                JsonArrayRequest checkRequest = new JsonArrayRequest(
                        Request.Method.GET,
                        checkUrl,
                        null,
                        response -> {
                            if (response.length() > 0) {
                                appliedJobs.put(jobId, true);
                                // Find the job in allJobsList and add to appliedJobsList
                                for (Job jobInList : allJobsList) {
                                    if (jobInList.getJobId().equals(jobId)) {
                                        // Check if already in applied list
                                        boolean exists = false;
                                        for (Job appliedJob : appliedJobsList) {
                                            if (appliedJob.getJobId().equals(jobId)) {
                                                exists = true;
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            appliedJobsList.add(jobInList);
                                        }
                                        break;
                                    }
                                }
                                // Update button state for the top card if it's this job
                                updateButtonStateForJob(jobId);
                            }
                        },
                        error -> {
                            // Silent fail - don't show error for background checks
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
                        headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
                        headers.put("Content-Type", "application/json");
                        return headers;
                    }
                };
                
                ApiClient.getRequestQueue(requireContext()).add(checkRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCardDragging(Direction direction, float ratio) {
        View topView = layoutManager.getTopView();
        if (topView == null) return;
        View overlayInterested = topView.findViewById(R.id.overlay_interested);
        View overlayNotNow = topView.findViewById(R.id.overlay_not_now);
        View overlaySaved = topView.findViewById(R.id.overlay_saved);

        if (direction == Direction.Right) {
            overlayInterested.setVisibility(View.VISIBLE);
            overlayNotNow.setVisibility(View.GONE);
            overlaySaved.setVisibility(View.GONE);
            overlayInterested.setAlpha(ratio);
        } else if (direction == Direction.Left) {
            overlayInterested.setVisibility(View.GONE);
            overlayNotNow.setVisibility(View.VISIBLE);
            overlaySaved.setVisibility(View.GONE);
            overlayNotNow.setAlpha(ratio);
        } else if (direction == Direction.Top) {
            overlayInterested.setVisibility(View.GONE);
            overlayNotNow.setVisibility(View.GONE);
            overlaySaved.setVisibility(View.VISIBLE);
            overlaySaved.setAlpha(ratio);
        }
    }

    @Override
    public void onCardSwiped(Direction direction) {
        if (adapter == null) return;

        int position = layoutManager.getTopPosition() - 1;
        if (position < 0 || position >= adapter.getItemCount()) return;

        Job swipedJob = adapter.getItems().get(position);

        if (direction == Direction.Right) {
            // ✅ PASS TRUE: This tells the dialog "We came from a swipe!"
            showApplicationDialog(swipedJob, true);

        } else if (direction == Direction.Top) {
            SavedJobsManager.getInstance().saveJob(swipedJob);
            Toast.makeText(requireContext(), "Job Saved!", Toast.LENGTH_SHORT).show();
        }

        if (layoutManager.getTopPosition() == adapter.getItemCount()) {
            cardStackView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCardCanceled() {
        View topView = layoutManager.getTopView();
        if (topView != null) {
            topView.findViewById(R.id.overlay_interested).setVisibility(View.GONE);
            topView.findViewById(R.id.overlay_not_now).setVisibility(View.GONE);
            topView.findViewById(R.id.overlay_saved).setVisibility(View.GONE);
        }
        swipedCardView = null;
    }

    @Override public void onCardRewound() { }
    @Override 
    public void onCardAppeared(View view, int position) {
        // Update button states when card appears
        if (adapter != null && position >= 0 && position < adapter.getItemCount()) {
            Job job = adapter.getItems().get(position);
            String jobId = job.getJobId();
            
            MaterialButton interestedButton = view.findViewById(R.id.interested_button);
            MaterialButton notNowButton = view.findViewById(R.id.not_now_button);
            MaterialButton saveButton = view.findViewById(R.id.save_button);
            
            if (appliedJobs.containsKey(jobId) && appliedJobs.get(jobId)) {
                // Already applied - disable Interested and Save buttons, but keep Not Now enabled
                if (interestedButton != null) {
                    interestedButton.setText("Applied");
                    interestedButton.setEnabled(false);
                }
                // Not Now button stays enabled so students can swipe away to see other jobs
                if (notNowButton != null) {
                    notNowButton.setEnabled(true);
                }
                if (saveButton != null) {
                    saveButton.setEnabled(false);
                }
            } else {
                // Not applied - enable all buttons
                if (interestedButton != null) {
                    interestedButton.setText("Interested");
                    interestedButton.setEnabled(true);
                }
                if (notNowButton != null) {
                    notNowButton.setEnabled(true);
                }
                if (saveButton != null) {
                    saveButton.setEnabled(true);
                }
            }
        }
    }
    @Override
    public void onCardDisappeared(View view, int position) {
        if (swipedCardView == view) {
            MaterialButton saveButton = view.findViewById(R.id.save_button);
            if (saveButton != null && !saveButton.isEnabled()) {
                saveButton.setIconResource(R.drawable.ic_save_for_later);
                saveButton.setEnabled(true);
            }
        }
        swipedCardView = null;
    }
}
