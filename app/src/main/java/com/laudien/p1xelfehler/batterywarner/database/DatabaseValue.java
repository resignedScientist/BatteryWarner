package com.laudien.p1xelfehler.batterywarner.database;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.jjoe64.graphview.series.DataPoint;

import java.util.Locale;

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
    @Nullable
    private final Integer
            batteryLevel,
            current,
            temperature,
            voltage;
    private final long utcTimeInMillis;
    private final long graphCreationTime;

    /**
     * The one and only constructor.
     *
     * @param batteryLevel      The battery level in percent.
     * @param temperature       The temperature in degrees celsius.
     * @param utcTimeInMillis   The UTC time in milliseconds.
     * @param graphCreationTime The UTC time in milliseconds of the first point of the graph.
     */
    public DatabaseValue(@Nullable Integer batteryLevel, @Nullable Integer temperature,
                         @Nullable Integer voltage, @Nullable Integer current, long utcTimeInMillis,
                         long graphCreationTime) {
        this.batteryLevel = batteryLevel;
        this.temperature = temperature;
        this.utcTimeInMillis = utcTimeInMillis;
        this.voltage = voltage;
        this.current = current;
        this.graphCreationTime = graphCreationTime;
    }

    @Nullable
    public static Double convertToCelsius(@Nullable Integer temperature) {
        return temperature == null ? null : temperature.doubleValue() / 10.0;
    }

    @Nullable
    public static Double convertToFahrenheit(Integer temperature) {
        return temperature != null ? (temperature.doubleValue() * 0.18) + 32 : null;
    }

    @Nullable
    public static Double convertToMilliAmperes(@Nullable Integer current, boolean reverseCurrent) {
        return current != null ? current.doubleValue() / (reverseCurrent ? 1000.0 : -1000.0) : null;
    }

    public static Double convertToVolts(@Nullable Integer voltage) {
        return voltage != null ? voltage.doubleValue() / 1000 : null;
    }

    public static String getTemperatureString(@Nullable Integer temperature, boolean useFahrenheit) {
        Double convertedTemp;
        String unit;
        if (useFahrenheit) {
            convertedTemp = convertToFahrenheit(temperature);
            unit = "°F";
        } else {
            convertedTemp = convertToCelsius(temperature);
            unit = "°C";
        }
        return String.format(
                Locale.getDefault(),
                "%.1f %s",
                convertedTemp,
                unit
        );
    }

    @Nullable
    public DatabaseValue diff(@NonNull DatabaseValue newerDatabaseValue) {
        if (equals(newerDatabaseValue)) { // it is the same value
            return null;
        }
        return new DatabaseValue(
                batteryLevel != null && batteryLevel.equals(newerDatabaseValue.batteryLevel) ? null : newerDatabaseValue.batteryLevel,
                temperature != null && temperature.equals(newerDatabaseValue.temperature) ? null : newerDatabaseValue.temperature,
                voltage != null && voltage.equals(newerDatabaseValue.voltage) ? null : newerDatabaseValue.voltage,
                current != null && current.equals(newerDatabaseValue.current) ? null : newerDatabaseValue.current,
                newerDatabaseValue.utcTimeInMillis,
                newerDatabaseValue.graphCreationTime
        );
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("[time=%d, batteryLevel=%d%%, temperature=%d, voltage=%d, current=%d]",
                utcTimeInMillis, batteryLevel, temperature, voltage, current);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DatabaseValue) {
            DatabaseValue other = (DatabaseValue) obj;
            return other.current != null && other.current.equals(current)
                    && other.voltage != null && other.voltage.equals(voltage)
                    && other.temperature != null && other.temperature.equals(temperature)
                    && other.batteryLevel != null && other.batteryLevel.equals(batteryLevel);
        }
        return false;
    }

    @Nullable
    Integer get(int index) {
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
    @Nullable
    public Integer getBatteryLevel() {
        return batteryLevel;
    }


    /**
     * Get the temperature saved in this instance.
     *
     * @return The temperature in degrees celsius * 10.
     */
    @Nullable
    public Integer getTemperature() {
        return temperature;
    }

    @Nullable
    Double getTemperatureInCelsius() {
        return temperature != null ? convertToCelsius(temperature) : null;
    }

    @Nullable
    private Double getTemperatureInFahrenheit() {
        return temperature != null ? convertToFahrenheit(temperature) : null;
    }

    /**
     * Get the voltage saved in this instance.
     *
     * @return The voltage in volts * 1000.
     */
    @Nullable
    Integer getVoltage() {
        return voltage;
    }

    @Nullable
    public Double getVoltageInVolts() {
        return voltage != null ? convertToVolts(voltage) : null;
    }

    /**
     * Get the current saved in this instance.
     *
     * @return The current in mA * -1000.
     */
    @Nullable
    Integer getCurrent() {
        return current;
    }

    @Nullable
    public Double getCurrentInMilliAmperes(boolean reverseCurrent) {
        return current != null ? convertToMilliAmperes(current, reverseCurrent) : null;
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
        Double temperature = useFahrenheit ? getTemperatureInFahrenheit() : getTemperatureInCelsius();
        Double voltage = getVoltageInVolts();
        Double current = getCurrentInMilliAmperes(reverseCurrent);
        if (batteryLevel != null)
            dataPoints[GRAPH_INDEX_BATTERY_LEVEL] = new DataPoint(timeInMinutes, batteryLevel);
        if (temperature != null)
            dataPoints[GRAPH_INDEX_TEMPERATURE] = new DataPoint(timeInMinutes, temperature);
        if (voltage != null)
            dataPoints[GRAPH_INDEX_VOLTAGE] = new DataPoint(timeInMinutes, voltage);
        if (current != null)
            dataPoints[GRAPH_INDEX_CURRENT] = new DataPoint(timeInMinutes, current);
        return dataPoints;
    }
}
