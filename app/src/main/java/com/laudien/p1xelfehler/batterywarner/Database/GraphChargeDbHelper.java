package com.laudien.p1xelfehler.batterywarner.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class GraphChargeDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "GraphChargeDbHelper";
    private static final String DATABASE_NAME = "ChargeCurveDB";
    private static final int DATABASE_VERSION = 3; // if the version is changed, a new database will be created!
    public static final String TABLE_NAME = "ChargeCurve";
    public static final String TABLE_COLUMN_TIME = "time";
    public static final String TABLE_COLUMN_PERCENTAGE = "percentage";
    public static final String TABLE_COLUMN_TEMP = "temperature";
    private static final String CREATE_QUERY =
            "CREATE TABLE " + TABLE_NAME
                    + " (" + TABLE_COLUMN_TIME + " TEXT,"
                    + TABLE_COLUMN_PERCENTAGE + " INTEGER,"
                    + TABLE_COLUMN_TEMP + " INTEGER);";

    public GraphChargeDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.i(TAG, "Database created/opened!");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_QUERY);
        Log.i(TAG, "Table created!");
    }


    public void addValue(long time, int percentage, int temperature) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TABLE_COLUMN_TIME, time);
        contentValues.put(TABLE_COLUMN_PERCENTAGE, percentage);
        contentValues.put(TABLE_COLUMN_TEMP, temperature);
        try {
            getWritableDatabase().insert(TABLE_NAME, null, contentValues);
        } catch (Exception e) {
            getWritableDatabase().execSQL(CREATE_QUERY);
            getWritableDatabase().insert(TABLE_NAME, null, contentValues);
        }
        Log.i(TAG, "Added value (" + percentage + "%/" + time + "ms/" + temperature / 10 + "°C)");
        close();
    }

    public void resetTable() {
        getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME);
        Log.i(TAG, "Table reset!");
        close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    @Override
    public synchronized void close() {
        super.close();
        Log.i(TAG, "Database closed!");
    }
}
