package com.laudien.p1xelfehler.batterywarner.database;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatabaseValueTest {
    @Test
    public void convertToCelsiusTest() throws Exception {
        int inputValue = 300;
        double expectedOutput = 30.0;
        double actualOutput = DatabaseValue.convertToCelsius(inputValue);
        assertEquals(expectedOutput, actualOutput, 0d);
    }

    @Test
    public void convertToFahrenheitTest() throws Exception {
        int inputValue = 300;
        double expectedOutput = 86.0;
        double actualOutput = DatabaseValue.convertToFahrenheit(inputValue);
        assertEquals(expectedOutput, actualOutput, 0d);
    }

    @Test
    public void convertToVoltsTest() {
        int input = 4125;
        double output = DatabaseValue.convertToVolts(input);
        assertEquals(4.125, output, 0d);
    }

    @Test
    public void convertToMilliAmperesTest() {
        int input = -1234567;
        double output = DatabaseValue.convertToMilliAmperes(input, false);
        assertEquals(1234.567, output, 0d);

        int input2 = 1234567;
        double output2 = DatabaseValue.convertToMilliAmperes(input2, true);
        assertEquals(1234.567, output2, 0d);
    }

    @Test
    public void getTimeFromStartInMinutesTest() {
        // time = creation time
        long timeNow = System.currentTimeMillis();
        DatabaseValue databaseValue = new DatabaseValue(20, 200, 3500, -1500000, timeNow, timeNow);
        assertEquals(0d, databaseValue.getTimeFromStartInMinutes(), 0d);
    }

    @Test
    public void getTimeFromStartInMinutesTest2() {
        // time != creation time
        long timeNow = System.currentTimeMillis();
        long timeValue = timeNow + 120000;
        DatabaseValue databaseValue = new DatabaseValue(20, 200, 3500, -1500000, timeValue, timeNow);
        assertEquals(2d, databaseValue.getTimeFromStartInMinutes(), 0d);
    }
}