package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.TwoStatePreference;
import android.util.Log;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.R;

import java.io.File;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

import static android.os.Build.BRAND;
import static android.os.Build.MODEL;
import static android.os.Build.PRODUCT;

/**
 * Helper class that helps with all root queries in the app.
 * All methods have to run outside of the main/ui thread.
 */
public final class RootHelper {

    private static final String TAG = "RootHelper";

    /**
     * Checks if the app has root permissions. If the device is rooted, this method will trigger
     * a dialog to ask for root permissions depending on the super user app used.
     *
     * @return Returns true if the app has root permissions, false if not.
     */
    public static boolean isRootAvailable() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new InMainThreadException();
        }
        return Shell.SU.available();
    }

    /**
     * Enables charging for the device again.
     *
     * @throws NotRootedException thrown if the app has no root permissions.
     */
    public static void enableCharging() throws NotRootedException, NoBatteryFileFoundException {
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

    public static void handleRootDependingPreference(final Context context, final TwoStatePreference twoStatePreference) {
        if (context != null && twoStatePreference.isChecked()) {
            new AsyncTask<Void, Void, Boolean>() {
                // returns false if not rooted
                @Override
                protected Boolean doInBackground(Void... voids) {
                    try {
                        RootHelper.isChargingEnabled();
                    } catch (RootHelper.NotRootedException e) {
                        return false;
                    } catch (NoBatteryFileFoundException e) {
                        NotificationHelper.showNotification(context,
                                NotificationHelper.ID_STOP_CHARGING_NOT_WORKING);
                    }
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean rooted) {
                    super.onPostExecute(rooted);
                    if (!rooted) { // show a toast if not rooted
                        Toast.makeText(context, context.getString(R.string.toast_not_rooted), Toast.LENGTH_SHORT).show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                twoStatePreference.setChecked(false);
                            }
                        }, context.getResources().getInteger(R.integer.root_check_switch_back_delay));
                    }
                }
            }.execute();
        }
    }

    private static ToggleChargingFile getAvailableFile() throws NoBatteryFileFoundException {
        if (MODEL.contains("Pixel")) {
            return new ToggleChargingFile("/sys/class/power_supply/battery/battery_charging_enabled", "1", "0");
        }
        if (BRAND.equals("OnePlus") || PRODUCT.equals("angler") || BRAND.equals("motorola") || BRAND.equals("lge")) {
            return new ToggleChargingFile("/sys/class/power_supply/battery/charging_enabled", "1", "0");
        }
        if (BRAND.equals("samsung")) {
            return new ToggleChargingFile("/sys/class/power_supply/battery/batt_slate_mode", "0", "1");
        }
        ToggleChargingFile[] files = new ToggleChargingFile[]{
                new ToggleChargingFile("/sys/class/power_supply/battery/battery_charging_enabled", "1", "0"),
                new ToggleChargingFile("/sys/class/power_supply/battery/charging_enabled", "1", "0"),
                new ToggleChargingFile("/sys/class/power_supply/battery/store_mode", "0", "1"),
                new ToggleChargingFile("/sys/class/power_supply/battery/batt_slate_mode", "0", "1"),
                new ToggleChargingFile("/sys/class/hw_power/charger/charge_data/enable_charger", "1", "0"),
                new ToggleChargingFile("/sys/module/pm8921_charger/parameters/disabled", "0", "1")
        };
        for (ToggleChargingFile file : files) {
            if (new File(file.path).exists()) {
                Log.d("RootHelper", "File found: " + file.path);
                return file;
            }
        }
        Log.d("RootHelper", "No file found!");
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
        private String path, chargeOn, chargeOff;

        private ToggleChargingFile(String path, String chargeOn, String chargeOff) {
            this.path = path;
            this.chargeOn = chargeOn;
            this.chargeOff = chargeOff;
        }
    }
}
