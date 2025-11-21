package com.example.lottomatic.utility;

import android.os.CountDownTimer;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class DrawCountdownManager {
    private CountDownTimer countDownTimer;
    private TextView textView;
    private long targetTimeMillis;
    private OnCountdownListener listener;

    public interface OnCountdownListener {
        void onCountdownFinished();
    }

    public DrawCountdownManager(TextView textView, long targetTimeMillis) {
        this.textView = textView;
        this.targetTimeMillis = targetTimeMillis;
    }

    public void setOnCountdownListener(OnCountdownListener listener) {
        this.listener = listener;
    }

    public void startCountdown() {
        long remaining = targetTimeMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            textView.setText("00:00:00");
            if (listener != null) listener.onCountdownFinished();
            return;
        }

        countDownTimer = new CountDownTimer(remaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                String formatted = String.format("%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60,
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60);
                textView.setText(formatted);
            }

            @Override
            public void onFinish() {
                textView.setText("00:00:00");
                if (listener != null) listener.onCountdownFinished();
            }
        }.start();
    }

    public void stopCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
