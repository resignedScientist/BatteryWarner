package com.laudien.p1xelfehler.batterywarner.database;

public class DatabaseValue {
    private int batteryLevel;
    private double temperature;
    private long utcTimeInMillis;

    public DatabaseValue(int batteryLevel, double temperature, long utcTimeInMillis) {
        this.batteryLevel = batteryLevel;
        this.temperature = temperature;
        this.utcTimeInMillis = utcTimeInMillis;
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
