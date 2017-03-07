package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.R;

public class DisableRootFeaturesReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, context.getString(R.string.toast_root_denied),
                Toast.LENGTH_LONG).show();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_stop_charging), false)
                .putBoolean(context.getString(R.string.pref_usb_charging_disabled), false)
                .apply();
    }
}
