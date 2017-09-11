package com.laudien.p1xelfehler.batterywarner.helper;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.twofortyfouram.assertion.BundleAssertions;
import com.twofortyfouram.log.Lumberjack;

public class TaskerHelper {
    public static final int ACTION_TOGGLE_CHARGING = 0;
    public static final int ACTION_TOGGLE_STOP_CHARGING = 1;
    public static final int ACTION_TOGGLE_SMART_CHARGING = 2;
    public static final int ACTION_TOGGLE_WARNING_HIGH = 3;
    public static final int ACTION_TOGGLE_WARNING_LOW = 4;
    public static final int ACTION_SET_WARNING_HIGH = 5;
    public static final int ACTION_SET_WARNING_LOW = 6;
    public static final int ACTION_SET_SMART_CHARGING_LIMIT = 7;
    public static final int ACTION_SET_SMART_CHARGING_TIME = 8;
    public static final int ACTION_SAVE_GRAPH = 9;
    public static final int ACTION_RESET_GRAPH = 10;
    private static final String EXTRA_VALUE = "toggleCharging"; // leave it like this due to backwards compatibility!
    private static final String EXTRA_ACTION = "action";

    public static boolean isBundleValid(Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        try {
            int action = getAction(bundle);
            switch (action) {
                case ACTION_TOGGLE_CHARGING:
                case ACTION_TOGGLE_STOP_CHARGING:
                case ACTION_TOGGLE_SMART_CHARGING:
                case ACTION_TOGGLE_WARNING_HIGH:
                case ACTION_TOGGLE_WARNING_LOW:
                    BundleAssertions.assertHasBoolean(bundle, EXTRA_VALUE);
                    break;
                case ACTION_SET_WARNING_HIGH:
                case ACTION_SET_WARNING_LOW:
                case ACTION_SET_SMART_CHARGING_LIMIT:
                    BundleAssertions.assertHasInt(bundle, EXTRA_VALUE);
                    break;
                case ACTION_SET_SMART_CHARGING_TIME:
                    BundleAssertions.assertHasLong(bundle, EXTRA_VALUE);
                    break;
                default:
                    throw new AssertionError("Unknown action!");
            }
        } catch (AssertionError e) {
            Lumberjack.e("Bundle failed verification%s", e);
            return false;
        }
        return true;
    }

    public static Bundle buildBundle(int action, Object value) {
        Bundle bundle = new Bundle(1);
        bundle.putInt(EXTRA_ACTION, action);
        if (value instanceof Boolean) {
            bundle.putBoolean(EXTRA_VALUE, (Boolean) value);
        } else if (value instanceof Integer) {
            bundle.putInt(EXTRA_VALUE, (Integer) value);
        } else if (value instanceof Long) {
            bundle.putLong(EXTRA_VALUE, (Long) value);
        }
        return bundle;
    }

    public static Object getValue(@NonNull Bundle bundle) {
        return bundle.get(EXTRA_VALUE);
    }

    public static int getAction(@NonNull Bundle bundle) {
        return bundle.getInt(EXTRA_ACTION, ACTION_TOGGLE_CHARGING);
    }
}
