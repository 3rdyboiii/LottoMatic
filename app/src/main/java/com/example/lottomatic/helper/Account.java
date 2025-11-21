package com.example.lottomatic.helper;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class Account {
    private static Account instance;
    private String username;
    private String password;
    private String name;
    private String code;
    private String group;
    private double gross;
    private double prize4D;
    private double prize3D;
    private double prize2D;


    private static final String PREFS_NAME = "account_prefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_NAME = "name";
    private static final String KEY_CODE = "code";
    private static final String KEY_GROUP = "group";
    private static final String KEY_GROSS = "gross";
    private static final String KEY_PRIZE4D = "prize4D";
    private static final String KEY_PRIZE3D = "prize3D";
    private static final String KEY_PRIZE2D = "prize2D";
    private static final String KEY_LIMITS = "bet_limits";
    private static final String KEY_LIMITS_TIMESTAMP = "limits_timestamp";

    private SharedPreferences sharedPreferences;

    private Account(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        username = sharedPreferences.getString(KEY_USERNAME, null);
        password = sharedPreferences.getString(KEY_PASSWORD, null);
        name = sharedPreferences.getString(KEY_NAME, null);
        code = sharedPreferences.getString(KEY_CODE, null);
        group = sharedPreferences.getString(KEY_GROUP, null);
        gross = sharedPreferences.getFloat(KEY_GROSS, 0.0f); // Load gross from SharedPreferences
        prize4D = sharedPreferences.getFloat(KEY_PRIZE4D, 0.0f);
        prize3D = sharedPreferences.getFloat(KEY_PRIZE3D, 0.0f);
        prize2D = sharedPreferences.getFloat(KEY_PRIZE2D, 0.0f);
    }

    public static synchronized Account getInstance(Context context) {
        if (instance == null) {
            instance = new Account(context);
        }
        return instance;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getGroup() {
        return group;
    }

    public double getGross() {
        return gross;
    }

    public double getPrize4D() {
        return prize4D;
    }

    public double getPrize3D() {
        return prize3D;
    }

    public double getPrize2D() {
        return prize2D;
    }

    public void setBetLimits(Map<String, Double> limits) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        JSONObject jsonLimits = new JSONObject(limits);
        editor.putString(KEY_LIMITS, jsonLimits.toString());
        editor.putLong(KEY_LIMITS_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }

    public double getBetLimit(String gameType, double defaultLimit) {
        String jsonString = sharedPreferences.getString(KEY_LIMITS, null);
        if (jsonString != null) {
            try {
                JSONObject jsonLimits = new JSONObject(jsonString);
                return jsonLimits.optDouble(gameType, defaultLimit);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return defaultLimit;
    }

    public void setUsername(String username) {
        this.username = username;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public void setPassword(String password) {
        this.password = password;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PASSWORD, password);
        editor.apply();
    }

    public void setName(String name) {
        this.name = name;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_NAME, name);
        editor.apply();
    }

    public void setCode(String code) {
        this.code = code;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CODE, code);
        editor.apply();
    }

    public void setGroup(String group) {
        this.group = group;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_GROUP, group);
        editor.apply();
    }

    public void setGross(double gross) {
        this.gross = gross;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_GROSS, (float) gross); // Store as float in SharedPreferences
        editor.apply();
    }

    // Optional: Format gross as currency string
    public String getFormattedGross() {
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###.00");
        return "₱ " + formatter.format(gross);
    }

    // Optional: Update gross by adding/subtracting amount
    public void updateGross(double amount) {
        this.gross += amount;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_GROSS, (float) this.gross);
        editor.apply();
    }

    public void setPrize4D(double prize4D) {
        this.prize4D = prize4D;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_PRIZE4D, (float) prize4D);
        editor.apply();
    }

    public void setPrize3D(double prize3D) {
        this.prize3D = prize3D;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_PRIZE3D, (float) prize3D);
        editor.apply();
    }

    public void setPrize2D(double prize2D) {
        this.prize2D = prize2D;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_PRIZE2D, (float) prize2D);
        editor.apply();
    }

}