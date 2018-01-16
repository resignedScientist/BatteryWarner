package com.laudien.p1xelfehler.batterywarner.database;

import android.annotation.SuppressLint;
import android.os.Build;

import com.jjoe64.graphview.series.DataPoint;
import com.laudien.p1xelfehler.batterywarner.helper.TemperatureConverter;

import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.NUMBER_OF_GRAPHS;

/**
 * A class that contains all the data of one point in a graph database.
 * It should only be used by DatabaseController and DatabaseModel.
 */
public class DatabaseValue {
    private int batteryLevel, current, temperature, voltage;
    private long utcTimeInMillis, graphCreationTime;

    /**
     * The one and only constructor.
     *
     * @param batteryLevel    The battery level in percent.
     * @param temperature     The temperature in degrees celsius.
     * @param utcTimeInMillis The UTC time in milliseconds.
     * @param graphCreationTime The UTC time in milliseconds of the first point of the graph.
     */
    public DatabaseValue(int batteryLevel, int temperature, int voltage, int current, long utcTimeInMillis, long graphCreationTime) {
        this.batteryLevel = batteryLevel;
        this.temperature = temperature;
        this.utcTimeInMillis = utcTimeInMillis;
        this.voltage = voltage;
        this.current = current;
        this.graphCreationTime = graphCreationTime;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("[time=%d, batteryLevel=%d%%, temperature=%.1f°C, voltage=%.3f V, current=%.3f mAh]",
                utcTimeInMillis, batteryLevel, (double) temperature / 10, (double) voltage / 1000, (double) current / -1000);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DatabaseValue) {
            DatabaseValue other = (DatabaseValue) obj;
            return other.utcTimeInMillis == utcTimeInMillis
                    && other.current == current
                    && other.voltage == voltage
                    && other.temperature == temperature
                    && other.batteryLevel == batteryLevel;
        } else {
            return false;
        }
    }

    int get(int index) {
        switch (index) {
            case GRAPH_INDEX_BATTERY_LEVEL:
                return batteryLevel;
            case GRAPH_INDEX_TEMPERATURE:
                return temperature;
            case GRAPH_INDEX_VOLTAGE:
                return voltage;
            case GRAPH_INDEX_CURRENT:
                return current;
            default:
                throw new RuntimeException("Unknown index: + " + index);
        }
    }

    /**
     * Get the battery level saved in this instance.
     *
     * @return The battery level in percent.
     */
    public int getBatteryLevel() {
        return batteryLevel;
    }


    /**
     * Get the temperature saved in this instance.
     *
     * @return The temperature in degrees celsius * 10.
     */
    public int getTemperature() {
        return temperature;
    }

    double getTemperatureInCelsius() {
        return (double) temperature / 10.0;
    }

    double getTemperatureInFahrenheit() {
        return TemperatureConverter.convertCelsiusToFahrenheit(getTemperatureInCelsius());
    }

    /**
     * Get the voltage saved in this instance.
     *
     * @return The voltage in volts * 1000.
     */
    int getVoltage() {
        return voltage;
    }

    public double getVoltageInVolts() {
        return getVoltage() / 1000;
    }

    /**
     * Get the current saved in this instance.
     *
     * @return The current in mA * -1000.
     */
    int getCurrent() {
        return current;
    }

    public double getCurrentInMilliAmperes(boolean reverseCurrent) {
        return getCurrent() / (reverseCurrent ? 1000 : -1000);
    }

    /**
     * Get the time saved in this instance.
     *
     * @return The UTC time in milliseconds.
     */
    public long getUtcTimeInMillis() {
        return utcTimeInMillis;
    }

    public double getTimeFromStartInMinutes() {
        return (double) (utcTimeInMillis - graphCreationTime) / (double) (1000 * 60);
    }

    public long getGraphCreationTime() {
        return graphCreationTime;
    }

    public DataPoint[] toDataPoints(boolean useFahrenheit, boolean reverseCurrent) {
        DataPoint[] dataPoints = new DataPoint[NUMBER_OF_GRAPHS];
        double timeInMinutes = getTimeFromStartInMinutes();
        double temperature = useFahrenheit ? getTemperatureInFahrenheit() : getTemperatureInCelsius();
        double voltage = getVoltageInVolts();
        double current = getCurrentInMilliAmperes(reverseCurrent);
        dataPoints[GRAPH_INDEX_BATTERY_LEVEL] = new DataPoint(timeInMinutes, batteryLevel);
        dataPoints[GRAPH_INDEX_TEMPERATURE] = new DataPoint(timeInMinutes, temperature);
        dataPoints[GRAPH_INDEX_VOLTAGE] = new DataPoint(timeInMinutes, voltage);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dataPoints[GRAPH_INDEX_CURRENT] = new DataPoint(timeInMinutes, current);
        }
        return dataPoints;
    }
}
