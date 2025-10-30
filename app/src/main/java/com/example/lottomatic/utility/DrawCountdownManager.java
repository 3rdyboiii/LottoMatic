package com.example.lottomatic.util;

import android.os.CountDownTimer;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Locale;

public class DrawCountdownManager {
    private TextView timeTextView;
    private String drawTime;
    private CountDownTimer countDownTimer;
    private OnCountdownListener listener;

    public interface OnCountdownListener {
        void onDrawTimeReached();
    }

    public DrawCountdownManager(TextView timeTextView, String drawTime) {
        this.timeTextView = timeTextView;
        this.drawTime = drawTime;
    }

    public void setOnCountdownListener(OnCountdownListener listener) {
        this.listener = listener;
    }

    public void startCountdown() {
        stopCountdown(); // Stop any existing timer

        long targetTime = getTargetTimeInMillis();
        long currentTime = System.currentTimeMillis();
        long timeDifference = targetTime - currentTime;

        // If draw time already passed, show "DRAW TIME" and notify
        if (timeDifference <= 0) {
            timeTextView.setText("DRAW TIME");
            if (listener != null) {
                listener.onDrawTimeReached();
            }
            return;
        }

        countDownTimer = new CountDownTimer(timeDifference, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimeText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timeTextView.setText("DRAW TIME");
                if (listener != null) {
                    listener.onDrawTimeReached();
                }
            }
        }.start();
    }

    public void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    public boolean isDrawTime() {
        long targetTime = getTargetTimeInMillis();
        return System.currentTimeMillis() >= targetTime;
    }

    private long getTargetTimeInMillis() {
        Calendar calendar = Calendar.getInstance();

        // Parse time like "2:00 PM"
        String[] parts = drawTime.split(":");
        int hour = Integer.parseInt(parts[0].trim());
        String[] minutePart = parts[1].trim().split(" ");
        int minute = Integer.parseInt(minutePart[0]);
        String period = minutePart[1];

        // Convert to 24-hour format
        if (period.equalsIgnoreCase("PM") && hour != 12) {
            hour += 12;
        } else if (period.equalsIgnoreCase("AM") && hour == 12) {
            hour = 0;
        }

        // Set target time for today
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long targetTime = calendar.getTimeInMillis();
        long currentTime = System.currentTimeMillis();

        // If target time passed, set for tomorrow
        if (targetTime <= currentTime) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            return calendar.getTimeInMillis();
        }

        return targetTime;
    }

    private void updateTimeText(long millisUntilFinished) {
        long hours = millisUntilFinished / (1000 * 60 * 60);
        long minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (millisUntilFinished % (1000 * 60)) / 1000;

        String timeText = String.format(Locale.getDefault(),
                "%02dH : %02dM : %02dS", hours, minutes, seconds);
        timeTextView.setText(timeText);
    }
}