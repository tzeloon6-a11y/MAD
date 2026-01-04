package com.example.campuslink;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.example.campuslink.SavedJobsManager;

import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.Duration;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements CardStackListener {

    private CardStackLayoutManager layoutManager;
    private CardStackAdapter adapter;
    private CardStackView cardStackView;
    private LinearLayout emptyStateLayout;
    private View swipedCardView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
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
        List<Job> jobs = createDummyJobs();
        adapter = new CardStackAdapter(jobs);
        cardStackView.setAdapter(adapter);

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
                    // We disable the button to uniquely identify this action later
                    topCard.findViewById(R.id.save_button).setEnabled(false);
                }
                triggerSwipe(Direction.Top, Duration.Slow.duration);
            }
        });
    }

    private void animateButton(View button) {
        if (button != null) {
            button.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        button.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100);
                    })
                    .start();
        }
    }

    private void triggerSwipe(Direction direction, int duration) {
        swipedCardView = layoutManager.getTopView(); // Store the view reference BEFORE swiping
        SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                .setDirection(direction)
                .setDuration(duration)
                .build();
        layoutManager.setSwipeAnimationSetting(setting);
        cardStackView.swipe();
    }

    private List<Job> createDummyJobs() {
        List<Job> jobs = new ArrayList<>();
        jobs.add(new Job("job101", "Barista", "Campus Coffee", "RM15/hr", "Student Union Building", "Prepare and serve a variety of coffee drinks, teas, and pastries. Must have great customer service skills.", "recruiter1"));
        jobs.add(new Job("job102", "Library Assistant", "Main Library", "RM14/hr", "Central Campus", "Assist students with finding books, manage check-ins/check-outs, and help maintain a quiet study environment.", "recruiter2"));
        jobs.add(new Job("job103", "IT Help Desk Support", "Tech Services", "RM18/hr", "Science & Tech Hall", "Provide technical support to students and faculty. Troubleshoot hardware and software issues. Basic knowledge of networking is a plus.", "recruiter3"));
        jobs.add(new Job("job104", "Campus Tour Guide", "Admissions Office", "RM16/hr", "Welcome Center", "Lead tours for prospective students and their families. Share your positive campus experience and answer questions about student life.", "recruiter4"));
        jobs.add(new Job("job105", "Research Assistant", "Biology Department", "RM20/hr", "Darwin Hall", "Support a professor with ongoing research. Tasks include data collection, lab maintenance, and literature reviews.", "recruiter5"));
        return jobs;
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
            recordInterest(swipedJob);
        } else if (direction == Direction.Left) {
            Log.d("Logic", "Student not interested in job: " + swipedJob.getJobId());
        } else if (direction == Direction.Top) {
            SavedJobsManager.getInstance().saveJob(swipedJob);
            Log.d("Logic", "SAVED FOR LATER: Student saved job: " + swipedJob.getJobId());
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
        swipedCardView = null; // Clear the stored view on cancel
        Log.d("CardStackView", "onCardCanceled");
    }

    private void recordInterest(Job job) {
        String studentId = "student123";
        Log.d("Logic", "INTERESTED: Student: " + studentId + " | Job ID: " + job.getJobId());
        Toast.makeText(requireContext(), "Marked as Interested!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCardRewound() { }

    @Override
    public void onCardAppeared(View view, int position) { }

    @Override
    public void onCardDisappeared(View view, int position) {
        // THIS IS THE CORRECTED LOGIC
        if (swipedCardView == view) {
            // Check if the swipe was initiated by the save button.
            // We identify this because we set the button to disabled in onSaveClicked().
            MaterialButton saveButton = view.findViewById(R.id.save_button);
            if (saveButton != null && !saveButton.isEnabled()) {
                animateSaveButton(view);
            }
        }

        // Clear the stored view reference after using it.
        swipedCardView = null;
    }

    private void animateSaveButton(View cardView) {
        MaterialButton saveButton = cardView.findViewById(R.id.save_button);
        if (saveButton != null) {
            saveButton.setIconResource(R.drawable.ic_bookmark_filled);
            saveButton.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        saveButton.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(150);
                        // Re-enable the button after animation in case the user rewinds
                        saveButton.setEnabled(true);
                    })
                    .start();
        }
    }
}








