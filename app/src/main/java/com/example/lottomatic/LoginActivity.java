package com.example.lottomatic;

import android.app.Dialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.lottomatic.helper.Account;
import com.example.lottomatic.helper.AppVersion;
import com.example.lottomatic.helper.ConSQL;
import com.example.lottomatic.helper.FakeActivity;
import com.example.lottomatic.utility.NetworkChangeListener;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private EditText user;
    private EditText pass;
    private CheckBox rememberMe;
    private ProgressBar loginProgressBar;
    private TextView progressText;
    private Button loginButton;
    private SharedPreferences sharedPreferences;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    NetworkChangeListener networkChangeListener = new NetworkChangeListener();

    // Flag to track if login is in progress
    private boolean isLoggingIn = false;

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeListener, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkChangeListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }

        user = findViewById(R.id.usernameTxt);
        pass = findViewById(R.id.passwordTxt);
        loginButton = findViewById(R.id.loginButton);
        rememberMe = findViewById(R.id.rememberMeCheckBox);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        progressText = findViewById(R.id.progressText);

        sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        checkRememberedCredentials();

        loginButton.setOnClickListener(this::login_Click);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor to prevent memory leaks
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    private void checkRememberedCredentials() {
        String savedUsername = sharedPreferences.getString("username", null);
        String savedPassword = sharedPreferences.getString("password", null);
        boolean isRemembered = sharedPreferences.getBoolean("rememberMe", false);

        if (isRemembered) {
            user.setText(savedUsername);
            pass.setText(savedPassword);
            rememberMe.setChecked(true);
        }
    }

    private void login_Click(View view) {
        if (isLoggingIn) {
            return; // Prevent multiple login attempts
        }

        if (user.getText().toString().isEmpty()) {
            user.setError("Username is required");
            return;
        }
        if (pass.getText().toString().isEmpty()) {
            pass.setError("Password is required");
            return;
        }

        // Show progress bar and disable UI
        showProgress(true);
        isLoggingIn = true;

        String username = user.getText().toString();
        String password = pass.getText().toString();

        executor.execute(() -> {
            ConSQL c = new ConSQL();
            try (Connection connection = c.conclass()) {
                if (connection != null) {
                    String query = "SELECT username, password, name, version, code, [group] FROM UserTB WHERE username = ? AND password = ? AND [group] = 'BICOL'";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        preparedStatement.setString(1, username);
                        preparedStatement.setString(2, password);
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            if (resultSet.next()) {
                                String dbVersion = resultSet.getString("version");
                                String name = resultSet.getString("name");
                                String code = resultSet.getString("code");
                                String group = resultSet.getString("group");

                                if (!AppVersion.VERSION_NAME.equals(dbVersion)) {
                                    runOnUiThread(() -> {
                                        showProgress(false);
                                        isLoggingIn = false;
                                        showUpdateRequiredDialog();
                                    });
                                    return;
                                }

                                Account.getInstance(this).setName(name);
                                Account.getInstance(this).setCode(code);
                                Account.getInstance(this).setGroup(group);
                                fetchBetLimits(connection);

                                runOnUiThread(() -> {
                                    if (rememberMe.isChecked()) {
                                        saveCredentials(username, password, true);
                                    } else {
                                        clearCredentials();
                                        pass.setText("");
                                    }

                                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                });
                            } else {
                                runOnUiThread(() -> {
                                    showProgress(false);
                                    isLoggingIn = false;
                                    user.setError("Invalid username or password");
                                    pass.setText("");
                                    Toast.makeText(LoginActivity.this, "Login Error!", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    }
                } else {
                    runOnUiThread(() -> {
                        showProgress(false);
                        isLoggingIn = false;
                        Toast.makeText(LoginActivity.this, "Database connection failed", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showProgress(false);
                    isLoggingIn = false;
                    Toast.makeText(LoginActivity.this, "Database Error Occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showProgress(false);
                    isLoggingIn = false;
                    Toast.makeText(LoginActivity.this, "Unexpected error occurred", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showProgress(boolean show) {
        runOnUiThread(() -> {
            if (show) {
                loginProgressBar.setVisibility(View.VISIBLE);
                progressText.setVisibility(View.VISIBLE);
                loginButton.setEnabled(false);
                loginButton.setAlpha(0.5f);
                user.setEnabled(false);
                pass.setEnabled(false);
                rememberMe.setEnabled(false);
            } else {
                loginProgressBar.setVisibility(View.GONE);
                progressText.setVisibility(View.GONE);
                loginButton.setEnabled(true);
                loginButton.setAlpha(1.0f);
                user.setEnabled(true);
                pass.setEnabled(true);
                rememberMe.setEnabled(true);
                isLoggingIn = false;
            }
        });
    }

    private void fetchBetLimits(Connection connection) throws SQLException {
        String query = "SELECT GameType, LimitAmount FROM BetLimits WHERE [group] = 'BICOL'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            Map<String, Double> limits = new HashMap<>();
            while (rs.next()) {
                limits.put(rs.getString("GameType"), rs.getDouble("LimitAmount"));
            }

            // Store in Account class
            Account.getInstance(this).setBetLimits(limits);
        }
    }

    private void showUpdateRequiredDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Required");
        builder.setMessage("Your app version is outdated. Please update to the latest version to continue using the application.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveCredentials(String username, String password, boolean rememberMeChecked) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        editor.putBoolean("rememberMe", rememberMeChecked);
        editor.apply();
    }

    private void clearCredentials() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("username");
        editor.remove("password");
        editor.remove("rememberMe");
        editor.apply();
    }
}