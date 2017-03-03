package com.laudien.p1xelfehler.batterywarner;

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

import java.io.FileReader;

public class GraphDbHelper extends SQLiteOpenHelper {

    public static final int TYPE_PERCENTAGE = 0;
    public static final int TYPE_TEMPERATURE = 1;
    public static final String DATABASE_NAME = "ChargeCurveDB";
    private static final int DATABASE_VERSION = 4; // if the version is changed, a new database will be created!
    private static final String TABLE_NAME = "ChargeCurve";
    private static final String TABLE_COLUMN_TIME = "time";
    private static final String TABLE_COLUMN_PERCENTAGE = "percentage";
    private static final String TABLE_COLUMN_TEMP = "temperature";
    private static final String CREATE_QUERY =
            "CREATE TABLE " + TABLE_NAME
                    + " (" + TABLE_COLUMN_TIME + " TEXT,"
                    + TABLE_COLUMN_PERCENTAGE + " INTEGER,"
                    + TABLE_COLUMN_TEMP + " INTEGER);";
    private static GraphDbHelper instance;
    private static String[] columns = {
            GraphDbHelper.TABLE_COLUMN_TIME,
            GraphDbHelper.TABLE_COLUMN_PERCENTAGE,
            GraphDbHelper.TABLE_COLUMN_TEMP};
    private final String TAG = getClass().getSimpleName();
    private int color_percentage, color_percentageBackground, color_temperature;
    private boolean darkThemeEnabled = false;
    private DatabaseChangedListener dbChangedListener;
    private boolean dbChanged = true; // saves if the database changed since last query from dbChangedListener

    private GraphDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static GraphDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new GraphDbHelper(context);
        }
        return instance;
    }

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

    public void resetTable() {
        getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME);
        close();
        if (dbChangedListener != null) {
            dbChangedListener.onDatabaseCleared();
            dbChanged = false;
        } else {
            dbChanged = true;
        }
    }

    public void addValue(long time, int percentage, int temperature) {
        Log.d(TAG, "value added: " + time + " ms, " + percentage + "%, " + temperature + "°C");
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
            dbChangedListener.onValueAdded(timeInMinutes, percentage, temperature);
            dbChanged = false;
        } else {
            dbChanged = true;
        }
        close();
    }

    public void setDatabaseChangedListener(DatabaseChangedListener dbChangedListener) {
        this.dbChangedListener = dbChangedListener;
    }

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

    public LineGraphSeries<DataPoint>[] getGraphs(Context context) {
        LineGraphSeries<DataPoint>[] series = getGraphs(context, getReadableDatabase());
        dbChanged = dbChangedListener == null;
        return series;
    }

    public SQLiteDatabase getReadableDatabase(String fileName) {
        return SQLiteDatabase.openDatabase(
                fileName,
                null,
                SQLiteDatabase.OPEN_READONLY
        );
    }

    public boolean hasDbChanged() {
        return dbChanged;
    }

    public boolean isValidDatabase(String fileName) {
        try (FileReader fileReader = new FileReader(fileName)) {
            if (isTableEmpty(fileName)) {
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

    public interface DatabaseChangedListener {
        void onValueAdded(double timeInMinutes, int percentage, int temperature);

        void onDatabaseCleared();
    }
}
