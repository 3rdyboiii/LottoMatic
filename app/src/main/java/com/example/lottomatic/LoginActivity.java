package com.example.lottomatic;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lottomatic.helper.Account;
import com.example.lottomatic.helper.AppVersion;
import com.example.lottomatic.helper.FakeActivity;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameTxt, passwordTxt;
    private CheckBox rememberMe;
    private Button loginButton;
    private Dialog progressDialog;
    private SharedPreferences sharedPreferences;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private OkHttpClient httpClient = new OkHttpClient();
    private Gson gson = new Gson();

    // Simple data classes
    public static class LoginRequest {
        public String username, password, version;
        public LoginRequest(String u, String p, String v) {
            username = u; password = p; version = v;
        }
    }

    public static class LoginResponse {
        public boolean success;
        public String message, username, password, gross, name, code, status, version;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        usernameTxt = findViewById(R.id.usernameTxt);
        passwordTxt = findViewById(R.id.passwordTxt);
        rememberMe = findViewById(R.id.rememberMeCheckBox);
        loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(this::login_Click);

        // Progress dialog
        progressDialog = new Dialog(this);
        progressDialog.setContentView(R.layout.progress_layout);
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        checkRememberedCredentials();
    }

    private void checkRememberedCredentials() {
        String savedUser = sharedPreferences.getString("username", null);
        String savedPass = sharedPreferences.getString("password", null);
        boolean isRemembered = sharedPreferences.getBoolean("rememberMe", false);

        if (isRemembered && savedUser != null) {
            usernameTxt.setText(savedUser);
            passwordTxt.setText(savedPass);
            rememberMe.setChecked(true);
        }
    }

    private void login_Click(View view) {
        String username = usernameTxt.getText().toString().trim();
        String password = passwordTxt.getText().toString().trim();

        if (username.isEmpty()) {
            usernameTxt.setError("Username is required");
            return;
        }
        if (password.isEmpty()) {
            passwordTxt.setError("Password is required");
            return;
        }

        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                // Create and send request
                LoginRequest loginRequest = new LoginRequest(username, password, AppVersion.VERSION_NAME);
                String jsonBody = gson.toJson(loginRequest);

                Request request = new Request.Builder()
                        .url("https://tdie91wa4d.execute-api.ap-southeast-1.amazonaws.com/UserInfo/login")
                        .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.body() != null) {
                        String responseData = response.body().string();

                        if (response.isSuccessful()) {
                            // Parse nested response
                            JsonObject fullResponse = gson.fromJson(responseData, JsonObject.class);
                            String bodyJson = fullResponse.get("body").getAsString();
                            LoginResponse loginResponse = gson.fromJson(bodyJson, LoginResponse.class);

                            handleLoginResponse(loginResponse, username, password);
                        } else {
                            runOnUiThread(() -> {
                                usernameTxt.setError("Server error: " + response.code());
                                passwordTxt.setText("");
                            });
                        }
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void handleLoginResponse(LoginResponse loginResponse, String username, String password) {
        runOnUiThread(() -> {

            if (loginResponse.success) {
                // Save user data
                // Save username from server response\
                Account.getInstance(this).setUsername(loginResponse.username);  // ✅ Sets username
                Account.getInstance(this).setPassword(loginResponse.password);  // ✅ Sets username
                Account.getInstance(this).setName(loginResponse.name);          // ✅ Sets name
                Account.getInstance(this).setCode(loginResponse.code);

                // ✅ DEBUG: Verify what we're saving
                System.out.println("🔐 DEBUG: Saving user data - " +
                        "Username: " + Account.getInstance(this).getUsername() + ", " +
                        "Name: " + Account.getInstance(this).getName() + ", " +
                        "Code: " + Account.getInstance(this).getCode());

                // Handle status
                if ("OFF".equals(loginResponse.status)) {
                    startActivity(new Intent(this, FakeActivity.class));
                } else {
                    // Save credentials if remembered
                    if (rememberMe.isChecked()) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("username", username);
                        editor.putString("password", password);
                        editor.putBoolean("rememberMe", true);
                        editor.apply();
                    } else {
                        passwordTxt.setText("");
                    }

                    // Go to main activity
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            } else {
                usernameTxt.setError("Invalid username or password");
                passwordTxt.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!executor.isShutdown()) executor.shutdown();
    }
}