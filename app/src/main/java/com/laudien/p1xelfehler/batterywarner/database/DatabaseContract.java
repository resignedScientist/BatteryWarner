package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.util.ArrayList;

public class DatabaseContract {
    static final String TABLE_NAME = "ChargeCurve";
    static final String TABLE_COLUMN_TIME = "time";
    static final String TABLE_COLUMN_BATTERY_LEVEL = "percentage";
    static final String TABLE_COLUMN_TEMPERATURE = "temperature";
    static final String TABLE_COLUMN_VOLTAGE = "voltage";
    static final String TABLE_COLUMN_CURRENT = "current";

    interface Model {
        DatabaseValue[] readData();

        DatabaseValue[] readData(File databaseFile);

        Cursor getCursor();

        Cursor getCursor(File databaseFile);

        Cursor getCursor(SQLiteDatabase database);

        SQLiteDatabase getReadableDatabase(File databaseFile);

        SQLiteDatabase getWritableDatabase(File databaseFile);

        long addValue(DatabaseValue value);

        void resetTable();

        void closeLocalFile();

        void close(File file);
    }

    public interface Controller {
        ArrayList<File> getFileList();

        LineGraphSeries[] getAllGraphs(File databaseFile, boolean useFahrenheit, boolean reverseCurrent);

        LineGraphSeries[] getAllGraphs(boolean useFahrenheit, boolean reverseCurrent);

        long getEndTime();

        long getEndTime(File databaseFile);

        long getStartTime();

        long getStartTime(File databaseFile);

        boolean saveGraph(Context context);

        void resetTable();

        void registerDatabaseListener(DatabaseListener listener);

        void unregisterListener(DatabaseContract.DatabaseListener listener);

        void addValue(int batteryLevel, int temperature, int voltage, int current, long utcTimeInMillis, boolean useFahrenheit, boolean reverseCurrent);

        void notifyTransitionsFinished();

        void notifyTransitionsFinished(File file);

        void upgradeAllSavedDatabases(Context context);
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
        void onValueAdded(DataPoint[] dataPoints, long totalNumberOfRows);

        /**
         * Called when the app directory database has been cleared.
         */
        void onTableReset();
    }
}
