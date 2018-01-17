package com.laudien.p1xelfehler.batterywarner.database;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatabaseValueTest {
    @Test
    public void convertToCelsius() throws Exception {
        int inputValue = 300;
        double expectedOutput = 30.0;
        double actualOutput = DatabaseValue.convertToCelsius(inputValue);
        assertEquals(expectedOutput, actualOutput, 0d);
    }

    @Test
    public void convertToFahrenheit() throws Exception {
        int inputValue = 300;
        double expectedOutput = 86.0;
        double actualOutput = DatabaseValue.convertToFahrenheit(inputValue);
        assertEquals(expectedOutput, actualOutput, 0d);
    }
}