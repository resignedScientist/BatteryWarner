package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;

public class BothAppsInstalledReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Contract.BROADCAST_BOTH_APPS_INSTALLED)
                && Contract.IS_PRO) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(
                            context.getString(R.string.pref_is_enabled),
                            false
                    ).apply();
        }
    }
}
