package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;

import static android.content.Context.MODE_PRIVATE;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_GRANT_ROOT;

/**
 * A BroadcastReceiver that is called by the system if the app has been updated.
 * It starts some services if necessary and asks for root permission if some root settings are used.
 * Does only work after the intro was finished.
 */
public class AppUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), context.getResources().getBoolean(R.bool.pref_first_start_default)))
                return; // return if intro was not finished

            Log.d(getClass().getSimpleName(), "App update received!");
            // patch old shared preferences v1.109(144) =>
            SharedPreferences temporaryPrefs = context.getSharedPreferences(context.getString(R.string.prefs_temporary), MODE_PRIVATE);
            // last percentage (remove it, it is no longer used!)
            String key = "lastPercentage";
            if (sharedPreferences.contains(key)) {
                sharedPreferences.edit().remove(key).apply();
            }
            // intent time
            key = context.getString(R.string.pref_intent_time);
            if (sharedPreferences.contains(key)) {
                temporaryPrefs.edit().putLong(key, sharedPreferences.getLong(key, -1)).apply();
                sharedPreferences.edit().remove(key).apply();
            }
            // already notified
            key = context.getString(R.string.pref_already_notified);
            if (sharedPreferences.contains(key)) {
                temporaryPrefs.edit().putBoolean(key, sharedPreferences.getBoolean(key, false)).apply();
                sharedPreferences.edit().remove(key).apply();
            }
            // last chargingType
            key = context.getString(R.string.pref_last_chargingType);
            if (sharedPreferences.contains(key)) {
                temporaryPrefs.edit().putInt(key, sharedPreferences.getInt(key, -1)).apply();
                sharedPreferences.edit().remove(key).apply();
            }
            // screen on time
            key = context.getString(R.string.value_time_screen_on);
            if (sharedPreferences.contains(key)) {
                temporaryPrefs.edit().putLong(key, sharedPreferences.getLong(key, 0)).apply();
                sharedPreferences.edit().remove(key).apply();
            }
            // screen off time
            key = context.getString(R.string.value_time_screen_off);
            if (sharedPreferences.contains(key)) {
                temporaryPrefs.edit().putLong(key, sharedPreferences.getLong(key, 0)).apply();
                sharedPreferences.edit().remove(key).apply();
            }
            // screen on drain
            key = context.getString(R.string.value_drain_screen_on);
            if (sharedPreferences.contains(key)) {
                temporaryPrefs.edit().putInt(key, sharedPreferences.getInt(key, 0)).apply();
                sharedPreferences.edit().remove(key).apply();
            }
            // screen off drain
            key = context.getString(R.string.value_drain_screen_off);
            if (sharedPreferences.contains(key)) {
                temporaryPrefs.edit().putInt(key, sharedPreferences.getInt(key, 0)).apply();
                sharedPreferences.edit().remove(key).apply();
            }
            // <= patch old shared preferences
            // create notification channels
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationHelper.createNotificationChannels(context);
            }
            // show notification if not rooted anymore
            NotificationHelper.showNotification(context, ID_GRANT_ROOT);
            // start the services
            ServiceHelper.startService(context, sharedPreferences, ServiceHelper.ID_CHARGING);
            ServiceHelper.startService(context, sharedPreferences, ServiceHelper.ID_DISCHARGING);
        }
    }
}
