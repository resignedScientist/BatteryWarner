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
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_HEALTH;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_SCREEN_OFF;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_SCREEN_ON;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_TECHNOLOGY;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_VOLTAGE;

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

    public static int getTextViewId(byte index) {
        switch (index) {
            case INDEX_TECHNOLOGY:
                return R.id.textView_technology;
            case INDEX_TEMPERATURE:
                return R.id.textView_temp;
            case INDEX_HEALTH:
                return R.id.textView_health;
            case INDEX_BATTERY_LEVEL:
                return R.id.textView_batteryLevel;
            case INDEX_VOLTAGE:
                return R.id.textView_voltage;
            case INDEX_CURRENT:
                return R.id.textView_current;
            case INDEX_SCREEN_ON:
                return R.id.textView_screenOn;
            case INDEX_SCREEN_OFF:
                return R.id.textView_screenOff;
            default:
                throw new RuntimeException("That index does not exist!");
        }
    }

    public static class BatteryData {

        public static final byte INDEX_TECHNOLOGY = 0;
        public static final byte INDEX_TEMPERATURE = 1;
        public static final byte INDEX_HEALTH = 2;
        public static final byte INDEX_BATTERY_LEVEL = 3;
        public static final byte INDEX_VOLTAGE = 4;
        public static final byte INDEX_CURRENT = 5;
        public static final byte INDEX_SCREEN_ON = 6;
        public static final byte INDEX_SCREEN_OFF = 7;
        private String technology;
        private int health, batteryLevel;
        private long current;
        private double temperature, voltage, screenOn, screenOff;

        public BatteryData() {

        }

        public String[] getAsArray(Context context, SharedPreferences sharedPreferences) {
            return new String[]{
                    getTechnology(context, sharedPreferences),
                    getTemperature(context, sharedPreferences),
                    getHealth(context, sharedPreferences),
                    getBatteryLevel(context, sharedPreferences),
                    getVoltage(context, sharedPreferences),
                    getCurrent(context, sharedPreferences),
                    getScreenOn(context, sharedPreferences),
                    getScreenOff(context, sharedPreferences)};
        }

        public String getTechnology(Context context, SharedPreferences sharedPreferences) {
            boolean technologyEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_info_technology), context.getResources().getBoolean(R.bool.pref_info_technology_default));
            if (technologyEnabled) {
                return getTechnology(context);
            } else {
                return null;
            }
        }

        public String getTechnology(Context context){
            return context.getString(R.string.technology) + ": " + technology;
        }

        public void setTechnology(String technology) {
            this.technology = technology;
        }

        public String getHealth(Context context, SharedPreferences sharedPreferences) {
            boolean healthEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_info_health), context.getResources().getBoolean(R.bool.pref_info_health_default));
            if (healthEnabled) {
                return getHealth(context);
            } else {
                return null;
            }
        }

        public String getHealth(Context context){
            return context.getString(R.string.health) + ": " + getHealthString(context, health);
        }

        public void setHealth(int health) {
            this.health = health;
        }

        public String getBatteryLevel(Context context, SharedPreferences sharedPreferences) {
            boolean batteryLevelEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_info_battery_level), context.getResources().getBoolean(R.bool.pref_info_battery_level_default));
            if (batteryLevelEnabled) {
                return getBatteryLevel(context);
            } else {
                return null;
            }
        }

        public String getBatteryLevel(Context context){
            return String.format(context.getString(R.string.battery_level) + ": %d%%", batteryLevel);
        }

        public void setBatteryLevel(int batteryLevel) {
            this.batteryLevel = batteryLevel;
        }

        public String getCurrent(Context context, SharedPreferences sharedPreferences) {
            if (SDK_INT >= LOLLIPOP) {
                boolean currentEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_info_current), context.getResources().getBoolean(R.bool.pref_info_current_default));
                if (currentEnabled) {
                    return getCurrent(context);
                } else {
                    return null;
                }
            } else { // KitKat
                return null;
            }
        }

        public String getCurrent(Context context){
            return String.format(Locale.getDefault(), "%s: %d mA", context.getString(R.string.current), current / -1000);
        }

        public void setCurrent(long current) {
            this.current = current;
        }

        public String getTemperature(Context context, SharedPreferences sharedPreferences) {
            boolean temperatureEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_info_temperature), context.getResources().getBoolean(R.bool.pref_info_temperature_default));
            if (temperatureEnabled) {
                return getTemperature(context);
            } else {
                return null;
            }
        }

        public String getTemperature(Context context){
            return String.format(Locale.getDefault(), context.getString(R.string.temperature) + ": %.1f °C", temperature);
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public String getVoltage(Context context, SharedPreferences sharedPreferences) {
            boolean voltageEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_info_voltage), context.getResources().getBoolean(R.bool.pref_info_temperature_default));
            if (voltageEnabled) {
                return getVoltage(context);
            } else {
                return null;
            }
        }

        public String getVoltage(Context context){
            return String.format(Locale.getDefault(), context.getString(R.string.voltage) + ": %.3f V", voltage);
        }

        public void setVoltage(double voltage) {
            this.voltage = voltage;
        }

        public String getScreenOn(Context context, SharedPreferences sharedPreferences) {
            boolean dischargingServiceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            boolean screenOnEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_info_screen_on), context.getResources().getBoolean(R.bool.pref_info_screen_on_default));
            if (dischargingServiceEnabled && screenOnEnabled) {
                return getScreenOn(context);
            } else {
                return null;
            }
        }

        public String getScreenOn(Context context){
            if (screenOn == 0.0) {
                return String.format(Locale.getDefault(), "%s: %s %%/h", context.getString(R.string.screen_on), "N/A");
            } else {
                return String.format(Locale.getDefault(), "%s: %.2f %%/h", context.getString(R.string.screen_on), screenOn);
            }
        }

        public void setScreenOn(double screenOn) {
            this.screenOn = screenOn;
        }

        public String getScreenOff(Context context, SharedPreferences sharedPreferences) {
            boolean dischargingServiceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            boolean screenOffEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_info_screen_off), context.getResources().getBoolean(R.bool.pref_info_screen_off_default));
            if (dischargingServiceEnabled && screenOffEnabled) {
                return getScreenOff(context);
            } else {
                return null;
            }
        }

        public String getScreenOff(Context context){
            if (screenOff == 0.0) {
                return String.format(Locale.getDefault(), "%s: %s %%/h", context.getString(R.string.screen_off), "N/A");
            } else {
                return String.format(Locale.getDefault(), "%s: %.2f %%/h", context.getString(R.string.screen_off), screenOff);
            }
        }

        public void setScreenOff(double screenOff) {
            this.screenOff = screenOff;
        }
    }
}
