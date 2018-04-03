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

    /**
     * Checks if the given file is a valid SQLite database file.
     *
     * @param file The file to check.
     * @return True if it is a valid database file, false if not.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
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

    private static boolean saveGraphTask(@NonNull File databasePath, @NonNull DatabaseModel databaseModel) {
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
                try {
                    File directory = new File(DATABASE_HISTORY_PATH);
                    if (!directory.exists()) {
                        if (!directory.mkdirs()) {
                            return false;
                        }
                    }
                    FileInputStream inputStream = new FileInputStream(databasePath);
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

    public interface GraphSavedListener {
        void onFinishedSaving(boolean success);
    }

    private static class SaveGraphTask extends AsyncTask<Void, Void, Boolean> {
        private final File databasePath;
        private final DatabaseModel databaseModel;
        @Nullable
        private final GraphSavedListener graphSavedListener;

        private SaveGraphTask(@NonNull File databasePath, @NonNull DatabaseModel databaseModel, @Nullable GraphSavedListener graphSavedListener) {
            this.databasePath = databasePath;
            this.databaseModel = databaseModel;
            this.graphSavedListener = graphSavedListener;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return saveGraphTask(databasePath, databaseModel);
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
