package com.example.lottomatic.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class reprintDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "print_data.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "PrintHistory";
    private static final String COLUMN_TRANS_CODE = "transCode";
    private static final String COLUMN_PRINT_DATA = "printData";

    public reprintDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TRANS_CODE + " TEXT, " +
                COLUMN_PRINT_DATA + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
    public long insertPrintData(String transCode, String printData, String betDate, String drawTime, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_TRANS_CODE, transCode);    // Transaction code
        values.put(COLUMN_PRINT_DATA, printData);    // Print data (receipt content)

        // Insert the row into the table
        long result = db.insert(TABLE_NAME, null, values);

        // Close the database connection
        db.close();

        return result;  // Return the row ID of the newly inserted row, or -1 if an error occurred
    }

    // Method to get print data by transcode
    public String getPrintDataByTransCode(String transcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_PRINT_DATA + " FROM " + TABLE_NAME + " WHERE " + COLUMN_TRANS_CODE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{transcode});

        if (cursor.moveToFirst()) {
            String printData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRINT_DATA));
            cursor.close();
            return printData;
        } else {
            cursor.close();
            return null;
        }
    }
}
