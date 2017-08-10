package com.laudien.p1xelfehler.batterywarner.database;

import android.content.ContentValues;
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
        return readData(getCursor());
    }

    public void addValue(DatabaseValue value) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseContract.TABLE_COLUMN_TIME, value.getUtcTimeInMillis());
        contentValues.put(DatabaseContract.TABLE_COLUMN_PERCENTAGE, value.getBatteryLevel());
        contentValues.put(DatabaseContract.TABLE_COLUMN_TEMP, value.getTemperature());
        SQLiteDatabase database = getWritableDatabase();
        try {
            database.insert(DatabaseContract.TABLE_NAME, null, contentValues);
        } catch (Exception e) {
            onCreate(database);
            database.insert(DatabaseContract.TABLE_NAME, null, contentValues);
        }
    }

    public void resetTable() {
        getWritableDatabase().execSQL("DELETE FROM " + DatabaseContract.TABLE_NAME);
    }

    public Cursor getCursor() {
        return getCursor(getReadableDatabase());
    }

    // ==== ANY DATABASE FROM A FILE ====
    public DatabaseValue[] readData(File databaseFile) {
        return readData(getCursor(databaseFile));
    }

    public Cursor getCursor(File databaseFile) {
        SQLiteDatabase database = getReadableDatabase(databaseFile);
        return getCursor(database);
    }

    // ==== GENERAL STUFF ====

    private DatabaseValue[] readData(Cursor cursor) {
        DatabaseValue[] databaseValues = null;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                databaseValues = new DatabaseValue[cursor.getCount()];
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    int batteryLevel = cursor.getInt(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_PERCENTAGE));
                    double temperature = cursor.getDouble(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TEMP));
                    long time = cursor.getLong(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TIME));
                    databaseValues[i] = new DatabaseValue(batteryLevel, temperature, time);
                }
            }
            cursor.close();
        }
        return databaseValues;
    }

    private Cursor getCursor(SQLiteDatabase database) {
        String[] columns = {
                DatabaseContract.TABLE_COLUMN_TIME,
                DatabaseContract.TABLE_COLUMN_PERCENTAGE,
                DatabaseContract.TABLE_COLUMN_TEMP};
        return database.query(
                DatabaseContract.TABLE_NAME,
                columns,
                null,
                null,
                null,
                null,
                "length(" + DatabaseContract.TABLE_COLUMN_TIME + "), " + DatabaseContract.TABLE_COLUMN_TIME
        );
    }

    private SQLiteDatabase getReadableDatabase(File databaseFile) {
        return SQLiteDatabase.openDatabase(
                databaseFile.getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY
        );
    }
}
