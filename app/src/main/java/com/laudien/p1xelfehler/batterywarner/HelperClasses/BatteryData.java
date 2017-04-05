package com.laudien.p1xelfehler.batterywarner.HelperClasses;

public class BatteryData {

    private int batteryLevel, current;
    private double temperature, voltage, screenOn, screenOff;
    private String technology, health;

    public BatteryData(String technology, double temperature, String health, int batteryLevel,
                       double voltage, int current){
        this.technology = technology;
        this.temperature = temperature;
        this.health = health;
        this.batteryLevel = batteryLevel;
        this.voltage = voltage;
        this.current = current;
    }

    public BatteryData(String technology, double temperature, String health, int batteryLevel,
                       double voltage, int current, double screenOn, double screenOff){
        this(technology, temperature, health, batteryLevel, voltage, current);
        this.screenOn = screenOn;
        this.screenOff = screenOff;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public int getCurrent() {
        return current;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getVoltage() {
        return voltage;
    }

    public double getScreenOn() {
        return screenOn;
    }

    public double getScreenOff() {
        return screenOff;
    }

    public String getTechnology() {
        return technology;
    }

    public String getHealth() {
        return health;
    }
}
