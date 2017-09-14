package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class DatabaseModelTest {
    private Context context;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void readLastValueTest() {
        DatabaseModel databaseModel = new DatabaseModel(context);
        Cursor cursor;
        DatabaseValue originalDatabaseValue;
        DatabaseValue readDatabaseValue;

        // Cursor == null -> result should be null
        cursor = null;
        readDatabaseValue = databaseModel.readLastValue(cursor);
        assertNull(readDatabaseValue);

        // no values in the table -> result should be null
        databaseModel.resetTable();
        cursor = databaseModel.getCursor();
        readDatabaseValue = databaseModel.readLastValue(cursor);
        assertNull(readDatabaseValue);

        // add a value
        originalDatabaseValue = DatabaseControllerTest.getRandomDatabaseValue();
        databaseModel.addValue(originalDatabaseValue);

        // closed Cursor -> result should be null
        cursor = databaseModel.getCursor();
        cursor.close();
        readDatabaseValue = databaseModel.readLastValue(cursor);
        assertNull(readDatabaseValue);

        // closed database -> result should be null
        cursor = databaseModel.getCursor();
        databaseModel.close();
        readDatabaseValue = databaseModel.readLastValue(cursor);
        assertNull(readDatabaseValue);

        // Cursor != null and not closed -> result should not be null
        cursor = databaseModel.getCursor();
        readDatabaseValue = databaseModel.readLastValue(cursor);
        assertNotNull(readDatabaseValue);

        // check for correct Cursor data
        cursor = databaseModel.getCursor();
        readDatabaseValue = databaseModel.readLastValue(cursor);
        assertEquals(originalDatabaseValue.getBatteryLevel(), readDatabaseValue.getBatteryLevel());
        assertEquals(originalDatabaseValue.getTemperature(), readDatabaseValue.getTemperature());
        assertEquals(originalDatabaseValue.getVoltage(), readDatabaseValue.getVoltage());
        assertEquals(originalDatabaseValue.getCurrent(), readDatabaseValue.getCurrent());
    }
}
