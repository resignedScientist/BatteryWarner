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
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Calendar;

public class DischargingService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = "DischargingService";
    private SharedPreferences sharedPreferences;
    private long screenOnTime = 0, screenOffTime = 0;
    private long timeChanged = Calendar.getInstance().getTimeInMillis(); // time point when screen on/off was changed
    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "screen on received!");
            long timeNow = Calendar.getInstance().getTimeInMillis();
            long timeDifference = timeNow - timeChanged;
            timeChanged = timeNow;
            screenOffTime += timeDifference;
            sharedPreferences.edit()
                    .putLong(getString(R.string.pref_time_screen_off), screenOffTime)
                    .apply();
        }
    };
    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "screen off received!");
            long timeNow = Calendar.getInstance().getTimeInMillis();
            long timeDifference = timeNow - timeChanged;
            timeChanged = timeNow;
            screenOnTime += timeDifference;
            sharedPreferences.edit()
                    .putLong(getString(R.string.pref_time_screen_on), screenOnTime)
                    .apply();
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started!");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean serviceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
        boolean isEnabled = sharedPreferences.getBoolean(getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default));
        if (isEnabled && serviceEnabled) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            sharedPreferences.edit()
                    .putLong(getString(R.string.pref_time_screen_off), screenOffTime)
                    .putLong(getString(R.string.pref_time_screen_on), screenOnTime)
                    .apply();
            registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
            registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        } else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed!");
        super.onDestroy();
        unregisterReceiver(screenOnReceiver);
        unregisterReceiver(screenOffReceiver);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(getString(R.string.pref_discharging_service_enabled))
                && !sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_discharging_service_enabled_default))) {
            stopSelf();
        } else if (s.equals(getString(R.string.pref_is_enabled))
                && !sharedPreferences.getBoolean(s, getResources().getBoolean(R.bool.pref_is_enabled_default))) {
            stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
