package com.laudien.p1xelfehler.batterywarner.tiles;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;

import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;

@RequiresApi(api = Build.VERSION_CODES.N)
public abstract class PreferenceTileService extends TileService {
    private Tile tile;
    private SharedPreferences sharedPreferences;
    private String key;

    @Override
    public void onStartListening() {
        super.onStartListening();
        tile = getQsTile();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        key = getString(getPreferenceKeyStringResource());
        boolean defaultBool = getResources().getBoolean(getPreferenceDefaultBoolResource());
        boolean enabled = sharedPreferences.getBoolean(key, defaultBool);
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();

        // check for root
        if (requiresRoot()) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    if (!RootHelper.isRootAvailable()) {
                        try {
                            tile.setState(Tile.STATE_UNAVAILABLE);
                            tile.updateTile();
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean isActive = tile.getState() == Tile.STATE_ACTIVE;
        sharedPreferences.edit().putBoolean(key, !isActive).apply();
        tile.setState(isActive ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    protected abstract int getPreferenceKeyStringResource();

    protected abstract int getPreferenceDefaultBoolResource();

    protected abstract boolean requiresRoot();
}
