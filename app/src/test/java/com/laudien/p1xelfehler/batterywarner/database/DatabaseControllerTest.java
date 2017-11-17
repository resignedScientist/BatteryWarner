package com.laudien.p1xelfehler.batterywarner.database;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Iterator;
import java.util.Random;

import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.NUMBER_OF_GRAPHS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;

public class DatabaseControllerTest {
    private DatabaseController databaseController;

    @Mock
    private DatabaseContract.Model databaseModel;

    @Mock
    private DatabaseContract.DatabaseListener databaseListener;

    @Captor
    private ArgumentCaptor<DataPoint[]> captor;

    private static DatabaseValue getRandomDatabaseValue() {
        Random random = new Random();
        int batteryLevel = random.nextInt();
        int temperature = random.nextInt();
        int voltage = random.nextInt();
        int current = random.nextInt();
        long time = System.currentTimeMillis();
        return new DatabaseValue(batteryLevel, temperature, voltage, current, time);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        databaseController = new DatabaseController(databaseModel);
        databaseController.resetTable();
        databaseController.registerDatabaseListener(databaseListener);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getAllGraphs1() throws Exception {
        // databaseValue == null -> should return null
        assertNull(databaseController.getAllGraphs((DatabaseValue[]) null, false, false));
    }

    @Test
    public void getAllGraphs2() {
        // 2. create 2 dummy databaseValues, the second one is null
        int batteryLevel = 60;
        int temperature = 325;
        int voltage = 3876;
        int current = -1500000;
        long time = System.currentTimeMillis();
        DatabaseValue[] databaseValues = new DatabaseValue[2];
        databaseValues[0] = new DatabaseValue(batteryLevel, temperature, voltage, current, time);
        LineGraphSeries[] lineGraphSeries = databaseController.getAllGraphs(databaseValues, false, false);

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
    }

    @Test
    public void getAllGraphs3() {
        // 4. create 1 dummy databaseValue, all values are 0
        DatabaseValue[] databaseValues = new DatabaseValue[1];
        databaseValues[0] = new DatabaseValue(0, 0, 0, 0, System.currentTimeMillis());
        LineGraphSeries[] lineGraphSeries = databaseController.getAllGraphs(databaseValues, false, false);

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

    @Test
    public void getAllGraphs4() {
        // Adding lots of the same values
        DatabaseValue[] databaseValues = new DatabaseValue[20];
        DatabaseValue databaseValue = getRandomDatabaseValue();
        long time = databaseValue.getUtcTimeInMillis();
        long lastTime = time;
        for (int i = 0; i < databaseValues.length; i++) {
            lastTime = time + 1000 * i;
            databaseValues[i] = new DatabaseValue(
                    databaseValue.getBatteryLevel(),
                    databaseValue.getTemperature(),
                    databaseValue.getVoltage(),
                    databaseValue.getCurrent(),
                    lastTime
            );
        }

        // check the size -> size = 2 is expected!
        LineGraphSeries[] lineGraphSeries = databaseController.getAllGraphs(databaseValues, false, false);
        for (LineGraphSeries graph : lineGraphSeries) {
            int size = getSize(graph);
            assertEquals(2, size);
        }

        // check y axis
        assertEquals(databaseValue.getBatteryLevel(), lineGraphSeries[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY(), 0d);
        assertEquals((double) databaseValue.getTemperature() / 10, lineGraphSeries[GRAPH_INDEX_TEMPERATURE].getHighestValueY(), 0d);
        assertEquals((double) databaseValue.getVoltage() / 1000, lineGraphSeries[GRAPH_INDEX_VOLTAGE].getHighestValueY(), 0d);
        assertEquals((double) databaseValue.getCurrent() / -1000, lineGraphSeries[GRAPH_INDEX_CURRENT].getHighestValueY(), 0d);

        // check x axis
        double expectedTimeInMinutes = (double) (lastTime - time) / (1000 * 60);
        assertEquals(expectedTimeInMinutes, lineGraphSeries[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueX(), 0d);
    }

    @Test
    public void addValue() {
        // wrong inputs -> method should not be called!

    }

    @Test
    public void addValue1() {
        // add a valid value -> method should be called!
        databaseController.addValue(
                80,
                205,
                3500,
                -2100,
                60000,
                false,
                false
        );
        verify(databaseListener).onValueAdded(captor.capture(), Mockito.anyLong());

        // check the DataPoints
        DataPoint[] receivedDataPoints = captor.getValue();
        assertNotNull(receivedDataPoints);
        assertEquals(NUMBER_OF_GRAPHS, receivedDataPoints.length);
        assertEquals(80d, receivedDataPoints[GRAPH_INDEX_BATTERY_LEVEL].getY(), 0d);
        assertEquals(20.5, receivedDataPoints[GRAPH_INDEX_TEMPERATURE].getY(), 0d);
        assertEquals(3.5, receivedDataPoints[GRAPH_INDEX_VOLTAGE].getY(), 0d);
        assertEquals(2.1, receivedDataPoints[GRAPH_INDEX_CURRENT].getY(), 0d);

        // time (= x value) must be 0
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            assertEquals(0d, receivedDataPoints[i].getX(), 0d);
        }
    }

    @Test
    public void addValue2() {
        /* add all 0 values
        -> the method should be called
        -> each DataPoint (except batteryLevel and temperature) in the array should be null! */
        databaseController.addValue(
                0,
                0,
                0,
                0,
                0,
                false,
                false
        );
        verify(databaseListener).onValueAdded(captor.capture(), Mockito.anyLong());
        DataPoint[] receivedDataPoints = captor.getValue();
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            if (i != GRAPH_INDEX_BATTERY_LEVEL && i != GRAPH_INDEX_TEMPERATURE) {
                assertNull("Graph should be null: " + i, receivedDataPoints[i]);
            } else {
                assertNotNull("Graph should NOT be null: " + i, receivedDataPoints[i]);
            }
        }
    }

    @Test
    public void addValue3() {
        // test if the current is reversed correctly
        databaseController.addValue(
                80,
                205,
                3500,
                -2100,
                60000,
                false,
                true
        );
        verify(databaseListener).onValueAdded(captor.capture(), Mockito.anyLong());

        // check the DataPoints
        DataPoint[] receivedDataPoints = captor.getValue();
        assertNotNull(receivedDataPoints);
        assertEquals(NUMBER_OF_GRAPHS, receivedDataPoints.length);
        assertEquals(80d, receivedDataPoints[GRAPH_INDEX_BATTERY_LEVEL].getY(), 0d);
        assertEquals(20.5, receivedDataPoints[GRAPH_INDEX_TEMPERATURE].getY(), 0d);
        assertEquals(3.5, receivedDataPoints[GRAPH_INDEX_VOLTAGE].getY(), 0d);
        assertEquals("reverseCurrent is not working correctly!", -2.1, receivedDataPoints[GRAPH_INDEX_CURRENT].getY(), 0d);

        // time (= x value) must be 0
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            assertEquals(0d, receivedDataPoints[i].getX(), 0d);
        }
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

    @Test
    public void getInstance() throws Exception {
    }

    @Test
    public void getFileList() throws Exception {
    }

    @Test
    public void getAllGraphs() throws Exception {
    }

    @Test
    public void getEndTime() throws Exception {
    }

    @Test
    public void getEndTime1() throws Exception {
    }

    @Test
    public void getStartTime() throws Exception {
    }

    @Test
    public void getStartTime1() throws Exception {
    }

    @Test
    public void saveGraph() throws Exception {
    }

    @Test
    public void resetTable() throws Exception {
    }

    @Test
    public void registerDatabaseListener() throws Exception {
    }

    @Test
    public void unregisterListener() throws Exception {
    }

    @Test
    public void notifyTransitionsFinished() throws Exception {
    }

    @Test
    public void notifyTransitionsFinished1() throws Exception {
    }

    @Test
    public void upgradeAllSavedDatabases() throws Exception {
    }
}