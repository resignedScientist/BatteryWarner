package com.laudien.p1xelfehler.batterywarner.database;

import android.annotation.SuppressLint;

/**
 * A class that contains all the data of one point in a graph database.
 * It should only be used by DatabaseController and DatabaseModel.
 */
public class DatabaseValue {
    private int batteryLevel, current;
    private double temperature, voltage;
    private long utcTimeInMillis;

    /**
     * The one and only constructor.
     *
     * @param batteryLevel    The battery level in percent.
     * @param temperature     The temperature in degrees celsius.
     * @param utcTimeInMillis The UTC time in milliseconds.
     */
    DatabaseValue(int batteryLevel, double temperature, double voltage, int current, long utcTimeInMillis) {
        this.batteryLevel = batteryLevel;
        this.temperature = temperature;
        this.utcTimeInMillis = utcTimeInMillis;
        this.voltage = voltage;
        this.current = current;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("[time=%d, batteryLevel=%d, temperature=%.1f, voltage=%.0f, current=%d]",
                utcTimeInMillis, batteryLevel, temperature, voltage, current);
    }

    /**
     * Get the battery level saved in this instance.
     *
     * @return The battery level in percent.
     */
    int getBatteryLevel() {
        return batteryLevel;
    }


    /**
     * Get the temperature saved in this instance.
     *
     * @return The temperature in degrees celsius.
     */
    double getTemperature() {
        return temperature;
    }

    /**
     * Get the voltage saved in this instance.
     *
     * @return The voltage in volts.
     */
    double getVoltage() {
        return voltage;
    }

    /**
     * Get the current saved in this instance.
     *
     * @return The current in mA.
     */
    int getCurrent() {
        return current;
    }

    /**
     * Get the time saved in this instance.
     *
     * @return The UTC time in milliseconds.
     */
    long getUtcTimeInMillis() {
        return utcTimeInMillis;
    }
}
