package com.laudien.p1xelfehler.batterywarner.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.R;

/**
 * A Service started by the app which disables all the root features.
 * It shows a toast to notify the user about it and stops itself after it finished.
 */
public class DisableRootFeaturesService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, getString(R.string.toast_root_denied),
                Toast.LENGTH_LONG).show();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit()
                .putBoolean(getString(R.string.pref_stop_charging), false)
                .putBoolean(getString(R.string.pref_usb_charging_disabled), false)
                .putBoolean(getString(R.string.pref_smart_charging_enabled), false)
                .apply();
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
