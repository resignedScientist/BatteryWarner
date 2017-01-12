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

import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;

public class ChargingService extends Service {

    // private final static String TAG = "ChargingService";
    private BatteryAlarmManager batteryAlarmManager;

    private BroadcastReceiver ringerModeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int ringerMode = audioManager.getRingerMode();

            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(NotificationBuilder.NOTIFICATION_ID_SILENT_MODE);
            }
        }
    };

    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent batteryStatus) {
            SharedPreferences sharedPreferences = getSharedPreferences(Contract.SHARED_PREFS, MODE_PRIVATE);
            if (!BatteryAlarmManager.isChargingNotificationEnabled(sharedPreferences, batteryStatus)) {
                stopSelf();
                return;
            }
            batteryAlarmManager.checkAndNotify(context, batteryStatus);
            batteryAlarmManager.logInDatabase(context);
            int batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, Contract.NO_STATE);
            if (batteryLevel == 100) {
                stopSelf(); // stop service if battery is full
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        batteryAlarmManager = BatteryAlarmManager.getInstance(this);
        registerReceiver(
                ringerModeChangedReceiver,
                new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        );
        registerReceiver(
                batteryChangedReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        );
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
