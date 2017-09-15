package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Iterator;
import java.util.Random;

import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.NUMBER_OF_GRAPHS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DatabaseControllerTest {
    private Context context;
    private boolean methodCalled;
    private DataPoint[] receivedDataPoints = null;

    public static DatabaseValue getRandomDatabaseValue() {
        Random random = new Random();
        int batteryLevel = random.nextInt();
        int temperature = random.nextInt();
        int voltage = random.nextInt();
        int current = random.nextInt();
        long time = random.nextLong();
        return new DatabaseValue(batteryLevel, temperature, voltage, current, time);
    }

    @Before
    public void setup() {
        context = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void getAllGraphsTest() throws Exception {
        DatabaseController databaseController = DatabaseController.getInstance(context);
        // 1. databaseValue == null -> should return null
        assertNull(databaseController.getAllGraphs((DatabaseValue[]) null));

        // 2. create 2 dummy databaseValues, the second one is null
        int batteryLevel = 60;
        int temperature = 325;
        int voltage = 3876;
        int current = -1500000;
        long time = System.currentTimeMillis();
        DatabaseValue[] databaseValues = new DatabaseValue[2];
        databaseValues[0] = new DatabaseValue(batteryLevel, temperature, voltage, current, time);
        LineGraphSeries[] lineGraphSeries = databaseController.getAllGraphs(databaseValues);

        // 3. check graphs
        assertEquals(NUMBER_OF_GRAPHS, lineGraphSeries.length);
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            LineGraphSeries s = lineGraphSeries[i];
            assertNotNull(s); // no value is 0 -> so the graph must not be null
            int size = getSize(s);
            assertEquals(1, size); // only one valid value -> so only one graph value!
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

        // 6. Adding lots of the same values -> only 2 graph points are expected!
        databaseValues = new DatabaseValue[20];
        long lastTime = time;
        for (int i = 0; i < databaseValues.length; i++) {
            lastTime = time + 1000 * i;
            databaseValues[i] = new DatabaseValue(
                    batteryLevel, temperature, voltage, current, lastTime
            );
        }
        lineGraphSeries = databaseController.getAllGraphs(databaseValues);
        for (LineGraphSeries graph : lineGraphSeries) {
            int size = getSize(graph);
            assertEquals(2, size);
        }
        // also check for the correct value here
        double expectedTimeInMinutes = (double) (lastTime - time) / (1000 * 60);
        assertEquals(batteryLevel, lineGraphSeries[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY(), 0d);
        assertEquals((double) temperature / 10, lineGraphSeries[GRAPH_INDEX_TEMPERATURE].getHighestValueY(), 0d);
        assertEquals((double) voltage / 1000, lineGraphSeries[GRAPH_INDEX_VOLTAGE].getHighestValueY(), 0d);
        assertEquals((double) current / -1000, lineGraphSeries[GRAPH_INDEX_CURRENT].getHighestValueY(), 0d);
        assertEquals(expectedTimeInMinutes, lineGraphSeries[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueX(), 0d);
    }

    @Test
    public void notifyValueAddedTest() {
        DatabaseController databaseController = DatabaseController.getInstance(context);
        databaseController.resetTable();
        DatabaseValue databaseValue = getRandomDatabaseValue();
        receivedDataPoints = null;
        DatabaseController.DatabaseListener databaseListener = new DatabaseController.DatabaseListener() {
            @Override
            public void onValueAdded(DataPoint[] dataPoints, long totalNumberOfRows) {
                methodCalled = true;
                receivedDataPoints = dataPoints;
            }

            @Override
            public void onTableReset() {

            }
        };

        // register listener
        databaseController.registerDatabaseListener(databaseListener);

        // wrong inputs -> method should not be called!
        methodCalled = false;
        databaseController.notifyValueAdded(null, 10);
        assertFalse(methodCalled);
        methodCalled = false;
        databaseController.notifyValueAdded(getRandomDatabaseValue(), 0);
        assertFalse(methodCalled);
        methodCalled = false;
        databaseController.notifyValueAdded(getRandomDatabaseValue(), -1);
        assertFalse(methodCalled);

        // add value to empty table -> method should be called!
        methodCalled = false;
        databaseController.addValue(
                databaseValue.getBatteryLevel(),
                databaseValue.getTemperature(),
                databaseValue.getVoltage(),
                databaseValue.getCurrent(),
                databaseValue.getUtcTimeInMillis()
        );
        assertTrue(methodCalled);

        // check the DataPoints
        assertNotNull(receivedDataPoints);
        assertEquals(NUMBER_OF_GRAPHS, receivedDataPoints.length);
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            assertNotNull(receivedDataPoints[i]);
            double expectedValue = 0d;
            switch (i) {
                case GRAPH_INDEX_BATTERY_LEVEL:
                    expectedValue = databaseValue.getBatteryLevel();
                    break;
                case GRAPH_INDEX_TEMPERATURE:
                    expectedValue = (double) databaseValue.getTemperature() / 10;
                    break;
                case GRAPH_INDEX_VOLTAGE:
                    expectedValue = (double) databaseValue.getVoltage() / 1000;
                    break;
                case GRAPH_INDEX_CURRENT:
                    expectedValue = (double) databaseValue.getCurrent() / -1000;
                    break;
            }
            if (expectedValue == 0d && i != GRAPH_INDEX_BATTERY_LEVEL && i != GRAPH_INDEX_TEMPERATURE) {
                assertNull(receivedDataPoints[i]);
            } else {
                assertNotNull(receivedDataPoints[i]);
                assertEquals(0d, receivedDataPoints[i].getX(), 0d);
                assertEquals(expectedValue, receivedDataPoints[i].getY(), 0d);
            }
        }

        // add the same values again -> the method should be called again!
        methodCalled = false;
        databaseController.addValue(
                databaseValue.getBatteryLevel(),
                databaseValue.getTemperature(),
                databaseValue.getVoltage(),
                databaseValue.getCurrent(),
                databaseValue.getUtcTimeInMillis()
        );
        assertTrue(methodCalled);

        // add a DIFFERENT point -> the method should be called!
        methodCalled = false;
        DatabaseValue anotherValue = getRandomDatabaseValue();
        databaseController.addValue(
                anotherValue.getBatteryLevel(),
                anotherValue.getTemperature(),
                anotherValue.getVoltage(),
                anotherValue.getCurrent(),
                databaseValue.getUtcTimeInMillis() + 1
        );
        assertTrue(methodCalled);

        /* add all 0 values
        -> the method should be called
        -> each DataPoint (except batteryLevel and temperature) in the array should be null!
        */
        receivedDataPoints = null;
        methodCalled = false;
        databaseController.addValue(
                0,
                0,
                0,
                0,
                databaseValue.getUtcTimeInMillis() + 2
        );
        assertTrue(methodCalled);
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            if (i != GRAPH_INDEX_BATTERY_LEVEL && i != GRAPH_INDEX_TEMPERATURE) {
                assertNull("Graph should be null: " + i, receivedDataPoints[i]);
            } else {
                assertNotNull("Graph should NOT be null: " + i, receivedDataPoints[i]);
            }
        }

        // unregister the listener
        databaseController.unregisterListener(databaseListener);
    }

    private int getSize(LineGraphSeries graph) {
        Iterator<LineGraphSeries> iterator = graph.getValues(0d, Double.MAX_VALUE);
        int size = 0;
        while (iterator.hasNext()) {
            iterator.next();
            size++;
        }
        return size;
    }
}
