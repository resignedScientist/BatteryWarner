package com.laudien.p1xelfehler.batterywarner.services;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.AppInfoHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import static android.widget.Toast.LENGTH_SHORT;

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
        if (AppInfoHelper.isPro()) { // pro version
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
        if (!AppInfoHelper.isPro()) { // not pro
            ToastHelper.sendToast(getApplicationContext(), R.string.toast_not_pro, LENGTH_SHORT);
            return;
        }
        if (firstStart) { // intro not finished
            ToastHelper.sendToast(getApplicationContext(), R.string.toast_finish_intro_first, LENGTH_SHORT);
            return;
        }
        boolean isActive = tile.getState() == Tile.STATE_ACTIVE;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (isActive) { // disable battery warnings
            Log.d(TAG, "Disabling battery warnings...");
            tile.setState(Tile.STATE_INACTIVE);
            ToastHelper.sendToast(getApplicationContext(), R.string.toast_successfully_disabled, LENGTH_SHORT);
        } else { // enable battery warnings
            Log.d(TAG, "Enabling battery warnings...");
            SharedPreferences temporaryPrefs = getSharedPreferences(getString(R.string.prefs_temporary), MODE_PRIVATE);
            temporaryPrefs.edit().putBoolean(getString(R.string.pref_already_notified), false).apply();
            ServiceHelper.startService(this, sharedPreferences, ServiceHelper.ID_CHARGING);
            ServiceHelper.startService(this, sharedPreferences, ServiceHelper.ID_DISCHARGING);
            ToastHelper.sendToast(getApplicationContext(), R.string.toast_successfully_enabled, LENGTH_SHORT);
        }
        sharedPreferences.edit().putBoolean(getString(R.string.pref_is_enabled), !isActive).apply();
        tile.updateTile();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_is_enabled))) {
            boolean isEnabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_is_enabled_default));
            tile.setState(isEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        }
    }
}
