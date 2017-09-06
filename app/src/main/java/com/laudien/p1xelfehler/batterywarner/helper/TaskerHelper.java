package com.laudien.p1xelfehler.batterywarner.helper;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.twofortyfouram.assertion.BundleAssertions;
import com.twofortyfouram.log.Lumberjack;

public class TaskerHelper {
    public static final String EXTRA_TOGGLE_CHARGING = "toggleCharging";

    public static boolean isBundleValid(@NonNull Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        try {
            BundleAssertions.assertHasBoolean(bundle, EXTRA_TOGGLE_CHARGING);
        } catch (AssertionError e) {
            Lumberjack.e("Bundle failed verification%s", e);
            return false;
        }
        return true;
    }
}
