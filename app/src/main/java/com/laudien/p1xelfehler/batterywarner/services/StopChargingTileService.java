package com.laudien.p1xelfehler.batterywarner.services;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.AppInfoHelper.IS_PRO;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_NOT_ROOTED;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_STOP_CHARGING_NOT_WORKING;

@RequiresApi(api = Build.VERSION_CODES.N)
public class StopChargingTileService extends TileService {

    private final byte RESULT_NOT_ROOTED = 1;
    private final byte RESULT_STOP_CHARGING_NOT_WORKING = 2;
    private final byte RESULT_OK = 3;
    private Tile tile;
    private SharedPreferences sharedPreferences;

    @Override
    public void onStartListening() {
        super.onStartListening();
        tile = getQsTile();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean stopChargingEnabled = sharedPreferences.getBoolean(getString(R.string.pref_stop_charging), getResources().getBoolean(R.bool.pref_stop_charging_default));
        if (stopChargingEnabled) {
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onClick() {
        super.onClick();
        boolean isActive = tile.getState() == Tile.STATE_ACTIVE;
        if (isActive) { // deactivating the tile
            tile.setState(Tile.STATE_INACTIVE);
            sharedPreferences.edit()
                    .putBoolean(getString(R.string.pref_stop_charging), false)
                    .apply();
        } else { // activating the tile
            if (IS_PRO) {
                tile.setState(Tile.STATE_INACTIVE);
                new AsyncTask<Void, Void, Byte>() {
                    @Override
                    protected Byte doInBackground(Void... voids) {
                        try {
                            RootHelper.isChargingEnabled();
                            sharedPreferences.edit()
                                    .putBoolean(getString(R.string.pref_stop_charging), true)
                                    .apply();
                            return RESULT_OK;
                        } catch (RootHelper.NotRootedException e) {
                            return RESULT_NOT_ROOTED;
                        } catch (RootHelper.NoBatteryFileFoundException e) {
                            NotificationHelper.showNotification(StopChargingTileService.this, ID_STOP_CHARGING_NOT_WORKING);
                            return RESULT_STOP_CHARGING_NOT_WORKING;
                        }
                    }

                    @Override
                    protected void onPostExecute(Byte b) {
                        super.onPostExecute(b);
                        if (b == RESULT_NOT_ROOTED || b == RESULT_STOP_CHARGING_NOT_WORKING) {
                            tile.setState(Tile.STATE_INACTIVE);
                            tile.updateTile();
                            int notificationId = b == RESULT_NOT_ROOTED ? ID_NOT_ROOTED : ID_STOP_CHARGING_NOT_WORKING;
                            NotificationHelper.showNotification(StopChargingTileService.this, notificationId);
                        }
                    }
                }.execute();
            } else {
                ToastHelper.sendToast(getApplicationContext(), R.string.toast_not_pro, LENGTH_SHORT);
            }
        }
        tile.updateTile();
    }
}
