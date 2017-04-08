package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper;
import com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static com.laudien.p1xelfehler.batterywarner.Contract.NO_STATE;

@RequiresApi(api = Build.VERSION_CODES.N)
public class BatteryInfoNotificationService extends Service{

    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent batteryStatus) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            NotificationHelper.showBatteryInfoNotification(context,
                    BatteryHelper.getBatteryData(context, sharedPreferences, batteryStatus));
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
        registerReceiver(batteryChangedReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(getClass().getSimpleName(), "Service destroyed!");
        super.onDestroy();
        unregisterReceiver(batteryChangedReceiver);
    }
}
