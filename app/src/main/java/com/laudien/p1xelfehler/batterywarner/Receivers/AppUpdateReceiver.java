package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

public class AppUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "AppUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), context.getResources().getBoolean(R.bool.pref_first_start_default)))
            return; // return if intro was not finished

        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return;
        }
        boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
        if (isCharging) { // charging
            ChargingService.startService(context);
        } else { // discharging
            DischargingAlarmReceiver.cancelDischargingAlarm(context);
            context.sendBroadcast(new Intent(Contract.BROADCAST_DISCHARGING_ALARM));
        }

        // show notification if not rooted anymore
        NotificationBuilder.showNotification(context, NotificationBuilder.ID_GRANT_ROOT);

        Log.d(TAG, "The app was updated!");
    }
}
