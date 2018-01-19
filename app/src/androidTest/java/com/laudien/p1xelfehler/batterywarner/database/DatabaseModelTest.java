package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.NUMBER_OF_GRAPHS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DatabaseModelTest {
    DatabaseModel databaseModel;
    Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
        databaseModel = DatabaseModel.getInstance(context);
        databaseModel.resetTableTask();
    }

    @After
    public void tearDown() throws Exception {
        databaseModel.resetTableTask();
    }

    @Test
    public void readDataTest() throws Exception {
        // empty database should return null
        Data data = databaseModel.readData(databaseModel.getCursor(), false, false);
        assertNull(data);
    }

    @Test
    public void readDataTest2() throws Exception {
        // test with one valid data point
        long timeNow = System.currentTimeMillis();
        DatabaseValue inputValue = new DatabaseValue(20, 234, 5000, -1200000, timeNow, timeNow);
        databaseModel.addValueTask(inputValue);
        Data outputData = databaseModel.readData(databaseModel.getCursor(), false, false);
        assertNotNull(outputData);
        LineGraphSeries<DataPoint>[] graphs = outputData.getGraphs();
        GraphInfo graphInfo = outputData.getGraphInfo();

        // test output value
        assertEquals(inputValue.getBatteryLevel(), graphs[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY(), 0d);
        assertEquals(inputValue.getTemperatureInCelsius(), graphs[GRAPH_INDEX_TEMPERATURE].getHighestValueY(), 0d);
        assertEquals(inputValue.getVoltageInVolts(), graphs[GRAPH_INDEX_VOLTAGE].getHighestValueY(), 0d);
        if (SDK_INT > LOLLIPOP) {
            assertEquals(inputValue.getCurrentInMilliAmperes(false), graphs[GRAPH_INDEX_CURRENT].getHighestValueY(), 0d);
        } else {
            assertNull(graphs[GRAPH_INDEX_CURRENT]);
        }

        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            if (SDK_INT < LOLLIPOP && i == GRAPH_INDEX_CURRENT) {
                continue;
            }
            assertEquals(0d, graphs[i].getHighestValueX(), 0d);
            assertEquals(0d, graphs[i].getLowestValueX(), 0d);
        }

        // test graph info
        assertEquals(inputValue.getUtcTimeInMillis(), graphInfo.startTime);
        assertEquals(inputValue.getUtcTimeInMillis(), graphInfo.endTime);
        assertEquals(false, graphInfo.useFahrenheit);
        assertEquals(false, graphInfo.reverseCurrent);
        assertEquals(inputValue.getBatteryLevel(), graphInfo.firstBatteryLvl);
        assertEquals(inputValue.getBatteryLevel(), graphInfo.maxBatteryLvl);
        assertEquals(inputValue.getTimeFromStartInMinutes(), graphInfo.timeInMinutes, 0d);
        assertEquals(inputValue.getTemperature(), graphInfo.maxTemp);
        assertEquals(inputValue.getTemperature(), graphInfo.minTemp);
        int expectedCurrent = SDK_INT >= LOLLIPOP ? inputValue.getCurrent() : 0;
        assertEquals(expectedCurrent, graphInfo.minCurrent);
        assertEquals(expectedCurrent, graphInfo.maxCurrent);
        assertEquals(inputValue.getTimeFromStartInMinutes(), graphInfo.timeInMinutes, 0d);
        assertEquals(inputValue.getVoltageInVolts(), graphInfo.minVoltage, 0d);
        assertEquals(inputValue.getVoltageInVolts(), graphInfo.maxVoltage, 0d);
        assertEquals(Double.NaN, graphInfo.chargingSpeed, 0d);
    }

    @Test
    public void readDataTest3() throws Exception {
        // test min and max values in graph info
        long timeNow = System.currentTimeMillis();
        boolean useFahrenheit = false;
        boolean reverseCurrent = false;
        DatabaseValue[] values = new DatabaseValue[]{
                new DatabaseValue(20, 234, 5000, -1200000, timeNow, timeNow),
                new DatabaseValue(50, 300, 5100, -1210000, timeNow + 90000000, timeNow),
                new DatabaseValue(30, 250, 4900, -1000000, timeNow + 92000000, timeNow)
        };
        for (DatabaseValue value : values) {
            databaseModel.addValueTask(value);
        }
        GraphInfo graphInfo = databaseModel.readData(databaseModel.getCursor(), useFahrenheit, reverseCurrent).getGraphInfo();

        assertEquals(values[1].getBatteryLevel(), graphInfo.maxBatteryLvl);
        assertEquals(values[0].getBatteryLevel(), graphInfo.firstBatteryLvl);
        assertEquals(values[0].getTemperature(), graphInfo.minTemp);
        assertEquals(values[1].getTemperature(), graphInfo.maxTemp);
        assertEquals(values[2].getVoltageInVolts(), graphInfo.minVoltage, 0d);
        assertEquals(values[1].getVoltageInVolts(), graphInfo.maxVoltage, 0d);
        int expectedMinCurrent = SDK_INT >= LOLLIPOP ? values[2].getCurrent() : 0;
        int expectedMaxCurrent = SDK_INT >= LOLLIPOP ? values[1].getCurrent() : 0;
        assertEquals(expectedMinCurrent, graphInfo.minCurrent);
        assertEquals(expectedMaxCurrent, graphInfo.maxCurrent);
        assertEquals(values[2].getTimeFromStartInMinutes(), graphInfo.timeInMinutes, 0d);
        assertEquals(values[0].getUtcTimeInMillis(), graphInfo.startTime);
        assertEquals(values[2].getUtcTimeInMillis(), graphInfo.endTime);
        assertEquals(1.2, graphInfo.chargingSpeed, 0.001d);
    }

    @Test
    public void readDataTest4() {
        // test with valid, different values
        long timeNow = System.currentTimeMillis();
        DatabaseValue[] values = new DatabaseValue[]{
                new DatabaseValue(20, 334, 5000, -1280000, timeNow, timeNow),
                new DatabaseValue(50, 300, 5100, -1210000, timeNow + 1000000, timeNow),
                new DatabaseValue(30, 250, 5200, -1000000, timeNow + 2000000, timeNow)
        };
        for (DatabaseValue value : values) {
            databaseModel.addValueTask(value);
        }
        Data data = databaseModel.readData(databaseModel.getCursor(), false, false);
        assertNotNull(data);
        LineGraphSeries<DataPoint>[] output = data.getGraphs();

        assertNotNull(output);
        assertEquals(NUMBER_OF_GRAPHS, output.length);

        // check y axis
        assertEquals(values[1].getBatteryLevel(), output[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY(), 0d);
        assertEquals(values[0].getBatteryLevel(), output[GRAPH_INDEX_BATTERY_LEVEL].getLowestValueY(), 0d);
        assertEquals(values[0].getTemperatureInCelsius(), output[GRAPH_INDEX_TEMPERATURE].getHighestValueY(), 0d);
        assertEquals(values[2].getTemperatureInCelsius(), output[GRAPH_INDEX_TEMPERATURE].getLowestValueY(), 0d);
        assertEquals(values[2].getVoltageInVolts(), output[GRAPH_INDEX_VOLTAGE].getHighestValueY(), 0d);
        assertEquals(values[0].getVoltageInVolts(), output[GRAPH_INDEX_VOLTAGE].getLowestValueY(), 0d);
        if (SDK_INT < LOLLIPOP) {
            assertNull(output[GRAPH_INDEX_CURRENT]);
        } else {
            assertEquals(values[0].getCurrentInMilliAmperes(false), output[GRAPH_INDEX_CURRENT].getHighestValueY(), 0d);
            assertEquals(values[2].getCurrentInMilliAmperes(false), output[GRAPH_INDEX_CURRENT].getLowestValueY(), 0d);
        }

        // check x axis
        assertEquals(0d, output[0].getLowestValueX(), 0d);
        assertEquals(values[2].getTimeFromStartInMinutes(), output[0].getHighestValueX(), 0d);
    }

    @Test
    public void readDataTest5() {
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
        for (DatabaseValue v : databaseValues) {
            databaseModel.addValueTask(v);
        }
        Data data = databaseModel.readData(databaseModel.getCursor(), false, false);
        assertNotNull(data);
        LineGraphSeries[] output = data.getGraphs();
        for (LineGraphSeries graph : output) {
            if (SDK_INT < LOLLIPOP && graph == output[GRAPH_INDEX_CURRENT]) {
                assertNull(graph);
                continue;
            }
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
        if (SDK_INT >= LOLLIPOP) {
            assertEquals(value.getCurrentInMilliAmperes(false), output[GRAPH_INDEX_CURRENT].getHighestValueY(), 0d);
            assertEquals(value.getCurrentInMilliAmperes(false), output[GRAPH_INDEX_CURRENT].getLowestValueY(), 0d);
        }

        // check x axis
        assertEquals(0d, output[0].getLowestValueX(), 0d);
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            if (SDK_INT < LOLLIPOP && i == GRAPH_INDEX_CURRENT) {
                continue;
            }
            assertEquals(databaseValues[databaseValues.length - 1].getTimeFromStartInMinutes(), output[i].getHighestValueX(), 0d);
        }
    }

    @Test
    public void readDataTest6() {
        // create 1 dummy databaseValue, all values are 0
        DatabaseValue[] databaseValues = new DatabaseValue[1];
        long timeNow = System.currentTimeMillis();
        DatabaseValue value = new DatabaseValue(0, 0, 0, 0, timeNow, timeNow);
        databaseModel.addValueTask(value);

        // check graphs -> no graph should be null
        Data data = databaseModel.readData(databaseModel.getCursor(), false, false);
        assertNotNull(data);
        LineGraphSeries[] output = data.getGraphs();
        assertNotNull(output);
        assertEquals(NUMBER_OF_GRAPHS, output.length);
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            if (SDK_INT < LOLLIPOP && i == GRAPH_INDEX_CURRENT)
                assertNull(output[i]);
            else
                assertNotNull(output[i]);
        }
    }

    @Test
    public void readDataTest7() {
        // test for exact values
        long timeNow = System.currentTimeMillis();
        DatabaseValue inputValue = new DatabaseValue(23, 234, 4125, -1234567, timeNow, timeNow);
        databaseModel.addValueTask(inputValue);
        Data outputData = databaseModel.readData(databaseModel.getCursor(), false, false);
        assertNotNull(outputData);
        LineGraphSeries<DataPoint>[] graphs = outputData.getGraphs();
        GraphInfo graphInfo = outputData.getGraphInfo();

        // test graphs
        assertEquals(23d, graphs[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY(), 0d);
        assertEquals(23d, graphs[GRAPH_INDEX_BATTERY_LEVEL].getLowestValueY(), 0d);
        assertEquals(23.4, graphs[GRAPH_INDEX_TEMPERATURE].getHighestValueY(), 0d);
        assertEquals(23.4, graphs[GRAPH_INDEX_TEMPERATURE].getLowestValueY(), 0d);
        assertEquals(4.125, graphs[GRAPH_INDEX_VOLTAGE].getHighestValueY(), 0d);
        assertEquals(4.125, graphs[GRAPH_INDEX_VOLTAGE].getLowestValueY(), 0d);
        if (SDK_INT >= LOLLIPOP) {
            assertEquals(1234.567, graphs[GRAPH_INDEX_CURRENT].getHighestValueY(), 0d);
            assertEquals(1234.567, graphs[GRAPH_INDEX_CURRENT].getLowestValueY(), 0d);
        }

        // test graph info
        assertEquals(timeNow, graphInfo.startTime);
        assertEquals(timeNow, graphInfo.endTime);
        assertEquals(23, graphInfo.firstBatteryLvl);
        assertEquals(23, graphInfo.maxBatteryLvl);
        assertEquals(0d, graphInfo.timeInMinutes, 0d);
        assertEquals(234, graphInfo.maxTemp);
        assertEquals(234, graphInfo.minTemp);
        int expectedCurrent = SDK_INT >= LOLLIPOP ? -1234567 : 0;
        assertEquals(expectedCurrent, graphInfo.minCurrent);
        assertEquals(expectedCurrent, graphInfo.maxCurrent);
        assertEquals(0d, graphInfo.timeInMinutes, 0d);
        assertEquals(4.125, graphInfo.minVoltage, 0d);
        assertEquals(4.125, graphInfo.maxVoltage, 0d);

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