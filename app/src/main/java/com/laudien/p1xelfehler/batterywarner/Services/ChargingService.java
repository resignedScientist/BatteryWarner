package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Calendar;

public class ChargingService extends Service {

    private final static String TAG = "ChargingService";
    private BatteryAlarmManager batteryAlarmManager;
    private SharedPreferences sharedPreferences;

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
            if (sharedPreferences == null) {
                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            }
            boolean graphEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));
            if (!batteryAlarmManager.isChargingNotificationEnabled(context, batteryStatus)) {
                stopSelf();
                return;
            }
            batteryAlarmManager.checkAndNotify(context, batteryStatus);
            if (graphEnabled) {
                batteryAlarmManager.logInDatabase(context);
            }
            if (batteryAlarmManager.getBatteryLevel() == 100) {
                stopSelf(); // stop service if battery is full
            }
        }
    };

    public static void startService(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean graphEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));
        if (graphEnabled) {
            GraphDbHelper dbHelper = GraphDbHelper.getInstance(context);
            dbHelper.resetTable();
            sharedPreferences.edit()
                    .putLong(context.getString(R.string.pref_graph_time), Calendar.getInstance().getTimeInMillis())
                    .putInt(context.getString(R.string.pref_last_percentage), -1)
                    .apply();
        }
        if (BatteryAlarmManager.isCharging(BatteryAlarmManager.getBatteryStatus(context))) {
            context.startService(new Intent(context, ChargingService.class));
        } else {
            GraphFragment.notify(context);
        }
    }

    public static void stopService(Context context) {
        context.stopService(new Intent(context, ChargingService.class));
    }

    public static void restartService(Context context) {
        stopService(context);
        startService(context);
    }

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

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }
}
