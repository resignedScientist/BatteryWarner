package com.laudien.p1xelfehler.batterywarner.Services;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.AppInfoHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receivers.DischargingAlarmReceiver;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static com.laudien.p1xelfehler.batterywarner.AppInfoHelper.IS_PRO;

/**
 * Handles the QS tile of the app. It works only on Android 7.0 and above and
 * has the functionality to toggle all warnings and logging of the app.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class OnOffTileService extends TileService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = getClass().getSimpleName();
    private SharedPreferences sharedPreferences;
    private Tile tile;
    private boolean firstStart;

    @Override
    public void onStartListening() {
        super.onStartListening();
        Log.d(TAG, "start listening!");
        tile = getQsTile();
        if (IS_PRO) { // pro version
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            // check if the intro was finished first
            firstStart = sharedPreferences.getBoolean(getString(R.string.pref_first_start), getResources().getBoolean(R.bool.pref_first_start_default));
            boolean isEnabled = sharedPreferences.getBoolean(getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default));
            if (firstStart) {
                isEnabled = false;
            }
            // set state from shared preferences
            if (isEnabled) {
                tile.setState(STATE_ACTIVE);
            } else {
                tile.setState(STATE_INACTIVE);
            }

        } else { // free version
            tile.setState(STATE_INACTIVE);
        }
        tile.updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        Log.d(TAG, "stop listening!");
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        Log.d(TAG, "Tile clicked!");
        if (!IS_PRO) { // not pro
            Toast.makeText(getApplicationContext(), R.string.toast_not_pro, Toast.LENGTH_SHORT).show();
            return;
        }
        if (firstStart) { // intro not finished
            Toast.makeText(getApplicationContext(), getString(R.string.toast_finish_intro_first), Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isActive = tile.getState() == STATE_ACTIVE;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return;
        }
        boolean isCharging = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) != 0;

        if (isActive) { // disable battery warnings
            Log.d(TAG, "Disabling battery warnings...");
            tile.setState(STATE_INACTIVE);
            if (!isCharging) { // discharging
                DischargingAlarmReceiver.cancelDischargingAlarm(this);
            }
            Toast.makeText(getApplicationContext(), getString(R.string.toast_successfully_disabled), Toast.LENGTH_SHORT).show();
        } else { // enable battery warnings
            Log.d(TAG, "Enabling battery warnings...");
            tile.setState(STATE_ACTIVE);
            SharedPreferences temporaryPrefs = getSharedPreferences(getString(R.string.prefs_temporary), MODE_PRIVATE);
            temporaryPrefs.edit().putBoolean(getString(R.string.pref_already_notified), false).apply();
            if (isCharging) { // charging
                startService(new Intent(this, ChargingService.class));
            } else { // discharging
                sendBroadcast(new Intent(AppInfoHelper.BROADCAST_DISCHARGING_ALARM));
                startService(new Intent(this, DischargingService.class));
            }
            Toast.makeText(getApplicationContext(), getString(R.string.toast_successfully_enabled), Toast.LENGTH_SHORT).show();
        }
        sharedPreferences.edit().putBoolean(getString(R.string.pref_is_enabled), !isActive).apply();
        tile.updateTile();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_is_enabled))) {
            boolean isEnabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_is_enabled_default));
            tile.setState(isEnabled ? STATE_ACTIVE : STATE_INACTIVE);
        }
    }
}
