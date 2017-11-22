package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;
import com.twofortyfouram.assertion.BundleAssertions;
import com.twofortyfouram.log.Lumberjack;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Pattern;

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
    static final String[] ROOT_ACTIONS = new String[]{
            ACTION_TOGGLE_CHARGING,
            ACTION_TOGGLE_STOP_CHARGING,
            ACTION_TOGGLE_SMART_CHARGING,
            ACTION_SET_SMART_CHARGING_LIMIT,
            ACTION_SET_SMART_CHARGING_TIME
    };

    public static boolean isBundleValid(@NonNull Context context, @NonNull Bundle bundle) {
        if (bundle.isEmpty()
                || !containsKnownKey(bundle)
                || getAction(bundle) == null
                || !replaceStringsWithInts(bundle)) {
            return false;
        }
        for (String key : ALL_ACTIONS) {
            if (bundle.containsKey(key) && !isValueValid(context, key, bundle)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isVariableBundleValid(@NonNull Context context, @Nullable Bundle bundle) {
        if (bundle == null || bundle.isEmpty() || !containsKnownKey(bundle)) {
            return false;
        }
        for (String action : ALL_ACTIONS) {
            if (!bundle.containsKey(action)) {
                continue;
            }
            String value = bundle.getString(action);
            if (value == null && !isValueValid(context, action, bundle)
                    || value != null && !variableNameValid(value)) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkDependencies(@NonNull Context context, @NonNull Bundle bundle) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        for (String action : ALL_ACTIONS) {
            if (bundle.containsKey(action)
                    && !checkDependency(context, sharedPreferences, action)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if any root dependency is in the bundle
     *
     * @param bundle The bundle to check.
     * @return True = there are root dependencies in the bundle,
     * False = there are no root dependencies in the bundle.
     */
    public static boolean checkForRootDependencies(@NonNull Bundle bundle) {
        for (String action : ROOT_ACTIONS) {
            if (bundle.containsKey(action)) {
                return true;
            }
        }
        return false;
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

    private static boolean isValueValid(@NonNull Context context, @NonNull String action, @NonNull Bundle bundle) {
        try {
            int value;
            switch (action) {
                case ACTION_TOGGLE_CHARGING:
                case ACTION_TOGGLE_STOP_CHARGING:
                case ACTION_TOGGLE_SMART_CHARGING:
                case ACTION_TOGGLE_WARNING_HIGH:
                case ACTION_TOGGLE_WARNING_LOW:
                    BundleAssertions.assertHasBoolean(bundle, action);
                    break;
                case ACTION_SET_WARNING_HIGH:
                    BundleAssertions.assertHasInt(bundle, action);
                    value = bundle.getInt(ACTION_SET_WARNING_HIGH);
                    if (!isIntegerValid(context.getResources().getInteger(R.integer.pref_warning_high_min), context.getResources().getInteger(R.integer.pref_warning_high_max), value)) {
                        return false;
                    }
                    break;
                case ACTION_SET_WARNING_LOW:
                    BundleAssertions.assertHasInt(bundle, action);
                    value = bundle.getInt(ACTION_SET_WARNING_LOW);
                    if (!isIntegerValid(context.getResources().getInteger(R.integer.pref_warning_low_min), context.getResources().getInteger(R.integer.pref_warning_low_max), value)) {
                        return false;
                    }
                    break;
                case ACTION_SET_SMART_CHARGING_LIMIT:
                    BundleAssertions.assertHasInt(bundle, action);
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    int min = sharedPreferences.getInt(context.getString(R.string.pref_warning_high), context.getResources().getInteger(R.integer.pref_warning_high_default));
                    value = bundle.getInt(ACTION_SET_SMART_CHARGING_LIMIT);
                    if (!isIntegerValid(min, context.getResources().getInteger(R.integer.pref_smart_charging_limit_max), value)) {
                        return false;
                    }
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

    private static boolean checkDependency(@NonNull Context context, @NonNull SharedPreferences sharedPreferences, @NonNull String action) {
        switch (action) {
            case ACTION_TOGGLE_STOP_CHARGING:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
            case ACTION_TOGGLE_SMART_CHARGING:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default))
                        && checkDependency(context, sharedPreferences, ACTION_TOGGLE_STOP_CHARGING);
            case ACTION_SET_WARNING_HIGH:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
            case ACTION_SET_WARNING_LOW:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_warning_low_enabled), context.getResources().getBoolean(R.bool.pref_warning_low_enabled_default));
            case ACTION_SET_SMART_CHARGING_LIMIT:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_enabled), context.getResources().getBoolean(R.bool.pref_smart_charging_enabled_default))
                        && checkDependency(context, sharedPreferences, ACTION_TOGGLE_SMART_CHARGING);
            case ACTION_SAVE_GRAPH:
            case ACTION_RESET_GRAPH:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));
            case ACTION_SET_SMART_CHARGING_TIME: // same dependencies as smart charging limit
                boolean dependenciesFulfilled = checkDependency(context, sharedPreferences, ACTION_SET_SMART_CHARGING_LIMIT);
                if (dependenciesFulfilled) {
                    sharedPreferences.edit()
                            .putBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), false)
                            .apply();
                }
                return dependenciesFulfilled;
            default:
                return true;
        }
    }

    private static boolean variableNameValid(@NonNull String varName) {
        Pattern pattern = Pattern.compile(TaskerPlugin.VARIABLE_NAME_MATCH_EXPRESSION, 0);
        return pattern.matcher(varName).matches();
    }
}
