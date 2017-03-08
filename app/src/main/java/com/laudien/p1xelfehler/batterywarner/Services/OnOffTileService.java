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

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receivers.DischargingAlarmReceiver;

/**
 * Handles the QS tile of the app. It works only on Android 7.0 and above and
 * has the functionality to toggle all warnings and logging of the app.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class OnOffTileService extends TileService {

    private final String TAG = getClass().getSimpleName();
    private Tile tile;
    private boolean firstStart;

    @Override
    public void onStartListening() {
        super.onStartListening();
        Log.d(TAG, "start listening!");
        tile = getQsTile();
        if (Contract.IS_PRO) { // pro version
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            // check if the intro was finished first
            firstStart = sharedPreferences.getBoolean(getString(R.string.pref_first_start), getResources().getBoolean(R.bool.pref_first_start_default));
            boolean isEnabled = sharedPreferences.getBoolean(getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default));
            if (firstStart) {
                isEnabled = false;
            }
            // set state from shared preferences
            if (isEnabled) {
                tile.setState(Tile.STATE_ACTIVE);
            } else {
                tile.setState(Tile.STATE_INACTIVE);
            }

        } else { // free version
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Log.d(TAG, "Tile clicked!");
        if (!Contract.IS_PRO) { // not pro
            Toast.makeText(getApplicationContext(), getString(R.string.not_pro), Toast.LENGTH_SHORT).show();
            return;
        }
        if (firstStart) { // intro not finished
            Toast.makeText(getApplicationContext(), getString(R.string.please_finish_intro), Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isActive = tile.getState() == Tile.STATE_ACTIVE;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return;
        }
        boolean isCharging = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;

        if (isActive) { // disable battery warnings
            Log.d(TAG, "Disabling battery warnings...");
            tile.setState(Tile.STATE_INACTIVE);
            if (!isCharging) { // discharging
                DischargingAlarmReceiver.cancelDischargingAlarm(this);
            }
            Toast.makeText(getApplicationContext(), getString(R.string.disabled_info), Toast.LENGTH_SHORT).show();
        } else { // enable battery warnings
            Log.d(TAG, "Enabling battery warnings...");
            tile.setState(Tile.STATE_ACTIVE);
            sharedPreferences.edit().putBoolean(getString(R.string.pref_already_notified), false).apply();
            if (isCharging) { // charging
                startService(new Intent(this, ChargingService.class));
            } else { // discharging
                sendBroadcast(new Intent(Contract.BROADCAST_DISCHARGING_ALARM));
                startService(new Intent(this, DischargingService.class));
            }
            Toast.makeText(getApplicationContext(), getString(R.string.enabled_info), Toast.LENGTH_SHORT).show();
        }
        sharedPreferences.edit().putBoolean(getString(R.string.pref_is_enabled), !isActive).apply();
        tile.updateTile();

        sendBroadcast(new Intent(Contract.BROADCAST_ON_OFF_CHANGED));
    }
}
