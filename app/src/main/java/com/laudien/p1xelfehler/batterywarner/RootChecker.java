package com.laudien.p1xelfehler.batterywarner;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

/**
 * Helper class that helps with all root queries in the app.
 * All methods have to run outside of the main/ui thread.
 */
public final class RootChecker {
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
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default))) {
            return;
        }
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        Shell.SU.run("echo 1 > /sys/class/power_supply/battery/charging_enabled");
    }

    /**
     * Disables charging for the device.
     *
     * @param context An instance of the Context class.
     * @throws NotRootedException thrown if the app has no root permissions.
     */
    public static void disableCharging(Context context) throws NotRootedException {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default))) {
            return;
        }
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        Shell.SU.run("echo 0 > /sys/class/power_supply/battery/charging_enabled");
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

    public static List<Long> getAlarmList() throws NotRootedException {
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        List<Long> alarmList = new ArrayList<>();
        List<String> output = Shell.SU.run("dumpsys alarm");
        String line;
        long start = 0, end = 0;
        for (int i = 0; i < output.size(); i++) {
            line = output.get(i);
            if (line.contains("deskclock") && line.contains("RTC_WAKEUP")) {
                String batchLine = output.get(i - 1);
                System.out.println(batchLine);
                String[] subStrings = batchLine.split(" ");
                for (String s : subStrings) {
                    if (s.contains("start")) {
                        start = Long.parseLong(s.substring(6));
                    }
                    if (s.contains("end")) {
                        end = Long.parseLong(s.substring(4).replace("}:", ""));
                    }
                }
                if (start == end && start != 0) {
                    alarmList.add(start);
                }
            }
        }
        if (alarmList.isEmpty()) {
            return null;
        } else {
            return alarmList;
        }
    }

    public static void handleRootDependingPreference(final Context context, final TwoStatePreference twoStatePreference) {
        if (context != null && twoStatePreference.isChecked()) {
            new AsyncTask<Void, Void, Boolean>() {
                // returns false if not rooted
                @Override
                protected Boolean doInBackground(Void... voids) {
                    try {
                        RootChecker.isChargingEnabled();
                    } catch (RootChecker.NotRootedException e) {
                        return false;
                    } catch (RootChecker.BatteryFileNotFoundException e) {
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
                        }, 500);
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
