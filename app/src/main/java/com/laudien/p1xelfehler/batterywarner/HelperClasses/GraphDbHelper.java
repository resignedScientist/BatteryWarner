package com.laudien.p1xelfehler.batterywarner.HelperClasses;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;
import android.util.TypedValue;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.R;

import java.io.FileReader;

/**
 * This class helps reading to or writing from/to a graph database. You can ether use the database
 * in the app folder or any other database with the correct format.
 */
public class GraphDbHelper extends SQLiteOpenHelper {

    /**
     * The array index of the battery level graph. You get the array with the getGraphs() method.
     */
    public static final int TYPE_PERCENTAGE = 0;
    /** The array index of the battery temperature graph. You get the array with the getGraphs() method. */
    public static final int TYPE_TEMPERATURE = 1;
    /** The name of the database. */
    public static final String DATABASE_NAME = "ChargeCurveDB";
    private static final int DATABASE_VERSION = 4; // if the version is changed, a new database will be created!
    private static final String TABLE_NAME = "ChargeCurve";
    private static final String TABLE_COLUMN_TIME = "time";
    private static final String TABLE_COLUMN_PERCENTAGE = "percentage";
    private static final String TABLE_COLUMN_TEMP = "temperature";
    private static final String CREATE_QUERY = String.format("CREATE TABLE %s (%s TEXT,%s INTEGER,%s INTEGER);",
            TABLE_NAME, TABLE_COLUMN_TIME, TABLE_COLUMN_PERCENTAGE, TABLE_COLUMN_TEMP);
    private static GraphDbHelper instance;
    private static String[] columns = {
            TABLE_COLUMN_TIME,
            TABLE_COLUMN_PERCENTAGE,
            TABLE_COLUMN_TEMP};
    private final String TAG = getClass().getSimpleName();
    private int color_percentage, color_percentageBackground, color_temperature;
    private boolean darkThemeEnabled = false;
    private DatabaseChangedListener dbChangedListener;
    private boolean dbChanged = true; // saves if the database changed since last query from dbChangedListener

