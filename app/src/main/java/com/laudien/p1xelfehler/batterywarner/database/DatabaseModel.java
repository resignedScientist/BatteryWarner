package com.laudien.p1xelfehler.batterywarner.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.util.HashMap;

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
    private static final int MAX_DATA_POINTS = 200;
    private HashMap<String, SQLiteDatabase> openedDatabases = new HashMap<>();

    DatabaseModel(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(
                String.format("CREATE TABLE %s (%s TEXT,%s INTEGER,%s INTEGER, %s INTEGER, %s INTEGER);",
                        DatabaseContract.TABLE_NAME,
                        DatabaseContract.TABLE_COLUMN_TIME,
                        DatabaseContract.TABLE_COLUMN_BATTERY_LEVEL,
                        DatabaseContract.TABLE_COLUMN_TEMPERATURE,
                        DatabaseContract.TABLE_COLUMN_VOLTAGE,
                        DatabaseContract.TABLE_COLUMN_CURRENT
                )
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.d(getClass().getSimpleName(), "onUpgrade() -> oldVersion = " + oldVersion + ", newVersion = " + newVersion);
        if (oldVersion < 5) {
            Log.d(getClass().getSimpleName(), "Upgrading file: " + sqLiteDatabase.getPath());
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
        return readData(getCursor());
    }

    /**
     * Add a value to the app directory database.
     *
     * @param value A DatabaseValue containing all the data of the new graph point.
     * @return Returns the total number of rows in the database after adding the value.
     */
    long addValue(DatabaseValue value) {
        SQLiteDatabase database = getWritableDatabase();
        long totalNumberOfRows = 0;
        if (database != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseContract.TABLE_COLUMN_TIME, value.getUtcTimeInMillis());
            contentValues.put(DatabaseContract.TABLE_COLUMN_BATTERY_LEVEL, value.getBatteryLevel());
            contentValues.put(DatabaseContract.TABLE_COLUMN_TEMPERATURE, value.getTemperature());
            contentValues.put(DatabaseContract.TABLE_COLUMN_VOLTAGE, value.getVoltage());
            contentValues.put(DatabaseContract.TABLE_COLUMN_CURRENT, value.getCurrent());
            database.insert(DatabaseContract.TABLE_NAME, null, contentValues);
            totalNumberOfRows = DatabaseUtils.queryNumEntries(database, DatabaseContract.TABLE_NAME);
            database.close();
        }
        return totalNumberOfRows;
    }

    /**
     * Clears the table of the app directory database.
     */
    void resetTable() {
        SQLiteDatabase database = getWritableDatabase();
        if (database != null) {
            database.execSQL("DELETE FROM " + DatabaseContract.TABLE_NAME);
            database.close();
        }
    }

    /**
     * Get a Cursor instance which points to the app directory database.
     *
     * @return A Cursor instance which points to the app directory database.
     */
    Cursor getCursor() {
        return getCursor(getReadableDatabase());
    }

    // ==== ANY DATABASE FROM A FILE ====

    /**
     * Get all the data of the given database file.
     *
     * @param databaseFile A valid SQLite database file.
     * @return An array of all the data inside given database.
     */
    DatabaseValue[] readData(File databaseFile) {
        return readData(getCursor(databaseFile));
    }

    /**
     * Get a Cursor instance which points to the given database.
     *
     * @param databaseFile A valid SQLite database file.
     * @return A Cursor instance which points to the given database.
     */
    Cursor getCursor(File databaseFile) {
        return getCursor(getReadableDatabase(databaseFile));
    }

    void close(File file) {
        SQLiteDatabase database = openedDatabases.get(file.getPath());
        if (database != null) {
            database.close();
            openedDatabases.remove(file.getPath());
        }
    }

    void shortenGraph(File file) {
        SQLiteDatabase database = getWritableDatabase(file);
        if (database == null) {
            return;
        }
        Cursor cursor = getCursor(database);
        if (cursor == null || cursor.isClosed() || cursor.getCount() <= MAX_DATA_POINTS * 2) {
            return;
        }
        int count = cursor.getCount();
        int divisor = count / MAX_DATA_POINTS;
        database.beginTransaction();
        for (int i = 0; i < count; i++) {
            if (i % divisor == 0) {
                continue;
            }
            cursor.moveToPosition(i);
            long time = cursor.getLong(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TIME));
            database.delete(
                    DatabaseContract.TABLE_NAME,
                    DatabaseContract.TABLE_COLUMN_TIME + "=?",
                    new String[]{String.valueOf(time)}
            );
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        cursor.close();
        close(file);
    }

    // ==== GENERAL STUFF ====

    private DatabaseValue[] readData(Cursor cursor) {
        DatabaseValue[] databaseValues = null;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                databaseValues = new DatabaseValue[cursor.getCount()];
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    int batteryLevel = cursor.getInt(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_BATTERY_LEVEL));
                    int temperature = cursor.getInt(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TEMPERATURE));
                    int voltage = cursor.getInt(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_VOLTAGE));
                    int current = cursor.getInt(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_CURRENT));
                    long time = cursor.getLong(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TIME));
                    databaseValues[i] = new DatabaseValue(batteryLevel, temperature, voltage, current, time);
                }
            }
            cursor.close();
        }
        return databaseValues;
    }

    Cursor getCursor(SQLiteDatabase database) {
        if (database != null) {
            String[] columns = {
                    DatabaseContract.TABLE_COLUMN_TIME,
                    DatabaseContract.TABLE_COLUMN_BATTERY_LEVEL,
                    DatabaseContract.TABLE_COLUMN_TEMPERATURE,
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
        } else {
            return null;
        }
    }

    SQLiteDatabase getReadableDatabase(File databaseFile) {
        if (openedDatabases.containsKey(databaseFile.getPath())) {
            return openedDatabases.get(databaseFile.getPath());
        }
        try {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(
                    databaseFile.getPath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY
            );
            // upgrade database if necessary
            if (database.getVersion() < DATABASE_VERSION) {
                long lastModified = databaseFile.lastModified();
                database.close();
                database = SQLiteDatabase.openDatabase(
                        databaseFile.getPath(),
                        null,
                        SQLiteDatabase.OPEN_READWRITE
                );
                onUpgrade(database, database.getVersion(), DATABASE_VERSION);
                database.setVersion(DATABASE_VERSION);
                database.close();
                databaseFile.setLastModified(lastModified); // keep last modified date the same
                database = SQLiteDatabase.openDatabase(
                        databaseFile.getPath(),
                        null,
                        SQLiteDatabase.OPEN_READONLY
                );
            }
            openedDatabases.put(databaseFile.getPath(), database);
            return database;
        } catch (SQLiteException e) {
            e.printStackTrace();
            return null;
        }
    }

    private SQLiteDatabase getWritableDatabase(File databaseFile) {
        if (openedDatabases.containsKey(databaseFile.getPath())) {
            return openedDatabases.get(databaseFile.getPath());
        }
        try {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(
                    databaseFile.getPath(),
                    null,
                    SQLiteDatabase.OPEN_READWRITE
            );
            openedDatabases.put(databaseFile.getPath(), database);
            return database;
        } catch (SQLiteException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        try {
            return super.getReadableDatabase();
        } catch (SQLiteException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        try {
            return super.getWritableDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
