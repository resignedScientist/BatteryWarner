package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;
import com.twofortyfouram.assertion.BundleAssertions;
import com.twofortyfouram.log.Lumberjack;

import java.text.DateFormat;
import java.util.Date;

public class TaskerHelper {
    public static final String ACTION_TOGGLE_CHARGING = "com.laudien.p1xelfehler.batterywarner.toggle_charging";
    public static final String ACTION_TOGGLE_STOP_CHARGING = "com.laudien.p1xelfehler.batterywarner.toggle_stop_charging";
    public static final String ACTION_TOGGLE_SMART_CHARGING = "com.laudien.p1xelfehler.batterywarner.toggle_smart_charging";
    public static final String ACTION_TOGGLE_WARNING_HIGH = "com.laudien.p1xelfehler.batterywarner.toggle_warning_high";
    public static final String ACTION_TOGGLE_WARNING_LOW = "com.laudien.p1xelfehler.batterywarner.toggle_warning_low";
    public static final String ACTION_SET_WARNING_HIGH = "com.laudien.p1xelfehler.batterywarner.set_warning_high";
    public static final String ACTION_SET_WARNING_LOW = "com.laudien.p1xelfehler.batterywarner.set_warning_low";
    public static final String ACTION_SET_SMART_CHARGING_LIMIT = "com.laudien.p1xelfehler.batterywarner.set_smart_charging_limit";
    public static final String ACTION_SET_SMART_CHARGING_TIME = "com.laudien.p1xelfehler.batterywarner.set_smart_charging_time";
    public static final String ACTION_SAVE_GRAPH = "com.laudien.p1xelfehler.batterywarner.save_graph";
    public static final String ACTION_RESET_GRAPH = "com.laudien.p1xelfehler.batterywarner.reset_graph";
    public static final String[] ALL_ACTIONS = new String[]{
            ACTION_TOGGLE_CHARGING,
            ACTION_TOGGLE_STOP_CHARGING,
            ACTION_TOGGLE_SMART_CHARGING,
            ACTION_TOGGLE_WARNING_HIGH,
            ACTION_TOGGLE_WARNING_LOW,
            ACTION_SET_WARNING_HIGH,
            ACTION_SET_WARNING_LOW,
            ACTION_SET_SMART_CHARGING_LIMIT,
            ACTION_SET_SMART_CHARGING_TIME,
            ACTION_SAVE_GRAPH,
            ACTION_RESET_GRAPH
    };

    public static boolean isBundleValid(@NonNull Bundle bundle) {
        if (bundle.isEmpty()
                || !containsKnownKey(bundle)
                || getAction(bundle) == null
                || !replaceStringsWithInts(bundle)) {
            return false;
        }
        try {
            if (bundle.containsKey(ACTION_TOGGLE_CHARGING))
                BundleAssertions.assertHasBoolean(bundle, ACTION_TOGGLE_CHARGING);
            if (bundle.containsKey(ACTION_TOGGLE_STOP_CHARGING))
                BundleAssertions.assertHasBoolean(bundle, ACTION_TOGGLE_STOP_CHARGING);
            if (bundle.containsKey(ACTION_TOGGLE_SMART_CHARGING))
                BundleAssertions.assertHasBoolean(bundle, ACTION_TOGGLE_SMART_CHARGING);
            if (bundle.containsKey(ACTION_TOGGLE_WARNING_HIGH))
                BundleAssertions.assertHasBoolean(bundle, ACTION_TOGGLE_WARNING_HIGH);
            if (bundle.containsKey(ACTION_TOGGLE_WARNING_LOW))
                BundleAssertions.assertHasBoolean(bundle, ACTION_TOGGLE_WARNING_LOW);
            if (bundle.containsKey(ACTION_SET_WARNING_HIGH)) {
                BundleAssertions.assertHasInt(bundle, ACTION_SET_WARNING_HIGH);
                int value = bundle.getInt(ACTION_SET_WARNING_HIGH);
                if (!isIntegerValid(60, 100, value)) {
                    return false;
                }
            }
            if (bundle.containsKey(ACTION_SET_WARNING_LOW)) {
                BundleAssertions.assertHasInt(bundle, ACTION_SET_WARNING_LOW);
                int value = bundle.getInt(ACTION_SET_WARNING_LOW);
                if (!isIntegerValid(5, 40, value)) {
                    return false;
                }
            }
            if (bundle.containsKey(ACTION_SET_SMART_CHARGING_LIMIT)) {
                BundleAssertions.assertHasInt(bundle, ACTION_SET_SMART_CHARGING_LIMIT);
                int value = bundle.getInt(ACTION_SET_SMART_CHARGING_LIMIT);
                if (!isIntegerValid(80, 100, value)) {
                    return false;
                }
            }
            if (bundle.containsKey(ACTION_SET_SMART_CHARGING_TIME))
                BundleAssertions.assertHasLong(bundle, ACTION_SET_SMART_CHARGING_TIME);
        } catch (AssertionError e) {
            Lumberjack.e("Bundle failed verification%s", e);
            return false;
        }
        return true;
    }

