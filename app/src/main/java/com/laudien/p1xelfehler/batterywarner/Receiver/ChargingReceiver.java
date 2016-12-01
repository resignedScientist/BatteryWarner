package com.laudien.p1xelfehler.batterywarner.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;

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

        // reset graph values
        GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(context);
        dbHelper.resetTable();
        sharedPreferences.edit().putInt(Contract.PREF_GRAPH_TIME, -1)
                .putInt(Contract.PREF_LAST_PERCENTAGE, -1)
                .apply();

        BatteryAlarmReceiver.cancelExistingAlarm(context);
        if (context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE)
                .getBoolean(Contract.PREF_WARNING_HIGH_ENABLED, true))
            new BatteryAlarmReceiver().onReceive(context, intent);
    }
}
