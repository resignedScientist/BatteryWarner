package com.laudien.p1xelfehler.batterywarner.HelperClasses;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.support.annotation.RequiresApi;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.ArrayList;
import java.util.Locale;

import static android.os.BatteryManager.BATTERY_HEALTH_COLD;
import static android.os.BatteryManager.BATTERY_HEALTH_DEAD;
import static android.os.BatteryManager.BATTERY_HEALTH_GOOD;
import static android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT;
import static android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE;
import static android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE;
import static android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW;
import static android.os.BatteryManager.EXTRA_HEALTH;
import static android.os.BatteryManager.EXTRA_LEVEL;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.os.BatteryManager.EXTRA_TECHNOLOGY;
import static android.os.BatteryManager.EXTRA_TEMPERATURE;
import static android.os.BatteryManager.EXTRA_VOLTAGE;
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
    private static BatteryData batteryData;

    public static BatteryData getBatteryData(Intent batteryStatus, Context context, SharedPreferences sharedPreferences) {
        if (batteryData == null) {
            batteryData = new BatteryData(batteryStatus, context, sharedPreferences);
        }
        return batteryData;
    }

    static BatteryData getBatteryData(){
        if (batteryData == null){
            return null;
        }
        return batteryData;
    }

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

    public static boolean isCharging(Intent batteryStatus) {
        return batteryStatus.getIntExtra(EXTRA_PLUGGED, -1) != 0;
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

    public static class BatteryData {

        public static final int INDEX_TECHNOLOGY = 0;
        public static final int INDEX_TEMPERATURE = 1;
        public static final int INDEX_HEALTH = 2;
        public static final int INDEX_BATTERY_LEVEL = 3;
        public static final int INDEX_VOLTAGE = 4;
        public static final int INDEX_CURRENT = 5;
        public static final int INDEX_SCREEN_ON = 6;
        public static final int INDEX_SCREEN_OFF = 7;
        private static final int NUMBER_OF_ITEMS = 8;
        private String[] values = new String[NUMBER_OF_ITEMS];
        private String technology;
        private int health, batteryLevel;
        private long current;
        private double temperature, voltage, screenOn, screenOff;
        private ArrayList<OnBatteryValueChangedListener> listeners;

        private BatteryData(Intent batteryStatus, Context context, SharedPreferences sharedPreferences) {
            update(batteryStatus, context, sharedPreferences);
        }

        public void update(Intent batteryStatus, Context context, SharedPreferences sharedPreferences) {
            setTechnology(batteryStatus.getStringExtra(EXTRA_TECHNOLOGY), context);
            setTemperature(BatteryHelper.getTemperature(batteryStatus), context);
            setHealth(batteryStatus.getIntExtra(EXTRA_HEALTH, NO_STATE), context);
            setBatteryLevel(batteryStatus.getIntExtra(EXTRA_LEVEL, NO_STATE), context);
            setVoltage(BatteryHelper.getVoltage(batteryStatus), context);
            if (SDK_INT >= LOLLIPOP) {
                BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                setCurrent(BatteryHelper.getCurrent(batteryManager), context);
            }
            boolean dischargingServiceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            if (dischargingServiceEnabled) {
                setScreenOn(BatteryHelper.getScreenOn(context, sharedPreferences), context);
                setScreenOff(BatteryHelper.getScreenOff(context, sharedPreferences), context);
            }
        }

        public String[] getAsArray() {
            return values;
        }

        public String[] getEnabledOnly(Context context, SharedPreferences sharedPreferences) {
            boolean[] enabledBooleans = new boolean[NUMBER_OF_ITEMS];
            enabledBooleans[INDEX_TECHNOLOGY] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_technology), context.getResources().getBoolean(R.bool.pref_info_technology_default));
            enabledBooleans[INDEX_TEMPERATURE] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_temperature), context.getResources().getBoolean(R.bool.pref_info_temperature_default));
            enabledBooleans[INDEX_HEALTH] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_health), context.getResources().getBoolean(R.bool.pref_info_health_default));
            enabledBooleans[INDEX_BATTERY_LEVEL] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_battery_level), context.getResources().getBoolean(R.bool.pref_info_battery_level_default));
            enabledBooleans[INDEX_VOLTAGE] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_voltage), context.getResources().getBoolean(R.bool.pref_info_voltage_default));
            enabledBooleans[INDEX_CURRENT] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_current), context.getResources().getBoolean(R.bool.pref_info_current_default));
            enabledBooleans[INDEX_SCREEN_ON] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_screen_on), context.getResources().getBoolean(R.bool.pref_info_screen_on_default));
            enabledBooleans[INDEX_SCREEN_OFF] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_screen_off), context.getResources().getBoolean(R.bool.pref_info_screen_off_default));
            String[] enabledValues = new String[NUMBER_OF_ITEMS];
            for (byte i = 0; i < NUMBER_OF_ITEMS; i++) {
                if (enabledBooleans[i]) {
                    enabledValues[i] = values[i];
                }
            }
            return enabledValues;
        }

        public String getValue(int index) {
            return values[index];
        }

        public void setTechnology(String technology, Context context) {
            if (this.technology == null || !this.technology.equals(technology)) {
                this.technology = technology;
                values[INDEX_TECHNOLOGY] = context.getString(R.string.technology) + ": " + technology;
                notifyListeners(INDEX_TECHNOLOGY);
            }
        }

        public void setHealth(int health, Context context) {
            if (this.health != health || values[INDEX_HEALTH] == null) {
                this.health = health;
                values[INDEX_HEALTH] = context.getString(R.string.health) + ": " + BatteryHelper.getHealthString(context, health);
                notifyListeners(INDEX_HEALTH);
            }
        }

        public void setBatteryLevel(int batteryLevel, Context context) {
            if (this.batteryLevel != batteryLevel || values[INDEX_BATTERY_LEVEL] == null) {
                this.batteryLevel = batteryLevel;
                values[INDEX_BATTERY_LEVEL] = String.format(context.getString(R.string.battery_level) + ": %d%%", batteryLevel);
                notifyListeners(INDEX_BATTERY_LEVEL);
            }
        }

        public int getBatteryLevel() {
            return batteryLevel;
        }

        @RequiresApi(api = LOLLIPOP)
        public void setCurrent(long current, Context context) {
            if (this.current != current || values[INDEX_CURRENT] == null) {
                this.current = current;
                values[INDEX_CURRENT] = String.format(Locale.getDefault(), "%s: %d mA", context.getString(R.string.current), current / -1000);
                notifyListeners(INDEX_CURRENT);
            }
        }

        public void setTemperature(double temperature, Context context) {
            if (this.temperature != temperature || values[INDEX_TEMPERATURE] == null) {
                this.temperature = temperature;
                values[INDEX_TEMPERATURE] = String.format(Locale.getDefault(), context.getString(R.string.temperature) + ": %.1f °C", temperature);
                notifyListeners(INDEX_TEMPERATURE);
            }
        }

        public void setVoltage(double voltage, Context context) {
            if (this.voltage != voltage || values[INDEX_VOLTAGE] == null) {
                this.voltage = voltage;
                values[INDEX_VOLTAGE] = String.format(Locale.getDefault(), context.getString(R.string.voltage) + ": %.3f V", voltage);
                notifyListeners(INDEX_VOLTAGE);
            }
        }

        public void setScreenOn(double screenOn, Context context) {
            if (this.screenOn != screenOn || values[INDEX_SCREEN_ON] == null) {
                this.screenOn = screenOn;
                if (screenOn == 0.0) {
                    values[INDEX_SCREEN_ON] = String.format(Locale.getDefault(), "%s: %s %%/h", context.getString(R.string.screen_on), "N/A");
                } else {
                    values[INDEX_SCREEN_ON] = String.format(Locale.getDefault(), "%s: %.2f %%/h", context.getString(R.string.screen_on), screenOn);
                }
                notifyListeners(INDEX_SCREEN_ON);
            }
        }

        public void setScreenOff(double screenOff, Context context) {
            if (this.screenOff != screenOff || values[INDEX_SCREEN_OFF] == null) {
                this.screenOff = screenOff;
                if (screenOff == 0.0) {
                    values[INDEX_SCREEN_OFF] = String.format(Locale.getDefault(), "%s: %s %%/h", context.getString(R.string.screen_off), "N/A");
                } else {
                    values[INDEX_SCREEN_OFF] = String.format(Locale.getDefault(), "%s: %.2f %%/h", context.getString(R.string.screen_off), screenOff);
                }
                notifyListeners(INDEX_SCREEN_OFF);
            }
        }

        public void addOnBatteryValueChangedListener(OnBatteryValueChangedListener listener) {
            if (listeners == null) {
                listeners = new ArrayList<>(1);
            }
            listeners.add(listener);
        }

        public void unregisterOnBatteryValueChangedListener(OnBatteryValueChangedListener listener) {
            if (listener != null) {
                listeners.remove(listener);
            }
        }

        private void notifyListeners(int index) {
            if (listeners != null) {
                for (OnBatteryValueChangedListener listener : listeners) {
                    listener.onBatteryValueChanged(index);
                }
            }
        }

        public interface OnBatteryValueChangedListener {
            void onBatteryValueChanged(int index);
        }
    }
}
