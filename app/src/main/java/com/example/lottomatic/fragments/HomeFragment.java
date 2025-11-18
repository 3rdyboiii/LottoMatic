package com.example.lottomatic.fragments;

import static androidx.core.content.ContextCompat.registerReceiver;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lottomatic.EntryActivity;
import com.example.lottomatic.LoginActivity;
import com.example.lottomatic.MainActivity;
import com.example.lottomatic.R;
import com.example.lottomatic.adapter.home_games_adapter;
import com.example.lottomatic.helper.Account;
import com.example.lottomatic.helper.AppVersion;
import com.example.lottomatic.helper.ConSQL;
import com.example.lottomatic.items.MenuItem;
import com.example.lottomatic.utility.NetworkChangeListener;

import java.math.BigDecimal;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.Date; // Add this import
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    TextView GrossTxt;
    ProgressBar progbar;
    RecyclerView gamelist;
    private home_games_adapter adapter;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isFragmentActive = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        progbar = view.findViewById(R.id.progressBar);
        gamelist = view.findViewById(R.id.GameLists);
        GrossTxt = view.findViewById(R.id.grossTxt);

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
        isFragmentActive = true;
        fetchGross();

        if (adapter != null) {
            adapter.startAllCountdowns();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
        progbar.setVisibility(View.GONE);

        if (adapter != null) {
            adapter.stopAllCountdowns();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
        progbar.setVisibility(View.GONE);

        if (adapter != null) {
            adapter.stopAllCountdowns();
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = new Date();
        return formatter.format(date);
    }

    private void fetchGross() {
        if (!isFragmentActive) {
            return;
        }

        // Get context safely at the start
        final Context context = getContext();
        if (context == null) {
            return;
        }

        // Get account name on UI thread before starting background task
        final String agentName = Account.getInstance(context).getName();

        progbar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            // Check fragment state at the beginning of background task
            if (!isFragmentActive) {
                return;
            }

            ConSQL c = new ConSQL();
            try (Connection connection = c.conclass()) {
                if (connection != null && isFragmentActive) {
                    String query = "SELECT SUM(bets) as gross FROM EntryTB WHERE agent = ? AND CAST([date] AS DATE) = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        preparedStatement.setString(1, agentName); // Use the pre-fetched agent name
                        preparedStatement.setString(2, getCurrentDate());
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            if (resultSet.next() && isFragmentActive) {
                                Double TotalGross = resultSet.getDouble("gross");
                                DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
                                String total = decimalFormat.format(TotalGross);

                                // Update UI on main thread with safety checks
                                updateUIOnMainThread(total);
                            } else if (isFragmentActive) {
                                showErrorOnMainThread("Error fetching Gross");
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (isFragmentActive) {
                    showErrorOnMainThread("Database Error: " + e.getMessage());
                }
            }
        });
    }

    private void  updateUIOnMainThread(final String total) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            // Final safety check before updating UI
            if (!isFragmentActive || GrossTxt == null || progbar == null) {
                return;
            }

            GrossTxt.setText("₱ " + total);
            progbar.setVisibility(View.GONE);
        });
    }

    private void showErrorOnMainThread(final String message) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (!isFragmentActive) return;

            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            if (progbar != null) {
                progbar.setVisibility(View.GONE);
            }
        });
    }

    private void handleGameItemClick(int position, MenuItem item) {
        if (!isFragmentActive || getActivity() == null) {
            return;
        }

        // For now, allow all clicks until you implement the enabled check
        int gameImage = item.getImageResId();
        String gameName = item.getGame();
        String gameDraw = item.getDraw();

        Intent intent = new Intent(getActivity(), EntryActivity.class);
        intent.putExtra("gameName", gameName);
        intent.putExtra("gameDraw", gameDraw);
        intent.putExtra("gameImage", gameImage);
        if (item.getGame().equals("4D") || item.getGame().equals("3D") || item.getGame().equals("2D")) {
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Coming Soon", Toast.LENGTH_SHORT).show();
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
        return list;
    }
}