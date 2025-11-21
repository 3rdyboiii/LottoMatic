package com.example.lottomatic.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lottomatic.R;
import com.example.lottomatic.helper.Account;
import com.example.lottomatic.helper.ConSQL;
import com.example.lottomatic.items.HistoryItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultFragment extends Fragment {

    String selectedDraw = "2PM"; // default

    TextView result4d;
    TextView result3d;
    TextView result2d;
    View bodylayout;
    private ProgressBar progressBar;
    private RadioGroup drawGroup;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isFragmentActive = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_result, container, false);

        // RESULTS
        result4d = v.findViewById(R.id.result4D);
        result3d = v.findViewById(R.id.result3D);
        result2d = v.findViewById(R.id.result2D);

        bodylayout = v.findViewById(R.id.bodylayout);

        // RADIO BUTTON GROUP
        drawGroup = v.findViewById(R.id.drawGroup); // ADD THIS ID IN XML

        progressBar = v.findViewById(R.id.progressBar); // if you have one

        // RadioGroup Listener
        drawGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.draw2PM) {
                selectedDraw = "2PM";
            } else if (checkedId == R.id.draw5PM) {
                selectedDraw = "5PM";
            } else if (checkedId == R.id.draw9PM) {
                selectedDraw = "9PM";
            }
            fetchResult(selectedDraw);
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;

        // Fetch initial value
        int id = drawGroup.getCheckedRadioButtonId();
        if (id != -1) {
            drawGroup.check(id);
        } else {
            drawGroup.check(R.id.draw2PM); // default
        }
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

    private void fetchResult(String draw) {
        if (!isFragmentActive) return;

        final Context context = getContext();
        if (context == null) return;

        showProgress(true);

        executor.execute(() -> {
            if (!isFragmentActive) return;

            String result4 = "";
            String result3 = "";
            String result2 = "";

            ConSQL c = new ConSQL();

            try (Connection connection = c.conclass()) {

                if (connection != null && isFragmentActive) {

                    String query = "SELECT game, result FROM ResultTB WHERE draw = ? AND game IN ('4D', '3D', '2D') AND [group] = 'BICOL'";

                    try (PreparedStatement ps = connection.prepareStatement(query)) {
                        ps.setString(1, draw);

                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next() && isFragmentActive) {
                                String game = rs.getString("game");
                                String res = rs.getString("result");

                                if ("4D".equals(game)) result4 = res;
                                if ("3D".equals(game)) result3 = res;
                                if ("2D".equals(game)) result2 = res;
                            }
                        }
                    }
                }

            } catch (SQLException e) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(context, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
                return;
            }

            // Update UI
            if (isFragmentActive && getActivity() != null) {
                String final4 = result4;
                String final3 = result3;
                String final2 = result2;

                getActivity().runOnUiThread(() -> {
                    if (!isFragmentActive) return;

                    result4d.setText(final4.isEmpty() ? "----" : final4);
                    result3d.setText(final3.isEmpty() ? "----" : final3);
                    result2d.setText(final2.isEmpty() ? "----" : final2);

                    showProgress(false);
                });
            }
        });
    }

    private void showProgress(boolean show) {
        if (getActivity() == null || progressBar == null) return;

        getActivity().runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            bodylayout.setVisibility(show ? View.GONE : View.VISIBLE);
        });
    }
}
