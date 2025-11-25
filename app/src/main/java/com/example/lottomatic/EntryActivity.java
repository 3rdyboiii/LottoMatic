package com.example.lottomatic;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottomatic.adapter.EntryAdapter;
import com.example.lottomatic.connection.DeviceConnection;
import com.example.lottomatic.connection.bluetooth.BluetoothConnection;
import com.example.lottomatic.helper.Account;
import com.example.lottomatic.helper.AppVersion;
import com.example.lottomatic.helper.RangeFormatInputFilter;
import com.example.lottomatic.helper.TextInputFilter;
import com.example.lottomatic.textparser.PrinterTextParserImg;
import com.example.lottomatic.items.EntryItem;
import com.example.lottomatic.helper.PermutationUtil;
import com.example.lottomatic.helper.reprintDBHelper;
import com.example.lottomatic.utility.AsyncBluetoothEscPosPrint;
import com.example.lottomatic.utility.AsyncEscPosPrint;
import com.example.lottomatic.utility.AsyncEscPosPrinter;
import com.example.lottomatic.helper.ConSQL;
import com.example.lottomatic.utility.NetworkChangeListener;
import com.example.lottomatic.utility.SaveDataManager;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EntryActivity extends AppCompatActivity implements EntryAdapter.OnTotalUpdateListener {
    private TextView progressText, totalTxt, displaytxt, winthreetxt, titleTxt, maxBetTxt, drawTxt;
    private TextInputLayout combo2layout;
    private EditText comboInput, combo2Input, stretInput, rambolInput;
    private Button addBtn, clearBtn, printBtn;
    private String draw = "", Name = "", Code = "", comborcpt = "", betrcpt = "", totalrcpt = "", username = "", transCode = "",
            transCode2 = "", transCode3 = "", combo = "", bet = "", activityType = "", selectedGame = "", drawTime = "", gameDraw, gameName;
    private ImageView gameimage;
    private double totalSum = 0, win4D = 4000, win3D = 500, win2D = 35, limit3D = 1000, limit4D = 100, limit2D = 1000;
    private double windouble = 450;

    private int gameImage;
    private ProgressBar progressBar;
    private View loadingContainer;
    private Date currentDate;
    private BluetoothConnection selectedDevice;

    private RecyclerView DisplayList;
    private EntryAdapter entryAdapter;
    private List<EntryItem> itemList;
    private TextInputLayout rambolInputLayout;

    Connection connection;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    NetworkChangeListener networkChangeListener = new NetworkChangeListener();

    @Override
    protected void onStart() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeListener, filter);
        super.onStart();
    }
    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeListener);
        super.onStop();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
            getWindow().setStatusBarColor(Color.BLACK);
        }
        setContentView(R.layout.activity_entry);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showBackConfirmationDialog();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        DisplayList = findViewById(R.id.displayList);
        totalTxt = findViewById(R.id.totalTxt);
        gameimage = findViewById(R.id.gameimage);
        drawTxt = findViewById(R.id.drawTxt);
        comboInput = findViewById(R.id.combo1Input);
        combo2Input = findViewById(R.id.combo2Input);
        combo2layout = findViewById(R.id.combo2layout);
        stretInput = findViewById(R.id.stretInput);
        rambolInput = findViewById(R.id.rambolInput);
        rambolInputLayout = findViewById(R.id.rambolInputLayout);
        addBtn = findViewById(R.id.addBtn);
        addBtn.setOnClickListener(v -> addCombination());
        printBtn = findViewById(R.id.printBtn);
        printBtn.setOnClickListener(v -> { printOrNoPrint();});
        progressBar = findViewById(R.id.progressBar);
        loadingContainer = findViewById(R.id.loadingContainer);
        progressText = findViewById(R.id.progressText);

        InputFilter[] filters = {new TextInputFilter()};

        itemList = new ArrayList<>();
        Account account = Account.getInstance(this);
        entryAdapter = new EntryAdapter(itemList, this, account);
        DisplayList.setLayoutManager(new LinearLayoutManager(this));
        DisplayList.setAdapter(entryAdapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // We don't support drag-and-drop, so return false
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Get the position of the swiped item
                int position = viewHolder.getAdapterPosition();
                // Remove the item from the adapter
                entryAdapter.removeItem(position);
                if (entryAdapter.getTotalUpdateListener() != null) {
                    entryAdapter.getTotalUpdateListener().onTotalUpdate(entryAdapter.getTotal());
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(DisplayList);

        Name = Account.getInstance(this).getName();
        Code = Account.getInstance(this).getCode();
        Intent intent = getIntent();
        activityType = intent.getStringExtra("activityType"); // This might be null if not passed
        gameImage = intent.getIntExtra("gameImage", -1); // -1 as default value
        gameDraw = intent.getStringExtra("gameDraw");
        gameName = intent.getStringExtra("gameName");
        gameimage.setImageResource(gameImage);
        switch (gameDraw) {
            case "2:00 PM":
                draw = "2PM DRAW";
                drawTime = "2PM";
                break;
            case "5:00 PM":
                draw = "5PM DRAW";
                drawTime = "5PM";
                break;
            case "9:00 PM":
                draw = "9PM DRAW";
                drawTime = "9PM";
                break;
        }
        drawTxt.setText(draw);

        if (gameImage == R.drawable.icon_3d) {
            int maxLength = 3;
            comboInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(maxLength) });
            rambolInputLayout.setVisibility(View.VISIBLE);
        } else if (gameImage == R.drawable.icon_4d) {
            int maxLength = 4;
            comboInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(maxLength) });
            rambolInputLayout.setVisibility(View.GONE);
        } else if (gameImage == R.drawable.icon_ultra) {
            drawTxt.setText("ULTRA DRAW");
        } else if (gameImage == R.drawable.icon_649) {
            drawTxt.setText("6/49 DRAW");
        } else if (gameImage == R.drawable.icon_642) {
            drawTxt.setText("6/42 DRAW");
        } else if (gameImage == R.drawable.icon_6d) {
            drawTxt.setText("6D DRAW");
        } else if (gameImage == R.drawable.icon_2d) {
            int maxLength = 2;

            InputFilter lengthFilter = new InputFilter.LengthFilter(maxLength);
            InputFilter rangeFilter = new RangeFormatInputFilter(1, 31);

            comboInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    autoPad2D(comboInput);
                }
            });

            combo2Input.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    autoPad2D(combo2Input);
                }
            });

            comboInput.setFilters(new InputFilter[] { lengthFilter, rangeFilter });
            combo2Input.setFilters(new InputFilter[] { lengthFilter, rangeFilter });

            combo2layout.setVisibility(View.VISIBLE);
            rambolInputLayout.setVisibility(View.VISIBLE);
        } else {
            drawTxt.setText("DRAW");
        }
        startCheckingTime();
    }

    private void autoPad2D(EditText editText) {
        String txt = editText.getText().toString().trim();
        if (txt.isEmpty()) return;

        try {
            int num = Integer.parseInt(txt);
            if (num >= 1 && num <= 31) {
                editText.setText(String.format("%02d", num));
            }
        } catch (Exception ignored) {}
    }


    private void showBackConfirmationDialog() {
        if (entryAdapter.getItemCount() != 0) {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.custom_confirmdialog);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
            dialog.setCancelable(false);

            Button positiveButton = dialog.findViewById(R.id.positiveButton);
            Button negativeButton = dialog.findViewById(R.id.negativeButton);
            TextView title = dialog.findViewById(R.id.dialogTitle);
            TextView description = dialog.findViewById(R.id.dialogDescription);

            title.setText("Confirmation:");
            description.setText("You have added combination/s, Do you want to go back?");

            positiveButton.setOnClickListener(buttonView -> {
                finish();
                dialog.dismiss();
            });

            negativeButton.setOnClickListener(buttonView -> {
                dialog.dismiss();
            });
            dialog.show();
        } else {
            finish();
        }
    }

    private boolean hasMinimum4Digits(String input) {
        String digitsOnly = input.replaceAll("\\D", "");
        return digitsOnly.length() >= 4;
    }
    private boolean hasMinimum3Digits(String input) {
        String digitsOnly = input.replaceAll("\\D", "");
        return digitsOnly.length() >= 3;
    }
    private boolean hasMinimum2Digits(String input) {
        String digitsOnly = input.replaceAll("\\D", "");
        return digitsOnly.length() >= 2;
    }
    @Override
    public void onTotalUpdate(double total) {
        totalTxt.setText("Total: ₱" + String.format("%.2f", total));
        /*checkRecyclerViewContent();*/
    }
    @Override
    public void onComboLimitReached(String combo, double remaining, double limit) {
        showErrorDialog(
                "Bet Limit Reached",
                String.format("Maximum total bet for %s is ₱%.2f",
                        combo,
                        limit)
        );
    }
    private void showErrorDialog(String title, String message) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_errordialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
        dialog.setCancelable(false);

        Button positiveButton = dialog.findViewById(R.id.positiveButton);
        TextView titleView = dialog.findViewById(R.id.dialogTitle);
        TextView description = dialog.findViewById(R.id.dialogDescription);

        titleView.setText(title);
        description.setText(message);

        positiveButton.setOnClickListener(buttonView -> dialog.dismiss());
        dialog.show();
    }
    private void showErrorMessage(String combo) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_errordialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
        dialog.setCancelable(false);

        Button positiveButton = dialog.findViewById(R.id.positiveButton);
        TextView title = dialog.findViewById(R.id.dialogTitle);
        TextView description = dialog.findViewById(R.id.dialogDescription);

        title.setText("Limit Reached:");
        description.setText("The combination " + combo + " has reached its limit.");

        positiveButton.setOnClickListener(buttonView -> {
            comboInput.setText("");
            stretInput.setText("");
            rambolInput.setText("");
            dialog.dismiss();
        });
        dialog.show();
    }

    public interface BetLimitCallback {
        void onResult(String exceededCombo);
    }

    private void getCurrentTotalBet(List<String> combos, double amount, String game, BetLimitCallback callback) {

        runOnUiThread(() -> {
            loadingContainer.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            progressText.setText("Checking...");
        });

        comboInput.setText("");
        combo2Input.setText("");
        stretInput.setText("");
        rambolInput.setText("");

        clearFocusAndHideKeyboard();

        executor.execute(() -> {

            String exceededCombo = null;
            Account account = Account.getInstance(this);

            int total = combos.size();
            int index = 0;

            try (Connection connection = new ConSQL().conclass()) {
                if (connection != null) {

                    for (String combo : combos) {

                        // compute progress percentage
                        int finalIndex = index;
                        runOnUiThread(() -> {
                            int percent = (int) (((double) finalIndex / total) * 100);
                            progressBar.setProgress(percent);
                            progressText.setText(percent + "%");
                        });

                        index++;

                        String query =
                                "SELECT SUM(bets) AS totalBet " +
                                        "FROM EntryTB " +
                                        "WHERE combo = ? AND draw = ? AND game = ? AND CAST([date] AS DATE) = CAST(GETDATE() AS DATE)";

                        try (PreparedStatement ps = connection.prepareStatement(query)) {
                            ps.setString(1, combo);
                            ps.setString(2, drawTime);
                            ps.setString(3, game);

                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {

                                    double totalBet = rs.getDouble("totalBet");
                                    double betLimit = account.getBetLimit(
                                            game,
                                            game.equals("4D") ? 50.0 : 200.0
                                    );

                                    if ((totalBet + amount) > betLimit) {
                                        exceededCombo = combo;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            String finalExceeded = exceededCombo;

            runOnUiThread(() -> {
                progressBar.setProgress(100);
                progressText.setText("Done");
                loadingContainer.setVisibility(View.GONE);
                callback.onResult(finalExceeded);
            });
        });
    }

    private Set<String> SoldoutCombinations = new HashSet<>();
    private void fetchSoldout() {
        executor.execute(() -> {
            ConSQL c = new ConSQL();
            try (Connection connection = c.conclass()) {
                if (connection != null) {
                    String query = "SELECT * FROM SoldoutTB WHERE [group] = 'BICOL'";
                    try (Statement smt = connection.createStatement();
                         ResultSet set = smt.executeQuery(query)) {

                        while (set.next()) {
                            String sold = set.getString("soldout").trim();
                            // Split by commas and add each number individually
                            String[] numbers = sold.split(",");
                            for (String num : numbers) {
                                num = num.trim(); // Remove extra spaces
                                if (!num.isEmpty()) {
                                    SoldoutCombinations.add(num);
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                Log.e("Error:", e.getMessage());
            }
        });
    }

    // Helper method to create SQL placeholders
    private String createPlaceholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    // Improved combo normalization
    private String normalizeCombo(String combo) {
        if (combo == null) return "";

        // Remove "T-" prefix if present
        if (combo.startsWith("T-")) {
            combo = combo.substring(2);
        }

        // Remove formatting characters (like * in "12*34")
        return combo.replaceAll("[^0-9]", "");
    }

    /*private double getLimitForGame(String game) {
        Account account = Account.getInstance(this);
        switch (game) {
            case "2D":
                return account.getBetLimit(game, 1000.0);
            case "3D":
                return account.getBetLimit(game, 1000.0);
            case "4D":
                return account.getBetLimit(game, 1000.0);
            default:
                return 100.0; // Default limit
        }
    }*/

    private String format2D(String val) {
        if (val == null || val.isEmpty()) return "";
        if (val.length() == 1) return "0" + val;
        return val; // already 2 digits
    }

    private void addCombination() {
        Account account = Account.getInstance(this);
        String combo = comboInput.getText().toString().trim();
        String combo2 = combo2Input.getText().toString().trim();
        String stret = stretInput.getText().toString();
        String rambol = rambolInput.getText().toString();
        double stretBet = stret.isEmpty() ? 0.0 : Double.parseDouble(stret);
        double rambolBet = rambol.isEmpty() ? 0.0 : Double.parseDouble(rambol);
        double currentBet = stretBet + rambolBet;
        double amount = stretBet + rambolBet;
        double prize4D = stretBet * win4D;
        double prize3D = stretBet * win3D;
        double prize2D = stretBet * win2D;
        DecimalFormat decimalFormat = new DecimalFormat("####");

        double prizeAmount4D = Account.getInstance(this).getPrize4D();
        double prizeAmount3D = Account.getInstance(this).getPrize3D();
        double prizeAmount2D = Account.getInstance(this).getPrize2D();

        String Prize4D = decimalFormat.format(prize4D);
        String Prize3D = decimalFormat.format(prize3D);
        String Prize2D = decimalFormat.format(prize2D);

        if (combo.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        } else if (!combo.isEmpty() && stret.isEmpty() && rambol.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String game = gameName;

        // Build list of combos (including permutations if Rambol)
        List<String> rcomboList = new ArrayList<>();
        if (!rambol.isEmpty()) {
            if (gameName.equals("2D")) {
                String original = combo + combo2;
                String reversed = combo2 + combo;

                List<String> permutations = Arrays.asList(original, reversed);
                rcomboList.addAll(permutations);
            } else {
                rcomboList.addAll(PermutationUtil.getPermutations(combo));
            }
        } else {
            rcomboList.add(combo);
        }

        // 🚀 USE THE CALLBACK VERSION (with progress bar)
        getCurrentTotalBet(rcomboList, amount, game, exceededCombo -> {

            if (exceededCombo != null) {
                showErrorMessage(exceededCombo);
                return;
            }

        fetchSoldout();

        String code = "";

        /*if (game.equals("4D")) {
            String formattedResult = combo.substring(0, 2) + "-" + combo.substring(2);

            List<String> rcomboList = new ArrayList<>();
            if (!rambol.isEmpty()) {
                List<String> permutations = PermutationUtil.getPermutations(formattedResult);
                rcomboList.addAll(permutations);
            } else {
                rcomboList.add(formattedResult);
            }

            double currentLimit = account.getBetLimit(game, 1000.0);

            String limitExceededCombo = checkComboLimits(rcomboList, amount, game);

            if (limitExceededCombo != null) {
                showErrorMessage(limitExceededCombo, currentLimit);
                return;
            }

            // Check against limit
            if (currentBet > currentLimit) {
                Toast.makeText(this,
                        String.format("Maximum bet for %s is %.2f", selectedGame, currentLimit),
                        Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            List<String> rcomboList = new ArrayList<>();
            if (!rambol.isEmpty()) {
                List<String> permutations = PermutationUtil.getPermutations(combo);
                rcomboList.addAll(permutations);
            } else {
                rcomboList.add(combo);
            }

            double currentLimit = account.getBetLimit(game, 1000.0);

            String limitExceededCombo = checkComboLimits(rcomboList, amount, game);

            if (limitExceededCombo != null) {
                showErrorMessage(limitExceededCombo, currentLimit);
                return;
            }

            // Check against limit
            if (currentBet > currentLimit) {
                Toast.makeText(this,
                        String.format("Maximum bet for %s is %.2f", selectedGame, currentLimit),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }*/

        /*Map<String, Double> currentTotals = getCurrentTotals();
        for (String Combo : rcomboList) {
            double currentTotal = currentTotals.getOrDefault(Combo, 0.0);
            double proposedTotal = currentTotal + currentBet;

            // Define your limits based on game type
            double comboLimit = account.getBetLimit(game, 1000.0); // Default limit
            switch (game) {
                case "P131":
                    comboLimit = account.getBetLimit(game, 100.0);
                    break;
                case "3D":
                    comboLimit = account.getBetLimit(game, 100.0);
                    break;
                case "4D":
                    comboLimit = account.getBetLimit(game, 100.0);
                    break;
                // Add other cases as needed
            }

            if (proposedTotal > comboLimit) {
                showErrorMessage(Combo);
                return;
            }
        }*/

        if (game.equals("4D")) {
            if (!hasMinimum4Digits(combo)) {
                Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.custom_errordialog);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
                dialog.setCancelable(false);

                Button positiveButton = dialog.findViewById(R.id.positiveButton);
                TextView title = dialog.findViewById(R.id.dialogTitle);
                TextView description = dialog.findViewById(R.id.dialogDescription);

                title.setText("Invalid Input:");
                description.setText("Please enter 4 digits for 4D combination");

                positiveButton.setOnClickListener(buttonView -> {
                    dialog.dismiss();
                });
                dialog.show();
                return;
            }

            /*// Get current date parts
            Calendar calendar = Calendar.getInstance();
            int month = calendar.get(Calendar.MONTH) + 1; // Month is 0-based
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Format date parts to 2-digit strings
            String monthStr = String.format("%02d", month);
            String dayStr = String.format("%02d", day);

            // Extract first and last 2 digits from combo
            String firstTwo = combo.substring(0, 2);
            String lastTwo = combo.substring(2);

            // Check if combo matches date pattern (either order) or has same digits
            boolean isDateMatch = (firstTwo.equals(monthStr) && lastTwo.equals(dayStr));
            boolean isSameDigits = firstTwo.equals(lastTwo);

            *//*double prizeMultiplier = (isDateMatch || isSameDigits) ? 225 : 450;*/

            if (!stret.isEmpty()) {
                String formattedResult = combo.substring(0, 2) + combo.substring(2);
                double prize = stretBet * prizeAmount4D;
                String formattedPrize = decimalFormat.format(prize);
                code = "Standard";
                EntryItem stretItem = new EntryItem(formattedResult, stretBet, game, code, formattedPrize);
                entryAdapter.addItem(stretItem);
            }
            /*if (!rambol.isEmpty()) {
                double rambolValue = Double.parseDouble(rambol);
                BigDecimal ramble;
                double prize;

                // Calculate the base prize (without division)
                prize = prizeMultiplier * rambolValue;

                String formattedResult = combo.substring(0, 2) + "*" + combo.substring(2);

                if (!isSameDigits) {
                    // Case 1: When we have both original and reversed combinations
                    String reversedResult = combo.substring(2) + "*" + combo.substring(0, 2);

                    // Divide the bet amount by 2 for each combination
                    double halfBet = rambolValue / 2.0;

                    // Calculate prize for each half
                    double halfPrize = prizeMultiplier * halfBet;
                    String formattedHalfPrize = decimalFormat.format(halfPrize);

                    // Add both items with half the bet amount
                    EntryItem rambolItem1 = new EntryItem(formattedResult, halfBet, code, formattedHalfPrize);
                    EntryItem rambolItem2 = new EntryItem(reversedResult, halfBet, code, formattedHalfPrize);

                    entryAdapter.addItem(rambolItem1);
                    entryAdapter.addItem(rambolItem2);
                } else {
                    // Case 2: When we only have the original combination (same digits)
                    String formattedPrize = decimalFormat.format(prize);
                    EntryItem rambolItem1 = new EntryItem(formattedResult, rambolValue, code, formattedPrize);
                    entryAdapter.addItem(rambolItem1);
                }
            }*/
        } else if (game.equals("3D")) {
            if (!hasMinimum3Digits(combo)) {
                Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.custom_errordialog);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
                dialog.setCancelable(false);

                Button positiveButton = dialog.findViewById(R.id.positiveButton);
                TextView title = dialog.findViewById(R.id.dialogTitle);
                TextView description = dialog.findViewById(R.id.dialogDescription);

                title.setText("Invalid Input:");
                description.setText("Please enter 3 digits for 3D combination");

                positiveButton.setOnClickListener(buttonView -> {
                    dialog.dismiss();
                });
                dialog.show();
                return;
            }
            if (!stret.isEmpty()) {
                double prize = prizeAmount3D;
                double prizeWin = stretBet * prizeAmount3D;
                String formattedPrize = decimalFormat.format(prizeWin);
                code = "Standard";
                EntryItem stretItem = new EntryItem(combo, stretBet, game, code, formattedPrize);
                entryAdapter.addItem(stretItem);
            }

            if (!rambol.isEmpty()) {
                double rambolValue = Double.parseDouble(rambol);
                BigDecimal ramble;
                double prize;

                if (hasTwoDuplicateDigits(combo)) {
                    ramble = new BigDecimal(rambolValue).divide(new BigDecimal("3.0"), 10, RoundingMode.HALF_UP); // Duplicate digits prize distribution
                    prize = prizeAmount3D * ramble.doubleValue();
                } else {
                    ramble = new BigDecimal(rambolValue).divide(new BigDecimal("6.0"), 10, RoundingMode.HALF_UP); // No duplicate digits prize distribution
                    prize = prizeAmount3D * ramble.doubleValue();
                }
                String formattedPrize = decimalFormat.format(prize);
                code = "Rambolito 3";
                EntryItem rambolItem = new EntryItem(combo, rambolBet, game, code, formattedPrize);
                entryAdapter.addItem(rambolItem);
            }
        } else if (game.equals("2D")) {
            if (!hasMinimum2Digits(combo)) {
                Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.custom_errordialog);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
                dialog.setCancelable(false);

                Button positiveButton = dialog.findViewById(R.id.positiveButton);
                TextView title = dialog.findViewById(R.id.dialogTitle);
                TextView description = dialog.findViewById(R.id.dialogDescription);

                title.setText("Invalid Input:");
                description.setText("Please enter 4 digits for 2D combination");

                positiveButton.setOnClickListener(buttonView -> {
                    dialog.dismiss();
                });
                dialog.show();
                return;
            }

            // Get current date parts
            Calendar calendar = Calendar.getInstance();
            int month = calendar.get(Calendar.MONTH) + 1; // Month is 0-based
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Format date parts to 2-digit strings
            String monthStr = String.format("%02d", month);
            String dayStr = String.format("%02d", day);

            // Extract first and last 2 digits from combo
            String firstTwo = combo.substring(0, 2);
            String lastTwo = combo.substring(2);

            code = "Standard";

            // Check if combo matches date pattern (either order) or has same digits
            boolean isDateMatch = (firstTwo.equals(monthStr) && lastTwo.equals(dayStr)) ||
                    (firstTwo.equals(dayStr) && lastTwo.equals(monthStr));
            boolean isSameDigits = firstTwo.equals(lastTwo);

            /*double prizeMultiplier = (isDateMatch || isSameDigits) ? 225 : 450;*/ // Half prize if condition met

            if (!stret.isEmpty()) {
                String formattedResult = combo + combo2;
                double prize = stretBet * prizeAmount2D;
                String formattedPrize = decimalFormat.format(prize);
                EntryItem stretItem = new EntryItem(formattedResult, stretBet, game, code, formattedPrize);
                entryAdapter.addItem(stretItem);
            }

            if (!rambol.isEmpty()) {
                double rambolValue = Double.parseDouble(rambol);
                BigDecimal ramble;
                double prize;
                code = "Rambolito";
                /*ramble = new BigDecimal(rambolValue).divide(new BigDecimal("2.0"), 10, RoundingMode.HALF_UP);*/
                prize = prizeAmount2D * rambolBet;
                String formattedPrize = decimalFormat.format(prize);
                String formattedResult = combo + combo2;
                /*String reversedResult = combo.substring(2) + combo.substring(0, 2);*/

                // Add both results to the adapter
                EntryItem rambolItem1 = new EntryItem(formattedResult, rambolBet, game, code, formattedPrize);
                /*EntryItem rambolItem2 = new EntryItem(reversedResult, rambolBet, game, code, formattedPrize);*/

                entryAdapter.addItem(rambolItem1);
                /*entryAdapter.addItem(rambolItem2);*/
            }
        } /*else if (game.equals("L23D")) {
            if (!hasMinimum2Digits(combo)) {
                Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.custom_errordialog);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
                dialog.setCancelable(false);

                Button positiveButton = dialog.findViewById(R.id.positiveButton);
                TextView title = dialog.findViewById(R.id.dialogTitle);
                TextView description = dialog.findViewById(R.id.dialogDescription);

                title.setText("Invalid Input:");
                description.setText("Please enter 2 digits for L23D combination");

                positiveButton.setOnClickListener(buttonView -> {
                    dialog.dismiss();
                });
                dialog.show();
                return;
            }
            String targetcombo = "T-" + combo;
            EntryItem stretItem = new EntryItem(targetcombo, stretBet, game, Prize2D);
            entryAdapter.addItem(stretItem);
        } else if (game.equals("L24D")) {
            if (!hasMinimum2Digits(combo)) {
                Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.custom_errordialog);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
                dialog.setCancelable(false);

                Button positiveButton = dialog.findViewById(R.id.positiveButton);
                TextView title = dialog.findViewById(R.id.dialogTitle);
                TextView description = dialog.findViewById(R.id.dialogDescription);

                title.setText("Invalid Input:");
                description.setText("Please enter 2 digits for L24D combination");

                positiveButton.setOnClickListener(buttonView -> {
                    dialog.dismiss();
                });
                dialog.show();
                return;
            }
            String targetcombo = "T-" + combo;
            EntryItem stretItem = new EntryItem(targetcombo, stretBet, game, Prize2D);
            entryAdapter.addItem(stretItem);
        } else if (game.equals("L26D")) {
            if (!hasMinimum2Digits(combo)) {
                Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.custom_errordialog);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
                dialog.setCancelable(false);

                Button positiveButton = dialog.findViewById(R.id.positiveButton);
                TextView title = dialog.findViewById(R.id.dialogTitle);
                TextView description = dialog.findViewById(R.id.dialogDescription);

                title.setText("Invalid Input:");
                description.setText("Please enter 2 digits for L26D combination");

                positiveButton.setOnClickListener(buttonView -> {
                    dialog.dismiss();
                });
                dialog.show();
                return;
            }
            String targetcombo = "T-" + combo;
            EntryItem stretItem = new EntryItem(targetcombo, stretBet, game, Prize2D);
            entryAdapter.addItem(stretItem);
        }*/
        });
    }

    private void clearFocusAndHideKeyboard() {
        // Get the current view with focus
        View currentFocus = this.getCurrentFocus();
        if (currentFocus != null) {
            // Clear the focus from the current view
            currentFocus.clearFocus();

            // Hide the keyboard if it's visible
            InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    private String formatNumber(double number) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(number);
    }
    private boolean hasTwoDuplicateDigits(String input) {
        int[] digitCounts = new int[10];
        for (char c : input.toCharArray()) {
            digitCounts[c - '0']++;
        }

        boolean hasTwoDuplicates = false;
        boolean allSameDigit = false;

        for (int count : digitCounts) {
            if (count == 2) {
                hasTwoDuplicates = true; // Detects exactly two duplicates
            }
            if (count == input.length()) {
                allSameDigit = true; // Detects all digits being the same
            }
        }

        return hasTwoDuplicates || allSameDigit;
    }

    private String generateTransCode() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMddyy", Locale.getDefault());
        String dateTime = dateFormat.format(new Date());
        String randomString1 = getRandomAlphaNumericString(10);
        String randomString2 = getRandomAlphaNumericString(4);
        String randomString3 = getRandomAlphaNumericString(4);

        return randomString1;
    }
    private String generateTransCode2() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd", Locale.getDefault());
        String dateTime = dateFormat.format(new Date());
        String randomString4 = getRandomAlphaNumericString2(8);
        String randomString5 = getRandomAlphaNumericString(11);

        return dateTime + randomString5;
    }
    private String generateTransCode3() {
        String randomString1 = getRandomAlphaNumericString3(64);

        return randomString1;
    }
    private String getRandomAlphaNumericString(int length) {
        String characters = "0123456789ABCDEFGHIJKLMNOQPRSTUVWXYZ";
        String numbers = "1234567890";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(numbers.charAt(random.nextInt(numbers.length())));
        }
        return sb.toString();
    }
    private String getRandomAlphaNumericString2(int length) {
        String characters = "0123456789ABCDEFGHIJKLMNOQPRSTUVWXYZ";
        String Bigletters = "ABCDEFGHIJKLMNOQPRSTUVWXYZ";
        String numbers = "1234567890";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }
    private String getRandomAlphaNumericString3(int length) {
        String characters = "0123456789ABCDEFGHIJKLMNOQPRSTUVWXYZ";
        String smallcharacters = "0123456789abcdefghijklmnopqrstuvwxyz";
        String Bigletters = "ABCDEFGHIJKLMNOQPRSTUVWXYZ";
        String numbers = "1234567890";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(smallcharacters.charAt(random.nextInt(smallcharacters.length())));
        }
        return sb.toString();
    }

    public interface OnBluetoothPermissionsGranted {
        void onPermissionsGranted();
    }

    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    public OnBluetoothPermissionsGranted onBluetoothPermissionsGranted;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case PERMISSION_BLUETOOTH:
                case PERMISSION_BLUETOOTH_ADMIN:
                case PERMISSION_BLUETOOTH_CONNECT:
                case PERMISSION_BLUETOOTH_SCAN:
                    checkBluetoothPermissions(onBluetoothPermissionsGranted);
                    break;
            }
        }
    }

    public void checkBluetoothPermissions(OnBluetoothPermissionsGranted onBluetoothPermissionsGranted) {
        this.onBluetoothPermissionsGranted = onBluetoothPermissionsGranted;

        // Check if the device's SDK version is less than Android 12 (API level 31)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, PERMISSION_BLUETOOTH);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, PERMISSION_BLUETOOTH_ADMIN);
            } else {
                // All necessary permissions are granted
                this.onBluetoothPermissionsGranted.onPermissionsGranted();
            }
        } else {
            // For Android 12 (API level 31) and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_BLUETOOTH_CONNECT);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, PERMISSION_BLUETOOTH_SCAN);
            } else {
                // All necessary permissions are granted
                this.onBluetoothPermissionsGranted.onPermissionsGranted();
            }
        }
    }
    private void printOrNoPrint() {
        transCode = generateTransCode();
        transCode2 = generateTransCode2();
        transCode3 = generateTransCode3();

        if (entryAdapter.getItemCount() == 0) {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.custom_errordialog);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
            dialog.setCancelable(false);

            Button positiveButton = dialog.findViewById(R.id.positiveButton);
            TextView title = dialog.findViewById(R.id.dialogTitle);
            TextView description = dialog.findViewById(R.id.dialogDescription);

            title.setText("Invalid Input:");
            description.setText("Please add a combination/s first before submitting.");

            positiveButton.setOnClickListener(buttonView -> {
                dialog.dismiss();
            });
            dialog.show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_choicesdialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
        dialog.setCancelable(false);

        Button positiveButton = dialog.findViewById(R.id.positiveButton);
        Button negativeButton = dialog.findViewById(R.id.negativeButton);
        Button neutralButton = dialog.findViewById(R.id.neutralButton);
        TextView title = dialog.findViewById(R.id.dialogTitle);

        title.setText("Choose Option:");
        positiveButton.setText("Print \nReceipt");
        negativeButton.setText("No \nReceipt");

        positiveButton.setOnClickListener(buttonView -> {
            printBluetooth();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(buttonView -> {
            List<EntryItem> itemList = entryAdapter.getItemList();
            boolean isInserted = insertCombinationToDatabase(itemList);
            if (isInserted) {
                Toast.makeText(this, "Combination/s Saved Successfully", Toast.LENGTH_SHORT).show();
                comboInput.setText("");
                stretInput.setText("");
                rambolInput.setText("");
                entryAdapter.clearItemList();
                totalSum = 0;
                totalTxt.setText("Total: ₱" + String.format("%.2f", (double) totalSum));
            }
            dialog.dismiss();
        });

        neutralButton.setOnClickListener(buttonView -> {
            dialog.dismiss();
        });
        dialog.show();
    }
    public void printSavedData(DeviceConnection printerConnection) {
        String printData = SaveDataManager.getSavedPrintData(this);
        if (printData != null) {
            AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);
            printer.addTextToPrint(printData);
            new AsyncBluetoothEscPosPrint(this, new AsyncEscPosPrint.OnPrintFinished() {
                @Override
                public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                    Dialog dialog = new Dialog(EntryActivity.this);
                    dialog.setContentView(R.layout.custom_confirmdialog);
                    dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                    dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(EntryActivity.this, R.drawable.custom_dialog_bg));
                    dialog.setCancelable(false);

                    Button positiveButton = dialog.findViewById(R.id.positiveButton);
                    Button negativeButton = dialog.findViewById(R.id.negativeButton);
                    TextView title = dialog.findViewById(R.id.dialogTitle);
                    TextView description = dialog.findViewById(R.id.dialogDescription);

                    title.setText("Error Printing:");
                    description.setText("An error occurred while printing. Do you wish to reprint the receipt again?");

                    positiveButton.setOnClickListener(buttonView -> {
                        printSavedData(selectedDevice);
                        dialog.dismiss();
                    });
                    negativeButton.setOnClickListener(buttonView -> {
                        dialog.dismiss();
                    });
                    dialog.show();
                    comboInput.setText("");
                    stretInput.setText("");
                    rambolInput.setText("");
                    entryAdapter.clearItemList();
                    totalSum = 0;
                    totalTxt.setText("Total: ₱" + String.format("%.2f", (double) totalSum));
                }
                @Override
                public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                    SaveDataManager.clearPrintData(EntryActivity.this);
                    comboInput.setText("");
                    stretInput.setText("");
                    rambolInput.setText("");
                    entryAdapter.clearItemList();
                    totalSum = 0;
                    totalTxt.setText("Total: ₱" + String.format("%.2f", (double) totalSum));
                }
            }).execute(printer);
        } else {
            Toast.makeText(this, "No saved print data available.", Toast.LENGTH_SHORT).show();
        }
    }

    public void printBluetooth() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.custom_errordialog);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
            dialog.setCancelable(false);

            Button positiveButton = dialog.findViewById(R.id.positiveButton);
            TextView title = dialog.findViewById(R.id.dialogTitle);
            TextView description = dialog.findViewById(R.id.dialogDescription);

            title.setText("Error Printing:");
            description.setText("Your Bluetooth is currently OFF, Please turn ON and try again.");

            positiveButton.setOnClickListener(buttonView -> {
                dialog.dismiss();
            });
            dialog.show();
            return;
        }
        List<EntryItem> itemList = entryAdapter.getItemList(); // Retrieve data from adapter
        boolean isInserted = insertCombinationToDatabase(itemList);

        if (isInserted) {
            this.checkBluetoothPermissions(() -> {
                try {
                    new AsyncBluetoothEscPosPrint(
                            this,
                            new AsyncEscPosPrint.OnPrintFinished() {
                                @Override
                                public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                                    Dialog dialog = new Dialog(EntryActivity.this);
                                    dialog.setContentView(R.layout.custom_confirmdialog);
                                    dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                                    dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(EntryActivity.this, R.drawable.custom_dialog_bg));
                                    dialog.setCancelable(false);

                                    Button positiveButton = dialog.findViewById(R.id.positiveButton);
                                    Button negativeButton = dialog.findViewById(R.id.negativeButton);
                                    TextView title = dialog.findViewById(R.id.dialogTitle);
                                    TextView description = dialog.findViewById(R.id.dialogDescription);

                                    title.setText("Error Printing:");
                                    description.setText("An error occurred while printing. Do you wish to reprint the receipt again?");

                                    positiveButton.setOnClickListener(buttonView -> {
                                        printSavedData(selectedDevice);
                                        dialog.dismiss();
                                    });
                                    negativeButton.setOnClickListener(buttonView -> {
                                        dialog.dismiss();
                                    });
                                    dialog.show();
                                    comboInput.setText("");
                                    stretInput.setText("");
                                    rambolInput.setText("");
                                    entryAdapter.clearItemList();
                                    totalSum = 0;
                                    totalTxt.setText("Total: ₱" + String.format("%.2f", (double) totalSum));
                                }

                                @Override
                                public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                                    Log.i("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : Print is finished!");
                                    comboInput.setText("");
                                    stretInput.setText("");
                                    rambolInput.setText("");
                                    entryAdapter.clearItemList();
                                    totalSum = 0;
                                    totalTxt.setText("Total: ₱" + String.format("%.2f", (double) totalSum));
                                }
                            }
                    )
                            .execute(this.getAsyncEscPosPrinter(selectedDevice));
                } catch (WriterException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            Toast.makeText(this, "Failed to save combination(s) to the database", Toast.LENGTH_LONG).show();
        }
    }

    /*@RequiresApi(api = Build.VERSION_CODES.Q)
    public Bitmap textAsBitmap(String text, float textSize, int textColor, int maxWidth,
                               boolean isBold, Paint.Align alignment, int[] lineSpacings, float wordSpacing, float strokeWidth) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(alignment);
        paint.setSubpixelText(true);
        paint.setDither(true);
        paint.setWordSpacing(wordSpacing);

        if (isBold) {
            // Use bold typeface
            paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));

            // Add stroke to make it even bolder
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(strokeWidth); // Typically 1.0f-2.0f

            // Enable fake bold for extra weight
            paint.setFakeBoldText(true);
        } else {
            paint.setTypeface(Typeface.DEFAULT);
            paint.setStyle(Paint.Style.FILL);
        }

        // Calculate required width
        String[] lines = text.split("\n");
        Rect bounds = new Rect();
        int requiredWidth = 0;
        for (String line : lines) {
            paint.getTextBounds(line, 0, line.length(), bounds);
            if (bounds.width() > requiredWidth) {
                requiredWidth = bounds.width();
            }
        }

        // Add padding (20% of text width or fixed amount)
        int padding = Math.max((int)(requiredWidth * 0.2), 20);
        int bitmapWidth = requiredWidth + 2 * padding;

        // Ensure we don't exceed maximum width
        if (bitmapWidth > maxWidth) {
            padding = (maxWidth - requiredWidth) / 2;
            bitmapWidth = maxWidth;
        }

        // Calculate height
        int lineHeight = (int) (-paint.ascent() + paint.descent());
        int totalHeight = lineHeight;
        for (int i = 0; i < lines.length; i++) {
            totalHeight += (i < lineSpacings.length) ? lineSpacings[i] : 0;
        }

        // Create bitmap with dynamic width
        Bitmap image = Bitmap.createBitmap(bitmapWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(Color.WHITE);

        int yPosition = (int) -paint.ascent();
        for (int i = 0; i < lines.length; i++) {
            float xPosition;
            switch (alignment) {
                case CENTER:
                    xPosition = bitmapWidth / 2f;
                    break;
                case RIGHT:
                    xPosition = bitmapWidth - padding;
                    break;
                default: // LEFT
                    xPosition = padding;
            }

            canvas.drawText(lines[i], xPosition, yPosition, paint);
            yPosition += lineHeight + ((i < lineSpacings.length) ? lineSpacings[i] : 0);
        }

        return image;
    }*/
    private Bitmap convertToMonochrome(Bitmap original) {
        Bitmap monochrome = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(monochrome);

        // Convert to grayscale first
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        Paint paint = new Paint();
        paint.setColorFilter(filter);

        canvas.drawBitmap(original, 0, 0, paint);

        // Apply threshold to make it pure black/white (1-bit)
        for (int x = 0; x < monochrome.getWidth(); x++) {
            for (int y = 0; y < monochrome.getHeight(); y++) {
                int pixel = monochrome.getPixel(x, y);
                int gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3;
                // Use a threshold (adjust 128 as needed)
                monochrome.setPixel(x, y, gray > 150 ? Color.WHITE : Color.BLACK);
            }
        }

        return monochrome;
    }
    /*@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public Bitmap applySharpenFilter(Bitmap original) {
        // Initialize RenderScript
        RenderScript rs = RenderScript.create(this);

        // Create input allocation from bitmap
        Allocation input = Allocation.createFromBitmap(rs, original);

        // Create output allocation with the same type
        Allocation output = Allocation.createTyped(rs, input.getType());

        // Create a ScriptIntrinsicConvolve3x3 to apply the kernel
        ScriptIntrinsicConvolve3x3 convolve = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));

        // Define a sharpen kernel (3x3)
        float[] sharpenKernel = {
                0, -1, 0,
                -1, 5, -1,
                0, -1, 0
        };

        // Set the kernel to the ScriptIntrinsicConvolve3x3
        convolve.setCoefficients(sharpenKernel);

        // Apply the filter
        convolve.setInput(input);
        convolve.forEach(output);

        // Copy the result back to a bitmap
        output.copyTo(original);

        // Destroy resources
        input.destroy();
        output.destroy();
        convolve.destroy();
        rs.destroy();

        return original;
    }
    private Bitmap addHeightToBitmap(Bitmap originalBitmap, int additionalHeight) {
        int newHeight = originalBitmap.getHeight() + additionalHeight;
        Bitmap paddedBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), newHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(paddedBitmap);
        canvas.drawColor(Color.WHITE); // Background color (optional)
        canvas.drawBitmap(originalBitmap, 0, additionalHeight / 2, null); // Centering the original bitmap
        return paddedBitmap;
    }*/

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private String createStyledTextSection(AsyncEscPosPrinter printer, String game, String combo, String amount) {

        Paint normalPaint = new Paint();
        normalPaint.setAntiAlias(false);
        normalPaint.setSubpixelText(false);
        normalPaint.setFilterBitmap(false);
        normalPaint.setDither(false);
        normalPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        normalPaint.setColor(Color.BLACK);
        normalPaint.setTextSize(20);
        normalPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        Paint comboPaint = new Paint(normalPaint);
        comboPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        comboPaint.setTextSize(24);

        combo = combo.replace("*", "");

        StringBuilder spacedCombo = new StringBuilder();
        for (int i = 0; i < combo.length(); i++) {
            if (i > 0) spacedCombo.append("  ");
            spacedCombo.append(combo.charAt(i));
        }
        combo = spacedCombo.toString();

        int totalWidth = 384;
        int lineHeight = (int) (comboPaint.getTextSize() + 5);
        int totalHeight = 2 * lineHeight;

        Bitmap bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        int topMargin = 10;
        float y = -normalPaint.ascent() + topMargin;

        // Draw left game text
        canvas.drawText(game, 0, y, normalPaint);

        // RIGHT-ALIGN the combo
        float comboWidth = comboPaint.measureText(combo);
        float comboX = totalWidth - comboWidth - 10; // 10px padding from the right
        canvas.drawText(combo, comboX, y, comboPaint);

        // Draw price on next line (still right aligned)
        String priceText = "Price: ₱" + amount;
        float priceX = totalWidth - normalPaint.measureText(priceText) - 10;
        canvas.drawText(priceText, priceX, y + lineHeight, normalPaint);

        String result = PrinterTextParserImg.bitmapToHexadecimalString(printer, bitmap);
        bitmap.recycle();
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private String createTicketSection(AsyncEscPosPrinter printer, String transCode,
                                       int normalTextSize, int boldTextSize) {
        // Configure normal paint
        Paint normalPaint = new Paint();
        normalPaint.setAntiAlias(false);
        normalPaint.setSubpixelText(false);
        normalPaint.setFilterBitmap(false);
        normalPaint.setDither(false);
        normalPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        normalPaint.setColor(Color.BLACK);
        normalPaint.setTextSize(normalTextSize + 4);
        normalPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

        // Configure bold paint
        Paint boldPaint = new Paint(normalPaint);
        boldPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        boldPaint.setTextSize(boldTextSize);

        // Text content
        String ticketText = "Ticket Number";
        String signatureText = "Signature";

        // Add space between each character of transaction code
        StringBuilder spacedTransCode = new StringBuilder();
        for (int i = 0; i < transCode.length(); i++) {
            if (i > 0) spacedTransCode.append(" ");
            spacedTransCode.append(transCode.charAt(i));
        }

        // Dimensions
        int totalWidth = 384;
        int lineHeight = (int) (boldPaint.getTextSize() + 8);
        int totalHeight = (int) (7 * lineHeight); // a bit taller to fit image

        // Create bitmap
        Bitmap bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        float y = -normalPaint.ascent() + 15;

        // Draw transaction code
        float transCodeWidth = normalPaint.measureText(spacedTransCode.toString());
        float transCodeX = (totalWidth - transCodeWidth) / 2;
        canvas.drawText(spacedTransCode.toString(), transCodeX, y, normalPaint);

        // Draw "Ticket Number"
        float ticketWidth = boldPaint.measureText(ticketText);
        float ticketX = (totalWidth - ticketWidth) / 2;
        y += lineHeight * 1f;
        canvas.drawText(ticketText, ticketX, y, boldPaint);

        y += lineHeight * 0.5f;

        // === DRAW IMAGE ABOVE SIGNATURE ===
        Bitmap topImage = BitmapFactory.decodeResource(getResources(), R.drawable.rectangle);
        if (topImage != null) {
            int imageWidth = totalWidth;
            int imageHeight = (int) ((float) topImage.getHeight() / topImage.getWidth() * imageWidth);
            Bitmap scaledImage = Bitmap.createScaledBitmap(topImage, imageWidth, imageHeight, false);

            float imageX = (totalWidth - imageWidth) / 2f;
            canvas.drawBitmap(scaledImage, imageX, y, null);

            y += imageHeight + 10; // space below image
            scaledImage.recycle();
            topImage.recycle();
        }

        y += lineHeight * 0.5f;

        // Draw "Signature"
        float signatureWidth = boldPaint.measureText(signatureText);
        float signatureX = (totalWidth - signatureWidth) / 2;
        canvas.drawText(signatureText, signatureX, y, boldPaint);

        // Convert to printer format
        String result = PrinterTextParserImg.bitmapToHexadecimalString(printer, bitmap);
        bitmap.recycle();
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private String createImageOnlySection(AsyncEscPosPrinter printer) {
        Bitmap leftImage = null;
        Bitmap rightImage = null;
        Bitmap compositeBitmap = null;

        try {
            // Load and process right image
            rightImage = getBitmapFromVectorDrawableWithSize(this, R.drawable.lottomatik, 300, 160);
            if (rightImage != null) {
                rightImage = convertToMonochrome(rightImage);
                rightImage = Bitmap.createScaledBitmap(rightImage, 170, 80, false);
            }

            // Load and process left image based on game type
            int leftImageResId = 0;
            if ("3D".equals(gameName)) {
                leftImageResId = R.drawable.icon3d;
            } else if ("2D".equals(gameName)) {
                leftImageResId = R.drawable.icon2d;
            } else if ("4D".equals(gameName)) {
                leftImageResId = R.drawable.icon4d;
            }

            if (leftImageResId != 0) {
                leftImage = getBitmapFromVectorDrawableWithSize(this, leftImageResId, 300, 160);
                if (leftImage != null) {
                    leftImage = convertToMonochrome(leftImage);
                    leftImage = Bitmap.createScaledBitmap(leftImage, 120, 100, false);
                }
            }

            // Create composite image
            compositeBitmap = createCompositeBitmap(leftImage, rightImage, 384);
            if (compositeBitmap == null) {
                return ""; // Return empty string if bitmap creation failed
            }

            return PrinterTextParserImg.bitmapToHexadecimalString(printer, compositeBitmap);

        } catch (Exception e) {
            Log.e("Printer", "Error creating image section", e);
            return ""; // Return empty string on error
        } finally {
            safeRecycleBitmap(leftImage);
            safeRecycleBitmap(rightImage);
            safeRecycleBitmap(compositeBitmap);
        }
    }

    private Bitmap createCompositeBitmap(Bitmap leftImage, Bitmap rightImage, int totalWidth) {
        int imageTopMargin = 5;
        int imageSideMargin = 5;

        int maxHeight = Math.max(
                leftImage != null ? leftImage.getHeight() : 0,
                rightImage != null ? rightImage.getHeight() : 0
        );

        if (maxHeight == 0) return null; // No images to process

        int totalHeight = imageTopMargin + maxHeight + 5;

        Bitmap bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        // Draw left image
        if (leftImage != null) {
            int leftImageY = imageTopMargin + (maxHeight - leftImage.getHeight()) / 2;
            canvas.drawBitmap(leftImage, imageSideMargin, leftImageY, null);
        }

        // Draw right image
        if (rightImage != null) {
            int rightImageX = totalWidth - rightImage.getWidth() - imageSideMargin;
            int rightImageY = imageTopMargin + (maxHeight - rightImage.getHeight()) / 2;
            canvas.drawBitmap(rightImage, rightImageX, rightImageY, null);
        }

        return bitmap;
    }

    private void safeRecycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private Bitmap getBitmapFromVectorDrawableWithSize(Context context, int drawableId, int width, int height) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, drawableId);
            if (drawable == null) {
                return null;
            }

            // Create bitmap with higher quality settings
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // Use high-quality rendering settings
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);

            // Draw with high quality
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);

            return bitmap;
        } catch (Exception e) {
            Log.e("VectorDrawable", "Error creating high-quality bitmap: " + e.getMessage());
            return null;
        }
    }

    private List<String> wrapText(String text, Paint paint, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (paint.measureText(currentLine + word + " ") <= maxWidth) {
                currentLine.append(word).append(" ");
            } else {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder(word).append(" ");
            }
        }

        if (!currentLine.toString().isEmpty()) {
            lines.add(currentLine.toString().trim());
        }

        return lines;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private String createReceiptWithQR(AsyncEscPosPrinter printer,
                                       String betDate, String drawDate, String code, String transCode, String price,
                                       Bitmap qrBitmap) {

        // 1. Configure paints - make text smaller
        Paint paint = new Paint();
        paint.setAntiAlias(false);
        paint.setSubpixelText(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(0.5f);
        paint.setColor(Color.BLACK);
        paint.setTextSize(14); // Reduced from 22 to 18
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

        Paint boldPaint = new Paint(paint);
        boldPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        // 2. Layout parameters
        int lineSpacing = 6; // Slightly reduced line spacing
        int padding = 10;
        int totalWidth = 384;

        // Build left lines with proper spacing
        List<String> leftLinesList = new ArrayList<>();
        leftLinesList.add("Bet Date:");
        leftLinesList.add(betDate);
        leftLinesList.add(""); // Spacer
        leftLinesList.add("Draw Date:");
        leftLinesList.add(drawDate);
        leftLinesList.add(""); // Spacer
        leftLinesList.add("Draw ID: " + transCode);
        leftLinesList.add(""); // Spacer
        leftLinesList.add("Agent ID: " + code);
        leftLinesList.add(""); // Spacer
        leftLinesList.add("Ticket Price: ₱" + price);

        String[] leftLines = leftLinesList.toArray(new String[0]);

        // Calculate line height
        int lineHeight = (int) (paint.getTextSize() + lineSpacing);

        // Make QR code bigger
        int qrWidth = 160;
        int qrHeight = 160;
        int qrPosX = totalWidth - qrWidth - 10;
        int qrPosY = 0; // Add some top margin for QR code

        // Calculate total height needed for text content
        int textContentHeight = 0;
        for (String line : leftLines) {
            if (line.isEmpty()) {
                textContentHeight += lineHeight / 2; // Half height for spacers
            } else {
                textContentHeight += lineHeight;
            }
        }

        // Use the larger of text height or QR code height for final height
        int finalHeight = Math.max(textContentHeight, qrPosY + qrHeight) + 20; // Add bottom margin

        // Create bitmap
        Bitmap result = Bitmap.createBitmap(totalWidth, finalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.WHITE);

        // Draw left column
        int y = padding + (int) -paint.ascent();
        for (int i = 0; i < leftLines.length; i++) {
            String line = leftLines[i];

            if (line.isEmpty()) {
                y += lineHeight / 2; // Add half line height for spacer
                continue;
            }

            // Use bold paint for labels
            if (line.endsWith(":") || line.startsWith("Draw ID:") || line.startsWith(betDate) || line.startsWith(drawDate) ||
                    line.startsWith("Agent ID:") || line.startsWith("Ticket Price:")) {
                canvas.drawText(line, padding, y, boldPaint);
            } else {
                canvas.drawText(line, padding, y, paint);
            }

            y += lineHeight;
        }

        // Draw QR code - FIXED THIS LINE
        Bitmap scaledQR = Bitmap.createScaledBitmap(
                removeQRPadding(qrBitmap),
                qrWidth,
                qrHeight,
                false
        );
        canvas.drawBitmap(scaledQR, qrPosX, qrPosY, null); // Use qrPosX for X coordinate

        // Convert to hexadecimal string
        String hex = PrinterTextParserImg.bitmapToHexadecimalString(printer, result);

        // Clean up
        scaledQR.recycle();
        result.recycle();

        return hex;
    }

    private Bitmap removeQRPadding(Bitmap qrBitmap) {
        // Find content bounds
        int[] pixels = new int[qrBitmap.getWidth() * qrBitmap.getHeight()];
        qrBitmap.getPixels(pixels, 0, qrBitmap.getWidth(), 0, 0,
                qrBitmap.getWidth(), qrBitmap.getHeight());

        // Detect actual QR content area
        int left = qrBitmap.getWidth(), top = qrBitmap.getHeight();
        int right = 0, bottom = 0;

        for (int y = 0; y < qrBitmap.getHeight(); y++) {
            for (int x = 0; x < qrBitmap.getWidth(); x++) {
                if (pixels[y * qrBitmap.getWidth() + x] != Color.WHITE) {
                    if (x < left) left = x;
                    if (x > right) right = x;
                    if (y < top) top = y;
                    if (y > bottom) bottom = y;
                }
            }
        }

        // Crop to content
        return Bitmap.createBitmap(qrBitmap, left, top,
                right - left + 1, bottom - top + 1);
    }
    private String formatAmount(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        String formattedAmount = formatter.format(amount);

        return formattedAmount.replaceAll("[.,]00$", "").replace(",", "");
    }
    private String formatPrize(double prize) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        String formattedPrize = formatter.format(prize);

        // Remove both commas and .00 if present
        return formattedPrize.replaceAll("[.,]00$", "").replace(",", "");
    }
    private double parsePrize(String prize) {
        try {
            NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
            return formatter.parse(prize).doubleValue();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0; // or handle as needed
        }
    }
    private boolean areAllDigitsSame(String combo) {
        char firstChar = combo.charAt(0);
        for (char c : combo.toCharArray()) {
            if (c != firstChar) {
                return false; // Found a different character
            }
        }
        return true; // All characters are the same
    }
    private boolean hasDoubleDigits(String combo) {
        // Count occurrences of each digit
        Map<Character, Integer> digitCount = new HashMap<>();
        for (char c : combo.toCharArray()) {
            digitCount.put(c, digitCount.getOrDefault(c, 0) + 1);
        }

        // Check if there are exactly two identical digits and one different digit
        return digitCount.containsValue(2) && digitCount.containsValue(1);
    }
    private String createPerfectlyAlignedLine(String label, String value) {
        // Fixed structure:
        // - Label (6 chars) + " : " (3 chars) = 9 chars total
        // - Value starts at column 12 (right-aligned)
        // - 2-character right margin

        String labelPart = String.format("%-6s: ", label); // "AGENT : " (6 + 3 chars)
        int paddingNeeded = 20 - value.length(); // Space before value

        StringBuilder line = new StringBuilder()
                .append(labelPart);

        // Add spaces for right alignment
        for (int i = 0; i < paddingNeeded; i++) {
            line.append(" ");
        }

        line.append(value)    // The actual value
                .append("  ");    // 2-space right margin

        return line.toString();
    }
    private Bitmap generateQRCode(String content, int size) throws WriterException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size
        );

        BarcodeEncoder encoder = new BarcodeEncoder();
        return encoder.createBitmap(bitMatrix);
    }

    /*private String formatTicketNumber(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        StringBuilder formatted = new StringBuilder("<font size='tall'>");

        for (int i = 0; i < input.length(); i++) {
            // Add the current character
            formatted.append(input.charAt(i));

            // Add spacing (3 spaces for first 4 gaps, 2 spaces after)
            if (i < input.length() - 1) {
                formatted.append(i < 4 ? "   " : "  ");
            }
        }

        return formatted.append("</font>\n").toString();
    }*/

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public AsyncEscPosPrinter getAsyncEscPosPrinter(DeviceConnection printerConnection) throws WriterException {
        SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd"); // Use MM for month, not mm
        SimpleDateFormat day = new SimpleDateFormat("E"); // "E" gives short day name (Mon, Tue, etc.)
        SimpleDateFormat time = new SimpleDateFormat("HH:mm");

        Date currentDate = new Date();
        String betDate = date.format(currentDate) + " " + day.format(currentDate) + " " + time.format(currentDate);
        String game = "";
        double total = 0;
        String Totalamount = "";
        DecimalFormat decimalFormat = new DecimalFormat("####.00");

        // Initialize the printer
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);

        StringBuilder comborcptBuilder = new StringBuilder();

        for (EntryItem item : itemList) {
            String combo = item.getCombo();
            double amount = item.getAmount();
            total += amount;
            Totalamount = decimalFormat.format(total);
            String formattedAmount = decimalFormat.format(amount);

            // Format combo: Replace '*' with space and split if length > 4 (e.g., "04*12" → "04 12")
            combo = combo.replace("*", " ");
            if (combo.length() > 4) {
                combo = combo.substring(0, 3) + " " + combo.substring(3);
            }
            String styledEntry = createStyledTextSection(printer,item.getType(),combo,formattedAmount);

            comborcptBuilder.append("[L]<img>")
                    .append(styledEntry)
                    .append("</img>\n");
        }

        String imageSection = createImageOnlySection(printer);
        Bitmap qrBitmap = generateQRCode(transCode3, 150);
        String hexImage = createReceiptWithQR(printer, betDate, betDate, Name, Code, Totalamount, qrBitmap);
        String ticketcode = createTicketSection(printer, transCode2,18, 16);


        String printData = "[L]<img>" + imageSection + "</img>\n" +
                "[L]<img>" + hexImage + "</img>\n" +
                "[L]<img>" + ticketcode + "</img>\n" +
                comborcptBuilder.toString();

