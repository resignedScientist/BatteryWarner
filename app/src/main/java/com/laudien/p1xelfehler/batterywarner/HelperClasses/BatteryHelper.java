package com.laudien.p1xelfehler.batterywarner.HelperClasses;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.support.annotation.RequiresApi;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

import static android.os.BatteryManager.BATTERY_HEALTH_COLD;
import static android.os.BatteryManager.BATTERY_HEALTH_DEAD;
import static android.os.BatteryManager.BATTERY_HEALTH_GOOD;
import static android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT;
import static android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE;
import static android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.laudien.p1xelfehler.batterywarner.Contract.NO_STATE;

public class BatteryHelper {
    private static String getHealthString(Context context, int health) {
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

    public static double getTemperature(Intent batteryStatus) {
        return (double) batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, NO_STATE) / 10;
    }

    public static double getVoltage(Intent batteryStatus) {
        return (double) batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, NO_STATE) / 1000;
    }

    @RequiresApi(api = LOLLIPOP)
    public static long getCurrent(BatteryManager batteryManager) {
        return batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
    }

    public static double getScreenOn(Context context, SharedPreferences sharedPreferences) {
        long screenOnTime = sharedPreferences.getLong(context.getString(R.string.value_time_screen_on), 0);
        int screenOnDrain = sharedPreferences.getInt(context.getString(R.string.value_drain_screen_on), 0);
        double screenOnTimeInHours = (double) screenOnTime / 3600000;
        double screenOnPercentPerHour = screenOnDrain / screenOnTimeInHours;
        if (screenOnPercentPerHour != 0.0 && !Double.isInfinite(screenOnPercentPerHour) && !Double.isNaN(screenOnPercentPerHour)) {
            return screenOnDrain / screenOnTimeInHours;
        } else {
            return 0.0;
        }
    }

    public static double getScreenOff(Context context, SharedPreferences sharedPreferences) {
        long screenOffTime = sharedPreferences.getLong(context.getString(R.string.value_time_screen_off), 0);
        int screenOffDrain = sharedPreferences.getInt(context.getString(R.string.value_drain_screen_off), 0);
        double screenOffTimeInHours = (double) screenOffTime / 3600000;
        double screenOffPercentPerHour = screenOffDrain / screenOffTimeInHours;
        if (screenOffPercentPerHour != 0.0 && !Double.isInfinite(screenOffPercentPerHour) && !Double.isNaN(screenOffPercentPerHour)) {
            return screenOffPercentPerHour;
        } else {
            return 0.0;
        }
    }

    public static class BatteryData {

        private String technology;
        private int health, batteryLevel;
        private long current;
        private double temperature, voltage, screenOn, screenOff;

        public BatteryData() {

        }

        public String getTechnology(Context context) {
            return context.getString(R.string.technology) + ": " + technology;
        }

        public void setTechnology(String technology) {
            this.technology = technology;
        }

        public String getHealth(Context context) {
            return context.getString(R.string.health) + ": " + getHealthString(context, health);
        }

        public void setHealth(int health) {
            this.health = health;
        }

        public String getBatteryLevel(Context context) {
            return String.format(context.getString(R.string.battery_level) + ": %d%%", batteryLevel);
        }

        public void setBatteryLevel(int batteryLevel) {
            this.batteryLevel = batteryLevel;
        }

        @RequiresApi(api = LOLLIPOP)
        public String getCurrent(Context context) {
            if (SDK_INT >= LOLLIPOP) {
                return String.format(Locale.getDefault(), "%s: %d mA", context.getString(R.string.current), current / -1000);
            } else {
                return null;
            }
        }

        @RequiresApi(api = LOLLIPOP)
        public void setCurrent(long current) {
            this.current = current;
        }

        public String getTemperature(Context context) {
            return String.format(Locale.getDefault(), context.getString(R.string.temperature) + ": %.1f °C", temperature);
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public String getVoltage(Context context) {
            return String.format(Locale.getDefault(), context.getString(R.string.voltage) + ": %.3f V", voltage);
        }

        public void setVoltage(double voltage) {
            this.voltage = voltage;
        }

        public String getScreenOn(Context context, SharedPreferences sharedPreferences) {
            boolean dischargingServiceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            if (dischargingServiceEnabled) {
                if (screenOn == 0.0) {
                    return String.format(Locale.getDefault(), "%s: %s %%/h", context.getString(R.string.screen_on), "N/A");
                } else {
                    return String.format(Locale.getDefault(), "%s: %.2f %%/h", context.getString(R.string.screen_on), screenOn);
                }
            } else {
                return null;
            }
        }

        public void setScreenOn(double screenOn) {
            this.screenOn = screenOn;
        }

        public String getScreenOff(Context context, SharedPreferences sharedPreferences) {
            boolean dischargingServiceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            if (dischargingServiceEnabled) {
                if (screenOff == 0.0){
                    return String.format(Locale.getDefault(), "%s: %s %%/h", context.getString(R.string.screen_off), "N/A");
                } else {
                    return String.format(Locale.getDefault(), "%s: %.2f %%/h", context.getString(R.string.screen_off), screenOff);
                }
            } else {
                return null;
            }
        }

        public void setScreenOff(double screenOff) {
            this.screenOff = screenOff;
        }
    }
}
