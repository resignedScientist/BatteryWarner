package com.laudien.p1xelfehler.batterywarner.HelperClasses;

import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

import static android.os.BatteryManager.BATTERY_HEALTH_COLD;
import static android.os.BatteryManager.BATTERY_HEALTH_DEAD;
import static android.os.BatteryManager.BATTERY_HEALTH_GOOD;
import static android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT;
import static android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE;
import static android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE;
import static com.laudien.p1xelfehler.batterywarner.Contract.NO_STATE;

public class BatteryHelper {
    public static String getHealthString (Context context, int health){
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

    public static BatteryData getBatteryData(Context context, Intent batteryStatus){
        String technology = batteryStatus.getStringExtra(android.os.BatteryManager.EXTRA_TECHNOLOGY);
        double temperature = (double) batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, NO_STATE) / 10;
        int health = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, NO_STATE);
        String healthString = getHealthString(context, health);
        int batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, NO_STATE);
        double voltage = (double) batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, NO_STATE) / 1000;
        long currentNow = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        }
        return new BatteryData(
                context.getString(R.string.technology) + ": " + technology, // technology
                String.format(Locale.getDefault(), context.getString(R.string.temperature) + ": %.1f °C", temperature), // temperature
                context.getString(R.string.health) + ": " + healthString, // health
                String.format(context.getString(R.string.battery_level) + ": %d%%", batteryLevel), // battery level
                String.format(Locale.getDefault(), context.getString(R.string.voltage) + ": %.3f V", voltage), // voltage
                String.format(Locale.getDefault(), "%s: %d mA", context.getString(R.string.current), currentNow / -1000), // current
                null, // screen on
                null // screen off
        );
    }
}
