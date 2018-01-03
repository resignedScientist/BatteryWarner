package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.Intent;

import com.laudien.p1xelfehler.batterywarner.R;

import static android.os.BatteryManager.BATTERY_HEALTH_COLD;
import static android.os.BatteryManager.BATTERY_HEALTH_DEAD;
import static android.os.BatteryManager.BATTERY_HEALTH_GOOD;
import static android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT;
import static android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE;
import static android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.os.BatteryManager.EXTRA_TEMPERATURE;
import static android.os.BatteryManager.EXTRA_VOLTAGE;

/**
 * Helper class for everything about battery status information.
 */
public class BatteryHelper {
    public static String getHealthString(Context context, int health) {
        switch (health) {
            case BATTERY_HEALTH_COLD:
                return context.getString(R.string.health_cold);
            case BATTERY_HEALTH_DEAD:
                return context.getString(R.string.health_dead);
            case BATTERY_HEALTH_GOOD:
                return context.getString(R.string.health_good);
            case BATTERY_HEALTH_OVER_VOLTAGE:
                return context.getString(R.string.health_overvoltage);
            case BATTERY_HEALTH_OVERHEAT:
                return context.getString(R.string.health_overheat);
            case BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return context.getString(R.string.health_unspecified_failure);
            default:
                return context.getString(R.string.health_unknown);
        }
    }

    /**
     * Reads the information if the device is currently charging out of the given intent.
     *
     * @param batteryStatus Intent that is provided by a receiver with the action ACTION_BATTERY_CHANGED.
     * @return Returns true if the device is charging, false if not.
     */
    public static boolean isCharging(Intent batteryStatus) {
        return batteryStatus.getIntExtra(EXTRA_PLUGGED, -1) != 0;
    }

    /**
     * Reads the temperature out of the given intent and calculates it to the correct format.
     *
     * @param batteryStatus Intent that is provided by a receiver with the action ACTION_BATTERY_CHANGED.
     * @return Returns the temperature in the correct format as double.
     */
    public static double getTemperature(Intent batteryStatus) {
        return (double) batteryStatus.getIntExtra(EXTRA_TEMPERATURE, -1) / 10;
    }

    /**
     * Reads the voltage out of the given intent and calculates it to the correct format.
     *
     * @param batteryStatus Intent that is provided by a receiver with the action ACTION_BATTERY_CHANGED.
     * @return Returns the voltage in the correct format as double.
     */
    public static double getVoltage(Intent batteryStatus) {
        return (double) batteryStatus.getIntExtra(EXTRA_VOLTAGE, -1) / 1000;
    }
}
