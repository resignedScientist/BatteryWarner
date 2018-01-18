package com.laudien.p1xelfehler.batterywarner.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.NUMBER_OF_GRAPHS;

/**
 * The Model for the charging graph databases. Only the DatabaseController should communicate to
 * instances of this class.
 */
public class DatabaseModel extends SQLiteOpenHelper implements DatabaseContract.Model {
    /**
     * The name of the database.
     */
    static final String DATABASE_NAME = "ChargeCurveDB";
    private static final int DATABASE_VERSION = 5; // if the version is changed, a new database will be created!
    private static DatabaseModel instance;
    private HashMap<String, SQLiteDatabase> openedDatabases = new HashMap<>();
    private HashSet<DatabaseContract.DatabaseListener> listeners = new HashSet<>();

    DatabaseModel(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @NonNull
    public static DatabaseModel getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new DatabaseModel(context);
        }
        return instance;
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

    @Override
    public void readData(boolean useFahrenheit, boolean reverseCurrent, @NonNull DatabaseContract.DataReceiver dataReceiver) {
        new ReadDataTask(null, useFahrenheit, reverseCurrent, dataReceiver).execute();
    }

    @Override
    public void readData(File databaseFile, boolean useFahrenheit, boolean reverseCurrent, @NonNull DatabaseContract.DataReceiver dataReceiver) {
        new ReadDataTask(databaseFile, useFahrenheit, reverseCurrent, dataReceiver).execute();
    }

    @Override
    public Cursor getCursor() {
        return getCursor(getReadableDatabase());
    }

    @Override
    @Nullable
    public Cursor getCursor(@NonNull File databaseFile) {
        SQLiteDatabase database = getReadableDatabase(databaseFile);
        if (database == null) {
            return null;
        }
        return getCursor(database);
    }

    @Override
    @Nullable
    public Cursor getCursor(@NonNull SQLiteDatabase database) {
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
    }

