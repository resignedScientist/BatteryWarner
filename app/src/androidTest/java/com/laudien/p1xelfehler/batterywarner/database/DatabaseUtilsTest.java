package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.NUMBER_OF_GRAPHS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DatabaseUtilsTest {
    Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void generateLineGraphSeriesTest() {
        // empty array should return null
        DatabaseValue[] values = new DatabaseValue[]{};
        LineGraphSeries<DataPoint>[] output = DatabaseUtils.generateLineGraphSeriesTask(values, false, false);
        assertNull(output);
    }

    @Test(expected = Exception.class)
    public void generateLineGraphSeriesTest2() {
        // one of the values is null -> crash
        long timeNow = System.currentTimeMillis();
        DatabaseValue value = new DatabaseValue(20, 234, 5000, -1200000, timeNow, timeNow);
        for (int i = 1; i < 10; i++) {
            DatabaseValue[] values = new DatabaseValue[10];
            for (int j = 0; j < values.length; j++) {
                if (j == i) {
                    values[j] = null;
                    continue;
                }
                values[j] = value;
            }
            DatabaseUtils.generateLineGraphSeriesTask(values, false, false);
        }
    }

    @Test(expected = Exception.class)
    public void generateLineGraphSeriesTest3() {
        // null array -> crash
        DatabaseValue[] values = new DatabaseValue[10];
        for (int i = 0; i < values.length; i++) {
            values[i] = null;
        }
        DatabaseUtils.generateLineGraphSeriesTask(values, false, false);
    }

    @Test
    public void generateLineGraphSeriesTest4() {
        // test with valid, different values
        long timeNow = System.currentTimeMillis();
        DatabaseValue[] values = new DatabaseValue[]{
                new DatabaseValue(20, 334, 5000, -1280000, timeNow, timeNow),
                new DatabaseValue(50, 300, 5100, -1210000, timeNow + 1000000, timeNow),
                new DatabaseValue(30, 250, 5200, -1000000, timeNow + 2000000, timeNow)
        };
        LineGraphSeries<DataPoint>[] output = DatabaseUtils.generateLineGraphSeriesTask(values, false, false);

        assertNotNull(output);
        assertEquals(NUMBER_OF_GRAPHS, output.length);

        // check y axis
        assertEquals(values[1].getBatteryLevel(), output[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY(), 0d);
        assertEquals(values[0].getBatteryLevel(), output[GRAPH_INDEX_BATTERY_LEVEL].getLowestValueY(), 0d);
        assertEquals(values[0].getTemperatureInCelsius(), output[GRAPH_INDEX_TEMPERATURE].getHighestValueY(), 0d);
        assertEquals(values[2].getTemperatureInCelsius(), output[GRAPH_INDEX_TEMPERATURE].getLowestValueY(), 0d);
        assertEquals(values[2].getVoltageInVolts(), output[GRAPH_INDEX_VOLTAGE].getHighestValueY(), 0d);
        assertEquals(values[0].getVoltageInVolts(), output[GRAPH_INDEX_VOLTAGE].getLowestValueY(), 0d);
        assertEquals(values[0].getCurrentInMilliAmperes(false), output[GRAPH_INDEX_CURRENT].getHighestValueY(), 0d);
        assertEquals(values[2].getCurrentInMilliAmperes(false), output[GRAPH_INDEX_CURRENT].getLowestValueY(), 0d);

        // check x axis
        assertEquals(0d, output[0].getLowestValueX(), 0d);
        assertEquals(values[2].getTimeFromStartInMinutes(), output[0].getHighestValueX(), 0d);
    }

    @Test
    public void generateLineGraphSeriesTest5() {
        // Adding lots of the same values
        DatabaseValue[] databaseValues = new DatabaseValue[20];
        long timeNow = System.currentTimeMillis();
        DatabaseValue value = new DatabaseValue(20, 334, 5000, -1280000, timeNow, timeNow);
        long lastTime;
        for (int i = 0; i < databaseValues.length; i++) {
            lastTime = timeNow + 1000 * i;
            databaseValues[i] = new DatabaseValue(
                    value.getBatteryLevel(),
                    value.getTemperature(),
                    value.getVoltage(),
                    value.getCurrent(),
                    lastTime,
                    value.getUtcTimeInMillis()
            );
        }

        // check the size -> size = 2 is expected!
        LineGraphSeries[] output = DatabaseUtils.generateLineGraphSeriesTask(databaseValues, false, false);
        for (LineGraphSeries graph : output) {
            int size = getSize(graph);
            assertEquals(2, size);
        }

        // check y axis
        assertEquals(value.getBatteryLevel(), output[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY(), 0d);
        assertEquals(value.getBatteryLevel(), output[GRAPH_INDEX_BATTERY_LEVEL].getLowestValueY(), 0d);
        assertEquals(value.getTemperatureInCelsius(), output[GRAPH_INDEX_TEMPERATURE].getHighestValueY(), 0d);
        assertEquals(value.getTemperatureInCelsius(), output[GRAPH_INDEX_TEMPERATURE].getLowestValueY(), 0d);
        assertEquals(value.getVoltageInVolts(), output[GRAPH_INDEX_VOLTAGE].getHighestValueY(), 0d);
        assertEquals(value.getVoltageInVolts(), output[GRAPH_INDEX_VOLTAGE].getLowestValueY(), 0d);
        assertEquals(value.getCurrentInMilliAmperes(false), output[GRAPH_INDEX_CURRENT].getHighestValueY(), 0d);
        assertEquals(value.getCurrentInMilliAmperes(false), output[GRAPH_INDEX_CURRENT].getLowestValueY(), 0d);

        // check x axis
        assertEquals(0d, output[0].getLowestValueX(), 0d);
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            assertEquals(databaseValues[databaseValues.length - 1].getTimeFromStartInMinutes(), output[i].getHighestValueX(), 0d);
        }
    }

    @Test
    public void generateLineGraphSeriesTest6() {
        // create 1 dummy databaseValue, all values are 0
        DatabaseValue[] databaseValues = new DatabaseValue[1];
        long timeNow = System.currentTimeMillis();
        databaseValues[0] = new DatabaseValue(0, 0, 0, 0, timeNow, timeNow);
        LineGraphSeries[] output = DatabaseUtils.generateLineGraphSeriesTask(databaseValues, false, false);

        // check graphs -> every graph except batteryLevel and temperature needs to be null!
        assertNotNull(output);
        assertEquals(NUMBER_OF_GRAPHS, output.length);
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            if (i == GRAPH_INDEX_BATTERY_LEVEL || i == GRAPH_INDEX_TEMPERATURE) {
                assertNotNull(output[i]);
            } else {
                assertNull(output[i]);
            }
        }
    }

    private int getSize(LineGraphSeries<DataPoint> graph) {
        Iterator<DataPoint> iterator = graph.getValues(0d, Double.MAX_VALUE);
        int size = 0;
        while (iterator.hasNext()) {
            iterator.next();
            size++;
        }
        return size;
    }
}