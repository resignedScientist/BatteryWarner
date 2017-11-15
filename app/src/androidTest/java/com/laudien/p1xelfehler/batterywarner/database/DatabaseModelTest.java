package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class DatabaseModelTest {
    private Context context;
    private DatabaseModel databaseModel;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getTargetContext();
        databaseModel = new DatabaseModel(context);
    }

    @Test
    public void getCursorTest1() {
        // database is null -> should return null
        Cursor cursor = databaseModel.getCursor((SQLiteDatabase) null);
        assertNull("If the database is null, the method should return null!", cursor);
    }

    @Test
    public void getCursorTest2() {
        // valid database -> check the returned Cursor
        SQLiteDatabase database = databaseModel.getReadableDatabase();
        Cursor cursor = databaseModel.getCursor(database);
        assertNotNull(cursor);
        assertFalse(cursor.isClosed());
        assertEquals(DatabaseContract.TABLE_COLUMN_TIME, cursor.getColumnName(0));
        assertEquals(DatabaseContract.TABLE_COLUMN_BATTERY_LEVEL, cursor.getColumnName(1));
        assertEquals(DatabaseContract.TABLE_COLUMN_TEMPERATURE, cursor.getColumnName(2));
        assertEquals(DatabaseContract.TABLE_COLUMN_VOLTAGE, cursor.getColumnName(3));
        assertEquals(DatabaseContract.TABLE_COLUMN_CURRENT, cursor.getColumnName(4));
    }

    @Test(expected = IllegalStateException.class)
    public void getCursorTest3() {
        // closed database
        SQLiteDatabase database = databaseModel.getReadableDatabase();
        database.close();
        databaseModel.getCursor(database);
    }

    @Test
    public void resetTableTest() {
        // write some values into the database
        DatabaseController databaseController = DatabaseController.getInstance(context);
        databaseController.addValue(80, 303, 4111, 1234456, System.currentTimeMillis(), false);

        // reset the table
        databaseModel.resetTable();

        // check if the table is empty
        Cursor cursor = databaseModel.getCursor();
        assertEquals(0, cursor.getCount());
    }
}
