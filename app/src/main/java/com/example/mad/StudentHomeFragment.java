package com.example.mad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We are using a UNIQUE layout name here to avoid conflicts
        View view = inflater.inflate(R.layout.fragment_student_home, container, false);

        cardStackView = view.findViewById(R.id.card_stack_view);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state);
        
        // Get current user ID
        SharedPreferences prefs = requireActivity().getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = prefs.getString(LoginActivity.KEY_USER_ID, null);
        
        setupCardStackView();
        loadJobsFromSupabase();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload jobs when returning to this fragment (e.g., after posting a new job)
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
                        List<Job> jobs = new ArrayList<>();
                        for (JobWithDate jobWithDate : jobsWithDates) {
                            jobs.add(jobWithDate.job);
                        }

                        // Update adapter with real data
                        if (adapter == null) {
                            adapter = new CardStackAdapter(jobs);
                            cardStackView.setAdapter(adapter);
                            setupButtonListeners();
                        } else {
                            // Update existing adapter
                            adapter = new CardStackAdapter(jobs);
                            cardStackView.setAdapter(adapter);
                            setupButtonListeners();
                        }
                        
                        // Check which jobs have been applied to
                        checkAppliedJobs();

                        // Show/hide empty state
                        if (jobs.isEmpty()) {
                            cardStackView.setVisibility(View.GONE);
                            emptyStateLayout.setVisibility(View.VISIBLE);
                        } else {
                            cardStackView.setVisibility(View.VISIBLE);
                            emptyStateLayout.setVisibility(View.GONE);
                        }

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
                if (adapter == null || adapter.getItemCount() == 0) return;
                
                // Get the top job
                int topPosition = layoutManager.getTopPosition();
                if (topPosition < 0 || topPosition >= adapter.getItemCount()) return;
                
                Job currentJob = adapter.getItems().get(topPosition);
                String jobId = currentJob.getJobId();
                
                // Don't allow swiping if already applied
                if (appliedJobs.containsKey(jobId) && appliedJobs.get(jobId)) {
                    Toast.makeText(requireContext(), "You have already applied to this job", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                View topCard = layoutManager.getTopView();
                if (topCard != null) {
                    animateButton(topCard.findViewById(R.id.not_now_button));
                    topCard.postDelayed(() -> triggerSwipe(Direction.Left, Duration.Normal.duration), 150);
                }
            }

            @Override
            public void onInterestedClicked() {
                if (adapter == null || adapter.getItemCount() == 0) return;
                
                // Get the top job (the one currently visible)
                int topPosition = layoutManager.getTopPosition();
                if (topPosition < 0 || topPosition >= adapter.getItemCount()) return;
                
                Job currentJob = adapter.getItems().get(topPosition);
                String jobId = currentJob.getJobId();
                
                // Check if already applied
                if (appliedJobs.containsKey(jobId) && appliedJobs.get(jobId)) {
                    Toast.makeText(requireContext(), "You have already applied to this job", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Check if user is logged in
                if (currentUserId == null) {
                    Toast.makeText(requireContext(), "Please login to apply", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Show dialog to enter message
                showApplicationDialog(currentJob);
            }

            @Override
            public void onSaveClicked() {
                if (adapter == null || adapter.getItemCount() == 0) return;
                
                // Get the top job
                int topPosition = layoutManager.getTopPosition();
                if (topPosition < 0 || topPosition >= adapter.getItemCount()) return;
                
                Job currentJob = adapter.getItems().get(topPosition);
                String jobId = currentJob.getJobId();
                
                // Don't allow saving if already applied
                if (appliedJobs.containsKey(jobId) && appliedJobs.get(jobId)) {
                    Toast.makeText(requireContext(), "You have already applied to this job", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                View topCard = layoutManager.getTopView();
                if (topCard != null) {
                    topCard.findViewById(R.id.save_button).setEnabled(false);
                }
                triggerSwipe(Direction.Top, Duration.Slow.duration);
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
    
    private void showApplicationDialog(Job job) {
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
            } else {
                saveApplication(job, pitch);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
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
        
        // Disable button on current card
        View topCard = layoutManager.getTopView();
        if (topCard != null) {
            MaterialButton interestedButton = topCard.findViewById(R.id.interested_button);
            if (interestedButton != null) {
                interestedButton.setText("Applied");
                interestedButton.setEnabled(false);
                // Disable other buttons too
                MaterialButton notNowButton = topCard.findViewById(R.id.not_now_button);
                MaterialButton saveButton = topCard.findViewById(R.id.save_button);
                if (notNowButton != null) notNowButton.setEnabled(false);
                if (saveButton != null) saveButton.setEnabled(false);
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
                        // Update button state immediately
                        updateButtonStateForJob(jobId);
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
                            // Update button state immediately
                            updateButtonStateForJob(jobId);
                        });
                    } else {
                        // Re-enable button on error
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Failed to submit application. Please try again.", Toast.LENGTH_SHORT).show();
                            View topCardView = layoutManager.getTopView();
                            if (topCardView != null) {
                                MaterialButton interestedBtn = topCardView.findViewById(R.id.interested_button);
                                if (interestedBtn != null) {
                                    interestedBtn.setText("Interested");
                                    interestedBtn.setEnabled(true);
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
                    if (notNowButton != null) {
                        notNowButton.setEnabled(false);
                    }
                    if (saveButton != null) {
                        saveButton.setEnabled(false);
                    }
                }
            }
        }
    }
    
    private void checkAppliedJobs() {
        if (currentUserId == null || adapter == null) return;
        
        // Check each job to see if student has applied
        for (Job job : adapter.getItems()) {
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
            Toast.makeText(requireContext(), "Marked as Interested!", Toast.LENGTH_SHORT).show();
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
                // Already applied - disable all buttons
                if (interestedButton != null) {
                    interestedButton.setText("Applied");
                    interestedButton.setEnabled(false);
                }
                if (notNowButton != null) {
                    notNowButton.setEnabled(false);
                }
                if (saveButton != null) {
                    saveButton.setEnabled(false);
                }
            } else {
                // Not applied - enable buttons
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
