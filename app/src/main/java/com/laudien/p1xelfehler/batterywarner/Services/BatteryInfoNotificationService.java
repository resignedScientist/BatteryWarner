package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper;
import com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData;
import com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.os.BatteryManager.EXTRA_HEALTH;
import static android.os.BatteryManager.EXTRA_LEVEL;
import static android.os.BatteryManager.EXTRA_TECHNOLOGY;
import static android.os.Build.VERSION_CODES.N;
import static com.laudien.p1xelfehler.batterywarner.Contract.NO_STATE;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.ID_BATTERY_INFO;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.cancelNotification;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.showBatteryInfoNotification;

@RequiresApi(api = N)
public class BatteryInfoNotificationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPreferences;
    private boolean dischargingServiceEnabled, notificationEnabled, technologyEnabled, temperatureEnabled,
            healthEnabled, batteryLevelEnabled, voltageEnabled, currentEnabled, screenOnEnabled, screenOffEnabled;
    private BatteryData batteryData;
    private BatteryManager batteryManager;
    private Intent batteryStatus;

    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent batteryStatus) {
            if (notificationEnabled) {
                BatteryInfoNotificationService.this.batteryStatus = batteryStatus;
                if (technologyEnabled)
                    batteryData.setTechnology(batteryStatus.getStringExtra(EXTRA_TECHNOLOGY));
                if (temperatureEnabled)
                    batteryData.setTemperature(BatteryHelper.getTemperature(batteryStatus));
                if (healthEnabled)
                    batteryData.setHealth(batteryStatus.getIntExtra(EXTRA_HEALTH, NO_STATE));
                if (batteryLevelEnabled)
                    batteryData.setBatteryLevel(batteryStatus.getIntExtra(EXTRA_LEVEL, NO_STATE));
                if (voltageEnabled)
                    batteryData.setVoltage(BatteryHelper.getVoltage(batteryStatus));
                if (currentEnabled) {
                    if (batteryManager == null){
                        batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
                    }
                    batteryData.setCurrent(BatteryHelper.getCurrent(batteryManager));
                }
                if (dischargingServiceEnabled) {
                    if (screenOnEnabled) {
                        batteryData.setScreenOn(BatteryHelper.getScreenOn(context, sharedPreferences));
                    }
                    if (screenOffEnabled) {
                        batteryData.setScreenOff(BatteryHelper.getScreenOff(context, sharedPreferences));
                    }
                }
                showBatteryInfoNotification(context, sharedPreferences, batteryData);
            } else {
                stopSelf();
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(getClass().getSimpleName(), "Service started!");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        notificationEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_notification_enabled), getResources().getBoolean(R.bool.pref_info_notification_enabled_default));
        if (notificationEnabled) {
            batteryData = new BatteryData();
            dischargingServiceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            technologyEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_technology), getResources().getBoolean(R.bool.pref_info_technology_default));
            temperatureEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_temperature), getResources().getBoolean(R.bool.pref_info_temperature_default));
            healthEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_health), getResources().getBoolean(R.bool.pref_info_health_default));
            batteryLevelEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_battery_level), getResources().getBoolean(R.bool.pref_info_battery_level_default));
            voltageEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_health), getResources().getBoolean(R.bool.pref_info_voltage_default));
            currentEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_current), getResources().getBoolean(R.bool.pref_info_current_default));
            screenOnEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_screen_on), getResources().getBoolean(R.bool.pref_info_screen_on_default));
            screenOffEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_screen_off), getResources().getBoolean(R.bool.pref_info_screen_off_default));
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            registerReceiver(batteryChangedReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
        } else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(getClass().getSimpleName(), "Service destroyed!");
        super.onDestroy();
        try {
            unregisterReceiver(batteryChangedReceiver);
        } catch (Exception ignored){}
        NotificationHelper.cancelNotification(this, ID_BATTERY_INFO);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        this.sharedPreferences = sharedPreferences;
        if (s.equals(getString(R.string.pref_discharging_service_enabled))) {
            dischargingServiceEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            if (dischargingServiceEnabled){
                if (screenOnEnabled)
                    batteryData.setScreenOn(BatteryHelper.getScreenOn(this, sharedPreferences));
                if (screenOffEnabled)
                    batteryData.setScreenOff(BatteryHelper.getScreenOff(this, sharedPreferences));
            }
            rebuildNotification(dischargingServiceEnabled);
        } else if (s.equals(getString(R.string.pref_info_technology))){
            technologyEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_technology_default));
            if (technologyEnabled) {
                batteryData.setTechnology(batteryStatus.getStringExtra(EXTRA_TECHNOLOGY));
            }
            rebuildNotification(technologyEnabled);
        } else if (s.equals(getString(R.string.pref_info_temperature))){
            temperatureEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_temperature_default));
            if (temperatureEnabled){
                batteryData.setTemperature(BatteryHelper.getTemperature(batteryStatus));
            }
            rebuildNotification(temperatureEnabled);
        } else if (s.equals(getString(R.string.pref_info_health))) {
            healthEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_health_default));
            if (healthEnabled){
                batteryData.setHealth(batteryStatus.getIntExtra(EXTRA_HEALTH, NO_STATE));
            }
            rebuildNotification(healthEnabled);
        } else if(s.equals(getString(R.string.pref_info_battery_level))){
            batteryLevelEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_battery_level_default));
            if (batteryLevelEnabled){
                batteryData.setBatteryLevel(batteryStatus.getIntExtra(EXTRA_LEVEL, NO_STATE));
            }
            rebuildNotification(batteryLevelEnabled);
        } else if (s.equals(getString(R.string.pref_info_voltage))){
            voltageEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_voltage_default));
            if (voltageEnabled){
                batteryData.setVoltage(BatteryHelper.getVoltage(batteryStatus));
            }
            rebuildNotification(voltageEnabled);
        } else if (s.equals(getString(R.string.pref_info_current))){
            currentEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_current_default));
            if (currentEnabled){
                if (batteryManager == null){
                    batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
                }
                batteryData.setCurrent(BatteryHelper.getCurrent(batteryManager));
            }
            rebuildNotification(currentEnabled);
        } else if (s.equals(getString(R.string.pref_info_screen_on))){
            screenOnEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_screen_on_default));
            if (screenOnEnabled && dischargingServiceEnabled){
                batteryData.setScreenOn(BatteryHelper.getScreenOn(this, sharedPreferences));
            }
            rebuildNotification(screenOnEnabled);
        } else if (s.equals(getString(R.string.pref_info_screen_off))){
            screenOffEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_info_screen_off_default));
            if (screenOffEnabled && dischargingServiceEnabled){
                batteryData.setScreenOff(BatteryHelper.getScreenOff(this, sharedPreferences));
            }
            rebuildNotification(screenOffEnabled);
        }
    }

    private void rebuildNotification(boolean cancelBefore){
        if (cancelBefore){
            cancelNotification(this, ID_BATTERY_INFO);
        }
        showBatteryInfoNotification(this, sharedPreferences, batteryData);
    }
}