    private GraphDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Returns the current instance of the class or creates a new one. This also opens or
     * creates the database in the app directory.
     *
     * @param context An instance of the context class.
     * @return The only instance of this singleton class.
     */
    public static GraphDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new GraphDbHelper(context);
        }
        return instance;
    }

    /**
     * Returns the latest time in the given database.
     *
     * @param db A readable database.
     * @return The highest (latest) time in the database. If the database is empty, it will return 0.
     */
    public static long getEndTime(SQLiteDatabase db) {
        Cursor cursor = getCursor(db);
        long result = 0;
        if (cursor.moveToLast()) {
            result = cursor.getLong(0);
        }
        db.close();
        return result;
    }

    private static Cursor getCursor(SQLiteDatabase db) {
        return db.query(GraphDbHelper.TABLE_NAME, columns, null, null, null, null,
                "length(" + GraphDbHelper.TABLE_COLUMN_TIME + "), " + GraphDbHelper.TABLE_COLUMN_TIME);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    private boolean isTableEmpty(String fileName) {
        SQLiteDatabase db = getReadableDatabase(fileName);
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        cursor.moveToFirst();
        if (cursor.getInt(0) == 0) {
            db.close();
            return true;
        }
        cursor.close();
        db.close();
        return false;
    }

    private LineGraphSeries<DataPoint>[] setGraphColors(Context context, LineGraphSeries<DataPoint>[] output) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean readDarkThemeEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_dark_theme_enabled), context.getResources().getBoolean(R.bool.pref_dark_theme_enabled_default));
        if (color_percentage == 0 || color_percentageBackground == 0 || color_temperature == 0 ||
                darkThemeEnabled != readDarkThemeEnabled) {
            darkThemeEnabled = readDarkThemeEnabled;
            // percentage
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = context.getTheme();
            theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
            color_percentage = typedValue.data;
            color_percentageBackground = ColorUtils.setAlphaComponent(color_percentage, 64);
            // temperature
            if (darkThemeEnabled) { // dark theme
                color_temperature = Color.GREEN;
            } else { // default theme
                theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
                color_temperature = typedValue.data;
            }
        }
        output[TYPE_PERCENTAGE].setDrawBackground(true);
        output[TYPE_PERCENTAGE].setColor(color_percentage);
        output[TYPE_PERCENTAGE].setBackgroundColor(color_percentageBackground);
        output[TYPE_TEMPERATURE].setColor(color_temperature);
        return output;
    }

    /**
     * Removes all the data from the database table and notifies the DatabaseChangedListener (if not null)
     * by calling onDatabaseCleared().
     */
    public void resetTable() {
        getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME);
        close();
        if (dbChangedListener != null) {
            dbChangedListener.onDatabaseCleared();
            dbChanged = false;
        } else {
            dbChanged = true;
        }
        Log.d(TAG, "The database has been cleared!");
    }

    /**
     * Adds a value to the database and notifies the DatabaseChangedListener (if not null)
     * by calling onValueAdded().
     * @param time The real time in Milliseconds (usually the current time).
     * @param percentage The battery level.
     * @param temperature The battery temperature.
     */
    public void addValue(long time, int percentage, int temperature) {
        Log.d(TAG, "value added: " + time + " ms, " + percentage + "%, " + temperature + "/10°C");
        ContentValues contentValues = new ContentValues();
        contentValues.put(TABLE_COLUMN_TIME, time);
        contentValues.put(TABLE_COLUMN_PERCENTAGE, percentage);
        contentValues.put(TABLE_COLUMN_TEMP, temperature);
        SQLiteDatabase database = getWritableDatabase();
        try {
            database.insert(TABLE_NAME, null, contentValues);
        } catch (Exception e) {
            database.execSQL(CREATE_QUERY);
            database.insert(TABLE_NAME, null, contentValues);
        }
        if (dbChangedListener != null) {
            Cursor cursor = getCursor(database);
            cursor.moveToFirst();
            long firstTime = cursor.getLong(0);
            double timeInMinutes = (double) (time - firstTime) / 60000;
            double tempDouble = (double) temperature / 10;
            dbChangedListener.onValueAdded(timeInMinutes, percentage, tempDouble);
            dbChanged = false;
        } else {
            dbChanged = true;
        }
        close();
    }

    /**
     * Set the DbChangedListener for listening for database changes.
     * @param dbChangedListener An instance of DbChangedListener.
     */
    public void setDatabaseChangedListener(DatabaseChangedListener dbChangedListener) {
        this.dbChangedListener = dbChangedListener;
    }

    /**
     * Generates the graphs with the data in the given database. The table must be in the correct format.
     * Use the variables TYPE_PERCENTAGE and TYPE_TEMPERATURE to get the correct graph out of the array.
     * For example: series[TYPE_PERCENTAGE].
     * @param context An instance of the context class.
     * @param database A readable SQLite database.
     * @return An array of graphs generated by the data in the given database.
     */
    public LineGraphSeries<DataPoint>[] getGraphs(Context context, SQLiteDatabase database) {
        LineGraphSeries<DataPoint>[] output = new LineGraphSeries[2];
        output[TYPE_PERCENTAGE] = new LineGraphSeries<>();
        output[TYPE_TEMPERATURE] = new LineGraphSeries<>();
        Cursor cursor = getCursor(database);

        if (cursor.moveToFirst()) { // if the cursor has data
            double time, temperature;
            int percentage;
            long firstTime = cursor.getLong(0);
            do {
                time = (double) (cursor.getLong(0) - firstTime) / 60000;
                percentage = cursor.getInt(1);
                temperature = (double) cursor.getInt(2) / 10;
                output[TYPE_PERCENTAGE].appendData(new DataPoint(time, percentage), true, 1000);
                output[TYPE_TEMPERATURE].appendData(new DataPoint(time, temperature), true, 1000);
            } while (cursor.moveToNext()); // while the cursor has data
            cursor.close();
            database.close();

            // styling
            output = setGraphColors(context, output);

            return output;
        } else {
            cursor.close();
            database.close();
            return null;
        }
    }

    /**
     * Generates the graphs with the data in the default database in the app directory.
     * The table must be in the correct format. Use the variables TYPE_PERCENTAGE and TYPE_TEMPERATURE
     * to get the correct graph out of the array. For example: series[TYPE_PERCENTAGE].
     * @param context An instance of the context class.
     * @return An array of graphs generated by the data in the default database.
     */
    public LineGraphSeries<DataPoint>[] getGraphs(Context context) {
        LineGraphSeries<DataPoint>[] series = getGraphs(context, getReadableDatabase());
        dbChanged = dbChangedListener == null;
        return series;
    }

    /**
     * Generates a SQLiteDatabase object from the file with the given file path.
     * The database file must be in the correct format.
     *
     * @param filePath The file path of the database file.
     * @return A SQLiteDatabase object from the given database file.
     */
    public SQLiteDatabase getReadableDatabase(String filePath) {
        return SQLiteDatabase.openDatabase(
                filePath,
                null,
                SQLiteDatabase.OPEN_READONLY
        );
    }

    /**
     * A method that tells you if the database has changed in the time no DatabaseChangedListener
     * was registered.
     * @return Returns true if the database was changed, false if not.
     */
    public boolean hasDbChanged() {
        return dbChanged;
    }

    /**
     * Checks the given file for the SQLite format.
     *
     * @param filePath The file path of the file to be checked.
     * @return Returns true if the database is in SQLite format, false if not.
     */
    public boolean isValidDatabase(String filePath) {
        try (FileReader fileReader = new FileReader(filePath)) {
            if (isTableEmpty(filePath)) {
                return false;
            }
            char[] buffer = new char[16];
            fileReader.read(buffer, 0, 16); // read first 16 bytes
            String string = String.valueOf(buffer);
            return string.equals("SQLite format 3\u0000");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method checks if there is enough data in the database to generate a graph (= at least 2 points).
     * @return Returns true if there is enough data, false if not.
     */
    public boolean hasEnoughData() {
        Cursor cursor = getCursor(getReadableDatabase());
        if (cursor.moveToFirst() && cursor.moveToNext()) {
            close();
            return true;
        } else {
            close();
            return false;
        }
    }

    /**
     * The listener that listens to database changes.
     */
    public interface DatabaseChangedListener {
        /**
         * Called whenever a value was added to the database.
         * @param timeInMinutes The time difference between the time of the first point and this point in minutes.
         * @param percentage The battery level that was added.
         * @param temperature The battery temperature that was added.
         */
        void onValueAdded(double timeInMinutes, int percentage, double temperature);

        /**
         * Called when the database was cleared.
         */
        void onDatabaseCleared();
    }
}
