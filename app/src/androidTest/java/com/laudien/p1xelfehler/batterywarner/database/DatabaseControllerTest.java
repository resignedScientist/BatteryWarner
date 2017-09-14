package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Iterator;

import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.NUMBER_OF_GRAPHS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class DatabaseControllerTest {
    private Context context;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void getAllGraphsTest() throws Exception {
        DatabaseController databaseController = DatabaseController.getInstance(context);
        // 1. databaseValue == null -> should return null
        assertNull(databaseController.getAllGraphs((DatabaseValue[]) null));

        // 2. create 3 dummy databaseValues, the second one is null
        int batteryLevel = 60;
        int temperature = 325;
        int voltage = 3876;
        int current = -1500000;
        long time = System.currentTimeMillis();
        DatabaseValue[] databaseValues = new DatabaseValue[3];
        databaseValues[0] = new DatabaseValue(batteryLevel, temperature, voltage, current, time);
        databaseValues[2] = new DatabaseValue(batteryLevel, temperature, voltage, current, time + 1);
        LineGraphSeries[] lineGraphSeries = databaseController.getAllGraphs(databaseValues);

        // 3. check graphs
        assertEquals(NUMBER_OF_GRAPHS, lineGraphSeries.length);
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            LineGraphSeries s = lineGraphSeries[i];
            assertNotNull(s);
            Iterator<DataPoint> iterator = s.getValues(s.getLowestValueX(), s.getHighestValueX());
            iterator.next();
            assertFalse(iterator.hasNext()); // there has to be no second value!
        }
        assertEquals(batteryLevel, lineGraphSeries[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY(), 0d);
        assertEquals((double) temperature / 10, lineGraphSeries[GRAPH_INDEX_TEMPERATURE].getHighestValueY(), 0d);
        assertEquals((double) voltage / 1000, lineGraphSeries[GRAPH_INDEX_VOLTAGE].getHighestValueY(), 0d);
        assertEquals((double) current / -1000, lineGraphSeries[GRAPH_INDEX_CURRENT].getHighestValueY(), 0d);
        assertEquals(0d, lineGraphSeries[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueX(), 0d);

        // 4. create 1 dummy databaseValue, all values are 0
        databaseValues = new DatabaseValue[1];
        databaseValues[0] = new DatabaseValue(0, 0, 0, 0, System.currentTimeMillis());
        lineGraphSeries = databaseController.getAllGraphs(databaseValues);

        // 5. check graphs again -> this time every graph except batteryLevel and temperature needs to be null!
        assertEquals(NUMBER_OF_GRAPHS, lineGraphSeries.length);
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            if (i == GRAPH_INDEX_BATTERY_LEVEL || i == GRAPH_INDEX_TEMPERATURE) {
                assertNotNull(lineGraphSeries[i]);
            } else {
                assertNull(lineGraphSeries[i]);
            }
        }
    }
}
