package com.laudien.p1xelfehler.batterywarner.Services;

import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ToggleChargingTileService extends TileService {
    private Tile tile;

    @Override
    public void onStartListening() {
        super.onStartListening();
        tile = getQsTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean isActive = tile.getState() == STATE_ACTIVE;
        if (isActive) { // deactivating the tile
            tile.setState(STATE_INACTIVE);
        } else { // activating the tile
            tile.setState(STATE_ACTIVE);
        }
        tile.updateTile();
    }
}
