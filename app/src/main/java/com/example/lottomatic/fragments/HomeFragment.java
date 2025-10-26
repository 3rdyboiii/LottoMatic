package com.example.lottomatic.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lottomatic.EntryActivity;
import com.example.lottomatic.R;
import com.example.lottomatic.adapter.home_games_adapter;
import com.example.lottomatic.helper.Account;
import com.example.lottomatic.items.MenuItem;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    TextView gross;
    RecyclerView gamelist;
    private home_games_adapter adapter;
    private OkHttpClient httpClient = new OkHttpClient();
    private Gson gson = new Gson();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isRefreshing = false;

    // Response class for login response
    public static class LoginResponse {
        public boolean success;
        public String message, username, name, code, status;
        public double gross; // Make sure this field exists
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        gamelist = view.findViewById(R.id.GameLists);
        gross = view.findViewById(R.id.grossTxt);

        System.out.println("🏠 DEBUG: onCreateView - Initializing HomeFragment");

        // ✅ Display cached data immediately
        displayGrossFromAccount();

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        gamelist.setLayoutManager(layoutManager);

        List<MenuItem> gameMenuList = getGameMenuList();
        adapter = new home_games_adapter(gameMenuList);

        adapter.setOnItemClickListener(new home_games_adapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, MenuItem item) {
                handleGameItemClick(position, item);
            }
        });

        gamelist.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("🏠 DEBUG: onResume - HomeFragment is now visible");

        // ✅ Auto-refresh when user comes to home screen
        refreshGrossData();

        // Restart countdowns
        if (adapter != null) {
            adapter.startAllCountdowns();
        }
    }

    // ✅ SIMPLE: Display cached data (fast)
    private void displayGrossFromAccount() {
        Account account = Account.getInstance(requireContext());
        double grossAmount = account.getGross();
        String formattedGross = formatAsCurrency(grossAmount);

        System.out.println("💰 DEBUG: Displaying gross from Account - " + grossAmount + " -> " + formattedGross);

        gross.setText("₱ " + formattedGross);
    }

    // ✅ REFRESH: Fetch latest data from server
    private void refreshGrossData() {
        if (isRefreshing) {
            System.out.println("🔄 DEBUG: Refresh already in progress, skipping");
            return; // Prevent multiple simultaneous refreshes
        }

        Account account = Account.getInstance(requireContext());
        String username = account.getUsername();
        String password = account.getPassword();

        System.out.println("🔐 DEBUG: refreshGrossData - Username: " + username + ", Password: " + (password != null ? "***" : "NULL"));

        if (username == null || username.isEmpty()) {
            System.out.println("❌ DEBUG: Username is null or empty");
            gross.setText("₱ 0.00");
            return;
        }

        if (password == null || password.isEmpty()) {
            System.out.println("❌ DEBUG: Password is null or empty - cannot refresh");
            Toast.makeText(getActivity(), "Cannot refresh: Password not available", Toast.LENGTH_SHORT).show();
            return;
        }

        isRefreshing = true;
        System.out.println("🔄 DEBUG: Starting refresh process");

        executor.execute(() -> {
            try {
                System.out.println("🌐 DEBUG: Executing API call for username: " + username);

                // Create request data
                JsonObject requestData = new JsonObject();
                requestData.addProperty("username", username);
                requestData.addProperty("password", password);
                requestData.addProperty("version", "1.0.0"); // Add version if required

                String jsonBody = requestData.toString();
                System.out.println("📦 DEBUG: Request body: " + jsonBody);

                Request request = new Request.Builder()
                        .url("https://tdie91wa4d.execute-api.ap-southeast-1.amazonaws.com/UserInfo/login")
                        .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("🌐 DEBUG: Request URL: " + request.url());
                System.out.println("🌐 DEBUG: Request headers: " + request.headers());

                try (Response response = httpClient.newCall(request).execute()) {
                    System.out.println("🌐 DEBUG: Response received - Code: " + response.code() + ", Message: " + response.message());

                    if (response.body() != null) {
                        String responseData = response.body().string();
                        System.out.println("📄 DEBUG: Raw response: " + responseData);

                        if (response.isSuccessful()) {
                            System.out.println("✅ DEBUG: Response is successful");

                            JsonObject fullResponse = gson.fromJson(responseData, JsonObject.class);
                            System.out.println("📄 DEBUG: Full response parsed: " + fullResponse);

                            // Check if response has "body" field (API Gateway format)
                            if (fullResponse.has("body")) {
                                String bodyJson = fullResponse.get("body").getAsString();
                                System.out.println("📄 DEBUG: Body content: " + bodyJson);

                                LoginResponse loginResponse = gson.fromJson(bodyJson, LoginResponse.class);
                                System.out.println("📄 DEBUG: LoginResponse - success: " + loginResponse.success + ", gross: " + loginResponse.gross);

                                if (loginResponse.success) {
                                    // ✅ Update the gross amount in Account
                                    account.setGross(loginResponse.gross);
                                    System.out.println("💰 DEBUG: Updated gross in Account: " + loginResponse.gross);

                                    getActivity().runOnUiThread(() -> {
                                        displayGrossFromAccount(); // Update display
                                        Toast.makeText(getActivity(), "Gross updated!", Toast.LENGTH_SHORT).show();
                                        System.out.println("✅ DEBUG: UI updated successfully");
                                    });
                                } else {
                                    System.out.println("❌ DEBUG: LoginResponse success is false");
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getActivity(), "Update failed: " + loginResponse.message, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            } else {
                                System.out.println("❌ DEBUG: Response does not have 'body' field");
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getActivity(), "Invalid response format", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            System.out.println("❌ DEBUG: Response not successful - HTTP " + response.code());
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getActivity(), "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        System.out.println("❌ DEBUG: Response body is null");
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getActivity(), "Empty response from server", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("💥 DEBUG: Exception occurred: " + e.getMessage());
                e.printStackTrace();
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                isRefreshing = false;
                System.out.println("🔄 DEBUG: Refresh process completed");
            }
        });
    }

    // ✅ Add this method for manual refresh (call from button)
    public void manualRefresh() {
        System.out.println("🔄 DEBUG: Manual refresh triggered");
        refreshGrossData();
    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("🏠 DEBUG: onPause - HomeFragment is pausing");
        if (adapter != null) {
            adapter.stopAllCountdowns();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        System.out.println("🏠 DEBUG: onDestroyView - Cleaning up resources");
        if (adapter != null) {
            adapter.stopAllCountdowns();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private String formatAsCurrency(double amount) {
        try {
            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###.00");
            return formatter.format(amount);
        } catch (Exception e) {
            System.out.println("❌ DEBUG: Error formatting currency: " + e.getMessage());
            return "0.00";
        }
    }

    private void handleGameItemClick(int position, MenuItem item) {
        int gameImage = item.getImageResId();
        String gameName = item.getGame();
        String gameDraw = item.getDraw();

        System.out.println("🎮 DEBUG: Game clicked - Position: " + position + ", Game: " + gameName + ", Draw: " + gameDraw);

        Intent intent = new Intent(getActivity(), EntryActivity.class);
        intent.putExtra("gameName", gameName);
        intent.putExtra("gameDraw", gameDraw);
        intent.putExtra("gameImage", gameImage);

        if (gameName.equals("4D") || gameName.equals("3D") || gameName.equals("2D")) {
            System.out.println("🚀 DEBUG: Starting EntryActivity for " + gameName);
            startActivity(intent);
        } else {
            System.out.println("ℹ️ DEBUG: Game " + gameName + " not implemented yet");
            Toast.makeText(getActivity(), gameName + " coming soon!", Toast.LENGTH_SHORT).show();
        }
    }

    private List<MenuItem> getGameMenuList() {
        List<MenuItem> list = new ArrayList<>();

        list.add(new MenuItem(R.drawable.icon_ultra, "ultra", "9:00 PM", "12H : 30M : 06S", "363,258,451"));
        list.add(new MenuItem(R.drawable.icon_649, "649", "9:00 PM", "12H : 30M : 06S", "84,961,231"));
        list.add(new MenuItem(R.drawable.icon_642, "642", "9:00 PM", "12H : 30M : 06S", "35,318,653"));
        list.add(new MenuItem(R.drawable.icon_6d, "6D", "9:00 PM", "12H : 30M : 06S", "150,000"));
        list.add(new MenuItem(R.drawable.icon_4d, "4D", "9:00 PM", "12H : 30M : 06S", "40,000"));
        list.add(new MenuItem(R.drawable.icon_3d, "3D", "9:00 PM", "12H : 30M : 06S", "4,500"));
        list.add(new MenuItem(R.drawable.icon_3d, "3D", "5:00 PM", "12H : 30M : 06S", "4,500"));
        list.add(new MenuItem(R.drawable.icon_3d, "3D", "2:00 PM", "12H : 30M : 06S", "4,500"));
        list.add(new MenuItem(R.drawable.icon_2d, "2D", "9:00 PM", "12H : 30M : 06S", "4,000"));
        list.add(new MenuItem(R.drawable.icon_2d, "2D", "5:00 PM", "12H : 30M : 06S", "4,000"));
        list.add(new MenuItem(R.drawable.icon_2d, "2D", "2:00 PM", "12H : 30M : 06S", "4,000"));

        System.out.println("🎲 DEBUG: Created " + list.size() + " game menu items");
        return list;
    }
}