    public static boolean isVariableBundleValid(@Nullable Bundle bundle) {
        if (bundle == null || bundle.isEmpty() || !containsKnownKey(bundle)) {
            return false;
        }
        for (String action : ALL_ACTIONS) {
            if (!bundle.containsKey(action)) {
                continue;
            }
            String value = bundle.getString(action);
            if (value == null && !isValueValid(action, bundle)
                    || value != null && !TaskerPlugin.variableNameValid(value)) {
                return false;
            }
        }
        return true;
    }

    public static Bundle buildBundle(String action, long value) {
        Bundle bundle = new Bundle();
        bundle.putLong(action, value);
        return bundle;
    }

    public static Bundle buildBundle(String action, boolean value) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(action, value);
        return bundle;
    }

    public static Bundle buildBundle(String action, int value) {
        Bundle bundle = new Bundle();
        bundle.putInt(action, value);
        return bundle;
    }

    public static Bundle buildBundle(String action, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(action, value);
        return bundle;
    }

    @Nullable
    public static String getAction(@NonNull Bundle bundle) {
        if (bundle.containsKey(ACTION_TOGGLE_CHARGING))
            return ACTION_TOGGLE_CHARGING;
        if (bundle.containsKey(ACTION_TOGGLE_STOP_CHARGING))
            return ACTION_TOGGLE_STOP_CHARGING;
        if (bundle.containsKey(ACTION_TOGGLE_SMART_CHARGING))
            return ACTION_TOGGLE_SMART_CHARGING;
        if (bundle.containsKey(ACTION_TOGGLE_WARNING_HIGH))
            return ACTION_TOGGLE_WARNING_HIGH;
        if (bundle.containsKey(ACTION_TOGGLE_WARNING_LOW))
            return ACTION_TOGGLE_WARNING_LOW;
        if (bundle.containsKey(ACTION_SET_WARNING_HIGH))
            return ACTION_SET_WARNING_HIGH;
        if (bundle.containsKey(ACTION_SET_WARNING_LOW))
            return ACTION_SET_WARNING_LOW;
        if (bundle.containsKey(ACTION_SET_SMART_CHARGING_LIMIT))
            return ACTION_SET_SMART_CHARGING_LIMIT;
        if (bundle.containsKey(ACTION_SET_SMART_CHARGING_TIME))
            return ACTION_SET_SMART_CHARGING_TIME;
        if (bundle.containsKey(ACTION_SAVE_GRAPH))
            return ACTION_SAVE_GRAPH;
        if (bundle.containsKey(ACTION_RESET_GRAPH))
            return ACTION_RESET_GRAPH;
        return null;
    }

    public static String getResultBlurb(Context context, @NonNull Bundle bundle) {
        String action = getAction(bundle);
        Object value = bundle.get(action);
        if (action == null || value == null) {
            return null;
        }
        switch (action) {
            case ACTION_TOGGLE_CHARGING:
                return context.getString((Boolean) value ? R.string.tasker_enable_charging : R.string.tasker_disable_charging);
            case ACTION_TOGGLE_STOP_CHARGING:
                return context.getString((Boolean) value ? R.string.tasker_enable_stop_charging : R.string.tasker_disable_stop_charging);
            case ACTION_TOGGLE_SMART_CHARGING:
                return context.getString((Boolean) value ? R.string.tasker_enable_smart_charging : R.string.tasker_disable_smart_charging);
            case ACTION_TOGGLE_WARNING_HIGH:
                return context.getString((Boolean) value ? R.string.tasker_enable_warning_high : R.string.tasker_disable_warning_high);
            case ACTION_TOGGLE_WARNING_LOW:
                return context.getString((Boolean) value ? R.string.tasker_enable_warning_low : R.string.tasker_disable_warning_low);
            case ACTION_SET_WARNING_HIGH:
                return context.getString(R.string.tasker_set_warning_high) + " " + context.getString(R.string.tasker_to) + " " + value + "%";
            case ACTION_SET_WARNING_LOW:
                return context.getString(R.string.tasker_set_warning_low) + " " + context.getString(R.string.tasker_to) + " " + value + "%";
            case ACTION_SET_SMART_CHARGING_LIMIT:
                return context.getString(R.string.tasker_set_smart_charging_limit) + " " + context.getString(R.string.tasker_to) + " " + value + "%";
            case ACTION_SET_SMART_CHARGING_TIME:
                DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
                return context.getString(R.string.tasker_set_smart_charging_time) + " " + context.getString(R.string.tasker_to) + " " + dateFormat.format(new Date((Long) value));
            case ACTION_SAVE_GRAPH:
                return context.getString(R.string.tasker_save_graph);
            case ACTION_RESET_GRAPH:
                return context.getString(R.string.tasker_reset_graph);
            default: // cannot happen because the bundles are validated first
                return null;
        }
    }

    private static boolean containsKnownKey(@NonNull Bundle bundle) {
        return bundle.containsKey(ACTION_TOGGLE_CHARGING)
                || bundle.containsKey(ACTION_TOGGLE_STOP_CHARGING)
                || bundle.containsKey(ACTION_TOGGLE_SMART_CHARGING)
                || bundle.containsKey(ACTION_TOGGLE_WARNING_HIGH)
                || bundle.containsKey(ACTION_TOGGLE_WARNING_LOW)
                || bundle.containsKey(ACTION_SET_WARNING_HIGH)
                || bundle.containsKey(ACTION_SET_WARNING_LOW)
                || bundle.containsKey(ACTION_SET_SMART_CHARGING_LIMIT)
                || bundle.containsKey(ACTION_SET_SMART_CHARGING_TIME)
                || bundle.containsKey(ACTION_SAVE_GRAPH)
                || bundle.containsKey(ACTION_RESET_GRAPH);
    }

    private static boolean replaceStringsWithInts(Bundle bundle) {
        for (String action : ALL_ACTIONS) {
            if (!bundle.containsKey(action) || bundle.getString(action) == null) {
                continue;
            }
            String value = bundle.getString(action);
            try {
                int intValue = Integer.valueOf(value);
                bundle.putInt(action, intValue);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValueValid(String action, Bundle bundle) {
        try {
            switch (action) {
                case ACTION_TOGGLE_CHARGING:
                case ACTION_TOGGLE_STOP_CHARGING:
                case ACTION_TOGGLE_SMART_CHARGING:
                case ACTION_TOGGLE_WARNING_HIGH:
                case ACTION_TOGGLE_WARNING_LOW:
                    BundleAssertions.assertHasBoolean(bundle, action);
                    break;
                case ACTION_SET_WARNING_HIGH:
                case ACTION_SET_WARNING_LOW:
                case ACTION_SET_SMART_CHARGING_LIMIT:
                    BundleAssertions.assertHasInt(bundle, action);
                    break;
                case ACTION_SET_SMART_CHARGING_TIME:
                    BundleAssertions.assertHasLong(bundle, action);
                    break;
                case ACTION_SAVE_GRAPH:
                case ACTION_RESET_GRAPH:
                    break;
                default:
                    return false;
            }
        } catch (AssertionError e) {
            Lumberjack.e("Bundle failed verification%s", e);
            return false;
        }
        return true;
    }

    private static boolean isIntegerValid(int min, int max, int value) {
        return !(value < min || value > max);
    }
}
