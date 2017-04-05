package com.laudien.p1xelfehler.batterywarner.HelperClasses;

public class BatteryData {

    private String technology, temperature, health, batteryLevel, voltage, current, screenOn, screenOff;

    public BatteryData(String technology, String temperature, String health, String batteryLevel,
                       String voltage, String current) {
        this.technology = technology;
        this.temperature = temperature;
        this.health = health;
        this.batteryLevel = batteryLevel;
        this.voltage = voltage;
        this.current = current;
    }

    public BatteryData(String technology, String  temperature, String health, String batteryLevel,
                       String voltage, String current, String screenOn, String screenOff) {
        this(technology, temperature, health, batteryLevel, voltage, current);
        this.screenOn = screenOn;
        this.screenOff = screenOff;
    }

    public String getTechnology() {
        return technology;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getHealth() {
        return health;
    }

    public String getBatteryLevel() {
        return batteryLevel;
    }

    public String getVoltage() {
        return voltage;
    }

    public String getCurrent() {
        return current;
    }

    public String getScreenOn() {
        return screenOn;
    }

    public String getScreenOff() {
        return screenOff;
    }
}
