package com.example.lottomatic;

import static android.app.PendingIntent.getActivity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.lottomatic.fragments.AccountFragment;
import com.example.lottomatic.fragments.HistoryFragment;
import com.example.lottomatic.fragments.HomeFragment;
import com.example.lottomatic.fragments.ResultFragment;
import com.example.lottomatic.helper.Account;
import com.example.lottomatic.helper.ConSQL;
import com.example.lottomatic.utility.CaptureAct;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bnv_bottom;
    private String Name = "";
    FloatingActionButton scanBtn;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }

        Name = Account.getInstance(this).getName();

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showLogoutConfirmationDialog();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        bnv_bottom = findViewById(R.id.bnv_bottom);
        scanBtn = findViewById(R.id.scanBtn);
        scanBtn.setOnClickListener(v -> scanCode());

        loadFragment(new HomeFragment());

        bnv_bottom.setBackground(null);
        bnv_bottom.setOnApplyWindowInsetsListener(null);
        bnv_bottom.getMenu().getItem(2).setEnabled(false);
        bnv_bottom.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();
            if (id == R.id.tab_home) selectedFragment = new HomeFragment();
            else if (id == R.id.tab_history) selectedFragment = new HistoryFragment();
            else if (id == R.id.tab_results) selectedFragment = new ResultFragment();
            else if (id == R.id.tab_account) selectedFragment = new AccountFragment();

            if (selectedFragment != null) loadFragment(selectedFragment);
            return true;
        });
    }

    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to flash on");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }

    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            String ticketNo = result.getContents();
            executor.execute(() -> {
                boolean claimed = isTicketAlreadyClaimed(ticketNo);
                runOnUiThread(() -> {
                    if (claimed) {
                        showDialog("Ticket has already claimed!", ticketNo);
                    } else {
                        fetchWinningCode(ticketNo); // continue normal flow
                    }
                });
            });
        }
    });


    private boolean isTicketAlreadyClaimed(String ticketNo) {
        ConSQL c = new ConSQL();
        String query = "SELECT * FROM EntryTB WHERE transcode = ? AND claimed = 'yes'";
        try (Connection connection = c.conclass()) {
            if (connection != null) {
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, ticketNo);
                    ResultSet rs = stmt.executeQuery();
                    return rs.next(); // true if already claimed
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // default to not claimed
    }

    private void fetchWinningCode(String ticketNo) {
        ConSQL c = new ConSQL();
        try (Connection conn = c.conclass()) {
            if (conn == null) return;
        String fetchQuery = "SELECT combo FROM EntryTB WHERE combo = result AND agent = ? AND qrcode = ?";
        try (PreparedStatement stmt = conn.prepareStatement(fetchQuery)) {
            stmt.setString(1, Name);
            stmt.setString(2, ticketNo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String winningCombo = rs.getString("combo");
                runOnUiThread(() -> showDialog("Winning Ticket No.:", winningCombo, ticketNo));
            } else {
                runOnUiThread(() -> showDialog("Ticket No. did not win:", ticketNo));
            }
        }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showDialog(String title, String message, String ticketNo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", (dialog, which) -> {
            if (ticketNo != null) approveWinCombination(ticketNo);
            dialog.dismiss();
        });
        builder.show();
    }

    private void showDialog(String title, String message) {
        showDialog(title, message, null);
    }

    private void approveWinCombination(String transcode) {
        executor.execute(() -> {
            ConSQL c = new ConSQL();
            String updateQuery = "UPDATE EntryTB SET claimed = 'yes' WHERE agent = ? AND qrcode = ? AND claimed IS NULL";
            try (Connection conn = c.conclass();
                 PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                stmt.setString(1, Name);
                stmt.setString(2, transcode);
                int affectedRows = stmt.executeUpdate();
                runOnUiThread(() -> {
                    if (affectedRows > 0) {
                        Toast.makeText(this, "Winning Combination has been Claimed!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Winning Combination already Claimed!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void showLogoutConfirmationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_confirmdialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        Button positive = dialog.findViewById(R.id.positiveButton);
        Button negative = dialog.findViewById(R.id.negativeButton);
        TextView title = dialog.findViewById(R.id.dialogTitle);
        TextView desc = dialog.findViewById(R.id.dialogDescription);

        title.setText("Confirmation to logout:");
        desc.setText("Do you want to logout?");

        positive.setOnClickListener(v -> {
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            dialog.dismiss();
        });

        negative.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
