package com.example.lottomatic.utility;

import android.os.CountDownTimer;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Locale;

public class DrawCountdownManager {
    private TextView timeTxt;
    private TextView drawTxt;
    private CountDownTimer countDownTimer;
    private String drawTime;

    public DrawCountdownManager(TextView timeTextView, TextView drawTextView, String drawTime) {
        this.timeTxt = timeTextView;
        this.drawTxt = drawTextView;
        this.drawTime = drawTime;
        this.drawTxt.setText(drawTime);
    }

    public void startCountdown() {
        // Stop any existing timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        long targetTimeInMillis = getNextDrawTimeInMillis(drawTime);
        long currentTimeInMillis = System.currentTimeMillis();
        long timeDifference = targetTimeInMillis - currentTimeInMillis;

        // If the draw time has passed for today, set for next day
        if (timeDifference < 0) {
            targetTimeInMillis += 24 * 60 * 60 * 1000; // Add 24 hours
            timeDifference = targetTimeInMillis - currentTimeInMillis;
        }

        countDownTimer = new CountDownTimer(timeDifference, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateCountdownText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                // When countdown finishes, restart for next draw
                timeTxt.setText("DRAW TIME!");
                // Restart countdown for next day after a short delay
                new CountDownTimer(5000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        // Optional: Show "Drawing..." or similar
                    }
                    @Override
                    public void onFinish() {
                        startCountdown(); // Restart for next day
                    }
                }.start();
            }
        }.start();
    }

    public void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private long getNextDrawTimeInMillis(String drawTime) {
        Calendar calendar = Calendar.getInstance();

        // Parse the draw time (format: "2:00 PM", "5:00 PM", "9:00 PM")
        String[] timeParts = drawTime.split(":");
        int hour = Integer.parseInt(timeParts[0].trim());
        String minuteAndPeriod = timeParts[1].trim();
        int minute = Integer.parseInt(minuteAndPeriod.split(" ")[0]);
        String period = minuteAndPeriod.split(" ")[1];

        // Convert to 24-hour format
        if (period.equalsIgnoreCase("PM") && hour != 12) {
            hour += 12;
        } else if (period.equalsIgnoreCase("AM") && hour == 12) {
            hour = 0;
        }

        // Set the target time
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    private void updateCountdownText(long millisUntilFinished) {
        long hours = millisUntilFinished / (1000 * 60 * 60);
        long minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (millisUntilFinished % (1000 * 60)) / 1000;

        String countdownText = String.format(Locale.getDefault(),
                "%02dH : %02dM : %02dS", hours, minutes, seconds);
        timeTxt.setText(countdownText);
    }
}
