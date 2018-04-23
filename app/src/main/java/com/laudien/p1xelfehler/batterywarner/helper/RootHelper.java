package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.R;

import java.io.File;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

import static android.os.Build.BRAND;
import static android.os.Build.MODEL;
import static android.os.Build.PRODUCT;
import static com.laudien.p1xelfehler.batterywarner.receivers.RootCheckFinishedReceiver.ACTION_ROOT_CHECK_FINISHED;
import static com.laudien.p1xelfehler.batterywarner.receivers.RootCheckFinishedReceiver.EXTRA_PREFERENCE;
import static com.laudien.p1xelfehler.batterywarner.receivers.RootCheckFinishedReceiver.EXTRA_ROOT_ALLOWED;

/**
 * Helper class that helps with all root queries in the app.
 * All methods have to run outside of the main/ui thread.
 */
public final class RootHelper {

    private static final String TAG = "RootHelper";

    /**
     * Checks if the app has root permissions. If the device is rooted, this method will possibly
     * trigger a dialog to ask for root permissions depending on the super user app used.
     *
     * @return Returns true if the app has root permissions, false if not.
     */
    public static boolean isRootAvailable() {
        Log.d(TAG, "Checking for root...");
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new InMainThreadException();
        }
        boolean rootAvailable = Shell.SU.available();
        if (rootAvailable) {
            Log.d(TAG, "Root is available! :)");
        } else {
            Log.e(TAG, "Root is not available! :(");
        }
        return rootAvailable;
    }

    /**
     * Checks if any preference is enabled that requires root.
     *
     * @param context           An instance of the Context class.
     * @param sharedPreferences The default SharedPreferences. Can be null.
     * @return Returns true, if any root preference is enabled and false if all root preferences are disabled.
     */
    public static boolean isAnyRootPreferenceEnabled(Context context, @Nullable SharedPreferences sharedPreferences) {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        String[] rootPreferences = context.getResources().getStringArray(R.array.root_preferences);
        for (String prefKey : rootPreferences) {
            boolean enabled = sharedPreferences.getBoolean(prefKey, false);
            if (enabled) {
                return true;
            }
        }
        return false;
    }

    /**
     * Disables all preferences that require root.
     *
     * @param context An instance of the Context class.
     */
    public static void disableAllRootPreferences(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String[] rootPreferences = context.getResources().getStringArray(R.array.root_preferences);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String key : rootPreferences) {
            editor.putBoolean(key, false);
        }
        editor.apply();
    }

    /**
     * Enables charging for the device again.
     *
     * @throws NotRootedException thrown if the app has no root permissions.
     */
    public static void enableCharging() throws NotRootedException, NoBatteryFileFoundException {
        Log.d(TAG, "Enabling charging...");
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        ToggleChargingFile toggleChargingFile = getAvailableFile();
        Shell.SU.run(String.format("echo %s > %s", toggleChargingFile.chargeOn, toggleChargingFile.path));
        Log.d(TAG, "Charging was enabled!");
    }

    /**
     * Disables charging for the device.
     *
     * @throws NotRootedException thrown if the app has no root permissions.
     */
    public static void disableCharging() throws NotRootedException, NoBatteryFileFoundException {
        Log.d(TAG, "Disabling charging...");
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        ToggleChargingFile toggleChargingFile = getAvailableFile();
        Shell.SU.run(String.format("echo %s > %s", toggleChargingFile.chargeOff, toggleChargingFile.path));
        Log.d(TAG, "Charging was disabled!");
    }

    /**
     * Checks if charging is enabled on the device.
     *
     * @return Returns true if charging is enabled, false if not.
     * @throws NotRootedException          thrown if the app has no root permissions.
     * @throws NoBatteryFileFoundException thrown if the file to change was not found.
     *                                     That means that the stop charging feature is not working with this device.
     */
    public static boolean isChargingEnabled() throws NotRootedException, NoBatteryFileFoundException {
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        ToggleChargingFile toggleChargingFile = getAvailableFile();
        List output = Shell.SU.run("cat " + toggleChargingFile.path);
        if (output != null && !output.isEmpty()) {
            return output.get(0).equals(toggleChargingFile.chargeOn);
        } else {
            throw new NoBatteryFileFoundException();
        }
    }

    /**
     * Toggles the power saving mode on or off.
     *
     * @param turnOn True = turn power saving mode on, False = turn it off
     * @throws NotRootedException thrown if the app has no root permissions.
     */
    public static void togglePowerSavingMode(boolean turnOn) throws NotRootedException {
        if (isRootAvailable()) {
            Log.d(TAG, "Turning " + (turnOn ? "on" : "off") + " power saving mode...");
            Shell.SU.run("settings put global low_power " + (turnOn ? "1" : "0"));
        } else {
            throw new NotRootedException();
        }
    }

    /**
     * Resets the android internal battery stats
     *
     * @throws NotRootedException thrown if the app has no root permissions.
     */
    public static void resetBatteryStats() throws NotRootedException {
        if (isRootAvailable()) {
            Log.d(TAG, "Resetting android battery stats...");
            Shell.SU.run("dumpsys batterystats --reset");
        } else {
            throw new NotRootedException();
        }
    }

    public static void handleRootDependingPreference(final Context context, final String preference) {
        if (context != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(ACTION_ROOT_CHECK_FINISHED);
                    intent.putExtra(EXTRA_PREFERENCE, preference);
                    boolean rootAllowed = true;
                    try {
                        RootHelper.isChargingEnabled();
                    } catch (RootHelper.NotRootedException e) {
                        rootAllowed = false;
                    } catch (NoBatteryFileFoundException e) {
                        NotificationHelper.showNotification(context,
                                NotificationHelper.ID_STOP_CHARGING_NOT_WORKING);
                    }
                    intent.putExtra(EXTRA_ROOT_ALLOWED, rootAllowed);
                    context.sendBroadcast(intent);
                }
            });
        }
    }

    private static ToggleChargingFile getAvailableFile() throws NoBatteryFileFoundException {
        ToggleChargingFile toggleChargingFile = null;
        if (PRODUCT.toLowerCase().equals("walleye") || MODEL.equals("Pixel 2") || PRODUCT.equals("OnePlus5T")) { // Pixel 2 or OnePlus 5T
            return new ToggleChargingFile("/sys/class/power_supply/battery/input_suspend", "0", "1");
        } else if (MODEL.contains("Pixel")) {
            toggleChargingFile = new ToggleChargingFile("/sys/class/power_supply/battery/battery_charging_enabled", "1", "0");
        } else if (BRAND.equals("OnePlus") || PRODUCT.equals("angler") || BRAND.equals("motorola") || BRAND.equals("lge")) {
            toggleChargingFile = new ToggleChargingFile("/sys/class/power_supply/battery/charging_enabled", "1", "0");
        } else if (BRAND.equals("samsung")) {
            toggleChargingFile = new ToggleChargingFile("/sys/class/power_supply/battery/batt_slate_mode", "0", "1");
        }
        if (toggleChargingFile != null && new File(toggleChargingFile.path).exists()) {
            return toggleChargingFile;
        }
        ToggleChargingFile[] files = new ToggleChargingFile[]{
                new ToggleChargingFile("/sys/class/power_supply/battery/battery_charging_enabled", "1", "0"),
                new ToggleChargingFile("/sys/class/power_supply/battery/charging_enabled", "1", "0"),
                new ToggleChargingFile("/sys/class/power_supply/battery/batt_slate_mode", "0", "1"),
                new ToggleChargingFile("/sys/class/hw_power/charger/charge_data/enable_charger", "1", "0"),
                new ToggleChargingFile("/sys/module/pm8921_charger/parameters/disabled", "0", "1"),
                new ToggleChargingFile("/sys/devices/qpnp-charger-f2d04c00/power_supply/battery/charging_enabled", "1", "0"),
                new ToggleChargingFile("/sys/devices/qpnp-charger-14/power_supply/battery/charging_enabled", "1", "0"),
                new ToggleChargingFile("/sys/class/power_supply/battery/input_suspend", "0", "1"),
                new ToggleChargingFile("/sys/class/power_supply/ac/charging_enabled", "1", "0"),
                new ToggleChargingFile("/sys/class/power_supply/battery/charge_enabled", "1", "0"),
                new ToggleChargingFile("/sys/class/power_supply/battery/device/Charging_Enable", "1", "0"),
                new ToggleChargingFile("/sys/devices/platform/7000c400.i2c/i2c-1/1-006b/charging_state", "enabled", "disabled"),
                new ToggleChargingFile("/sys/class/power_supply/battery/charge_disable", "0", "1")
                /*new ToggleChargingFile("/sys/class/power_supply/battery/charger_control", "1", "0"), // experimental
                new ToggleChargingFile("/sys/class/power_supply/bq2589x_charger/enable_charging", "1", "0"), // experimental
                new ToggleChargingFile("/sys/class/power_supply/chargalg/disable_charging", "0", "1"), // experimental
                new ToggleChargingFile("/sys/class/power_supply/dollar_cove_charger/present", "1", "0"), // experimental
                new ToggleChargingFile("/sys/devices/platform/battery/ChargerEnable", "1", "0"), // experimental
                new ToggleChargingFile("/sys/devices/platform/huawei_charger/enable_charger", "1", "0"), // experimental
                new ToggleChargingFile("/sys/devices/platform/mt-battery/disable_charger", "0", "1"), // experimental
                new ToggleChargingFile("/sys/devices/platform/tegra12-i2c.0/i2c-0/0-006b/charging_state", "enabled", "disabled"), // experimental
                new ToggleChargingFile("/sys/devices/virtual/power_supply/manta-battery/charge_enabled", "1", "0")*/ // experimental
        };
        for (ToggleChargingFile file : files) {
            if (new File(file.path).exists()) {
                Log.d("RootHelper", "File found: " + file.path);
                return file;
            }
        }
        Log.e("RootHelper", "No battery file found to toggle charging! :(");
        throw new NoBatteryFileFoundException();
    }

    /**
     * Exception that is thrown if the app has no root permissions.
     */
    public static class NotRootedException extends Exception {
        private NotRootedException() {
            super("The device is not rooted!");
        }
    }

    /**
     * RuntimeException that is thrown if one of the methods in this class runs in the main/ui thread.
     */
    private static class InMainThreadException extends RuntimeException {
        private InMainThreadException() {
            super("Root calls must be done outside of the main thread!");
        }
    }

    /**
     * Exception that is thrown if the file to change was not found.
     * That means that the stop charging feature is not working with this device.
     */
    public static class NoBatteryFileFoundException extends Exception {
        private NoBatteryFileFoundException() {
            super("The battery file was not found. Stop charging does not work with this device!");
        }
    }

    private static class ToggleChargingFile {
        private final String path;
        private final String chargeOn;
        private final String chargeOff;

        private ToggleChargingFile(String path, String chargeOn, String chargeOff) {
            this.path = path;
            this.chargeOn = chargeOn;
            this.chargeOff = chargeOff;
        }
    }
}
