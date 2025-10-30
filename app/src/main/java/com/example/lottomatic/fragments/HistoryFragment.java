package com.example.lottomatic.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.lottomatic.R;
import com.example.lottomatic.adapter.HistoryAdapter;
import com.example.lottomatic.helper.Account;
import com.example.lottomatic.helper.ConSQL;
import com.example.lottomatic.items.HistoryItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment {

    RecyclerView displayList;
    private HistoryAdapter adapter;
    private List<HistoryItem> itemList;
    private List<HistoryItem> filteredList;
    private SearchView searchView;
    private ImageView calendar;
    private ProgressBar progressBar;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isFragmentActive = false;

    private String selectedDate; // Store the selected date

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);

        displayList = v.findViewById(R.id.recyclerView);
        searchView = v.findViewById(R.id.searchView);
        calendar = v.findViewById(R.id.calendarBtn);
        progressBar = v.findViewById(R.id.progressBar);

        // Initialize with current date
        selectedDate = getCurrentDate();
        updateDateText();

        itemList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new HistoryAdapter(getContext(), filteredList);
        displayList.setLayoutManager(new LinearLayoutManager(getActivity()));
        displayList.setAdapter(adapter);

        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });

        // Set calendar click listener
        calendar.setOnClickListener(v1 -> {
            showDatePickerDialog();
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        fetchHistory(selectedDate);
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = new Date();
        return formatter.format(date);
    }

    private void updateDateText() {
        try {
            // Format the date for display (e.g., "October 27, 2024")
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

            Date date = inputFormat.parse(selectedDate);
            String displayDate = displayFormat.format(date);

            // Add "Today" indicator if it's the current date
            if (selectedDate.equals(getCurrentDate())) {
                displayDate += " (Today)";
            }

        } catch (Exception e) {
        }
    }

    private void showDatePickerDialog() {
        // Parse the current selected date or use current date
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(selectedDate);
            if (date != null) {
                calendar.setTime(date);
            }
        } catch (Exception e) {
            // Use current date if parsing fails
            calendar.setTime(new Date());
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Month is 0-based in Calendar, so we need to add 1 for display but not for storage
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    // Format the selected date for storage
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = sdf.format(selectedCalendar.getTime());

                    // Update the date text and fetch history for the selected date
                    updateDateText();
                    fetchHistory(selectedDate);
                },
                year, month, day
        );

        // Optional: Set maximum date to today (prevent future dates)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // Optional: Set minimum date if needed (e.g., one year ago)
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -1); // One year ago
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void fetchHistory(String date) {
        if (!isFragmentActive) {
            return;
        }

        final Context context = getContext();
        if (context == null) {
            return;
        }

        showProgress(true);

        final String agentName = Account.getInstance(context).getName();
        final String targetDate = date;

        executor.execute(() -> {
            if (!isFragmentActive) {
                return;
            }

            ConSQL c = new ConSQL();
            try (Connection connection = c.conclass()) {
                if (connection != null && isFragmentActive) {
                    String query = "SELECT combo, bets, prize, transcode, game, draw, date, result FROM EntryTB WHERE agent = ? AND CAST([date] AS DATE) = ? ORDER BY date DESC";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        preparedStatement.setString(1, agentName);
                        preparedStatement.setString(2, targetDate);
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            List<HistoryItem> tempList = new ArrayList<>();

                            while (resultSet.next() && isFragmentActive) {
                                String combo = resultSet.getString("combo");
                                double bets = resultSet.getDouble("bets");
                                double prize = resultSet.getDouble("prize");
                                String transcode = resultSet.getString("transcode");
                                String game = resultSet.getString("game");
                                String draw = resultSet.getString("draw");
                                String dateStr = resultSet.getString("date");
                                String result = resultSet.getString("result");

                                DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
                                String formattedBets = "₱" + decimalFormat.format(bets);
                                String formattedPrize = "₱" + decimalFormat.format(prize);

                                HistoryItem item = new HistoryItem(combo, formattedBets, draw, game, transcode, dateStr, formattedPrize, result);
                                tempList.add(item);
                            }

                            if (isFragmentActive && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (isFragmentActive) {
                                        itemList.clear();
                                        itemList.addAll(tempList);
                                        filteredList.clear();
                                        filteredList.addAll(tempList);
                                        adapter.notifyDataSetChanged();
                                        showProgress(false);

                                        if (tempList.isEmpty()) {
                                            Toast.makeText(getContext(), "No records found for " + getFormattedDisplayDate(targetDate), Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getContext(), "Found " + tempList.size() + " records", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isFragmentActive) {
                            showProgress(false);
                            Toast.makeText(getContext(), "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private String getFormattedDisplayDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return displayFormat.format(date);
        } catch (Exception e) {
            return dateString;
        }
    }

    private void filterList(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(itemList);
        } else {
            String searchText = text.toLowerCase();
            for (HistoryItem item : itemList) {
                if (item.getCombo().toLowerCase().contains(searchText) ||
                        item.getGame().toLowerCase().contains(searchText) ||
                        item.getDraw().toLowerCase().contains(searchText) ||
                        item.getTranscode().toLowerCase().contains(searchText)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showProgress(boolean show) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (!isFragmentActive) return;

            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (displayList != null) {
                displayList.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });
    }
}