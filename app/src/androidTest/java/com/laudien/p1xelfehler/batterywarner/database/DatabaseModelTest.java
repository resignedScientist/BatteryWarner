package com.laudien.p1xelfehler.batterywarner.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
        boolean useFahrenheit = false;
        boolean reverseCurrent = false;
        DatabaseValue inputValue = new DatabaseValue(20, 234, 5000, -1200000, timeNow, timeNow);
        databaseModel.addValueTask(inputValue);
        Data outputData = databaseModel.readData(databaseModel.getCursor(), useFahrenheit, reverseCurrent);
        DatabaseValue outputValue = outputData.getDatabaseValues()[0];
        GraphInfo graphInfo = outputData.getGraphInfo();

        // test output value
        assertEquals(inputValue.getBatteryLevel(), outputValue.getBatteryLevel());
        assertEquals(inputValue.getTemperature(), outputValue.getTemperature());
        assertEquals(inputValue.getTemperature(), outputValue.getTemperature());
        assertEquals(inputValue.getVoltage(), outputValue.getVoltage());
        assertEquals(inputValue.getCurrent(), outputValue.getCurrent());
        assertEquals(inputValue.getUtcTimeInMillis(), outputValue.getUtcTimeInMillis());
        assertEquals(inputValue.getGraphCreationTime(), outputValue.getGraphCreationTime());

        // test graph info
        assertEquals(inputValue.getUtcTimeInMillis(), graphInfo.startTime);
        assertEquals(inputValue.getUtcTimeInMillis(), graphInfo.endTime);
        assertEquals(useFahrenheit, graphInfo.useFahrenheit);
        assertEquals(reverseCurrent, graphInfo.reverseCurrent);
        assertEquals(inputValue.getBatteryLevel(), graphInfo.firstBatteryLvl);
        assertEquals(inputValue.getBatteryLevel(), graphInfo.maxBatteryLvl);
        assertEquals(inputValue.getTimeFromStartInMinutes(), graphInfo.timeInMinutes, 0d);
        assertEquals(inputValue.getTemperature(), graphInfo.maxTemp);
        assertEquals(inputValue.getTemperature(), graphInfo.minTemp);
        assertEquals(inputValue.getCurrent(), graphInfo.minCurrent);
        assertEquals(inputValue.getCurrent(), graphInfo.maxCurrent);
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
                new DatabaseValue(50, 300, 5100, -1210000, timeNow + 1000000, timeNow),
                new DatabaseValue(30, 250, 4900, -1000000, timeNow + 2000000, timeNow)
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
        assertEquals(values[2].getCurrent(), graphInfo.minCurrent);
        assertEquals(values[1].getCurrent(), graphInfo.maxCurrent);
        assertEquals(values[2].getTimeFromStartInMinutes(), graphInfo.getTimeInMinutes(), 0d);
        assertEquals(values[0].getUtcTimeInMillis(), graphInfo.startTime);
        assertEquals(values[2].getUtcTimeInMillis(), graphInfo.endTime);
    }
}