package com.laudien.p1xelfehler.batterywarner.database;

import android.annotation.SuppressLint;

public class DatabaseValue {
    private int batteryLevel;
    private double temperature;
    private long utcTimeInMillis;

    public DatabaseValue(int batteryLevel, double temperature, long utcTimeInMillis) {
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

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public double getTemperature() {
        return temperature;
    }

    public long getUtcTimeInMillis() {
        return utcTimeInMillis;
    }
}
