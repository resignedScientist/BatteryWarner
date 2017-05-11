package com.laudien.p1xelfehler.batterywarner.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper;
import com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;

import java.util.Calendar;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_HEALTH;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_SCREEN_OFF;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_SCREEN_ON;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_TECHNOLOGY;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_BATTERY_INFO;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_WARNING_LOW;

/**
 * Background service that runs while discharging. It logs the percentage loss and times
 * for screen on and off and saves it in the default shared preferences.
 * It stops automatically if the user starts to charge or it is disabled in the settings.
 */
public class DischargingService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = getClass().getSimpleName();
    private SharedPreferences sharedPreferences, temporaryPrefs;
    private long screenOnTime, screenOffTime;
    private long timeChanged = Calendar.getInstance().getTimeInMillis(); // time point when screen on/off was changed
    private int lastPercentage = -1, // that is a different value as in sharedPreferences!
            screenOnDrain, screenOffDrain;
    private boolean isScreenOn, dischargingServiceEnabled, infoNotificationEnabled, isCharging;
    private BroadcastReceiver screenOnReceiver, screenOffReceiver, batteryChangedReceiver;
    private BatteryData batteryData;
    private Intent lastBatteryStatus;
    private MyOnBatteryValueChangedListener batteryValueChangedListener;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service...");
        if (batteryChangedReceiver == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isEnabled = sharedPreferences.getBoolean(getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default));
            dischargingServiceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            infoNotificationEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_notification_enabled), getResources().getBoolean(R.bool.pref_info_notification_enabled_default));
            if (isEnabled && (dischargingServiceEnabled || infoNotificationEnabled)) {
                batteryChangedReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent batteryStatus) {
                        isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0;
                        if (!isCharging && dischargingServiceEnabled) {
                            int batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                            int warningLow = sharedPreferences.getInt(getString(R.string.pref_warning_low), getResources().getInteger(R.integer.pref_warning_low_default));
                            if (batteryLevel <= warningLow) {
                                NotificationHelper.showNotification(context, ID_WARNING_LOW);
                            }
                            if (lastPercentage == -1) { // first value -> take current percent
                                lastPercentage = batteryLevel;
                            } else { // not the first value
                                if (batteryLevel != lastPercentage) {
                                    int diff = lastPercentage - batteryLevel;
                                    lastPercentage = batteryLevel;
                                    if (isScreenOn) { // screen is on
                                        temporaryPrefs.edit().putInt(getString(R.string.value_drain_screen_on), screenOnDrain + diff).apply();
                                    } else { // screen is off
                                        temporaryPrefs.edit().putInt(getString(R.string.value_drain_screen_off), screenOffDrain + diff).apply();
                                    }
                                }
                            }
                        }
                        if (infoNotificationEnabled) {
                            lastBatteryStatus = batteryStatus;
                            if (batteryData != null) {
                                batteryData.update(batteryStatus, context, sharedPreferences);
                            } else {
                                batteryData = BatteryHelper.getBatteryData(batteryStatus, context, sharedPreferences);
                            }
                        }
                        // stop service if not needed anymore
                        if ((!dischargingServiceEnabled && !infoNotificationEnabled) || (isCharging && dischargingServiceEnabled && !infoNotificationEnabled)) {
                            stopSelf();
                        }
                    }
                };
                screenOnReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Log.d(TAG, "screen on received!");
                        isScreenOn = true;
                        if (dischargingServiceEnabled) {
                            long timeNow = Calendar.getInstance().getTimeInMillis();
                            long timeDifference = timeNow - timeChanged;
                            timeChanged = timeNow;
                            temporaryPrefs.edit().putLong(getString(R.string.value_time_screen_off), screenOffTime + timeDifference).apply();
                        }
                        if (infoNotificationEnabled) {
                            if (batteryValueChangedListener == null) {
                                batteryValueChangedListener = new MyOnBatteryValueChangedListener();
                            }
                            batteryData.addOnBatteryValueChangedListener(batteryValueChangedListener);
                            registerReceiver(batteryChangedReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
                        }
                    }
                };
                screenOffReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Log.d(TAG, "screen off received!");
                        isScreenOn = false;
                        if (dischargingServiceEnabled) {
                            long timeNow = Calendar.getInstance().getTimeInMillis();
                            long timeDifference = timeNow - timeChanged;
                            timeChanged = timeNow;
                            temporaryPrefs.edit()
                                    .putLong(getString(R.string.value_time_screen_on), screenOnTime + timeDifference)
                                    .apply();
                        }
                        if (infoNotificationEnabled) {
                            if (isCharging || !dischargingServiceEnabled) {
                                unregisterReceiver(batteryChangedReceiver);
                            }
                            if (batteryValueChangedListener != null) {
                                batteryData.unregisterOnBatteryValueChangedListener(batteryValueChangedListener);
                            }
                        }
                    }
                };
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // lollipop and higher
                    isScreenOn = powerManager.isInteractive();
                } else { // kitKat or lower
                    isScreenOn = powerManager.isScreenOn();
                }
                setDischargingServiceEnabled(dischargingServiceEnabled);
                sharedPreferences.registerOnSharedPreferenceChangeListener(this);
                registerReceiver(screenOnReceiver, new IntentFilter(ACTION_SCREEN_ON));
                registerReceiver(screenOffReceiver, new IntentFilter(ACTION_SCREEN_OFF));
                registerReceiver(batteryChangedReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
            } else {
                stopSelf();
            }
            Log.d(TAG, "Service started!");
        } else {
            Log.d(TAG, "The service is already running!");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying service...");
        try {
            unregisterReceiver(screenOnReceiver);
            unregisterReceiver(screenOffReceiver);
            unregisterReceiver(batteryChangedReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (batteryData != null && batteryValueChangedListener != null) {
            batteryData.unregisterOnBatteryValueChangedListener(batteryValueChangedListener);
        }
        if (temporaryPrefs != null) {
            temporaryPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        if (infoNotificationEnabled) {
            NotificationHelper.cancelNotification(this, ID_BATTERY_INFO);
        }
        Log.d(TAG, "Service destroyed!");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (dischargingServiceEnabled && s.equals(getString(R.string.value_drain_screen_on))) {
            screenOnDrain = sharedPreferences.getInt(s, screenOnDrain);
        } else if (dischargingServiceEnabled && s.equals(getString(R.string.value_drain_screen_off))) {
            screenOffDrain = sharedPreferences.getInt(s, screenOffDrain);
        } else if (dischargingServiceEnabled && s.equals(getString(R.string.value_time_screen_on))) {
            screenOnTime = sharedPreferences.getLong(s, screenOnTime);
        } else if (dischargingServiceEnabled && s.equals(getString(R.string.value_time_screen_off))) {
            screenOffTime = sharedPreferences.getLong(s, screenOffTime);
        } else if (s.equals(getString(R.string.pref_info_technology))) {
            boolean technologyEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_technology_default));
            rebuildNotification(technologyEnabled);
        } else if (s.equals(getString(R.string.pref_info_temperature))) {
            boolean temperatureEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_temperature_default));
            rebuildNotification(temperatureEnabled);
        } else if (s.equals(getString(R.string.pref_info_health))) {
            boolean healthEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_health_default));
            rebuildNotification(healthEnabled);
        } else if (s.equals(getString(R.string.pref_info_battery_level))) {
            boolean batteryLevelEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_battery_level_default));
            rebuildNotification(batteryLevelEnabled);
        } else if (s.equals(getString(R.string.pref_info_voltage))) {
            boolean voltageEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_voltage_default));
            rebuildNotification(voltageEnabled);
        } else if (s.equals(getString(R.string.pref_info_current))) {
            boolean currentEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_current_default));
            rebuildNotification(currentEnabled);
        } else if (s.equals(getString(R.string.pref_info_screen_on))) {
            boolean screenOnEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_screen_on_default));
            rebuildNotification(screenOnEnabled);
        } else if (s.equals(getString(R.string.pref_info_screen_off))) {
            boolean screenOffEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_screen_off_default));
            rebuildNotification(screenOffEnabled);
        } else if (s.equals(getString(R.string.pref_dark_theme_enabled))) {
            rebuildNotification(false);
        } else if (s.equals(getString(R.string.pref_info_dark_theme))) {
            rebuildNotification(false);
        } else if (s.equals(getString(R.string.pref_info_notification_enabled))) {
            setInfoNotificationEnabled(sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_notification_enabled_default)));
        } else if (s.equals(getString(R.string.pref_discharging_service_enabled))) {
            setDischargingServiceEnabled(sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_discharging_service_enabled_default)));
        } else if (s.equals(getString(R.string.pref_is_enabled))
                && !sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_is_enabled_default))) {
            stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setInfoNotificationEnabled(boolean enabled) {
        infoNotificationEnabled = enabled;
        if (!enabled) { // if it was disabled -> stop if service is not used anymore or clean up
            NotificationHelper.cancelNotification(this, ID_BATTERY_INFO);
            if (isCharging || !dischargingServiceEnabled) {
                stopSelf();
            } else { // service is needed
                if (batteryData != null) {
                    if (batteryValueChangedListener != null) {
                        batteryData.unregisterOnBatteryValueChangedListener(batteryValueChangedListener);
                        batteryValueChangedListener = null;
                    }
                    batteryData = null;
                }
                lastBatteryStatus = null;
            }
        }
    }

    private void setDischargingServiceEnabled(boolean enabled) {
        dischargingServiceEnabled = enabled;
        rebuildNotification(enabled);
        if (enabled) {
            temporaryPrefs = getSharedPreferences(getString(R.string.prefs_temporary), MODE_PRIVATE);
            screenOnDrain = temporaryPrefs.getInt(getString(R.string.value_drain_screen_on), 0);
            screenOffDrain = temporaryPrefs.getInt(getString(R.string.value_drain_screen_off), 0);
            screenOnTime = temporaryPrefs.getLong(getString(R.string.value_time_screen_on), 0);
            screenOffTime = temporaryPrefs.getLong(getString(R.string.value_time_screen_off), 0);
            temporaryPrefs.registerOnSharedPreferenceChangeListener(this);
        } else {
            if (infoNotificationEnabled) {
                if (temporaryPrefs != null) {
                    temporaryPrefs.unregisterOnSharedPreferenceChangeListener(this);
                    temporaryPrefs = null;
                }
            } else { // service is not needed anymore
                stopSelf();
            }
        }
    }

    private void rebuildNotification(boolean updateData) {
        if (updateData) {
            NotificationHelper.cancelNotification(this, ID_BATTERY_INFO);
            if (batteryData != null && lastBatteryStatus != null) {
                batteryData.update(lastBatteryStatus, this, sharedPreferences);
            }
        }
        NotificationHelper.showNotification(this, ID_BATTERY_INFO);
    }

    private class MyOnBatteryValueChangedListener implements BatteryData.OnBatteryValueChangedListener {
        @Override
        public void onBatteryValueChanged(int index) {
            switch (index) {
                case INDEX_TECHNOLOGY:
                    if (!sharedPreferences.getBoolean(getString(R.string.pref_info_technology), getResources().getBoolean(R.bool.pref_info_technology_default)))
                        return;
                    break;
                case INDEX_TEMPERATURE:
                    if (!sharedPreferences.getBoolean(getString(R.string.pref_info_temperature), getResources().getBoolean(R.bool.pref_info_temperature_default)))
                        return;
                    break;
                case INDEX_HEALTH:
                    if (!sharedPreferences.getBoolean(getString(R.string.pref_info_health), getResources().getBoolean(R.bool.pref_info_health_default)))
                        return;
                    break;
                case INDEX_BATTERY_LEVEL:
                    if (!sharedPreferences.getBoolean(getString(R.string.pref_info_battery_level), getResources().getBoolean(R.bool.pref_info_battery_level_default)))
                        return;
                    break;
                case INDEX_VOLTAGE:
                    if (!sharedPreferences.getBoolean(getString(R.string.pref_info_voltage), getResources().getBoolean(R.bool.pref_info_voltage_default)))
                        return;
                    break;
                case INDEX_CURRENT:
                    if (SDK_INT >= LOLLIPOP && !sharedPreferences.getBoolean(getString(R.string.pref_info_current), getResources().getBoolean(R.bool.pref_info_current_default)))
                        return;
                    break;
                case INDEX_SCREEN_ON:
                    if (!dischargingServiceEnabled || !sharedPreferences.getBoolean(getString(R.string.pref_info_screen_on), getResources().getBoolean(R.bool.pref_info_screen_on_default)))
                        return;
                    break;
                case INDEX_SCREEN_OFF:
                    if (!dischargingServiceEnabled || !sharedPreferences.getBoolean(getString(R.string.pref_info_screen_off), getResources().getBoolean(R.bool.pref_info_screen_off_default)))
                        return;
                    break;
            }
            NotificationHelper.showNotification(DischargingService.this, ID_BATTERY_INFO);
        }
    }
}
