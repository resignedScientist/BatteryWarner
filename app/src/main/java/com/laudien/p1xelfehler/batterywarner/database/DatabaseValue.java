package com.laudien.p1xelfehler.batterywarner.database;

import android.annotation.SuppressLint;

/**
 * A class that contains all the data of one point in a graph database.
 * It should only be used by DatabaseController and DatabaseModel.
 */
public class DatabaseValue {
    private int batteryLevel;
    private double temperature;
    private long utcTimeInMillis;

    /**
     * The one and only constructor.
     *
     * @param batteryLevel    The battery level in percent.
     * @param temperature     The temperature in degrees celsius.
     * @param utcTimeInMillis The UTC time in milliseconds.
     */
    DatabaseValue(int batteryLevel, double temperature, long utcTimeInMillis) {
        this.batteryLevel = batteryLevel;
        this.temperature = temperature;
        this.utcTimeInMillis = utcTimeInMillis;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("[time=%d, batteryLevel=%d, temperature=%.1f]",
                utcTimeInMillis, batteryLevel, temperature);
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
     * Get the time saved in this instance.
     *
     * @return The UTC time in milliseconds.
     */
    long getUtcTimeInMillis() {
        return utcTimeInMillis;
    }
}
