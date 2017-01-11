package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Receivers.BatteryAlarmManager;

public class ChargingService extends Service {

    private final static String TAG = "ChargingService";
    private BatteryAlarmManager batteryAlarmManager;

    private BroadcastReceiver ringerModeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int ringerMode = audioManager.getRingerMode();

            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(Contract.NOTIFICATION_ID_SILENT_MODE);
            }
        }
    };

    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryAlarmManager.checkBattery(true);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started!");
        batteryAlarmManager = new BatteryAlarmManager(this);
        SharedPreferences sharedPreferences = getSharedPreferences(Contract.SHARED_PREFS, MODE_PRIVATE);
        IntentFilter batteryChangedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, batteryChangedFilter);
        if (!BatteryAlarmManager.isChargingModeEnabled(sharedPreferences, batteryStatus)) {
            stopSelf();
        } else {
            registerReceiver(ringerModeChangedReceiver,
                    new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));
            registerReceiver(batteryChangedReceiver, batteryChangedFilter);
            batteryAlarmManager.checkBattery(true); // check immediately
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service destroyed!");
        unregisterReceiver(ringerModeChangedReceiver);
        unregisterReceiver(batteryChangedReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // not used, because i won't bind it to anything!
        return null;
    }
}
