package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;
import static com.laudien.p1xelfehler.batterywarner.helper.GraphDbHelper.DATABASE_NAME;
import static java.text.DateFormat.SHORT;

public class DatabaseController {
    public static final int GRAPH_INDEX_BATTERY_LEVEL = 0;
    public static final int GRAPH_INDEX_TEMPERATURE = 1;
    private static final int NUMBER_OF_GRAPHS = 2;
    private static final String DATABASE_HISTORY_PATH = Environment.getExternalStorageDirectory() + "/BatteryWarner";
    private static DatabaseController instance;
    private DatabaseModel databaseModel;
    private HashSet<DatabaseListener> listeners = new HashSet<>();

    private DatabaseController(Context context) {
        databaseModel = new DatabaseModel(context);
    }

    public static DatabaseController getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseController(context);
        }
        return instance;
    }

    public void registerDatabaseListener(DatabaseListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(DatabaseListener listener) {
        listeners.remove(listener);
    }

    // ==== DEFAULT DATABASE IN THE APP DIRECTORY ====

    public void addValue(int batteryLevel, double temperature, long utcTimeInMillis) {
        DatabaseValue databaseValue = new DatabaseValue(batteryLevel, temperature, utcTimeInMillis);
        databaseModel.addValue(databaseValue);
        notifyValueAdded(databaseValue);
    }

    public LineGraphSeries<DataPoint> getBatteryLevelGraph() {
        return null;
    }

    public LineGraphSeries<DataPoint> getTemperatureGraph() {
        return null;
    }

    public LineGraphSeries[] getAllGraphs() {
        LineGraphSeries[] lineGraphSeries = new LineGraphSeries[NUMBER_OF_GRAPHS];
        lineGraphSeries[GRAPH_INDEX_BATTERY_LEVEL] = getBatteryLevelGraph();
        lineGraphSeries[GRAPH_INDEX_TEMPERATURE] = getTemperatureGraph();
        return lineGraphSeries;
    }

    public long getEndTime() {
        Cursor cursor = databaseModel.getCursor();
        return getEndTime(cursor);
    }

    public long getStartTime() {
        Cursor cursor = databaseModel.getCursor();
        return getStartTime(cursor);
    }

    public void resetTable() {
        databaseModel.resetTable();
        notifyTableReset();
    }

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
            if (cursor != null) { // check if there is enough data
                if (cursor.getCount() > 1) {
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
                    File inputFile = context.getDatabasePath(DATABASE_NAME);
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
                    Log.d("GraphSaver", "Graph saved!");
                    result = true;
                }
                cursor.close();
            }
        }
        return result;
    }

    // ==== ANY DATABASE FROM A FILE ====

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

    public LineGraphSeries<DataPoint> getBatteryLevelGraph(File databaseFile) {
        return null;
    }

    public LineGraphSeries<DataPoint> getTemperatureGraph(File databaseFile) {
        return null;
    }

    public LineGraphSeries[] getAllGraphs(File databaseFile) {
        LineGraphSeries[] lineGraphSeries = new LineGraphSeries[NUMBER_OF_GRAPHS];
        lineGraphSeries[GRAPH_INDEX_BATTERY_LEVEL] = getBatteryLevelGraph(databaseFile);
        lineGraphSeries[GRAPH_INDEX_TEMPERATURE] = getTemperatureGraph(databaseFile);
        return lineGraphSeries;
    }

    public long getEndTime(File databaseFile) {
        Cursor cursor = databaseModel.getCursor(databaseFile);
        return getEndTime(cursor);
    }

    public long getStartTime(File databaseFile) {
        Cursor cursor = databaseModel.getCursor(databaseFile);
        return getStartTime(cursor);
    }

    private boolean isFileValid(File file) {
        try {
            FileReader fileReader = new FileReader(file.getPath());
            char[] buffer = new char[16];
            fileReader.read(buffer, 0, 16); // read first 16 bytes
            fileReader.close();
            String string = String.valueOf(buffer);
            return string.equals("SQLite format 3\u0000");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==== GENERAL STUFF ====

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

    private void notifyValueAdded(DatabaseValue databaseValue) {
        for (DatabaseListener listener : listeners) {
            listener.onValueAdded(databaseValue);
        }
    }

    private void notifyTableReset() {
        for (DatabaseListener listener : listeners) {
            listener.onTableReset();
        }
    }

    public interface DatabaseListener {
        void onValueAdded(DatabaseValue databaseValue);

        void onTableReset();
    }
}
