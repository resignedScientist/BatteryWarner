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
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_HEALTH;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_SCREEN_OFF;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_SCREEN_ON;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_TECHNOLOGY;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData.INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.ID_BATTERY_INFO;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.cancelNotification;

public class BatteryInfoNotificationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = getClass().getSimpleName();
    private boolean dischargingServiceEnabled;
    private SharedPreferences sharedPreferences;
    private BatteryData batteryData;
    private Intent lastBatteryStatus;
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
            dischargingServiceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            onBatteryValueChangedListener = new BatteryData.OnBatteryValueChangedListener() {
                @Override
                public void onBatteryValueChanged(int index) {
                    switch (index){
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
                    Log.d(TAG, "batteryData was updated! index: " + index);
                    NotificationHelper.showNotification(BatteryInfoNotificationService.this, ID_BATTERY_INFO);
                }
            };
            batteryChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent batteryStatus) {
                    lastBatteryStatus = batteryStatus;
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
            dischargingServiceEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
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

    private void rebuildNotification(boolean updateData) {
        if (updateData) {
            batteryData.update(lastBatteryStatus, this, sharedPreferences);
        }
        NotificationHelper.showNotification(this, ID_BATTERY_INFO);
    }
}
