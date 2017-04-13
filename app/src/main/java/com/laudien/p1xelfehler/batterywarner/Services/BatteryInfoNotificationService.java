package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper;
import com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData;
import com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.ID_BATTERY_INFO;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.cancelNotification;

public class BatteryInfoNotificationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = getClass().getSimpleName();
    private SharedPreferences sharedPreferences;
    private BatteryData batteryData;
    private BroadcastReceiver batteryChangedReceiver, screenOnReceiver, screenOffReceiver;
    private BatteryData.OnBatteryValueChangedListener onBatteryValueChangedListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started!");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean notificationEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_notification_enabled), getResources().getBoolean(R.bool.pref_info_notification_enabled_default));
        if (notificationEnabled) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            onBatteryValueChangedListener = new BatteryData.OnBatteryValueChangedListener() {
                @Override
                public void onBatteryValueChanged(int index) {
                    Log.d(TAG, "batteryData was updated! index: " + index);
                    NotificationHelper.showNotification(BatteryInfoNotificationService.this, ID_BATTERY_INFO);
                }
            };
            batteryChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent batteryStatus) {
                    batteryData.update(batteryStatus, context, sharedPreferences);
                }
            };
            screenOnReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "listening resumed (screen on)");
                    batteryData.addOnBatteryValueChangedListener(onBatteryValueChangedListener);
                    registerReceiver(batteryChangedReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
                }
            };
            screenOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "listening paused (screen off)");
                    unregisterReceiver(batteryChangedReceiver);
                    batteryData.unregisterOnBatteryValueChangedListener(onBatteryValueChangedListener);
                }
            };
            Intent batteryStatus = registerReceiver(batteryChangedReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
            batteryData = BatteryHelper.getBatteryData(batteryStatus, this, sharedPreferences);
            batteryData.addOnBatteryValueChangedListener(onBatteryValueChangedListener);
            registerReceiver(screenOnReceiver, new IntentFilter(ACTION_SCREEN_ON));
            registerReceiver(screenOffReceiver, new IntentFilter(ACTION_SCREEN_OFF));
            NotificationHelper.showNotification(this, ID_BATTERY_INFO);
        } else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed!");
        super.onDestroy();
        try {
            unregisterReceiver(batteryChangedReceiver);
            unregisterReceiver(screenOnReceiver);
            unregisterReceiver(screenOffReceiver);
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        } catch (Exception ignored) {
        }
        if (batteryData != null) {
            batteryData.unregisterOnBatteryValueChangedListener(onBatteryValueChangedListener);
        }
        NotificationHelper.cancelNotification(this, ID_BATTERY_INFO);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        this.sharedPreferences = sharedPreferences;
        if (s.equals(getString(R.string.pref_info_notification_enabled))) {
            if (!sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_notification_enabled_default))) {
                stopSelf();
            }
        } else if (s.equals(getString(R.string.pref_discharging_service_enabled))) {
            boolean dischargingServiceEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            rebuildNotification(dischargingServiceEnabled);
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
        }
    }

    private void rebuildNotification(boolean cancelBefore) {
        if (cancelBefore) {
            cancelNotification(this, ID_BATTERY_INFO);
        }
        NotificationHelper.showNotification(this, ID_BATTERY_INFO);
    }
}
