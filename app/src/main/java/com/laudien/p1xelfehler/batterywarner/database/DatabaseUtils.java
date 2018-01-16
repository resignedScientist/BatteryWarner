package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseContract.DATABASE_HISTORY_PATH;
import static java.util.Calendar.SHORT;

public final class DatabaseUtils {
    // returned graphs
    public static final int GRAPH_INDEX_BATTERY_LEVEL = 0;
    public static final int GRAPH_INDEX_TEMPERATURE = 1;
    public static final int GRAPH_INDEX_VOLTAGE = 2;
    public static final int GRAPH_INDEX_CURRENT = 3;
    public static final int NUMBER_OF_GRAPHS = 4;

    public static void generateLineGraphSeries(@NonNull DatabaseValue[] databaseValues, boolean useFahrenheit, boolean reverseCurrent, @NonNull LineGraphSeriesReceiver receiver) {
        new GenerateGraphsTask(databaseValues, useFahrenheit, reverseCurrent, receiver).execute();
    }

    public static void saveGraph(@NonNull Context context, @Nullable GraphSavedListener graphSavedListener) {
        // permission check
        if (ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(context.getString(R.string.pref_graph_autosave), false)
                    .apply();
            if (graphSavedListener != null) {
                graphSavedListener.onFinishedSaving(false);
            }
            return;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean graphEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));
        if (!graphEnabled) {
            if (graphSavedListener != null) {
                graphSavedListener.onFinishedSaving(false);
            }
            return;
        }
        File databasePath = context.getDatabasePath(DatabaseModel.DATABASE_NAME);
        DatabaseModel databaseModel = DatabaseModel.getInstance(context);
        new SaveGraphTask(databasePath, databaseModel, graphSavedListener).execute();
    }

    @NonNull
    public static ArrayList<File> getBatteryFiles() {
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

    public static void upgradeAllSavedDatabases(@NonNull Context context) {
        Log.d("DatabaseUtils", "Upgrading all saved databases...");
        if (ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            ArrayList<File> files = getBatteryFiles();
            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    if (f1.lastModified() == f2.lastModified()) {
                        return 0;
                    }
                    return f1.lastModified() < f2.lastModified() ? -1 : 1;
                }
            });
            DatabaseModel model = DatabaseModel.getInstance(context);
            for (File file : files) {
                Log.d("DatabaseUtils", "Upgrading file: " + file.getPath());
                Log.d("DatabaseUtils", "last modified: " + file.lastModified());
                model.getReadableDatabase(file);
            }
            model.closeAllExternalFiles();
            Log.d("DatabaseUtils", "Upgrade finished!");
        } else {
            Log.d("DatabaseUtils", "Storage permission not granted!");
        }
    }

    /**
     * Checks if the given file is a valid SQLite database file.
     *
     * @param file The file to check.
     * @return True if it is a valid database file, false if not.
     */
    private static boolean isFileValid(File file) {
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

    public interface LineGraphSeriesReceiver {
        void generatingFinished(@Nullable LineGraphSeries<DataPoint>[] graphs);
    }

    public interface GraphSavedListener {
        void onFinishedSaving(boolean success);
    }

    private static class GenerateGraphsTask extends AsyncTask<Void, Void, LineGraphSeries<DataPoint>[]> {
        private DatabaseValue[] databaseValues;
        private boolean useFahrenheit;
        private boolean reverseCurrent;
        private LineGraphSeriesReceiver receiver;

        private GenerateGraphsTask(@NonNull DatabaseValue[] databaseValues, boolean useFahrenheit, boolean reverseCurrent, @NonNull LineGraphSeriesReceiver receiver) {
            this.databaseValues = databaseValues;
            this.useFahrenheit = useFahrenheit;
            this.reverseCurrent = reverseCurrent;
            this.receiver = receiver;
        }

        @Override
        protected LineGraphSeries<DataPoint>[] doInBackground(Void... voids) {
            if (databaseValues[0] == null) {
                return null;
            }
            LineGraphSeries<DataPoint>[] graphs = new LineGraphSeries[NUMBER_OF_GRAPHS];
            graphs[GRAPH_INDEX_BATTERY_LEVEL] = new LineGraphSeries();
            graphs[GRAPH_INDEX_TEMPERATURE] = new LineGraphSeries();
            if (databaseValues[0].getVoltage() != 0)
                graphs[GRAPH_INDEX_VOLTAGE] = new LineGraphSeries();
            if (databaseValues[0].getCurrent() != 0)
                graphs[GRAPH_INDEX_CURRENT] = new LineGraphSeries();
            for (DatabaseValue value : databaseValues) {
                DataPoint[] dataPoints = value.toDataPoints(useFahrenheit, reverseCurrent);
                for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
                    if (graphs[i] != null && dataPoints[i] != null) {
                        graphs[i].appendData(dataPoints[i], false, databaseValues.length);
                    }
                }
            }
            return graphs;
        }

        @Override
        protected void onPostExecute(LineGraphSeries<DataPoint>[] graphs) {
            super.onPostExecute(graphs);
            receiver.generatingFinished(graphs);
        }
    }

    private static class SaveGraphTask extends AsyncTask<Void, Void, Boolean> {
        private File databasePath;
        private DatabaseModel databaseModel;
        @Nullable
        private GraphSavedListener graphSavedListener;

        private SaveGraphTask(@NonNull File databasePath, @NonNull DatabaseModel databaseModel, @Nullable GraphSavedListener graphSavedListener) {
            this.databasePath = databasePath;
            this.databaseModel = databaseModel;
            this.graphSavedListener = graphSavedListener;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean result = false;
            // return if graph disabled in settings or the database has not enough data
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
                    File inputFile = databasePath;
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
                    // TODO: shortenGraph(outputFile);
                    result = true;
                }
                cursor.close();
            }
            return result;
        }

        @Override
        protected void onPostExecute(@Nullable Boolean bool) {
            super.onPostExecute(bool);
            boolean success = bool != null ? bool : false;
            if (success) {
                Log.d("DatabaseUtils", "Graph Saving successful!");
            } else {
                Log.d("DatabaseUtils", "Graph Saving failed!");
            }
            if (graphSavedListener != null) {
                graphSavedListener.onFinishedSaving(success);
            }
        }
    }
}
