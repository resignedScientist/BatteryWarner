package com.laudien.p1xelfehler.batterywarner.HelperClasses;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    public static BatteryData getBatteryData(Context context, SharedPreferences sharedPreferences, Intent batteryStatus) {
        String technology = batteryStatus.getStringExtra(android.os.BatteryManager.EXTRA_TECHNOLOGY);
        double temperature = (double) batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, NO_STATE) / 10;
        int health = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, NO_STATE);
        String healthString = getHealthString(context, health);
        int batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, NO_STATE);
        double voltage = (double) batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, NO_STATE) / 1000;
        String currentString = null, screenOn = null, screenOff = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            long current = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            currentString = String.format(Locale.getDefault(), "%s: %d mA", context.getString(R.string.current), current / -1000);
        }
        boolean dischargingServiceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
        if (dischargingServiceEnabled) {
            long screenOnTime = sharedPreferences.getLong(context.getString(R.string.value_time_screen_on), 0);
            long screenOffTime = sharedPreferences.getLong(context.getString(R.string.value_time_screen_off), 0);
            int screenOnDrain = sharedPreferences.getInt(context.getString(R.string.value_drain_screen_on), 0);
            int screenOffDrain = sharedPreferences.getInt(context.getString(R.string.value_drain_screen_off), 0);
            double screenOnTimeInHours = (double) screenOnTime / 3600000;
            double screenOffTimeInHours = (double) screenOffTime / 3600000;
            double screenOnPercentPerHour = screenOnDrain / screenOnTimeInHours;
            double screenOffPercentPerHour = screenOffDrain / screenOffTimeInHours;
            if (screenOnPercentPerHour != 0.0 && !Double.isInfinite(screenOnPercentPerHour) && !Double.isNaN(screenOnPercentPerHour)
                    && screenOffPercentPerHour != 0.0 && !Double.isInfinite(screenOffPercentPerHour) && !Double.isNaN(screenOffPercentPerHour)) {
                screenOn = String.format(Locale.getDefault(), "%s: %.2f %%/h", context.getString(R.string.screen_on), screenOnPercentPerHour);
                screenOff = String.format(Locale.getDefault(), "%s: %.2f %%/h", context.getString(R.string.screen_off), screenOffPercentPerHour);
            } else { // show "no data" string
                screenOn = String.format(Locale.getDefault(), "%s: %s %%/h", context.getString(R.string.screen_on), "N/A");
                screenOff = String.format(Locale.getDefault(), "%s: %s %%/h", context.getString(R.string.screen_off), "N/A");
            }
        }
        return new BatteryData(
                context.getString(R.string.technology) + ": " + technology, // technology
                String.format(Locale.getDefault(), context.getString(R.string.temperature) + ": %.1f °C", temperature), // temperature
                context.getString(R.string.health) + ": " + healthString, // health
                String.format(context.getString(R.string.battery_level) + ": %d%%", batteryLevel), // battery level
                String.format(Locale.getDefault(), context.getString(R.string.voltage) + ": %.3f V", voltage), // voltage
                currentString, // current
                screenOn, // screen on
                screenOff // screen off
        );
    }

    public static class BatteryData {

        private String technology, temperature, health, batteryLevel, voltage, current, screenOn, screenOff;

        BatteryData(String technology, String temperature, String health, String batteryLevel,
                    String voltage, String current) {
            this.technology = technology;
            this.temperature = temperature;
            this.health = health;
            this.batteryLevel = batteryLevel;
            this.voltage = voltage;
            this.current = current;
        }

        BatteryData(String technology, String temperature, String health, String batteryLevel,
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
}
