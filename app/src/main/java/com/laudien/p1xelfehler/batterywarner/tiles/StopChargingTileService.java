package com.laudien.p1xelfehler.batterywarner.tiles;

import android.os.Build;
import android.support.annotation.RequiresApi;

import com.laudien.p1xelfehler.batterywarner.R;

@RequiresApi(api = Build.VERSION_CODES.N)
public class StopChargingTileService extends PreferenceTileService {

    @Override
    protected int getPreferenceKeyStringResource() {
        return R.string.pref_stop_charging;
    }

    @Override
    protected int getPreferenceDefaultBoolResource() {
        return R.bool.pref_stop_charging_default;
    }

    @Override
    protected boolean requiresRoot() {
        return true;
    }
}
