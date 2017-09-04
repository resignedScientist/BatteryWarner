package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.util.Calendar;
import java.util.Collection;
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
public class DatabaseController {
    /**
     * Array index for the battery level graph.
     */
    public static final int GRAPH_INDEX_BATTERY_LEVEL = 0;
    /**
     * Array index for the temperature graph.
     */
    public static final int GRAPH_INDEX_TEMPERATURE = 1;
    /**
     * The maximum DataPoints that can be displayed in a GraphView.
     */
    public static final int MAX_DATA_POINTS = 1000;
    /**
     * The number of different graphs.
     */
    public static final int NUMBER_OF_GRAPHS = 2;
    /**
     * The path where the graphs will be saved.
     */
    private static final String DATABASE_HISTORY_PATH = Environment.getExternalStorageDirectory() + "/BatteryWarner";
    private static DatabaseController instance;
    private final String TAG = getClass().getSimpleName();
    private DatabaseModel databaseModel;
    private HashSet<DatabaseListener> databaseListeners = new HashSet<>();
    private HashSet<OnGraphFileDeletedListener> graphFileDeletedListeners = new HashSet<>();

    private DatabaseController(Context context) {
        databaseModel = new DatabaseModel(context);
    }

    /**
     * Get an instance of this singleton class.
     *
     * @param context An instance of the Context class.
     * @return An instance of DatabaseController.
     */
    public static DatabaseController getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseController(context);
        }
        return instance;
    }

    /**
     * Registers a DatabaseListener which will be called everytime something in the database changes.
     * Don't forget to unregister the listener with unregisterListener() if not needed anymore!
     *
     * @param listener A DatabaseListener which listens for database changes.
     */
    public void registerDatabaseListener(DatabaseListener listener) {
        databaseListeners.add(listener);
    }

    /**
     * Unregisters a DatabaseListener.
     *
     * @param listener A DatabaseListener which listens for database changes.
     */
    public void unregisterListener(DatabaseListener listener) {
        databaseListeners.remove(listener);
    }

    public void registerOnGraphFileDeletedListener(OnGraphFileDeletedListener listener) {
        graphFileDeletedListeners.add(listener);
    }

    public void unregisterOnGraphFileDeletedListener(OnGraphFileDeletedListener listener) {
        graphFileDeletedListeners.remove(listener);
    }

    // ==== DEFAULT DATABASE IN THE APP DIRECTORY ====

    /**
     * Add a value to the database in the app directory.
     *
     * @param batteryLevel    The current battery level of the device.
     * @param temperature     The current battery temperature.
     * @param utcTimeInMillis The current UTC time in milliseconds.
     */
    public void addValue(int batteryLevel, double temperature, long utcTimeInMillis) {
        DatabaseValue databaseValue = new DatabaseValue(batteryLevel, temperature, utcTimeInMillis);
        databaseModel.addValue(databaseValue);
        notifyValueAdded(databaseValue);
        Log.d(TAG, "Value added: " + databaseValue);
    }

    /**
     * Get all the graphs inside the app directory database.
     *
     * @return An array of LineGraphSeries.
     * You can use the GRAPH_INDEX_* constants to get the graph that you want out of the array.
     */
    public LineGraphSeries[] getAllGraphs() {
        return getAllGraphs(databaseModel.readData());
    }

    /**
     * Get the latest time that is in the app directory database.
     *
     * @return The latest time (UTC time in milliseconds) inside the database.
     */
    public long getEndTime() {
        DatabaseValue databaseValue = databaseModel.getLast();
        return databaseValue.getUtcTimeInMillis();
    }

    /**
     * Get the first time that is in the app directory database.
     *
     * @return The first time (UTC time in milliseconds) inside the database.
     */
    public long getStartTime() {
        DatabaseValue databaseValue = databaseModel.getFirst();
        return databaseValue.getUtcTimeInMillis();
    }

    /**
     * Clears the table of the app directory database to make room for new data.
     */
    public void resetTable() {
        databaseModel.resetTable();
        notifyTableReset();
        Log.d(TAG, "Table cleared!");
    }

    /**
     * Saves the graphs inside the app directory database to the default history directory.
     *
     * @param context An instance of the Context class.
     * @return Returns true if the saving was successful, false if not.
     */
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
            DatabaseValue[] databaseValues = databaseModel.getFirstAndLast();
            long startTime = 0;
            long endTime = 0;
            if (databaseValues[0] != null && databaseValues[1] != null) {
                startTime = databaseValues[0].getUtcTimeInMillis();
                endTime = databaseValues[1].getUtcTimeInMillis();
            }
            if (startTime != endTime) { // check if there is enough data
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
                Log.d("GraphSaver", "Graph saved!");
                result = true;
            }
        }
        Log.d(TAG, "Graph Saving successful: " + result);
        return result;
    }

    // ==== ANY DATABASE FROM A FILE ====

    /**
     * Get a list of database files inside the default history directory.
     * It automatically checks each file if it is a valid database and only adds these to the list.
     *
     * @return A list of all valid database files in the default history directory.
     */
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

    public Collection<File> getOldGraphFiles(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int days = sharedPreferences.getInt(context.getString(R.string.pref_graph_auto_delete_time), context.getResources().getInteger(R.integer.pref_graph_auto_delete_time_default));
        Log.d(TAG, "Collecting Graph files older than " + days + " days...");
        // calculate the time - everything older than this time will be deleted
        long daysInMillis = days * 24 * 60 * 60 * 1000;
        Calendar calendar = Calendar.getInstance();
        long targetTime = calendar.getTimeInMillis() - daysInMillis;
        calendar.setTimeInMillis(targetTime);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long targetTimeFlattened = calendar.getTimeInMillis();
        // delete every file older than the target time
        File directory = new File(DatabaseController.DATABASE_HISTORY_PATH);
        File[] files = directory.listFiles();
        HashSet<File> oldFiles = new HashSet<>();
        for (File file : files) {
            if (file.lastModified() != 0L && file.lastModified() < targetTimeFlattened) {
                oldFiles.add(file);
            }
        }
        if (oldFiles.isEmpty()) {
            Log.d(TAG, "No old files found!");
        }
        return oldFiles;
    }

    /**
     * Get an array of all graphs inside the given database file.
     *
     * @param databaseFile A valid SQLite database file.
     * @return Array of all graphs inside the given database file.
     * You can use the GRAPH_INDEX_* constants to get the graph that you want out of the array.
     */
    public LineGraphSeries[] getAllGraphs(File databaseFile) {
        return getAllGraphs(databaseModel.readData(databaseFile));
    }

    /**
     * Get the latest time that is in the given database file.
     *
     * @param databaseFile A valid SQLite database file.
     * @return The latest time (UTC time in milliseconds) inside the database.
     */
    public long getEndTime(File databaseFile) {
        DatabaseValue databaseValue = databaseModel.getLast(databaseFile);
        return databaseValue.getUtcTimeInMillis();
    }

    /**
     * Get the first time that is in the given database file.
     *
     * @param databaseFile A valid SQLite database file.
     * @return The first time (UTC time in milliseconds) inside the database.
     */
    public long getStartTime(File databaseFile) {
        DatabaseValue databaseValue = databaseModel.getFirst(databaseFile);
        return databaseValue.getUtcTimeInMillis();
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
            e.printStackTrace();
            return false;
        }
    }

    // ==== GENERAL STUFF ====

    private LineGraphSeries[] getAllGraphs(DatabaseValue[] databaseValues) {
        if (databaseValues != null) {
            LineGraphSeries[] graphs = new LineGraphSeries[NUMBER_OF_GRAPHS];
            graphs[GRAPH_INDEX_BATTERY_LEVEL] = new LineGraphSeries();
            graphs[GRAPH_INDEX_TEMPERATURE] = new LineGraphSeries();
            long startTime = databaseValues[0].getUtcTimeInMillis();
            for (DatabaseValue databaseValue : databaseValues) {
                long time = databaseValue.getUtcTimeInMillis() - startTime;
                double timeInMinutes = (double) time / (1000 * 60);
                // battery level graph
                double batteryLevel = (double) databaseValue.getBatteryLevel();
                graphs[GRAPH_INDEX_BATTERY_LEVEL].appendData(new DataPoint(timeInMinutes, batteryLevel), false, MAX_DATA_POINTS);
                // temperature graph
                double temperature = databaseValue.getTemperature() / 10;
                graphs[GRAPH_INDEX_TEMPERATURE].appendData(new DataPoint(timeInMinutes, temperature), false, MAX_DATA_POINTS);
            }
            return graphs;
        }
        return null;
    }

    private void notifyValueAdded(DatabaseValue databaseValue) {
        if (!databaseListeners.isEmpty()) {
            DataPoint[] dataPoints = new DataPoint[NUMBER_OF_GRAPHS];
            long time = databaseValue.getUtcTimeInMillis() - getStartTime();
            double timeInMinutes = (double) time / (1000 * 60);
            // battery level point
            double batteryLevel = (double) databaseValue.getBatteryLevel();
            dataPoints[GRAPH_INDEX_BATTERY_LEVEL] = new DataPoint(timeInMinutes, batteryLevel);
            // temperature point
            double temperature = databaseValue.getTemperature() / 10;
            dataPoints[GRAPH_INDEX_TEMPERATURE] = new DataPoint(timeInMinutes, temperature);
            for (DatabaseListener listener : databaseListeners) {
                listener.onValueAdded(dataPoints);
            }
        }
    }

    private void notifyTableReset() {
        for (DatabaseListener listener : databaseListeners) {
            listener.onTableReset();
        }
    }

    public void notifyGraphFileDeleted(File file) {
        for (OnGraphFileDeletedListener listener : graphFileDeletedListeners) {
            listener.onGraphFileDeleted(file);
        }
    }

    /**
     * A DatabaseListener that listens for database changes in the app directory database.
     */
    public interface DatabaseListener {
        /**
         * Called when a graph point was added to the app directory database.
         *
         * @param dataPoints An array of DataPoints. You can distinguish which point belongs to
         *                   which graph with the GRAPH_INDEX_* constants.
         */
        void onValueAdded(DataPoint[] dataPoints);

        /**
         * Called when the app directory database has been cleared.
         */
        void onTableReset();
    }

    public interface OnGraphFileDeletedListener {
        void onGraphFileDeleted(File file);
    }
}
