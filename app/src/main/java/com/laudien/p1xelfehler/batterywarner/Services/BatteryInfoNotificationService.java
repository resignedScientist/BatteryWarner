package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
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
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.N;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.ID_BATTERY_INFO;

@RequiresApi(api = N)
public class BatteryInfoNotificationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPreferences;
    private boolean dischargingServiceEnabled;
    private BatteryData batteryData;
    private BatteryManager batteryManager;

    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent batteryStatus) {
            boolean notificationEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_info_notification_enabled), context.getResources().getBoolean(R.bool.pref_info_notification_enabled_default));
            if (notificationEnabled) {
                batteryData.setTemperature(BatteryHelper.getTemperature(batteryStatus));
                batteryData.setVoltage(BatteryHelper.getVoltage(batteryStatus));
                batteryData.setCurrent(BatteryHelper.getCurrent(batteryManager));
                if (dischargingServiceEnabled) {
                    batteryData.setScreenOn(BatteryHelper.getScreenOn(context, sharedPreferences));
                    batteryData.setScreenOff(BatteryHelper.getScreenOff(context, sharedPreferences));
                }
                NotificationHelper.showBatteryInfoNotification(context, sharedPreferences, batteryData);
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
        batteryData = new BatteryData();
        batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        dischargingServiceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        registerReceiver(batteryChangedReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(getClass().getSimpleName(), "Service destroyed!");
        super.onDestroy();
        unregisterReceiver(batteryChangedReceiver);
        NotificationHelper.cancelNotification(this, ID_BATTERY_INFO);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        this.sharedPreferences = sharedPreferences;
        if (s.equals(getString(R.string.pref_discharging_service_enabled))) {
            dischargingServiceEnabled = sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            NotificationHelper.showBatteryInfoNotification(BatteryInfoNotificationService.this, sharedPreferences, batteryData);
            if (!dischargingServiceEnabled) {
                stopSelf();
            }
        }
    }
}
