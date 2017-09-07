package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseController;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;

import static android.os.Build.VERSION.SDK_INT;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_GRANT_ROOT;

/**
 * A BroadcastReceiver that is called by the system if the app has been updated.
 * It starts the {@link com.laudien.p1xelfehler.batterywarner.services.BackgroundService}
 * and asks for root permission if some root settings are used.
 * Does only work after the intro was finished.
 */
public class AppUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), true))
                return; // return if intro was not finished

            Log.d(getClass().getSimpleName(), "App update received!");
            // create notification channels
            if (SDK_INT >= Build.VERSION_CODES.O) {
                NotificationHelper.createNotificationChannels(context);
            }
            // upgrade databases if necessary
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        DatabaseController databaseController = DatabaseController.getInstance(context);
                        databaseController.upgradeAllSavedDatabases(context);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            // check if one of the root preferences is enabled
            String[] rootPreferences = context.getResources().getStringArray(R.array.root_preferences);
            boolean oneRootPermissionIsEnabled = false;
            for (String prefKey : rootPreferences) {
                boolean enabled = sharedPreferences.getBoolean(prefKey, false);
                if (enabled) {
                    oneRootPermissionIsEnabled = true;
                    break;
                }
            }
            if (oneRootPermissionIsEnabled) { // this notification starts the service on click
                NotificationHelper.showNotification(context, ID_GRANT_ROOT);
            } else { // start the service directly
                ServiceHelper.startService(context.getApplicationContext());
            }
        }
    }
}