// Add content to the print  er
        printer.addTextToPrint(printData);

// Save the receipt content in the database
        reprintDBHelper dbHelper = new reprintDBHelper(this);
        long id = dbHelper.insertPrintData(transCode2, printData, betDate, drawTime, Name);

        SaveDataManager.savePrintData(this, printData);

        return printer;

    }

    private boolean insertCombinationToDatabase(List<EntryItem> itemList) {
        String version = AppVersion.VERSION_NAME;
        String group = Account.getInstance(this).getGroup();
        PreparedStatement preparedStatement = null;
        String insertSQL = "INSERT INTO EntryTB (combo, bets, prize, transcode, type, draw, game, agent, date, version, [group], qrcode) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            ConSQL c = new ConSQL();
            connection = c.conclass();
            if (connection != null) {
                connection.setAutoCommit(false);
                preparedStatement = connection.prepareStatement(insertSQL);

                int totalRows = 0;
                int successfulInserts = 0;

                for (EntryItem item : itemList) {
                    String combo = item.getCombo();
                    String game = item.getType();
                    BigDecimal totalAmount = BigDecimal.valueOf(item.getAmount());
                    BigDecimal shareAmount = totalAmount;
                    BigDecimal prize = new BigDecimal(item.getPrize().replace(",", ""));

                    // Check if combo ends with "R"
                    if (item.getType().startsWith("R")) {
                        // Generate permutations
                        if (gameName.equals("2D")) {
                            // 2-digit Rambolito only creates 2 permutations

                            String original = combo.substring(0, 2) + combo.substring(2);
                            String reversed = combo.substring(2) + combo.substring(0, 2);

                            List<String> permutations = Arrays.asList(original, reversed);

                            int numPermutations = 2;
                            shareAmount = totalAmount.divide(BigDecimal.valueOf(numPermutations), RoundingMode.HALF_UP);

                            for (String perm : permutations) {
                                preparedStatement.setString(1, perm);
                                preparedStatement.setBigDecimal(2, shareAmount);
                                preparedStatement.setBigDecimal(3, prize);
                                preparedStatement.setString(4, transCode2);
                                preparedStatement.setString(5, game);
                                preparedStatement.setString(6, drawTime);
                                preparedStatement.setString(7, gameName);
                                preparedStatement.setString(8, Name);
                                preparedStatement.setString(9, getCurrentDateTime());
                                preparedStatement.setString(10, version);
                                preparedStatement.setString(11, group);
                                preparedStatement.setString(12, transCode3);

                                preparedStatement.addBatch();
                                totalRows++;
                            }
                        } else {
                            List<String> permutations = PermutationUtil.getPermutations(combo);
                            int numPermutations = permutations.size();
                            shareAmount = totalAmount.divide(BigDecimal.valueOf(numPermutations), BigDecimal.ROUND_HALF_UP);

                            for (String perm : permutations) {
                                // Prepare entry for each permutation
                                preparedStatement.setString(1, perm);
                                preparedStatement.setBigDecimal(2, shareAmount);
                                preparedStatement.setBigDecimal(3, prize);
                                preparedStatement.setString(4, transCode2);
                                preparedStatement.setString(5, game);
                                preparedStatement.setString(6, drawTime);
                                preparedStatement.setString(7, gameName);
                                preparedStatement.setString(8, Name);
                                preparedStatement.setString(9, getCurrentDateTime());
                                preparedStatement.setString(10, version);
                                preparedStatement.setString(11, group);
                                preparedStatement.setString(12, transCode3);

                                preparedStatement.addBatch();
                                totalRows++;
                            }
                        }
                    } else {
                        preparedStatement.setString(1, combo);
                        preparedStatement.setBigDecimal(2, shareAmount);
                        preparedStatement.setBigDecimal(3, prize); // Assuming prize is not used here
                        preparedStatement.setString(4, transCode2);
                        preparedStatement.setString(5, game);
                        preparedStatement.setString(6, drawTime);
                        preparedStatement.setString(7, gameName);
                        preparedStatement.setString(8, Name);
                        preparedStatement.setString(9, getCurrentDateTime());
                        preparedStatement.setString(10, version);
                        preparedStatement.setString(11, group);
                        preparedStatement.setString(12, transCode3);

                        preparedStatement.addBatch();
                        totalRows++;
                    }
                }

                int[] results = preparedStatement.executeBatch();
                successfulInserts = results.length;

                if (successfulInserts == totalRows) {
                    connection.commit();
/*
                    Toast.makeText(this, " combination(s) saved successfully", Toast.LENGTH_SHORT).show();
*/
                    return true;
                } else {
                    connection.rollback();
/*
                    Toast.makeText(this, "Failed to save some combination(s)", Toast.LENGTH_SHORT).show();
*/
                    return false;
                }
            }
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback(); // Rollback if any exception occurs
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // Restore auto-commit mode
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            checkAndToggleButton();
            handler.postDelayed(this, 60000); // Check every 1 minute
        }
    };

    private void startCheckingTime() {
        handler.post(checkRunnable);
    }

    private void checkAndToggleButton() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        if (activityType == null) {
            Log.e("Error", "activityType is null");
            return;
        }

        // Determine cut-off times based on activityType
        boolean disableButtons = false;
        switch (activityType) {
            case "first":
                // 1:50 PM CUT OFF TIME
                if ((hour == 13 && minute >= 50) || hour > 13) {
                    disableButtons = true;
                    printBtn.setText("CLOSE");

                    Dialog dialog = new Dialog(this);
                    dialog.setContentView(R.layout.custom_errordialog);
                    dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                    dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
                    dialog.setCancelable(false);

                    Button positiveButton = dialog.findViewById(R.id.positiveButton);
                    TextView title = dialog.findViewById(R.id.dialogTitle);
                    TextView description = dialog.findViewById(R.id.dialogDescription);

                    title.setText("Info:");
                    description.setText("Draw has been closed, Cut off time 1:50 PM");

                    positiveButton.setOnClickListener(buttonView -> {
                        finish();
                        dialog.dismiss();
                    });
                    if (!isFinishing() && !isDestroyed()) {
                        dialog.show();
                    }
                }
                break;
            case "second":
                // 4:50 PM CUT OFF TIME
                if ((hour == 16 && minute >= 50) || hour > 16) {
                    disableButtons = true;
                    printBtn.setText("CLOSE");

                    Dialog dialog = new Dialog(this);
                    dialog.setContentView(R.layout.custom_errordialog);
                    dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                    dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
                    dialog.setCancelable(false);

                    Button positiveButton = dialog.findViewById(R.id.positiveButton);
                    TextView title = dialog.findViewById(R.id.dialogTitle);
                    TextView description = dialog.findViewById(R.id.dialogDescription);

                    title.setText("Info:");
                    description.setText("Draw has been closed, Cut off time 4:50 PM");

                    positiveButton.setOnClickListener(buttonView -> {
                        finish();
                        dialog.dismiss();
                    });
                    if (!isFinishing() && !isDestroyed()) {
                        dialog.show();
                    }
                }
                break;
            case "third":
                // 8:50 PM CUT OFF TIME
                if ((hour == 20 && minute >= 50) || hour > 20) {
                    disableButtons = true;
                    printBtn.setText("CLOSE");

                    Dialog dialog = new Dialog(this);
                    dialog.setContentView(R.layout.custom_errordialog);
                    dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                    dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.custom_dialog_bg));
                    dialog.setCancelable(false);

                    Button positiveButton = dialog.findViewById(R.id.positiveButton);
                    TextView title = dialog.findViewById(R.id.dialogTitle);
                    TextView description = dialog.findViewById(R.id.dialogDescription);

                    title.setText("Info:");
                    description.setText("Draw has been closed, Cut off time 8:50 PM");

                    positiveButton.setOnClickListener(buttonView -> {
                        finish();
                        dialog.dismiss();
                    });
                    if (!isFinishing() && !isDestroyed()) {
                        dialog.show();
                    }
                }
                break;
        }
        printBtn.setEnabled(!disableButtons);
        addBtn.setEnabled(!disableButtons);
        comboInput.setEnabled(!disableButtons);
        stretInput.setEnabled(!disableButtons);
        rambolInput.setEnabled(!disableButtons);
    }
    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");
        return dateFormat.format(new Date());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}