package com.laudien.p1xelfehler.batterywarner.database;

import org.junit.Test;

import java.util.Locale;

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

        long timeValue2 = timeNow + 90000000;
        DatabaseValue databaseValue2 = new DatabaseValue(20, 200, 3500, -1500000, timeValue2, timeNow);
        assertEquals(1500.0, databaseValue2.getTimeFromStartInMinutes(), 0d);
    }

    @Test
    public void getTemperatureStringTest() {
        int temp = 281;
        String expectedOutput = String.format(Locale.getDefault(), "%.1f °C", 28.1);
        String output = DatabaseValue.getTemperatureString(temp, false);
        assertEquals(expectedOutput, output);

        String expectedOutput2 = String.format(Locale.getDefault(), "%.1f °F", 82.58);
        String output2 = DatabaseValue.getTemperatureString(temp, true);
        assertEquals(expectedOutput2, output2);
    }
}