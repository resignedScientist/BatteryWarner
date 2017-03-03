package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Calendar;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static com.laudien.p1xelfehler.batterywarner.Contract.NO_STATE;

public class DischargingService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = "DischargingService";
    private SharedPreferences sharedPreferences;
    private long screenOnTime, screenOffTime;
    private long timeChanged = Calendar.getInstance().getTimeInMillis(); // time point when screen on/off was changed
    private int lastPercentage = -1, // that is a different value as in sharedPreferences!!!
            screenOnDrain, screenOffDrain;
    private boolean isScreenOn;
    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "screen on received!");
            isScreenOn = true;
            long timeNow = Calendar.getInstance().getTimeInMillis();
            long timeDifference = timeNow - timeChanged;
            timeChanged = timeNow;
            sharedPreferences.edit().putLong(getString(R.string.value_time_screen_off), screenOffTime + timeDifference).apply();
        }
    };
    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "screen off received!");
            isScreenOn = false;
            long timeNow = Calendar.getInstance().getTimeInMillis();
            long timeDifference = timeNow - timeChanged;
            timeChanged = timeNow;
            sharedPreferences.edit().putLong(getString(R.string.value_time_screen_on), screenOnTime + timeDifference).apply();
        }
    };
    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent batteryStatus) {
            boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE) != 0;
            if (!isCharging) { // discharging
                int batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, NO_STATE);
                if (lastPercentage == -1) { // first value -> take current percent
                    lastPercentage = batteryLevel;
                } else { // not the first value
                    if (batteryLevel != lastPercentage) {
                        int diff = lastPercentage - batteryLevel;
                        lastPercentage = batteryLevel;
                        if (isScreenOn) { // screen is on
                            sharedPreferences.edit().putInt(getString(R.string.value_drain_screen_on), screenOnDrain + diff).apply();
                        } else { // screen is off
                            sharedPreferences.edit().putInt(getString(R.string.value_drain_screen_off), screenOffDrain + diff).apply();
                        }
                    }
                }
            } else { // charging
                stopSelf();
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started!");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean serviceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
        boolean isEnabled = sharedPreferences.getBoolean(getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default));
        if (isEnabled && serviceEnabled) {
            screenOnDrain = sharedPreferences.getInt(getString(R.string.value_drain_screen_on), 0);
            screenOffDrain = sharedPreferences.getInt(getString(R.string.value_drain_screen_off), 0);
            screenOnTime = sharedPreferences.getLong(getString(R.string.value_time_screen_on), 0);
            screenOffTime = sharedPreferences.getLong(getString(R.string.value_time_screen_off), 0);
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            sharedPreferences.edit()
                    .putLong(getString(R.string.value_time_screen_off), screenOffTime)
                    .putLong(getString(R.string.value_time_screen_on), screenOnTime)
                    .apply();
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // lollipop and higher
                isScreenOn = powerManager.isInteractive();
            } else { // kitKat
                isScreenOn = powerManager.isScreenOn();
            }
            registerReceiver(screenOnReceiver, new IntentFilter(ACTION_SCREEN_ON));
            registerReceiver(screenOffReceiver, new IntentFilter(ACTION_SCREEN_OFF));
            registerReceiver(batteryChangedReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
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
        unregisterReceiver(batteryChangedReceiver);
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
        } else if (s.equals(getString(R.string.value_drain_screen_on))) {
            screenOnDrain = sharedPreferences.getInt(s, screenOnDrain);
        } else if (s.equals(getString(R.string.value_drain_screen_off))) {
            screenOffDrain = sharedPreferences.getInt(s, screenOffDrain);
        } else if (s.equals(getString(R.string.value_time_screen_on))) {
            screenOnTime = sharedPreferences.getLong(s, screenOnTime);
        } else if (s.equals(getString(R.string.value_time_screen_off))) {
            screenOffTime = sharedPreferences.getLong(s, screenOffTime);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
