package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.AppInfoHelper;
import com.laudien.p1xelfehler.batterywarner.R;

/**
 * A BroadcastReceiver that called by the app if both apps (free and pro version) are installed.
 * It disables all functionality of the app if this is the free version.
 */
public class BothAppsInstalledReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(AppInfoHelper.BROADCAST_BOTH_APPS_INSTALLED)
                && !AppInfoHelper.IS_PRO) {
            // change to disabled in shared preferences
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.pref_is_enabled), false).apply();
        }
    }
}
