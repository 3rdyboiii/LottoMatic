package com.example.lottomatic;

import android.app.Dialog;
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
import androidx.annotation.NonNull;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bnv_bottom;
    FloatingActionButton fab_btn;

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

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showLogoutConfirmationDialog();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        bnv_bottom = findViewById(R.id.bnv_bottom);
        View customFab = findViewById(R.id.custom_fab_button);

        loadFragment(new HomeFragment());

        bnv_bottom.setBackground(null);
        bnv_bottom.setOnApplyWindowInsetsListener(null);
        bnv_bottom.getMenu().getItem(2).setEnabled(false);
        bnv_bottom.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean  onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId(); // Get the ID of the selected item

                if (itemId == R.id.tab_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.tab_history) {
                    selectedFragment = new HistoryFragment();
                } else if (itemId == R.id.tab_results) {
                    selectedFragment = new ResultFragment();
                } else if (itemId == R.id.tab_account) {
                    selectedFragment = new AccountFragment();
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }
                return true;
            }
        });

        customFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Custom FAB clicked!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void showLogoutConfirmationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_confirmdialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        Button positiveButton = dialog.findViewById(R.id.positiveButton);
        Button negativeButton = dialog.findViewById(R.id.negativeButton);
        TextView title = dialog.findViewById(R.id.dialogTitle);
        TextView description = dialog.findViewById(R.id.dialogDescription);

        title.setText("Confirmation to logout:");
        description.setText("Do you want to logout?");

        positiveButton.setOnClickListener(buttonView -> {
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(buttonView -> {
            dialog.dismiss();
        });
        dialog.show();
    }
}