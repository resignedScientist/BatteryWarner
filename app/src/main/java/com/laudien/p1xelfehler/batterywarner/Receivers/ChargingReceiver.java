package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;

import java.util.Calendar;

import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class ChargingReceiver extends BroadcastReceiver {
    //private static final String TAG = "ChargingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED")) return;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(Contract.PREF_FIRST_START, true))
            return; // return if intro was not finished

        //Log.i(TAG, "User started charging!");

        BatteryAlarmManager.cancelExistingAlarm(context); // cancel alarm

        // send broadcast
        Intent databaseIntent = new Intent();
        databaseIntent.setAction(Contract.BROADCAST_STATUS_CHANGED);
        context.sendBroadcast(databaseIntent);

        // reset already notified
        sharedPreferences.edit().putBoolean(Contract.PREF_ALREADY_NOTIFIED, false).apply();

        // check for the correct charging method
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (!BatteryAlarmManager.isChargingModeEnabled(sharedPreferences, batteryStatus))
            return; // return if current charging type is disabled

        // reset graph values if graph is enabled
        if (sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true)) {
            GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(context);
            dbHelper.resetTable();
            sharedPreferences.edit().putLong(Contract.PREF_GRAPH_TIME, Calendar.getInstance().getTimeInMillis())
                    .putInt(Contract.PREF_LAST_PERCENTAGE, -1)
                    .apply();
        }

        // start new alarm
        new BatteryAlarmManager(context).checkBattery(true);
    }
}
