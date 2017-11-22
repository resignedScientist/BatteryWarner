package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;

import com.laudien.p1xelfehler.batterywarner.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.TreeSet;

import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_RESET_GRAPH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SAVE_GRAPH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_SMART_CHARGING_LIMIT;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_SMART_CHARGING_TIME;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_WARNING_HIGH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_WARNING_LOW;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_SMART_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_STOP_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_WARNING_HIGH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_WARNING_LOW;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ALL_ACTIONS;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ROOT_ACTIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TaskerHelperTest {
    private Context context;
    private SharedPreferences sharedPreferences;
    private int oldWarningHigh;
    private TreeSet<String> booleanKeys;
    private TreeSet<String> intKeys;
    private TreeSet<String> longKeys;
    private TreeSet<String> actionKeys;
    private TreeSet<String> noDependencyKeys;
    private TreeSet<String> rootKeys;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        oldWarningHigh = sharedPreferences.getInt(context.getString(R.string.pref_warning_high), context.getResources().getInteger(R.integer.pref_warning_high_default));
        sharedPreferences.edit()
                .putInt(context.getString(R.string.pref_warning_high), context.getResources().getInteger(R.integer.pref_warning_high_min))
                .apply();

        // keys that have a value of type boolean
        booleanKeys = new TreeSet<>();
        booleanKeys.add(ACTION_TOGGLE_CHARGING);
        booleanKeys.add(ACTION_TOGGLE_STOP_CHARGING);
        booleanKeys.add(ACTION_TOGGLE_SMART_CHARGING);
        booleanKeys.add(ACTION_TOGGLE_WARNING_HIGH);
        booleanKeys.add(ACTION_TOGGLE_WARNING_LOW);

        // keys that have a value of type integer
        intKeys = new TreeSet<>();
        intKeys.add(ACTION_SET_WARNING_HIGH);
        intKeys.add(ACTION_SET_WARNING_LOW);
        intKeys.add(ACTION_SET_SMART_CHARGING_LIMIT);

        // keys that have a value of type long
        longKeys = new TreeSet<>();
        longKeys.add(ACTION_SET_SMART_CHARGING_TIME);

        // keys that do not need a value because they trigger an action
        actionKeys = new TreeSet<>();
        actionKeys.add(ACTION_SAVE_GRAPH);
        actionKeys.add(ACTION_RESET_GRAPH);

        // keys that do not have dependencies
        noDependencyKeys = new TreeSet<>();
        noDependencyKeys.add(ACTION_TOGGLE_CHARGING);
        noDependencyKeys.add(ACTION_TOGGLE_WARNING_HIGH);
        noDependencyKeys.add(ACTION_TOGGLE_WARNING_LOW);

        // keys that require root
        rootKeys = new TreeSet<>();
        rootKeys.addAll(Arrays.asList(ROOT_ACTIONS));
    }

    @After
    public void tearDown() throws Exception {
        sharedPreferences.edit()
                .putInt(context.getString(R.string.pref_warning_high), oldWarningHigh)
                .apply();
    }

    @Test
    public void isBundleValid() throws Exception {
        // bundle is empty
        Bundle bundle = new Bundle();
        assertFalse(TaskerHelper.isBundleValid(context, bundle));
    }

    @Test
    public void isBundleValid1() throws Exception {
        // unknown key
        Bundle bundle = new Bundle();
        bundle.putLong("randomKey", 1337);
        assertFalse(TaskerHelper.isBundleValid(context, bundle));
    }

    @Test
    public void isBundleValid2() throws Exception {
        // unknown key and a valid key
        for (String key : ALL_ACTIONS) {
            Bundle bundle = new Bundle();
            bundle.putLong("randomKey", 1337);
            if (booleanKeys.contains(key)) {
                bundle.putBoolean(key, true);
            } else if (intKeys.contains(key)) {
                bundle.putInt(key, getDefaultInt(key));
            } else if (longKeys.contains(key)) {
                bundle.putLong(key, 1337);
            } else if (actionKeys.contains(key)) {
                bundle.putByte(key, (byte) 2);
            }
            assertTrue(key, TaskerHelper.isBundleValid(context, bundle));
        }
    }

    @Test
    public void isBundleValid3() throws Exception {
        // known keys but wrong value types
        for (String key : ALL_ACTIONS) {
            Bundle bundle = new Bundle();
            bundle.putByte(key, (byte) 2);
            if (actionKeys.contains(key)) {
                assertTrue(key, TaskerHelper.isBundleValid(context, bundle));
            } else {
                assertFalse(key, TaskerHelper.isBundleValid(context, bundle));
            }
        }
    }

    @Test
    public void isBundleValid4() throws Exception {
        // one valid key with valid type
        for (String key : ALL_ACTIONS) {
            Bundle bundle = new Bundle();
            if (booleanKeys.contains(key)) {
                bundle.putBoolean(key, true);
            } else if (intKeys.contains(key)) {
                bundle.putInt(key, getDefaultInt(key));
            } else if (longKeys.contains(key)) {
                bundle.putLong(key, 1337);
            } else if (actionKeys.contains(key)) {
                bundle.putByte(key, (byte) 2);
            }
            assertTrue(key, TaskerHelper.isBundleValid(context, bundle));
        }
    }

    @Test
    public void isBundleValid5() throws Exception {
        // one valid key, one known key with wrong type
        Bundle bundle = new Bundle();
        bundle.putBoolean(booleanKeys.first(), true);
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            bundle.putByte(key, (byte) 2);
            if (actionKeys.contains(key)) {
                assertTrue(key, TaskerHelper.isBundleValid(context, bundle));
            } else {
                assertFalse(key, TaskerHelper.isBundleValid(context, bundle));
            }
        }
    }

    @Test
    public void isBundleValid6() throws Exception {
        // valid keys with string values instead of integers
        for (String key : intKeys) {
            Bundle bundle = new Bundle();
            bundle.putString(key, getDefaultString(key));
            assertTrue(key, TaskerHelper.isBundleValid(context, bundle));
        }
    }

    @Test
    public void isBundleValid7() throws Exception {
        // valid keys with not parsable string values instead of integers
        for (String key : intKeys) {
            Bundle bundle = new Bundle();
            bundle.putString(key, "abc123");
            assertFalse(key, TaskerHelper.isBundleValid(context, bundle));
        }
    }

    @Test
    public void isBundleValid8() throws Exception {
        // testing warning high values (edge cases)
        int min = context.getResources().getInteger(R.integer.pref_warning_high_min);
        int max = context.getResources().getInteger(R.integer.pref_warning_high_max);
        testBundleValidValueEdgeCases(min, max, ACTION_SET_WARNING_HIGH);
    }

    @Test
    public void isBundleValid9() throws Exception {
        // testing warning low values (edge cases)
        int min = context.getResources().getInteger(R.integer.pref_warning_low_min);
        int max = context.getResources().getInteger(R.integer.pref_warning_low_max);
        testBundleValidValueEdgeCases(min, max, ACTION_SET_WARNING_LOW);
    }

    @Test
    public void isBundleValid10() throws Exception {
        // testing smart charging limit values (edge cases)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int min = sharedPreferences.getInt(context.getString(R.string.pref_warning_high), context.getResources().getInteger(R.integer.pref_warning_high_default));
        int max = context.getResources().getInteger(R.integer.pref_smart_charging_limit_max);
        testBundleValidValueEdgeCases(min, max, ACTION_SET_SMART_CHARGING_LIMIT);
    }

    @Test
    public void isVariableBundleValid() throws Exception {
        byte randomByte = (byte) 2;
        // bundle == null
        assertFalse(TaskerHelper.isVariableBundleValid(context, null));

        // bundle is empty
        Bundle bundle = new Bundle();
        assertFalse(TaskerHelper.isVariableBundleValid(context, bundle));

        // unknown key
        bundle.putLong("randomKey", 1337);
        assertFalse(TaskerHelper.isVariableBundleValid(context, bundle));

        // unknown key and a valid key
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            bundle.putLong("randomKey", 1337);
            if (booleanKeys.contains(key)) {
                bundle.putBoolean(key, true);
            } else if (intKeys.contains(key)) {
                bundle.putInt(key, getDefaultInt(key));
            } else if (longKeys.contains(key)) {
                bundle.putLong(key, 1337);
            } else if (actionKeys.contains(key)) {
                bundle.putByte(key, randomByte);
            }
            assertTrue(key, TaskerHelper.isVariableBundleValid(context, bundle));
        }

        // known keys but wrong value types
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            bundle.putByte(key, randomByte);
            if (actionKeys.contains(key)) {
                assertTrue(key, TaskerHelper.isVariableBundleValid(context, bundle));
            } else {
                assertFalse(key, TaskerHelper.isVariableBundleValid(context, bundle));
            }
        }

        // one valid key with valid type
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            if (booleanKeys.contains(key)) {
                bundle.putBoolean(key, true);
            } else if (intKeys.contains(key)) {
                bundle.putInt(key, getDefaultInt(key));
            } else if (longKeys.contains(key)) {
                bundle.putLong(key, 1337);
            } else if (actionKeys.contains(key)) {
                bundle.putByte(key, randomByte);
            }
            assertTrue(key, TaskerHelper.isVariableBundleValid(context, bundle));
        }

        // one valid key, one known key with wrong type
        bundle = new Bundle();
        bundle.putBoolean(booleanKeys.first(), true);
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            bundle.putByte(key, randomByte);
            if (actionKeys.contains(key)) {
                assertTrue(key, TaskerHelper.isVariableBundleValid(context, bundle));
            } else {
                assertFalse(key, TaskerHelper.isVariableBundleValid(context, bundle));
            }
        }

        // valid keys with variable instead of integers
        for (String key : intKeys) {
            bundle = new Bundle();
            bundle.putString(key, "%test");
            assertTrue(key, TaskerHelper.isVariableBundleValid(context, bundle));
        }

        // global variables
        for (String key: intKeys) {
            bundle = new Bundle();
            bundle.putString(key, "%TESTING");
            assertTrue(key, TaskerHelper.isVariableBundleValid(context, bundle));
        }

        // valid keys with wrong variable format instead of integers
        for (String key : intKeys) {
            bundle = new Bundle();
            bundle.putString(key, "abc123");
            assertFalse(key, TaskerHelper.isVariableBundleValid(context, bundle));
        }
    }

    @Test
    public void isVariableBundleValid1() throws Exception {
        // testing warning high values (edge cases)
        int min = context.getResources().getInteger(R.integer.pref_warning_high_min);
        int max = context.getResources().getInteger(R.integer.pref_warning_high_max);
        testVariableBundleValidValueEdgeCases(min, max, ACTION_SET_WARNING_HIGH);
    }

    @Test
    public void isVariableBundleValid2() throws Exception {
        // testing warning low values (edge cases)
        int min = context.getResources().getInteger(R.integer.pref_warning_low_min);
        int max = context.getResources().getInteger(R.integer.pref_warning_low_max);
        testVariableBundleValidValueEdgeCases(min, max, ACTION_SET_WARNING_LOW);
    }

    @Test
    public void isVariableBundleValid3() throws Exception {
        // testing smart charging limit values (edge cases)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int min = sharedPreferences.getInt(context.getString(R.string.pref_warning_high), context.getResources().getInteger(R.integer.pref_warning_high_default));
        int max = context.getResources().getInteger(R.integer.pref_smart_charging_limit_max);
        testVariableBundleValidValueEdgeCases(min, max, ACTION_SET_SMART_CHARGING_LIMIT);
    }

    @Test
    public void buildBundle() throws Exception {
        String key = booleanKeys.first();
        boolean value = true;
        Bundle bundle = TaskerHelper.buildBundle(key, value);
        assertEquals(value, bundle.get(key));
    }

    @Test
    public void buildBundle1() throws Exception {
        String key = intKeys.first();
        int value = 1337;
        Bundle bundle = TaskerHelper.buildBundle(key, value);
        assertEquals(value, bundle.get(key));
    }

    @Test
    public void buildBundle2() throws Exception {
        String key = longKeys.first();
        long value = 1337L;
        Bundle bundle = TaskerHelper.buildBundle(key, value);
        assertEquals(value, bundle.get(key));
    }

    @Test
    public void getAction() throws Exception {
        // normal valid values
        for (String key : ALL_ACTIONS) {
            Bundle bundle = new Bundle();
            if (booleanKeys.contains(key)) {
                bundle.putBoolean(key, true);
            } else if (intKeys.contains(key)) {
                bundle.putInt(key, 1337);
            } else if (longKeys.contains(key)) {
                bundle.putLong(key, 1337);
            } else if (actionKeys.contains(key)) {
                bundle.putByte(key, (byte) 5);
            }
            assertEquals(key, TaskerHelper.getAction(bundle));
        }

        // Strings instead of integers
        for (String key : intKeys) {
            Bundle bundle = new Bundle();
            bundle.putString(key, "1337");
            assertEquals(key, TaskerHelper.getAction(bundle));
        }
    }

    @Test
    public void getResultBlurb() throws Exception {
        // unknown key
        Bundle bundle = new Bundle();
        bundle.putBoolean("randomKey", false);
        assertNull(TaskerHelper.getResultBlurb(context, bundle));

        // one known key
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            if (booleanKeys.contains(key)) {
                bundle.putBoolean(key, true);
            } else if (intKeys.contains(key)) {
                bundle.putInt(key, 1337);
            } else if (longKeys.contains(key)) {
                bundle.putLong(key, 1337);
            } else if (actionKeys.contains(key)) {
                bundle.putByte(key, (byte) 2);
            }
            assertNotNull(key, TaskerHelper.getResultBlurb(context, bundle));
        }
    }

    @Test
    public void checkDependencies() throws Exception {
        // toggle stop charging

        // preparation
        boolean oldStopCharging = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
        boolean oldWarningHigh = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default));

        // create bundles
        Bundle trueBundle = new Bundle();
        trueBundle.putBoolean(ACTION_TOGGLE_STOP_CHARGING, true);
        Bundle falseBundle = new Bundle();
        falseBundle.putBoolean(ACTION_TOGGLE_STOP_CHARGING, false);

        // dependency not fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, trueBundle));
        assertFalse(TaskerHelper.checkDependencies(context, falseBundle));

        // dependency fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertTrue(TaskerHelper.checkDependencies(context, trueBundle));
        assertTrue(TaskerHelper.checkDependencies(context, falseBundle));

        // clean up
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_stop_charging), oldStopCharging)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), oldWarningHigh)
                .apply();
    }

    @Test
    public void checkDependencies1() throws Exception {
        // toggle smart charging

        // preparation
        boolean oldStopCharging = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
        boolean oldWarningHigh = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
        boolean oldSmartCharging = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_enabled), context.getResources().getBoolean(R.bool.pref_smart_charging_enabled_default));

        // create bundles
        Bundle trueBundle = new Bundle();
        trueBundle.putBoolean(ACTION_TOGGLE_SMART_CHARGING, true);
        Bundle falseBundle = new Bundle();
        falseBundle.putBoolean(ACTION_TOGGLE_SMART_CHARGING, false);

        // both dependencies fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_stop_charging), true)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertTrue(TaskerHelper.checkDependencies(context, trueBundle));
        assertTrue(TaskerHelper.checkDependencies(context, falseBundle));

        // both dependencies not fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_stop_charging), false)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, trueBundle));
        assertFalse(TaskerHelper.checkDependencies(context, falseBundle));

        // stop charging enabled, warning high disabled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_stop_charging), true)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, trueBundle));
        assertFalse(TaskerHelper.checkDependencies(context, falseBundle));

        // stop charging disabled, warning high enabled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_stop_charging), false)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, trueBundle));
        assertFalse(TaskerHelper.checkDependencies(context, falseBundle));

        // clean up
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_stop_charging), oldStopCharging)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), oldWarningHigh)
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), oldSmartCharging)
                .apply();
    }

    @Test
    public void checkDependencies2() throws Exception {
        // set high battery warning percentage

        // preparation
        boolean oldWarningHighEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
        int oldWarningHigh = sharedPreferences.getInt(context.getString(R.string.pref_warning_high), context.getResources().getInteger(R.integer.pref_warning_high_default));

        // create bundles
        Bundle bundle = new Bundle();
        bundle.putInt(ACTION_SET_WARNING_HIGH, oldWarningHigh);

        // dependency fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertTrue(TaskerHelper.checkDependencies(context, bundle));

        // dependency not fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));

        // clean up
        sharedPreferences.edit()
                .putInt(context.getString(R.string.pref_warning_high), oldWarningHigh)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), oldWarningHighEnabled)
                .apply();
    }

    @Test
    public void checkDependencies3() throws Exception {
        // set low battery warning percentage

        // preparation
        boolean oldWarningLowEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_low_enabled), context.getResources().getBoolean(R.bool.pref_warning_low_enabled_default));
        int oldWarningLow = sharedPreferences.getInt(context.getString(R.string.pref_warning_low), context.getResources().getInteger(R.integer.pref_warning_low_default));

        // create bundles
        Bundle bundle = new Bundle();
        bundle.putInt(ACTION_SET_WARNING_LOW, oldWarningLow);

        // dependency fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_warning_low_enabled), true)
                .apply();
        assertTrue(TaskerHelper.checkDependencies(context, bundle));

        // dependency not fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_warning_low_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));

        // clean up
        sharedPreferences.edit()
                .putInt(context.getString(R.string.pref_warning_low), oldWarningLow)
                .putBoolean(context.getString(R.string.pref_warning_low_enabled), oldWarningLowEnabled)
                .apply();
    }

    @Test
    public void checkDependencies4() throws Exception {
        // set smart charging percentage limit

        // preparation
        int oldSmartChargingPercentageLimit = sharedPreferences.getInt(context.getString(R.string.pref_smart_charging_limit), context.getResources().getInteger(R.integer.pref_smart_charging_limit_default));
        boolean oldSmartChargingEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_enabled), context.getResources().getBoolean(R.bool.pref_smart_charging_enabled_default));
        boolean oldStopChargingEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
        boolean oldWarningHighEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default));

        // create bundles
        Bundle bundle = new Bundle();
        bundle.putInt(ACTION_SET_SMART_CHARGING_LIMIT, oldSmartChargingPercentageLimit);

        // dependency fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), true)
                .putBoolean(context.getString(R.string.pref_stop_charging), true)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertTrue(TaskerHelper.checkDependencies(context, bundle));

        // none of the dependencies is fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), false)
                .putBoolean(context.getString(R.string.pref_stop_charging), false)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));

        // smart charging is disabled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), false)
                .putBoolean(context.getString(R.string.pref_stop_charging), true)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));

        // stop charging is disabled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), true)
                .putBoolean(context.getString(R.string.pref_stop_charging), false)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));

        // warning high is disabled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), true)
                .putBoolean(context.getString(R.string.pref_stop_charging), true)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));

        // smart charging and stop charging are disabled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), false)
                .putBoolean(context.getString(R.string.pref_stop_charging), false)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));

        // smart charging and warning high are disabled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), false)
                .putBoolean(context.getString(R.string.pref_stop_charging), true)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));

        // stop charging and warning high are disabled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), true)
                .putBoolean(context.getString(R.string.pref_stop_charging), false)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));

        // clean up
        sharedPreferences.edit()
                .putInt(context.getString(R.string.pref_smart_charging_limit), oldSmartChargingPercentageLimit)
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), oldSmartChargingEnabled)
                .putBoolean(context.getString(R.string.pref_stop_charging), oldStopChargingEnabled)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), oldWarningHighEnabled)
                .apply();
    }

    @Test
    public void checkDependencies5() throws Exception {
        // save graph

        // preparation
        boolean oldGraphEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));

        // create bundles
        Bundle bundle = new Bundle();
        bundle.putBoolean(ACTION_SAVE_GRAPH, true);

        // dependency fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_graph_enabled), true)
                .apply();
        assertTrue(TaskerHelper.checkDependencies(context, bundle));

        // dependency not fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_graph_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));

        // clean up
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_graph_enabled), oldGraphEnabled)
                .apply();
    }

    @Test
    public void checkDependencies6() throws Exception {
        // reset graph

        // preparation
        boolean oldGraphEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));

        // create bundles
        Bundle bundle = new Bundle();
        bundle.putBoolean(ACTION_RESET_GRAPH, true);

        // dependency fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_graph_enabled), true)
                .apply();
        assertTrue(TaskerHelper.checkDependencies(context, bundle));

        // dependency not fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_graph_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));

        // clean up
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_graph_enabled), oldGraphEnabled)
                .apply();
    }

    @Test
    public void checkDependencies7() throws Exception {
        // set smart charging time

        // preparation
        long oldSmartChargingTime = sharedPreferences.getLong(context.getString(R.string.pref_smart_charging_time), 0);
        boolean oldUseNextAlarm = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), context.getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        boolean oldSmartChargingEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_enabled), context.getResources().getBoolean(R.bool.pref_smart_charging_enabled_default));
        boolean oldStopChargingEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
        boolean oldWarningHighEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default));

        // create bundle
        Bundle bundle = new Bundle();
        bundle.putLong(ACTION_SET_SMART_CHARGING_TIME, 1337);

        // dependency fulfilled
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), false) // must be false!
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), true)
                .putBoolean(context.getString(R.string.pref_stop_charging), true)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertTrue(TaskerHelper.checkDependencies(context, bundle));

        // no dependency is fulfilled -> do not change useNextAlarm!
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), true) // must be false!
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), false)
                .putBoolean(context.getString(R.string.pref_stop_charging), false)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));
        boolean useNextAlarm = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), context.getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        assertTrue(useNextAlarm);

        // useNextAlarm is not fulfilled -> must be changed by the method!
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), true) // must be false!
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), true)
                .putBoolean(context.getString(R.string.pref_stop_charging), true)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertTrue(TaskerHelper.checkDependencies(context, bundle));
        useNextAlarm = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), context.getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        assertFalse(useNextAlarm);

        // smart charging is disabled -> do not change useNextAlarm!
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), true) // must be false!
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), false)
                .putBoolean(context.getString(R.string.pref_stop_charging), true)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));
        useNextAlarm = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), context.getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        assertTrue(useNextAlarm);

        // stop charging is disabled -> do not change useNextAlarm!
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), true) // must be false!
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), true)
                .putBoolean(context.getString(R.string.pref_stop_charging), false)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));
        useNextAlarm = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), context.getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        assertTrue(useNextAlarm);

        // warning high is disabled -> do not change useNextAlarm!
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), true) // must be false!
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), true)
                .putBoolean(context.getString(R.string.pref_stop_charging), true)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));
        useNextAlarm = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), context.getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        assertTrue(useNextAlarm);

        // smart charging and stop charging are disabled -> do not change useNextAlarm!
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), true) // must be false!
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), false)
                .putBoolean(context.getString(R.string.pref_stop_charging), false)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), true)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));
        useNextAlarm = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), context.getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        assertTrue(useNextAlarm);

        // smart charging and warning high are disabled -> do not change useNextAlarm!
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), true) // must be false!
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), false)
                .putBoolean(context.getString(R.string.pref_stop_charging), true)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));
        useNextAlarm = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), context.getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        assertTrue(useNextAlarm);

        // stop charging and warning high are disabled -> do not change useNextAlarm!
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), true) // must be false!
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), true)
                .putBoolean(context.getString(R.string.pref_stop_charging), false)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), false)
                .apply();
        assertFalse(TaskerHelper.checkDependencies(context, bundle));
        useNextAlarm = sharedPreferences.getBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), context.getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        assertTrue(useNextAlarm);

        // clean up
        sharedPreferences.edit()
                .putLong(context.getString(R.string.pref_smart_charging_time), oldSmartChargingTime)
                .putBoolean(context.getString(R.string.pref_smart_charging_use_alarm_clock_time), oldUseNextAlarm)
                .putBoolean(context.getString(R.string.pref_smart_charging_enabled), oldSmartChargingEnabled)
                .putBoolean(context.getString(R.string.pref_stop_charging), oldStopChargingEnabled)
                .putBoolean(context.getString(R.string.pref_warning_high_enabled), oldWarningHighEnabled)
                .apply();
    }

    @Test
    public void checkDependencies8() throws Exception {
        // actions without dependency
        for (String key : noDependencyKeys) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(key, true);
            assertTrue(key, TaskerHelper.checkDependencies(context, bundle));
            bundle = new Bundle();
            bundle.putBoolean(key, false);
            assertTrue(key, TaskerHelper.checkDependencies(context, bundle));
            bundle = new Bundle();
            bundle.putInt(key, 42);
            assertTrue(key, TaskerHelper.checkDependencies(context, bundle));
        }

        // unknown action
        Bundle bundle = new Bundle();
        bundle.putBoolean("UnknownAction", true);
        assertTrue(TaskerHelper.checkDependencies(context, bundle));
        bundle = new Bundle();
        bundle.putBoolean("UnknownAction", false);
        assertTrue(TaskerHelper.checkDependencies(context, bundle));
        bundle = new Bundle();
        bundle.putInt("UnknownAction", 42);
        assertTrue(TaskerHelper.checkDependencies(context, bundle));
    }

    @Test
    public void checkForRootDependencies() throws Exception {
        Bundle bundle = new Bundle();
        assertFalse(TaskerHelper.checkForRootDependencies(bundle));
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            bundle.putString(key, "test");
            if (rootKeys.contains(key)) {
                assertTrue(key, TaskerHelper.checkForRootDependencies(bundle));
            } else {
                assertFalse(key, TaskerHelper.checkForRootDependencies(bundle));
            }
        }
    }

    private void testBundleValidValueEdgeCases(int min, int max, String action) {
        // too high
        Bundle bundle = new Bundle();
        bundle.putInt(action, max + 1);
        assertFalse("max=" + max, TaskerHelper.isBundleValid(context, bundle));

        // too low
        bundle = new Bundle();
        bundle.putInt(action, min - 1);
        assertFalse("min=" + min, TaskerHelper.isBundleValid(context, bundle));

        // just right (upper limit)
        bundle = new Bundle();
        bundle.putInt(action, max);
        assertTrue("max=" + max, TaskerHelper.isBundleValid(context, bundle));

        // just right (lower limit)
        bundle = new Bundle();
        bundle.putInt(action, min);
        assertTrue("min=" + min, TaskerHelper.isBundleValid(context, bundle));
    }

    private void testVariableBundleValidValueEdgeCases(int min, int max, String action) {
        // too high
        Bundle bundle = new Bundle();
        bundle.putInt(action, max + 1);
        assertFalse("max=" + max, TaskerHelper.isVariableBundleValid(context, bundle));

        // too low
        bundle = new Bundle();
        bundle.putInt(action, min - 1);
        assertFalse("min=" + min, TaskerHelper.isVariableBundleValid(context, bundle));

        // just right (upper limit)
        bundle = new Bundle();
        bundle.putInt(action, max);
        assertTrue("max=" + max, TaskerHelper.isVariableBundleValid(context, bundle));

        // just right (lower limit)
        bundle = new Bundle();
        bundle.putInt(action, min);
        assertTrue("min=" + min, TaskerHelper.isVariableBundleValid(context, bundle));
    }

    private int getDefaultInt(String action) {
        int value = 1337;
        switch (action) {
            case ACTION_SET_WARNING_HIGH:
                value = context.getResources().getInteger(R.integer.pref_warning_high_default);
                break;
            case ACTION_SET_WARNING_LOW:
                value = context.getResources().getInteger(R.integer.pref_warning_low_default);
                break;
            case ACTION_SET_SMART_CHARGING_LIMIT:
                value = context.getResources().getInteger(R.integer.pref_smart_charging_limit_default);
                break;
        }
        return value;
    }

    private String getDefaultString(String action) {
        return String.valueOf(getDefaultInt(action));
    }
}