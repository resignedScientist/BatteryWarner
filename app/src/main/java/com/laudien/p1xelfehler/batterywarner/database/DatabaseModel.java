package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class DatabaseModel extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "ChargeCurveDB";
    public static final int DATABASE_VERSION = 4; // if the version is changed, a new database will be created!

    public DatabaseModel(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(
                String.format("CREATE TABLE %s (%s TEXT,%s INTEGER,%s INTEGER);",
                        DatabaseContract.TABLE_NAME,
                        DatabaseContract.TABLE_COLUMN_TIME,
                        DatabaseContract.TABLE_COLUMN_PERCENTAGE,
                        DatabaseContract.TABLE_COLUMN_TEMP)
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (oldVersion == 4 && newVersion == 5) {
            // TODO: version 4 -> version 5
        }
    }

    // ==== DEFAULT DATABASE IN THE APP DIRECTORY ====
    public DatabaseValue[] readData() {
        return null;
    }

    public void addValue(DatabaseValue value) {

    }

    public void resetTable() {
        getWritableDatabase().execSQL("DELETE FROM " + DatabaseContract.TABLE_NAME);
        close();
    }

    public Cursor getCursor() {
        return null;
    }

    // ==== ANY DATABASE FROM A FILE ====
    public DatabaseValue[] readData(File databaseFile) {
        return null;
    }

    public void addValue(File databaseFile, DatabaseValue value) {

    }

    public Cursor getCursor(File databaseFile) {
        return null;
    }
}