    @Override
    @Nullable
    public SQLiteDatabase getReadableDatabase(@NonNull File databaseFile) {
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
                database = SQLiteDatabase.openDatabase(
                        databaseFile.getPath(),
                        null,
                        SQLiteDatabase.OPEN_READWRITE
                );
                onUpgrade(database, database.getVersion(), DATABASE_VERSION);
                database.setVersion(DATABASE_VERSION);
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

    @Override
    public SQLiteDatabase getWritableDatabase(@NonNull File databaseFile) {
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
    public void addValue(@NonNull DatabaseValue value) {
        new AddValueTask().execute(value);
    }

    @Override
    public long getCreationTime() {
        Cursor cursor = getCursor();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TIME));
            }
        }
        return 0;
    }

    @Override
    public void resetTable() {
        new ResetTableTask().execute();
    }

    @Override
    public void closeAllExternalFiles() {
        for (SQLiteDatabase database : openedDatabases.values()) {
            if (database.isOpen()) {
                database.close();
            }
        }
        openedDatabases.clear();
    }

    @Override
    public void notifyValueAdded(DatabaseValue value, long totalNumberOfRows) {
        for (DatabaseContract.DatabaseListener listener : listeners) {
            listener.onValueAdded(value, totalNumberOfRows);
        }
    }

    @Override
    public void notifyTableReset() {
        for (DatabaseContract.DatabaseListener listener : listeners) {
            listener.onTableReset();
        }
    }

    @Override
    public void registerDatabaseListener(DatabaseContract.DatabaseListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterDatabaseListener(DatabaseContract.DatabaseListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            close();
        }
    }

    @Nullable
    Data readData(Cursor cursor, boolean useFahrenheit, boolean reverseCurrent) {
        if (cursor == null || cursor.isClosed() || cursor.getCount() <= 0) {
            return null;
        }
        LineGraphSeries<DataPoint>[] graphs = new LineGraphSeries[NUMBER_OF_GRAPHS];
        graphs[GRAPH_INDEX_BATTERY_LEVEL] = new LineGraphSeries();
        graphs[GRAPH_INDEX_TEMPERATURE] = new LineGraphSeries();
        int firstBatteryLvl = 0;
        int maxBatteryLvl = 0;
        int minTemp = 0;
        int maxTemp = 0;
        int minVoltage = 0;
        int maxVoltage = 0;
        int minCurrent = 0;
        int maxCurrent = 0;
        long startTime = 0;
        long time = 0;
        long timeOfValueWithMaxBatteryLvl = 0;
        DatabaseValue lastValue = null;

        for (int valueIndex = 0; valueIndex < cursor.getCount(); valueIndex++) {
            cursor.moveToPosition(valueIndex);
            int batteryLevel = cursor.getInt(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_BATTERY_LEVEL));
            int temperature = cursor.getInt(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TEMPERATURE));
            int voltage = cursor.getInt(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_VOLTAGE));
            int current = cursor.getInt(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_CURRENT));
            time = cursor.getLong(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TIME));
            if (valueIndex == 0) {
                firstBatteryLvl = batteryLevel;
                maxBatteryLvl = batteryLevel;
                minTemp = temperature;
                maxTemp = temperature;
                minVoltage = voltage;
                maxVoltage = voltage;
                minCurrent = current;
                maxCurrent = current;
                startTime = time;
                if (voltage != 0)
                    graphs[GRAPH_INDEX_VOLTAGE] = new LineGraphSeries<>();
                if (current != 0)
                    graphs[GRAPH_INDEX_CURRENT] = new LineGraphSeries<>();
            }

            DatabaseValue value = new DatabaseValue(batteryLevel, temperature, voltage, current, time, startTime);
            DataPoint[] dataPoints = value.toDataPoints(useFahrenheit, reverseCurrent);
            for (int graphIndex = 0; graphIndex < NUMBER_OF_GRAPHS; graphIndex++) {
                if (graphs[graphIndex] == null || dataPoints[graphIndex] == null) {
                    continue;
                }
                if (lastValue == null || lastValue.get(graphIndex) != value.get(graphIndex) || valueIndex == cursor.getCount() - 1) {
                    graphs[graphIndex].appendData(dataPoints[graphIndex], false, valueIndex + 1);
                }
            }
            lastValue = value;

            if (valueIndex == 0)
                continue;

            if (temperature > maxTemp)
                maxTemp = temperature;
            if (temperature < minTemp)
                minTemp = temperature;
            if (DatabaseValue.convertToMilliAmperes(current, reverseCurrent) > DatabaseValue.convertToMilliAmperes(maxCurrent, reverseCurrent))
                maxCurrent = current;
            if (DatabaseValue.convertToMilliAmperes(current, reverseCurrent) < DatabaseValue.convertToMilliAmperes(minCurrent, reverseCurrent))
                minCurrent = current;
            if (voltage > maxVoltage)
                maxVoltage = voltage;
            if (voltage < minVoltage)
                minVoltage = voltage;
            if (batteryLevel > maxBatteryLvl) {
                maxBatteryLvl = batteryLevel;
                timeOfValueWithMaxBatteryLvl = time;
            }
        }

        double chargingSpeed = cursor.getCount() < 2 ? Double.NaN :
                3600000.0 * ((double) (maxBatteryLvl - firstBatteryLvl) / (double) (timeOfValueWithMaxBatteryLvl - startTime));

        GraphInfo graphInfo = new GraphInfo(
                startTime,
                time,
                lastValue.getTimeFromStartInMinutes(),
                maxTemp,
                minTemp,
                chargingSpeed,
                minCurrent,
                maxCurrent,
                DatabaseValue.convertToVolts(minVoltage),
                DatabaseValue.convertToVolts(maxVoltage),
                maxBatteryLvl,
                firstBatteryLvl,
                useFahrenheit,
                reverseCurrent
        );

        return new Data(graphs, graphInfo);
    }

    boolean resetTableTask() {
        SQLiteDatabase database = getWritableDatabase();
        if (database == null) {
            return false;
        }
        database.execSQL("DELETE FROM " + DatabaseContract.TABLE_NAME);
        return true;
    }

    long addValueTask(DatabaseValue value) {
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
            Log.d("DatabaseModel", "value added: " + value);
        }
        return totalNumberOfRows;
    }

    private class ReadDataTask extends AsyncTask<Void, Void, Data> {
        private DatabaseContract.DataReceiver dataReceiver;
        private boolean useFahrenheit, reverseCurrent;
        private File file;

        private ReadDataTask(@Nullable File file, boolean useFahrenheit, boolean reverseCurrent, @NonNull DatabaseContract.DataReceiver dataReceiver) {
            this.file = file;
            this.useFahrenheit = useFahrenheit;
            this.reverseCurrent = reverseCurrent;
            this.dataReceiver = dataReceiver;
        }

        @Override
        protected Data doInBackground(Void... voids) {
            Cursor cursor;
            if (file == null) {
                cursor = getCursor();
            } else {
                cursor = getCursor(file);
            }
            return readData(cursor, useFahrenheit, reverseCurrent);
        }

        @Override
        protected void onPostExecute(Data data) {
            super.onPostExecute(data);
            if (data != null) {
                dataReceiver.onDataRead(data);
            }
        }
    }

    private class AddValueTask extends AsyncTask<DatabaseValue, Void, Long> {
        private DatabaseValue value;

        @Override
        protected Long doInBackground(DatabaseValue... databaseValues) {
            value = databaseValues[0];
            return addValueTask(value);
        }

        @Override
        protected void onPostExecute(Long totalNumberOfRows) {
            super.onPostExecute(totalNumberOfRows);
            notifyValueAdded(value, totalNumberOfRows);
        }
    }

    private class ResetTableTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            return resetTableTask();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success != null && success) {
                Log.d("DatabaseModel", "The graph has been reset successfully!");
                notifyTableReset();
            } else {
                Log.d("DatabaseModel", "Graph reset failed!");
            }
        }
    }
}
