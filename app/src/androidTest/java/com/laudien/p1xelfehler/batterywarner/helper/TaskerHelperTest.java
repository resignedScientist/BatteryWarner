package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TaskerHelperTest {
    private TreeSet<String> booleanKeys;
    private TreeSet<String> intKeys;
    private TreeSet<String> longKeys;
    private TreeSet<String> actionKeys;

    @Before
    public void setUp() throws Exception {
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
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void isBundleValid() throws Exception {
        byte randomByte = (byte) 2;
        // bundle == null
        assertFalse(TaskerHelper.isBundleValid(null));

        // bundle is empty
        Bundle bundle = new Bundle();
        assertFalse(TaskerHelper.isBundleValid(bundle));

        // unknown key
        bundle.putLong("randomKey", 1337);
        assertFalse(TaskerHelper.isBundleValid(bundle));

        // unknown key and a valid key
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            bundle.putLong("randomKey", 1337);
            if (booleanKeys.contains(key)) {
                bundle.putBoolean(key, true);
            } else if (intKeys.contains(key)) {
                bundle.putInt(key, 1337);
            } else if (longKeys.contains(key)) {
                bundle.putLong(key, 1337);
            } else if (actionKeys.contains(key)) {
                bundle.putByte(key, randomByte);
            }
            assertTrue(key, TaskerHelper.isBundleValid(bundle));
        }

        // known keys but wrong value types
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            bundle.putByte(key, randomByte);
            if (actionKeys.contains(key)) {
                assertTrue(key, TaskerHelper.isBundleValid(bundle));
            } else {
                assertFalse(key, TaskerHelper.isBundleValid(bundle));
            }
        }

        // one valid key with valid type
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            if (booleanKeys.contains(key)) {
                bundle.putBoolean(key, true);
            } else if (intKeys.contains(key)) {
                bundle.putInt(key, 1337);
            } else if (longKeys.contains(key)) {
                bundle.putLong(key, 1337);
            } else if (actionKeys.contains(key)) {
                bundle.putByte(key, randomByte);
            }
            assertTrue(key, TaskerHelper.isBundleValid(bundle));
        }

        // one valid key, one known key with wrong type
        bundle = new Bundle();
        bundle.putBoolean(booleanKeys.first(), true);
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            bundle.putByte(key, randomByte);
            if (actionKeys.contains(key)) {
                assertTrue(key, TaskerHelper.isBundleValid(bundle));
            } else {
                assertFalse(key, TaskerHelper.isBundleValid(bundle));
            }
        }

        // valid keys with string values instead of integers
        for (String key : intKeys) {
            bundle = new Bundle();
            bundle.putString(key, "50");
            assertTrue(key, TaskerHelper.isBundleValid(bundle));
        }

        // valid keys with not parsable string values instead of integers
        for (String key : intKeys) {
            bundle = new Bundle();
            bundle.putString(key, "abc123");
            assertFalse(key, TaskerHelper.isBundleValid(bundle));
        }
    }

    @Test
    public void isVariableBundleValid() throws Exception {
        byte randomByte = (byte) 2;
        // bundle == null
        assertFalse(TaskerHelper.isVariableBundleValid(null));

        // bundle is empty
        Bundle bundle = new Bundle();
        assertFalse(TaskerHelper.isVariableBundleValid(bundle));

        // unknown key
        bundle.putLong("randomKey", 1337);
        assertFalse(TaskerHelper.isVariableBundleValid(bundle));

        // unknown key and a valid key
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            bundle.putLong("randomKey", 1337);
            if (booleanKeys.contains(key)) {
                bundle.putBoolean(key, true);
            } else if (intKeys.contains(key)) {
                bundle.putInt(key, 1337);
            } else if (longKeys.contains(key)) {
                bundle.putLong(key, 1337);
            } else if (actionKeys.contains(key)) {
                bundle.putByte(key, randomByte);
            }
            assertTrue(key, TaskerHelper.isVariableBundleValid(bundle));
        }

        // known keys but wrong value types
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            bundle.putByte(key, randomByte);
            if (actionKeys.contains(key)) {
                assertTrue(key, TaskerHelper.isVariableBundleValid(bundle));
            } else {
                assertFalse(key, TaskerHelper.isVariableBundleValid(bundle));
            }
        }

        // one valid key with valid type
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            if (booleanKeys.contains(key)) {
                bundle.putBoolean(key, true);
            } else if (intKeys.contains(key)) {
                bundle.putInt(key, 1337);
            } else if (longKeys.contains(key)) {
                bundle.putLong(key, 1337);
            } else if (actionKeys.contains(key)) {
                bundle.putByte(key, randomByte);
            }
            assertTrue(key, TaskerHelper.isVariableBundleValid(bundle));
        }

        // one valid key, one known key with wrong type
        bundle = new Bundle();
        bundle.putBoolean(booleanKeys.first(), true);
        for (String key : ALL_ACTIONS) {
            bundle = new Bundle();
            bundle.putByte(key, randomByte);
            if (actionKeys.contains(key)) {
                assertTrue(key, TaskerHelper.isVariableBundleValid(bundle));
            } else {
                assertFalse(key, TaskerHelper.isVariableBundleValid(bundle));
            }
        }

        // valid keys with variable instead of integers
        for (String key : intKeys) {
            bundle = new Bundle();
            bundle.putString(key, "%test");
            assertTrue(key, TaskerHelper.isVariableBundleValid(bundle));
        }

        // valid keys with wrong variable format instead of integers
        for (String key : intKeys) {
            bundle = new Bundle();
            bundle.putString(key, "abc123");
            assertFalse(key, TaskerHelper.isVariableBundleValid(bundle));
        }
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
        Context context = InstrumentationRegistry.getTargetContext();

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
}