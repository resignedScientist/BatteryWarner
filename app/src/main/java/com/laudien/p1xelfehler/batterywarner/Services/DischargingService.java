package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Calendar;

public class DischargingService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPreferences;
    private long screenOnTime, screenOffTime;
    private long timeChanged; // time point when screen on/off was changed
    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long timeNow = Calendar.getInstance().getTimeInMillis();
            long timeDifference = timeNow - timeChanged;
            timeChanged = timeNow;
            screenOffTime += timeDifference;
        }
    };
    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long timeNow = Calendar.getInstance().getTimeInMillis();
            long timeDifference = timeNow - timeChanged;
            timeChanged = timeNow;
            screenOnTime += timeDifference;
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        screenOnTime = sharedPreferences.getLong(getString(R.string.pref_time_screen_on), getResources().getInteger(R.integer.pref_time_screen_on_default));
        screenOffTime = sharedPreferences.getLong(getString(R.string.pref_time_screen_off), getResources().getInteger(R.integer.pref_time_screen_off_default));
        boolean serviceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
        if (serviceEnabled) {
            registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
            registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        } else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenOnReceiver);
        unregisterReceiver(screenOffReceiver);
        sharedPreferences.edit()
                .putLong(getString(R.string.pref_time_screen_on), screenOnTime)
                .putLong(getString(R.string.pref_time_screen_off), screenOffTime)
                .apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(getString(R.string.pref_discharging_service_enabled))
                && !sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_discharging_service_enabled_default))) {
            stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
