package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;

/**
 * A BroadcastReceiver called by the System when the device finished booting.
 * It starts some services if necessary. Does only work after the intro was finished.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || !action.equals("android.intent.action.BOOT_COMPLETED")) {
            return;
        }
        // check if intro was finished
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), true)) {
            return;
        }
        // start services/receivers
        Intent backgroundServiceResetIntent = new Intent(context.getApplicationContext(), BackgroundService.class);
        backgroundServiceResetIntent.setAction(BackgroundService.ACTION_RESET_ALL);
        ServiceHelper.startService(context.getApplicationContext(), backgroundServiceResetIntent);
    }
}
