package com.laudien.p1xelfehler.batterywarner.database;

import android.support.annotation.Nullable;

import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_VOLTAGE;

public class DiffValue {
    private final Integer
            batteryLevel,
            current,
            temperature,
            voltage;

    DiffValue(@Nullable Integer batteryLevel, @Nullable Integer temperature,
              @Nullable Integer voltage, @Nullable Integer current) {
        this.batteryLevel = batteryLevel;
        this.temperature = temperature;
        this.voltage = voltage;
        this.current = current;
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
}
