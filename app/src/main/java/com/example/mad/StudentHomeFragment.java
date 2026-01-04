package com.example.mad;

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
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.material.button.MaterialButton;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.Duration;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We are using a UNIQUE layout name here to avoid conflicts
        View view = inflater.inflate(R.layout.fragment_student_home, container, false);

        cardStackView = view.findViewById(R.id.card_stack_view);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state);
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
}
