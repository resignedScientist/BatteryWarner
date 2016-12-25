package com.laudien.p1xelfehler.batterywarner.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;

import java.util.Calendar;

import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class ChargingReceiver extends BroadcastReceiver {
    private static final String TAG = "ChargingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED")) return;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(Contract.PREF_FIRST_START, true))
            return; // return if intro was not finished

        Log.i(TAG, "User started charging!");

        BatteryAlarmManager.cancelExistingAlarm(context); // cancel alarm

        // check for the correct charging method first
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            int chargingType = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE);
            switch (chargingType) {
                case android.os.BatteryManager.BATTERY_PLUGGED_AC: // ac charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_AC_ENABLED, true)) return;
                    break;
                case android.os.BatteryManager.BATTERY_PLUGGED_USB: // usb charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_USB_ENABLED, true)) return;
                    break;
                case android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS: // wireless charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_WIRELESS_ENABLED, true)) return;
                    break;
            }
        }

        // reset graph values + already notified
        if(sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true)) {
            GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(context);
            dbHelper.resetTable();
            sharedPreferences.edit().putLong(Contract.PREF_GRAPH_TIME, Calendar.getInstance().getTimeInMillis())
                    .putInt(Contract.PREF_LAST_PERCENTAGE, -1)
                    .putBoolean(Contract.PREF_ALREADY_NOTIFIED, false)
                    .apply();
        }

        // start new alarm
        if (context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE)
                .getBoolean(Contract.PREF_WARNING_HIGH_ENABLED, true))
            new BatteryAlarmManager(context).checkBattery(true);
    }
}
