package com.laudien.p1xelfehler.batterywarner.HelperClasses;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.support.annotation.RequiresApi;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static android.os.BatteryManager.BATTERY_HEALTH_COLD;
import static android.os.BatteryManager.BATTERY_HEALTH_DEAD;
import static android.os.BatteryManager.BATTERY_HEALTH_GOOD;
import static android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT;
import static android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE;
import static android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE;
import static android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW;
import static android.os.BatteryManager.EXTRA_TEMPERATURE;
import static android.os.BatteryManager.EXTRA_VOLTAGE;
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
        return (double) batteryStatus.getIntExtra(EXTRA_TEMPERATURE, NO_STATE) / 10;
    }

    public static double getVoltage(Intent batteryStatus) {
        return (double) batteryStatus.getIntExtra(EXTRA_VOLTAGE, NO_STATE) / 1000;
    }

    @RequiresApi(api = LOLLIPOP)
    public static long getCurrent(BatteryManager batteryManager) {
        return batteryManager.getLongProperty(BATTERY_PROPERTY_CURRENT_NOW);
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
        private String[] values = new String[8];
        private String technology;
        private int health, batteryLevel;
        private long current;
        private double temperature, voltage, screenOn, screenOff;

        public BatteryData() {

        }

        public String[] getAsArray() {
            return values;
        }

        public String getTechnologyString() {
            return values[INDEX_TECHNOLOGY];
        }

        public void setTechnology(String technology, Context context) {
            if (this.technology == null || !this.technology.equals(technology)) {
                this.technology = technology;
                values[INDEX_TECHNOLOGY] = context.getString(R.string.technology) + ": " + technology;
            }
        }

        public void removeTechnology(){
            technology = null;
            values[INDEX_TECHNOLOGY] = null;
        }

        public String getHealthString() {
            return values[INDEX_HEALTH];
        }

        public void setHealth(int health, Context context) {
            if (this.health != health || values[INDEX_HEALTH] == null) {
                this.health = health;
                values[INDEX_HEALTH] = context.getString(R.string.health) + ": " + BatteryHelper.getHealthString(context, health);
            }
        }

        public void removeHealth(){
            health = 0;
            values[INDEX_HEALTH] = null;
        }

        public String getBatteryLevelString() {
            return values[INDEX_BATTERY_LEVEL];
        }

        public void setBatteryLevel(int batteryLevel, Context context) {
            if (this.batteryLevel != batteryLevel || values[INDEX_BATTERY_LEVEL] == null) {
                this.batteryLevel = batteryLevel;
                values[INDEX_BATTERY_LEVEL] = String.format(context.getString(R.string.battery_level) + ": %d%%", batteryLevel);
            }
        }

        public void removeBatteryLevel(){
            batteryLevel = 0;
            values[INDEX_BATTERY_LEVEL] = null;
        }

        public String getCurrentString() {
            return values[INDEX_CURRENT];
        }

        @RequiresApi(api = LOLLIPOP)
        public void setCurrent(long current, Context context) {
            if (this.current != current || values[INDEX_CURRENT] == null) {
                this.current = current;
                values[INDEX_CURRENT] = String.format(Locale.getDefault(), "%s: %d mA", context.getString(R.string.current), current / -1000);
            }
        }

        @RequiresApi(api = LOLLIPOP)
        public void removeCurrent(){
            current = 0L;
            values[INDEX_CURRENT] = null;
        }

        public String getTemperatureString() {
            return values[INDEX_TEMPERATURE];
        }

        public void setTemperature(double temperature, Context context) {
            if (this.temperature != temperature || values[INDEX_TEMPERATURE] == null) {
                this.temperature = temperature;
                values[INDEX_TEMPERATURE] = String.format(Locale.getDefault(), context.getString(R.string.temperature) + ": %.1f °C", temperature);
            }
        }

        public void removeTemperature(){
            temperature = 0.0;
            values[INDEX_TEMPERATURE] = null;
        }

        public String getVoltageString() {
            return values[INDEX_VOLTAGE];
        }

        public void setVoltage(double voltage, Context context) {
            if (this.voltage != voltage || values[INDEX_VOLTAGE] == null) {
                this.voltage = voltage;
                values[INDEX_VOLTAGE] = String.format(Locale.getDefault(), context.getString(R.string.voltage) + ": %.3f V", voltage);
            }
        }

        public void removeVoltage(){
            voltage = 0.0;
            values[INDEX_VOLTAGE] = null;
        }

        public String getScreenOnString() {
            return values[INDEX_SCREEN_ON];
        }

        public void setScreenOn(double screenOn, Context context) {
            if (this.screenOn != screenOn || values[INDEX_SCREEN_ON] == null) {
                this.screenOn = screenOn;
                if (screenOn == 0.0) {
                    values[INDEX_SCREEN_ON] = String.format(Locale.getDefault(), "%s: %s %%/h", context.getString(R.string.screen_on), "N/A");
                } else {
                    values[INDEX_SCREEN_ON] = String.format(Locale.getDefault(), "%s: %.2f %%/h", context.getString(R.string.screen_on), screenOn);
                }
            }
        }

        public void removeScreenOn(){
            screenOn = 0.0;
            values[INDEX_SCREEN_ON] = null;
        }

        public String getScreenOffString() {
            return values[INDEX_SCREEN_OFF];
        }

        public void setScreenOff(double screenOff, Context context) {
            if (this.screenOff != screenOff || values[INDEX_SCREEN_OFF] == null) {
                this.screenOff = screenOff;
                if (screenOff == 0.0) {
                    values[INDEX_SCREEN_OFF] = String.format(Locale.getDefault(), "%s: %s %%/h", context.getString(R.string.screen_off), "N/A");
                } else {
                    values[INDEX_SCREEN_OFF] = String.format(Locale.getDefault(), "%s: %.2f %%/h", context.getString(R.string.screen_off), screenOff);
                }
            }
        }

        public void removeScreenOff(){
            screenOff = 0.0;
            values[INDEX_SCREEN_OFF] = null;
        }
    }
}
