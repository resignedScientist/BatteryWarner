package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.R;

import static com.laudien.p1xelfehler.batterywarner.AppInfoHelper.IS_PRO;

/**
 * A BroadcastReceiver that called by the app if both apps (free and pro version) are installed.
 * It disables all functionality of the app if this is the free version.
 */
public class BothAppsInstalledReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!IS_PRO) {
            Log.d(getClass().getSimpleName(), "Broadcast received! Disabling app via main switch...");
            // change to disabled in shared preferences
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.pref_is_enabled), false).apply();
        }
    }
}
