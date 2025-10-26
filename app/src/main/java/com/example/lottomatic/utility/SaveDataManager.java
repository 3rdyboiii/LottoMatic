package com.example.lottomatic.utility;

import android.content.Context;
import android.content.SharedPreferences;

public class SaveDataManager {
    private static final String PREF_NAME = "PrintDataPrefs";

    // Keys for print data
    private static final String KEY_PRINT_DATA = "printData";

    // Keys for draw data
    private static final String KEY_DRAW_3D = "draw3D";
    private static final String KEY_DRAW_4D = "draw4D";
    private static final String KEY_DRAW_6D = "draw6D";
    private static final String KEY_DRAW_L2 = "drawL2";
    private static final String KEY_DRAW_P3 = "drawP3";

    public static void savePrintData(Context context, String data) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_PRINT_DATA, data);
        editor.apply();
    }

    public static String getSavedPrintData(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_PRINT_DATA, null);
    }

    public static void clearPrintData(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("PrintDataPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("printData");
        editor.apply();
    }
    // Methods to save and retrieve draw data
    public static void saveDrawData(Context context, String draw3D, String draw4D, String draw6D, String drawL2, String drawP3) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DRAW_3D, draw3D);
        editor.putString(KEY_DRAW_4D, draw4D);
        editor.putString(KEY_DRAW_6D, draw6D);
        editor.putString(KEY_DRAW_L2, drawL2);
        editor.putString(KEY_DRAW_P3, drawP3);
        editor.apply();
    }

    public static String[] getSavedDrawData(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String draw3D = preferences.getString(KEY_DRAW_3D, null);
        String draw4D = preferences.getString(KEY_DRAW_4D, null);
        String draw6D = preferences.getString(KEY_DRAW_6D, null);
        String drawL2 = preferences.getString(KEY_DRAW_L2, null);
        String drawP3 = preferences.getString(KEY_DRAW_P3, null);
        return new String[]{draw3D, draw4D, draw6D, drawL2, drawP3};
    }
}
