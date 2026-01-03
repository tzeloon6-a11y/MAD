package com.example.mad;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
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
    private CardStackLayoutManager layoutManager;
    private CardStackAdapter adapter;
    private CardStackView cardStackView;
    private LinearLayout emptyStateLayout;
    private View swipedCardView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We are using a UNIQUE layout name here to avoid conflicts
        View view = inflater.inflate(R.layout.fragment_student_home, container, false);

        cardStackView = view.findViewById(R.id.card_stack_view);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state);
        setupCardStackView();
        loadJobs();
        return view;
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

    private void loadJobs() {
        // 1. Build the Supabase URL (Fetch all job posts)
        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/job_posts?select=*"
                + "&order=created_at.desc";

        // 2. Create the Request
        com.android.volley.toolbox.JsonArrayRequest request = new com.android.volley.toolbox.JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<Job> realJobs = new ArrayList<>();

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            // MAPPING: Connect Supabase columns to your Job.java fields
                            // NOTE: checks for "salary" or "wage" in case your team named it differently
                            String id = String.valueOf(obj.optInt("id", 0));
                            String title = obj.optString("title", "Untitled Job");
                            String description = obj.optString("description", obj.optString("content", "No description"));

                            // Recruiter ID is usually stored as "user_id" in the job_posts table
                            String recruiterId = obj.optString("user_id", "");

                            // Optional fields (Provide defaults if they are empty in DB)
                            String company = obj.optString("recruiter_name", "Campus Employer");
                            String location = obj.optString("location", "On Campus");
                            String wage = obj.optString("salary", obj.optString("wage", "Negotiable"));

                            // Only add valid jobs
                            if (!recruiterId.isEmpty()) {
                                realJobs.add(new Job(
                                        id,
                                        title,
                                        company,
                                        wage,
                                        location,
                                        description,
                                        recruiterId
                                ));
                            }
                        }

                        // 3. Update the Adapter with Real Data
                        if (realJobs.isEmpty()) {
                            // If no real jobs exist, show empty state
                            cardStackView.setVisibility(View.GONE);
                            emptyStateLayout.setVisibility(View.VISIBLE);
                        } else {
                            adapter = new CardStackAdapter(realJobs);
                            cardStackView.setAdapter(adapter);
                            setupButtonListeners(); // Re-attach click listeners
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing jobs", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // If network fails, maybe show dummy data just so the screen isn't empty?
                    Toast.makeText(getContext(), "Failed to load jobs", Toast.LENGTH_SHORT).show();
                    // Optional: Fallback to dummy data for testing if offline
                    // List<Job> dummy = createDummyJobs();
                    // adapter = new CardStackAdapter(dummy);
                    // cardStackView.setAdapter(adapter);
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

        // 4. Send Request
        ApiClient.getRequestQueue(requireContext()).add(request);
    }

    // === ADD THIS HELPER METHOD TOO (To keep the code clean) ===
    private void setupButtonListeners() {
        adapter.setOnButtonClickListener(new CardStackAdapter.OnButtonClickListener() {
            @Override
            public void onNotNowClicked() {
                View topCard = layoutManager.getTopView();
                if (topCard != null) {
                    animateButton(topCard.findViewById(R.id.not_now_button));
                    topCard.postDelayed(() -> triggerSwipe(Direction.Left, Duration.Normal.duration), 150);
                }
            }

            @Override
            public void onInterestedClicked() {
                View topCard = layoutManager.getTopView();
                if (topCard != null) {
                    animateButton(topCard.findViewById(R.id.interested_button));
                    topCard.postDelayed(() -> triggerSwipe(Direction.Right, Duration.Normal.duration), 150);
                }
            }

            @Override
            public void onSaveClicked() {
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

    private List<Job> createDummyJobs() {
        List<Job> jobs = new ArrayList<>();
        // Format: ID, Title, Company, Wage, Location, Description, RecruiterID
        jobs.add(new Job("job101", "Barista", "Campus Coffee", "RM15/hr", "Student Union", "Make coffee.", "rec1"));
        jobs.add(new Job("job102", "Library Assistant", "Main Library", "RM14/hr", "Central", "Sort books.", "rec2"));
        jobs.add(new Job("job103", "IT Help Desk", "Tech Services", "RM18/hr", "Science Hall", "Fix PCs.", "rec3"));
        return jobs;
    }

    // Add inside StudentHomeFragment.java

    private void showPitchDialog(Job job) {
        // 1. Create the Dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Apply to " + job.getCompanyName());
        builder.setMessage("Send a short message to the recruiter:");

        // 2. Add the Input Field
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Hi, I am interested because...");
        input.setMinLines(2);
        builder.setView(input);

        // 3. Set "Send" Button
        builder.setPositiveButton("Send", (dialog, which) -> {
            String pitch = input.getText().toString();
            // Send to Supabase
            submitApplicationToSupabase(job, pitch);
        });

        // 4. Set "Cancel" Button (Snap Back Logic)
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            // The card was already swiped visually, so we must "Rewind" it
            cardStackView.rewind();
        });

        // Prevent closing by clicking outside (Force a choice)
        builder.setCancelable(false);

        builder.show();
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
        int position = layoutManager.getTopPosition() - 1;
        if (position < 0 || position >= adapter.getItemCount()) return;

        Job swipedJob = adapter.getItems().get(position);

        if (direction == Direction.Right) {
            // OPEN DIALOG instead of auto-sending
            showPitchDialog(swipedJob);

        } else if (direction == Direction.Top) {
            // Save to Database (We will write this helper next)
            saveJobToSupabase(swipedJob);
            Toast.makeText(requireContext(), "Job Saved for Later!", Toast.LENGTH_SHORT).show();
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
    @Override public void onCardAppeared(View view, int position) { }
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

    // =========================================================
    // NEW METHOD: Sends the interest to the database
    // =========================================================
    private void submitApplicationToSupabase(Job job, String pitch) {
        // 1. Get Current User Data
        // Using "UserPrefs" to match your MainActivity code
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        // Steve's LoginActivity uses "userId" key usually, but check your LoginActivity to be sure.
        // Assuming "userId" and "name" based on common patterns
        String currentUserId = prefs.getString("user_id", null);
        String studentName = prefs.getString("name", "Student");

        // Safety Check
        if (currentUserId == null) {
            // If testing without login, we can't submit
            android.util.Log.e("Supabase", "Cannot apply: User not logged in.");
            return;
        }

        // 2. Prepare Data
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/applications";
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

        // 3. Send Request
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    // Success!
                    // We don't show a Toast here because onCardSwiped already showed one.
                    android.util.Log.d("Supabase", "Application submitted successfully!");
                },
                error -> {
                    // Error handling
                    String errorMsg = "Failed to apply";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg += ": " + new String(error.networkResponse.data, StandardCharsets.UTF_8);
                    }
                    android.util.Log.e("Supabase", errorMsg);
                    // Only show toast on error so user knows it failed
                    Toast.makeText(getContext(), "Error saving application", Toast.LENGTH_SHORT).show();
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
                    JSONObject json = new JSONObject();
                    json.put("student_id", currentUserId);
                    json.put("student_name", studentName);
                    json.put("recruiter_id", job.getRecruiterId());
                    json.put("job_id", job.getJobId());
                    json.put("status", "PENDING"); // This triggers Steve's flow
                    json.put("initial_message", pitch);
                    json.put("timestamp", timestamp);
                    return json.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return null;
                }
            }
        };

        // Add to queue
        ApiClient.getRequestQueue(requireContext()).add(request);
    }


    private void saveJobToSupabase(Job job) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String currentUserId = prefs.getString("user_id", null);

        if (currentUserId == null) {
            android.util.Log.e("Supabase", "Save failed: User ID is null");
            return;
        }

        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/saved_jobs";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    android.util.Log.d("Supabase", "Job saved successfully!");
                    Toast.makeText(requireContext(), "Job Saved!", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    // NEW: Print the actual error from the server
                    String responseBody = "";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    android.util.Log.e("Supabase", "Failed to save job. Error: " + responseBody);
                    android.util.Log.e("Supabase", "Status Code: " + (error.networkResponse != null ? error.networkResponse.statusCode : "N/A"));
                    Toast.makeText(requireContext(), "Error saving: Check Logcat", Toast.LENGTH_SHORT).show();
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
                    JSONObject json = new JSONObject();
                    json.put("user_id", currentUserId);
                    json.put("job_id", job.getJobId());
                    json.put("job_title", job.getTitle());
                    json.put("company_name", job.getCompanyName());
                    json.put("recruiter_id", job.getRecruiterId());
                    return json.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return null;
                }
            }
        };

        ApiClient.getRequestQueue(requireContext()).add(request);
    }
}