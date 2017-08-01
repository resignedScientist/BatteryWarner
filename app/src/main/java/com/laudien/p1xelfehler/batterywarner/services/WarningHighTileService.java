package com.laudien.p1xelfehler.batterywarner.services;

import com.laudien.p1xelfehler.batterywarner.R;

public class WarningHighTileService extends PreferenceTileService {
    @Override
    protected int getPreferenceKeyStringResource() {
        return R.string.pref_warning_high_enabled;
    }

    @Override
    protected int getPreferenceDefaultBoolResource() {
        return R.bool.pref_warning_high_enabled_default;
    }

    @Override
    protected boolean requiresRoot() {
        return false;
    }
}
