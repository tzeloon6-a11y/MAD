package com.example.mad;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

public class NotificationHelper {
    
    private static final long NOTIFICATION_DURATION = 2500; // 2.5 seconds
    
    /**
     * Shows a notification banner for a specified duration
     * @param bannerView The notification banner view (should be initially GONE)
     * @param message The message to display
     */
    public static void showNotificationBanner(View bannerView, String message) {
        if (bannerView == null) return;
        
        TextView tvNotificationText = bannerView.findViewById(R.id.tv_notification_text);
        if (tvNotificationText != null) {
            tvNotificationText.setText(message);
        }
        
        // Show the banner with animation
        bannerView.setVisibility(View.VISIBLE);
        bannerView.setAlpha(0f);
        bannerView.setTranslationY(-bannerView.getHeight());
        
        ObjectAnimator slideDown = ObjectAnimator.ofFloat(bannerView, "translationY", 0f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(bannerView, "alpha", 1f);
        
        slideDown.setDuration(300);
        fadeIn.setDuration(300);
        
        slideDown.start();
        fadeIn.start();
        
        // Hide after duration
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> hideNotificationBanner(bannerView), NOTIFICATION_DURATION);
    }
    
    /**
     * Hides the notification banner with animation
     */
    private static void hideNotificationBanner(View bannerView) {
        if (bannerView == null || bannerView.getVisibility() != View.VISIBLE) return;
        
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(bannerView, "translationY", -bannerView.getHeight());
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(bannerView, "alpha", 0f);
        
        slideUp.setDuration(300);
        fadeOut.setDuration(300);
        
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                bannerView.setVisibility(View.GONE);
            }
        });
        
        slideUp.start();
        fadeOut.start();
    }
}


