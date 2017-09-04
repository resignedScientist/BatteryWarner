package com.laudien.p1xelfehler.batterywarner.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

/**
 * The Model for the charging graph databases. Only the DatabaseController should communicate to
 * instances of this class.
 */
class DatabaseModel extends SQLiteOpenHelper {
    /**
     * The name of the database.
     */
    static final String DATABASE_NAME = "ChargeCurveDB";
    private static final int DATABASE_VERSION = 5; // if the version is changed, a new database will be created!

    DatabaseModel(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(
                String.format("CREATE TABLE %s (%s TEXT,%s INTEGER,%s INTEGER, %s INTEGER, %s INTEGER);",
                        DatabaseContract.TABLE_NAME,
                        DatabaseContract.TABLE_COLUMN_TIME,
                        DatabaseContract.TABLE_COLUMN_PERCENTAGE,
                        DatabaseContract.TABLE_COLUMN_TEMP,
                        DatabaseContract.TABLE_COLUMN_VOLTAGE,
                        DatabaseContract.TABLE_COLUMN_CURRENT
                )
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.d(getClass().getSimpleName(), "onUpgrade() -> oldVersion = " + oldVersion + ", newVersion = " + newVersion);
        if (oldVersion < 5) {
            String statement = "ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT 0";
            try {
                sqLiteDatabase.execSQL(String.format(
                        statement, DatabaseContract.TABLE_NAME, DatabaseContract.TABLE_COLUMN_VOLTAGE));
                sqLiteDatabase.execSQL(String.format(
                        statement, DatabaseContract.TABLE_NAME, DatabaseContract.TABLE_COLUMN_CURRENT));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // ==== DEFAULT DATABASE IN THE APP DIRECTORY ====

    /**
     * Reads all the data inside the app directory database.
     *
     * @return An array of all the data inside the app directory database.
     */
    DatabaseValue[] readData() {
        SQLiteDatabase database = getReadableDatabase();
        DatabaseValue[] databaseValues = readData(getCursor(database));
        database.close();
        return databaseValues;
    }

    DatabaseValue getFirst() {
        SQLiteDatabase database = getReadableDatabase();
        DatabaseValue firstValue = getFirst(database);
        database.close();
        return firstValue;
    }

    DatabaseValue getLast() {
        SQLiteDatabase database = getReadableDatabase();
        DatabaseValue lastValue = getLast(database);
        database.close();
        return lastValue;
    }

    DatabaseValue[] getFirstAndLast() {
        DatabaseValue[] databaseValues = new DatabaseValue[2];
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = getCursor(database);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                databaseValues[0] = readCurrentDatabaseValue(cursor);
            }
            if (cursor.moveToLast()) {
                databaseValues[1] = readCurrentDatabaseValue(cursor);
            }
            cursor.close();
        }
        database.close();
        return databaseValues;
    }

    /**
     * Add a value to the app directory database.
     *
     * @param value A DatabaseValue containing all the data of the new graph point.
     */
    void addValue(DatabaseValue value) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseContract.TABLE_COLUMN_TIME, value.getUtcTimeInMillis());
        contentValues.put(DatabaseContract.TABLE_COLUMN_PERCENTAGE, value.getBatteryLevel());
        contentValues.put(DatabaseContract.TABLE_COLUMN_TEMP, value.getTemperature());
        contentValues.put(DatabaseContract.TABLE_COLUMN_VOLTAGE, value.getVoltage());
        contentValues.put(DatabaseContract.TABLE_COLUMN_CURRENT, value.getCurrent());
        SQLiteDatabase database = getWritableDatabase();
        try {
            database.insert(DatabaseContract.TABLE_NAME, null, contentValues);
        } catch (Exception e) {
            onCreate(database);
            database.insert(DatabaseContract.TABLE_NAME, null, contentValues);
        }
        database.close();
    }

    /**
     * Clears the table of the app directory database.
     */
    void resetTable() {
        getWritableDatabase().execSQL("DELETE FROM " + DatabaseContract.TABLE_NAME);
    }

    // ==== ANY DATABASE FROM A FILE ====

    /**
     * Get all the data of the given database file.
     *
     * @param databaseFile A valid SQLite database file.
     * @return An array of all the data inside given database.
     */
    DatabaseValue[] readData(File databaseFile) {
        SQLiteDatabase database = getReadableDatabase(databaseFile);
        DatabaseValue[] databaseValues = readData(getCursor(database));
        database.close();
        return databaseValues;
    }

    DatabaseValue getFirst(File databaseFile) {
        SQLiteDatabase database = getReadableDatabase(databaseFile);
        DatabaseValue firstValue = getFirst(database);
        database.close();
        return firstValue;
    }

    DatabaseValue getLast(File databaseFile) {
        SQLiteDatabase database = getReadableDatabase(databaseFile);
        DatabaseValue lastValue = getLast(database);
        database.close();
        return lastValue;
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
                    double voltage = cursor.getDouble(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_VOLTAGE));
                    int current = cursor.getInt(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_CURRENT));
                    long time = cursor.getLong(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TIME));
                    databaseValues[i] = new DatabaseValue(batteryLevel, temperature, voltage, current, time);
                }
            }
            cursor.close();
        }
        return databaseValues;
    }

    private DatabaseValue readCurrentDatabaseValue(Cursor cursor) {
        int batteryLevel = cursor.getInt(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_PERCENTAGE));
        double temperature = cursor.getDouble(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TEMP));
        long time = cursor.getLong(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TIME));
        return new DatabaseValue(batteryLevel, temperature, time);
    }

    private Cursor getCursor(SQLiteDatabase database) {
        String[] columns = {
                DatabaseContract.TABLE_COLUMN_TIME,
                DatabaseContract.TABLE_COLUMN_PERCENTAGE,
                DatabaseContract.TABLE_COLUMN_TEMP,
                DatabaseContract.TABLE_COLUMN_VOLTAGE,
                DatabaseContract.TABLE_COLUMN_CURRENT
        };
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
        SQLiteDatabase database = SQLiteDatabase.openDatabase(
                databaseFile.getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY
        );
        // upgrade database if necessary
        if (database.getVersion() < DATABASE_VERSION) {
            database.close();
            database = SQLiteDatabase.openDatabase(
                    databaseFile.getPath(),
                    null,
                    SQLiteDatabase.OPEN_READWRITE
            );
            onUpgrade(database, database.getVersion(), DATABASE_VERSION);
            database.setVersion(DATABASE_VERSION);
            database.close();
            database = SQLiteDatabase.openDatabase(
                    databaseFile.getPath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY
            );
        }
        return database;
    }

    private DatabaseValue getFirst(SQLiteDatabase database) {
        DatabaseValue databaseValue = null;
        Cursor cursor = getCursor(database);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                databaseValue = readCurrentDatabaseValue(cursor);
            }
            cursor.close();
        }
        return databaseValue;
    }

    private DatabaseValue getLast(SQLiteDatabase database) {
        DatabaseValue databaseValue = null;
        Cursor cursor = getCursor(database);
        if (cursor != null) {
            if (cursor.moveToLast()) {
                databaseValue = readCurrentDatabaseValue(cursor);
            }
            cursor.close();
        }
        return databaseValue;
    }
}
