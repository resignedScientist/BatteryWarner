package com.laudien.p1xelfehler.batterywarner;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.TwoStatePreference;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

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
     * @param context An instance of the Context class.
     * @throws NotRootedException thrown if the app has no root permissions.
     */
    public static void enableCharging(Context context) throws NotRootedException {
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        Shell.SU.run("echo 1 > /sys/class/power_supply/battery/charging_enabled");
        Log.d(TAG, "Charging was enabled!");
        /*Shell.SU.run(new String[]{
                "echo 1 > /sys/class/power_supply/battery/charging_enabled",
                "echo 1 > /sys/class/power_supply/battery/battery_charging_enabled"
        });*/
    }

    /**
     * Disables charging for the device.
     *
     * @param context An instance of the Context class.
     * @throws NotRootedException thrown if the app has no root permissions.
     */
    public static void disableCharging(Context context) throws NotRootedException {
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        Shell.SU.run("echo 0 > /sys/class/power_supply/battery/charging_enabled");
        Log.d(TAG, "Charging was disabled!");
        /*Shell.SU.run(new String[]{
                "echo 0 > /sys/class/power_supply/battery/charging_enabled",
                "echo 0 > /sys/class/power_supply/battery/battery_charging_enabled"
        });*/
    }

    /**
     * Checks if charging is enabled on the device.
     *
     * @return Returns true if charging is enabled, false if not.
     * @throws NotRootedException           thrown if the app has no root permissions.
     * @throws BatteryFileNotFoundException thrown if the file to change was not found.
     *                                      That means that the stop charging feature is not working with this device.
     */
    public static boolean isChargingEnabled() throws NotRootedException, BatteryFileNotFoundException {
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        List output = Shell.SU.run("cat /sys/class/power_supply/battery/charging_enabled");
        if (output != null && !output.isEmpty()) {
            return output.get(0).equals("1");
        } else {
            throw new BatteryFileNotFoundException();
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
                    } catch (RootHelper.BatteryFileNotFoundException e) {
                        NotificationBuilder.showNotification(context,
                                NotificationBuilder.ID_STOP_CHARGING_NOT_WORKING);
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
    public static class BatteryFileNotFoundException extends Exception {
        private BatteryFileNotFoundException() {
            super("The battery file was not found. Stop charging does not work with this device!");
        }
    }
}
