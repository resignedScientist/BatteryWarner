package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.laudien.p1xelfehler.batterywarner.R;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
    public static void enableCharging(@NonNull Context context) throws NotRootedException, NoBatteryFileFoundException {
        Log.d(TAG, "Enabling charging...");
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        ToggleChargingFile toggleChargingFile = getAvailableFile(context);
        Shell.SU.run(String.format("echo %s > %s", toggleChargingFile.chargeOn, toggleChargingFile.path));
        Log.d(TAG, "Charging was enabled!");
    }

    /**
     * Disables charging for the device.
     *
     * @throws NotRootedException thrown if the app has no root permissions.
     */
    public static void disableCharging(@NonNull Context context) throws NotRootedException, NoBatteryFileFoundException {
        Log.d(TAG, "Disabling charging...");
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        ToggleChargingFile toggleChargingFile = getAvailableFile(context);
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
    public static boolean isChargingEnabled(@NonNull Context context) throws NotRootedException, NoBatteryFileFoundException {
        if (!isRootAvailable()) {
            throw new NotRootedException();
        }
        ToggleChargingFile toggleChargingFile = getAvailableFile(context);
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
                        RootHelper.isChargingEnabled(context);
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

    @Nullable
    private static ToggleChargingFile findExistingFile(ToggleChargingFile[] files) {
        String[] commands = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            commands[i] = "[ -f " + files[i].path + " ] && echo \"yes\" || echo \"no\"";
        }
        List output = Shell.SU.run(commands);
        if (output == null) {
            return null;
        }
        int index = output.indexOf("yes");
        if (index > -1) {
            return files[index];
        }
        return null;
    }

    private static ToggleChargingFile[] readFiles(@NonNull Context context) {
        Gson gson = new Gson();
        InputStream inputStream = context.getResources().openRawResource(R.raw.stop_charging_files);
        Reader reader = new InputStreamReader(inputStream);
        return gson.fromJson(reader, ToggleChargingFile[].class);
    }

    private static ToggleChargingFile getAvailableFile(@NonNull Context context) throws NoBatteryFileFoundException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean autoPickEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging_auto_pick), context.getResources().getBoolean(R.bool.pref_stop_charging_auto_pick_default));
        if (!autoPickEnabled) {
            return new ToggleChargingFile(
                    sharedPreferences.getString(context.getString(R.string.pref_stop_charging_file), ""),
                    sharedPreferences.getString(context.getString(R.string.pref_stop_charging_enable_charging_text), context.getString(R.string.pref_stop_charging_enable_charging_text_default)),
                    sharedPreferences.getString(context.getString(R.string.pref_stop_charging_disable_charging_text), context.getString(R.string.pref_stop_charging_disable_charging_text_default))
            );
        }
        ToggleChargingFile[] files = readFiles(context);
        String product = PRODUCT.toLowerCase();
        String model = MODEL.toLowerCase();
        String brand = BRAND.toLowerCase();
        // find file by product, model or brand
        for (ToggleChargingFile file : files) {
            if (file.products != null && !product.isEmpty()) {
                for (String item : file.products) {
                    if (item.toLowerCase().contains(product)) {
                        Log.d("RootHelper", "File found with product: " + file.path);
                        return file;
                    }
                }
            }
            if (file.models != null && !model.isEmpty()) {
                for (String item : file.models) {
                    if (item.toLowerCase().contains(model)) {
                        Log.d("RootHelper", "File found with model: " + file.path);
                        return file;
                    }
                }
            }
            if (file.brands != null && !model.isEmpty()) {
                for (String item : file.brands) {
                    if (item.toLowerCase().contains(brand)) {
                        Log.d("RootHelper", "File found with brand: " + file.path);
                        return file;
                    }
                }
            }
        }
        // return first existing file
        ToggleChargingFile file = findExistingFile(files);
        if (file != null) {
            Log.d("RootHelper", "File found the long way: " + file.path);
            return file;
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
        @NonNull
        private final String path, chargeOn, chargeOff;
        @Nullable
        private List<String> products, models, brands;

        ToggleChargingFile(@NonNull String path, @NonNull String chargeOn, @NonNull String chargeOff) {
            this.path = path;
            this.chargeOn = chargeOn;
            this.chargeOff = chargeOff;
        }

        public ToggleChargingFile(@NonNull String path, @NonNull String chargeOn, @NonNull String chargeOff, @Nullable List<String> products, @Nullable List<String> models, @Nullable List<String> brands) {
            this.path = path;
            this.chargeOn = chargeOn;
            this.chargeOff = chargeOff;
            this.products = products;
            this.models = models;
            this.brands = brands;
        }
    }
}
