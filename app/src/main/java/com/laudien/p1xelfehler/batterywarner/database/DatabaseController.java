package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.TemperatureConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;
import static java.text.DateFormat.SHORT;

/**
 * The Controller for the charging graph databases. You can either use it with the database saved in
 * the app directory, or you can give the database file which should be used.
 */
public class DatabaseController implements DatabaseContract.Controller {
    /**
     * Array index for the battery level graph.
     */
    public static final int GRAPH_INDEX_BATTERY_LEVEL = 0;
    /**
     * Array index for the temperature graph.
     */
    public static final int GRAPH_INDEX_TEMPERATURE = 1;
    /**
     * Array index for the voltage graph.
     */
    public static final int GRAPH_INDEX_VOLTAGE = 2;
    /**
     * Array index for the current graph.
     */
    public static final int GRAPH_INDEX_CURRENT = 3;
    /**
     * The number of different graphs.
     */
    public static final int NUMBER_OF_GRAPHS = 4;
    private static final String DATABASE_HISTORY_PATH = Environment.getExternalStorageDirectory() + "/BatteryWarner";
    private static final int MAX_DATA_POINTS = 200;
    private static DatabaseController instance;
    private final String TAG = getClass().getSimpleName();
    private DatabaseContract.Model databaseModel;
    private HashSet<DatabaseContract.DatabaseListener> listeners = new HashSet<>();

    DatabaseController(DatabaseContract.Model databaseModel) {
        this.databaseModel = databaseModel;
        instance = this;
    }

