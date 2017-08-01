package com.laudien.p1xelfehler.batterywarner.services;

import com.laudien.p1xelfehler.batterywarner.R;

public class WarningLowTileService extends PreferenceTileService {
    @Override
    protected int getPreferenceKeyStringResource() {
        return R.string.pref_warning_low_enabled;
    }

    @Override
    protected int getPreferenceDefaultBoolResource() {
        return R.bool.pref_warning_low_enabled_default;
    }

    @Override
    protected boolean requiresRoot() {
        return false;
    }
}
