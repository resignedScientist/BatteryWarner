package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;

/**
 * A BroadcastReceiver called by the System when the device finished booting.
 * It starts some services if necessary. Does only work after the intro was finished.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) { // correct intent action
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (!sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), context.getResources().getBoolean(R.bool.pref_first_start_default))) { // intro was finished
                // start services/receivers
                ServiceHelper.startService(context.getApplicationContext());
            }
        }
    }
}