    /**
     * Get an instance of this singleton class.
     *
     * @param context An instance of the Context class.
     * @return An instance of DatabaseController.
     */
    public static DatabaseController getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseController(new DatabaseModel(context));
        }
        return instance;
    }

    @Override
    public ArrayList<File> getFileList() {
        File path = new File(DATABASE_HISTORY_PATH);
        File[] files = path.listFiles();
        ArrayList<File> fileList = new ArrayList<>();
        if (files != null) { // there are files in the database folder
            // sort the files by date
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (SDK_INT >= KITKAT) {
                        return -Long.compare(o1.lastModified(), o2.lastModified());
                    } else { // before KitKat
                        if (o1.lastModified() == o2.lastModified()) {
                            return 0;
                        }
                        if (o1.lastModified() > o2.lastModified()) {
                            return -1;
                        } else { // o1.lastModified() < o2.lastModified()
                            return 1;
                        }
                    }
                }
            });
            // add valid database file to the list
            for (File file : files) {
                if (isFileValid(file)) {
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }

    @Override
    public LineGraphSeries[] getAllGraphs(File databaseFile, boolean useFahrenheit, boolean reverseCurrent) {
        return getAllGraphs(databaseModel.readData(databaseFile), useFahrenheit, reverseCurrent);
    }

    @Override
    public LineGraphSeries[] getAllGraphs(boolean useFahrenheit, boolean reverseCurrent) {
        return getAllGraphs(databaseModel.readData(), useFahrenheit, reverseCurrent);
    }

    @Override
    public long getEndTime() {
        return getEndTime(databaseModel.getCursor());
    }

    @Override
    public long getEndTime(File databaseFile) {
        return getEndTime(databaseModel.getCursor(databaseFile));
    }

    @Override
    public long getStartTime() {
        return getStartTime(databaseModel.getCursor());
    }

    @Override
    public long getStartTime(File databaseFile) {
        return getStartTime(databaseModel.getCursor(databaseFile));
    }

    @Override
    public boolean saveGraph(Context context) {
        // permission check
        if (ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(context.getString(R.string.pref_graph_autosave), false)
                    .apply();
            return false;
        }
        boolean result = false;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean graphEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));
        // return if graph disabled in settings or the database has not enough data
        if (graphEnabled) {
            Cursor cursor = databaseModel.getCursor();
            if (cursor != null && !cursor.isClosed()) {
                if (cursor.getCount() > 1) { // check if there is enough data
                    cursor.moveToLast();
                    long endTime = cursor.getLong(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TIME));
                    cursor.close();
                    String outputFileDir = String.format(
                            Locale.getDefault(),
                            "%s/%s",
                            DATABASE_HISTORY_PATH,
                            DateFormat.getDateInstance(SHORT)
                                    .format(endTime)
                                    .replace("/", "_")
                    );
                    // rename the file if it already exists
                    File outputFile = new File(outputFileDir);
                    String baseFileDir = outputFileDir;
                    for (byte i = 1; outputFile.exists() && i < 127; i++) {
                        outputFileDir = baseFileDir + " (" + i + ")";
                        outputFile = new File(outputFileDir);
                    }
                    File inputFile = context.getDatabasePath(DatabaseModel.DATABASE_NAME);
                    try {
                        File directory = new File(DATABASE_HISTORY_PATH);
                        if (!directory.exists()) {
                            if (!directory.mkdirs()) {
                                return false;
                            }
                        }
                        FileInputStream inputStream = new FileInputStream(inputFile);
                        FileOutputStream outputStream = new FileOutputStream(outputFile, false);
                        byte[] buffer = new byte[1024];
                        while (inputStream.read(buffer) != -1) {
                            outputStream.write(buffer);
                        }
                        outputStream.flush();
                        outputStream.close();
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                    shortenGraph(outputFile);
                    Log.d("GraphSaver", "Graph saved!");
                    result = true;
                }
                cursor.close();
            }
        }
        Log.d(TAG, "Graph Saving successful: " + result);
        return result;
    }

    @Override
    public void resetTable() {
        databaseModel.resetTable();
        notifyTableReset();
        Log.d(TAG, "Table cleared!");
    }

    @Override
    public void registerDatabaseListener(DatabaseContract.DatabaseListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(DatabaseContract.DatabaseListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void addValue(int batteryLevel, int temperature, int voltage, int current, long utcTimeInMillis, boolean useFahrenheit, boolean reverseCurrent) {
        DatabaseValue newValue = new DatabaseValue(batteryLevel, temperature, voltage, current, utcTimeInMillis);
        long totalNumberOfRows = databaseModel.addValue(newValue);
        notifyValueAdded(newValue, totalNumberOfRows, useFahrenheit, reverseCurrent);
        Log.d(TAG, "Value added: " + newValue);
    }

    @Override
    public void notifyTransitionsFinished() {
        databaseModel.closeLocalFile();
    }

    @Override
    public void notifyTransitionsFinished(File file) {
        databaseModel.close(file);
    }

    @Override
    public void upgradeAllSavedDatabases(Context context) {
        Log.d(TAG, "Upgrading all saved databases...");
        if (ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            DatabaseContract.Controller databaseController = DatabaseController.getInstance(context);
            ArrayList<File> files = databaseController.getFileList();
            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    if (f1.lastModified() == f2.lastModified()) {
                        return 0;
                    }
                    return f1.lastModified() < f2.lastModified() ? -1 : 1;
                }
            });
            for (File file : files) {
                Log.d(TAG, "Upgrading file: " + file.getPath());
                Log.d(TAG, "last modified: " + file.lastModified());
                SQLiteDatabase database = databaseModel.getReadableDatabase(file);
                databaseModel.close(file);
            }
            Log.d(TAG, "Upgrade finished!");
        } else {
            Log.d(TAG, "Storage permission not granted!");
        }
    }

    private void notifyValueAdded(DatabaseValue databaseValue, long totalNumberOfRows, boolean useFahrenheit, boolean reverseCurrent) {
        if (databaseValue == null || listeners.isEmpty()) {
            return;
        }
        DataPoint[] dataPoints = new DataPoint[NUMBER_OF_GRAPHS];
        long startTime = getStartTime();
        long time = startTime != 0L ? databaseValue.getUtcTimeInMillis() - startTime : 0L;
        double timeInMinutes = (double) time / (1000 * 60);
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            double value = databaseValue.get(i);
            if (value != 0d || i == GRAPH_INDEX_BATTERY_LEVEL || i == GRAPH_INDEX_TEMPERATURE) {
                switch (i) {
                    case GRAPH_INDEX_TEMPERATURE:
                        value /= 10;
                        if (useFahrenheit) {
                            value = TemperatureConverter.convertCelsiusToFahrenheit(value);
                        }
                        break;
                    case GRAPH_INDEX_VOLTAGE:
                        value /= 1000;
                        break;
                    case GRAPH_INDEX_CURRENT:
                        value /= (reverseCurrent ? 1000 : -1000);
                        break;
                }
                dataPoints[i] = new DataPoint(timeInMinutes, value);
            }
        }
        // notify the listeners
        for (DatabaseContract.DatabaseListener listener : listeners) {
            listener.onValueAdded(dataPoints, totalNumberOfRows);
        }
    }

    private LineGraphSeries[] getAllGraphs(DatabaseValue[] databaseValues, boolean useFahrenheit, boolean reverseCurrent) {
        if (databaseValues == null || databaseValues[0] == null) {
            return null;
        }
        LineGraphSeries[] graphs = new LineGraphSeries[NUMBER_OF_GRAPHS];
        graphs[GRAPH_INDEX_BATTERY_LEVEL] = new LineGraphSeries();
        graphs[GRAPH_INDEX_TEMPERATURE] = new LineGraphSeries();
        if (databaseValues[0].getVoltage() != 0)
            graphs[GRAPH_INDEX_VOLTAGE] = new LineGraphSeries();
        if (databaseValues[0].getCurrent() != 0)
            graphs[GRAPH_INDEX_CURRENT] = new LineGraphSeries();
        long startTime = databaseValues[0].getUtcTimeInMillis();
        int maxDataPoints = databaseValues.length;
        int lastDatabaseValue = -1;
        int numberOfValidValues = 0;
        for (int i = 0; i < databaseValues.length; i++) {
            if (databaseValues[i] != null) {
                long time = databaseValues[i].getUtcTimeInMillis() - startTime;
                double timeInMinutes = (double) time / (1000 * 60);
                for (int j = 0; j < NUMBER_OF_GRAPHS; j++) {
                    if (lastDatabaseValue == -1 || databaseValues[i].get(j) != databaseValues[lastDatabaseValue].get(j)) {
                        appendValue(graphs[j], databaseValues[i], j, timeInMinutes, maxDataPoints, useFahrenheit, reverseCurrent);
                    }
                }
                lastDatabaseValue = i;
                numberOfValidValues++;
            }
        }
        if (lastDatabaseValue != -1 && numberOfValidValues > 1) {
            long time = databaseValues[lastDatabaseValue].getUtcTimeInMillis() - startTime;
            double timeInMinutes = (double) time / (1000 * 60);
            for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
                appendValue(graphs[i], databaseValues[lastDatabaseValue], i, timeInMinutes, maxDataPoints, useFahrenheit, reverseCurrent);
            }
        }
        return graphs;
    }

    /**
     * Checks if the given file is a valid SQLite database file.
     *
     * @param file The file to check.
     * @return True if it is a valid database file, false if not.
     */
    private boolean isFileValid(File file) {
        try {
            FileReader fileReader = new FileReader(file.getPath());
            char[] buffer = new char[16];
            fileReader.read(buffer, 0, 16); // read first 16 bytes
            fileReader.close();
            String string = String.valueOf(buffer);
            return string.equals("SQLite format 3\u0000");
        } catch (Exception e) {
            return false;
        }
    }

    private void appendValue(@Nullable LineGraphSeries graph, DatabaseValue databaseValue, int index, double timeInMinutes, int maxDataPoints, boolean useFahrenheit, boolean reverseCurrent) {
        if (graph != null) {
            double value = databaseValue.get(index);
            switch (index) {
                case GRAPH_INDEX_TEMPERATURE:
                    value /= 10;
                    if (useFahrenheit) {
                        value = TemperatureConverter.convertCelsiusToFahrenheit(value);
                    }
                    break;
                case GRAPH_INDEX_VOLTAGE:
                    value /= 1000;
                    break;
                case GRAPH_INDEX_CURRENT:
                    value /= (reverseCurrent ? 1000 : -1000);
                    break;
            }
            graph.appendData(new DataPoint(timeInMinutes, value), false, maxDataPoints);
        }
    }

    private long getEndTime(Cursor cursor) {
        long endTime = 0;
        if (cursor != null) {
            if (cursor.moveToLast()) {
                endTime = cursor.getLong(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TIME));
            }
            cursor.close();
        }
        return endTime;
    }

    private long getStartTime(Cursor cursor) {
        long startTime = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                startTime = cursor.getLong(cursor.getColumnIndex(DatabaseContract.TABLE_COLUMN_TIME));
            }
            cursor.close();
        }
        return startTime;
    }

    private void notifyTableReset() {
        for (DatabaseContract.DatabaseListener listener : listeners) {
            listener.onTableReset();
        }
    }

    private void shortenGraph(File file) {
        SQLiteDatabase database = databaseModel.getWritableDatabase(file);
        if (database == null) {
            return;
        }
        Cursor cursor = databaseModel.getCursor(database);
        if (cursor == null || cursor.isClosed() || cursor.getCount() <= MAX_DATA_POINTS * 2) {
            return;
        }
        int count = cursor.getCount();
        int divisor = count / MAX_DATA_POINTS;
        database.beginTransaction();
        for (int i = 1; i < count - 1; i++) {
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
        databaseModel.close(file);
    }
}
