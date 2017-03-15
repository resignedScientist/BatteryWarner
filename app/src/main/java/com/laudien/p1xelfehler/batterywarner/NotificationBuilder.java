package com.laudien.p1xelfehler.batterywarner;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.MainActivity;
import com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity.SettingsActivity;
import com.laudien.p1xelfehler.batterywarner.Services.DisableRootFeaturesService;
import com.laudien.p1xelfehler.batterywarner.Services.EnableChargingService;
import com.laudien.p1xelfehler.batterywarner.Services.GrantRootService;

import java.util.Locale;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Helper class to show a notification with the given type. All notifications used in the app are listed here.
 */
public final class NotificationBuilder {
    /**
     * Notification id of the notification that warns if silent/vibrate mode is turned on.
     */
    public static final int ID_SILENT_MODE = 1337;
    /** Notification id of the notification that warns that the battery is above X%. */
    public static final int ID_WARNING_HIGH = 1338;
    /** Notification id of the notification that warns that the battery is below Y%. */
    public static final int ID_WARNING_LOW = 1339;
    /** Notification id of the notification that the user has to click/dismiss after the device is unplugged.
     * Only shown if the stop charging feature is enabled. */
    public static final int ID_STOP_CHARGING = 1340;
    /** Notification id of the notification that asks the user for root again after the app was updated. */
    public static final int ID_GRANT_ROOT = 1341;
    /** Notification id of the notification that tells the user that the stop charging feature is not working
     * on this device.
     */
    public static final int ID_STOP_CHARGING_NOT_WORKING = 1342;
    /** Notification id of the notification that asks for root again if app has no root rights anymore. */
    public static final int ID_NOT_ROOTED = 1343;

    private NotificationBuilder() {
    }

    /**
     * Shows the notification with the given id.
     *
     * @param context        An instance of the Context class.
     * @param notificationID The id of the notification - usually one of the id constants.
     */
    public static void showNotification(final Context context, final int notificationID) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        switch (notificationID) {
            case ID_WARNING_HIGH:
                Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batteryStatus == null) {
                    return;
                }
                boolean isEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default));
                boolean warningHighEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
                boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
                boolean stopCharging = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
                if (!isEnabled || !warningHighEnabled || !isCharging) {
                    return; // return if disabled in settings or not charging
                }
                int warningHigh = sharedPreferences.getInt(context.getString(R.string.pref_warning_high), context.getResources().getInteger(R.integer.pref_warning_high_default));
                if (stopCharging) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                RootChecker.disableCharging(context);
                            } catch (RootChecker.NotRootedException e) {
                                e.printStackTrace();
                                showNotification(context, ID_NOT_ROOTED);
                            }
                        }
                    });
                }
                showNotification(
                        context,
                        String.format(Locale.getDefault(), "%s %d%%!", context.getString(R.string.warning_high), warningHigh),
                        notificationID,
                        getNotificationSound(context),
                        null,
                        null
                );
                sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), true).apply();
                break;
            case ID_WARNING_LOW:
                Intent batteryStatus1 = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batteryStatus1 == null) {
                    return;
                }
                boolean isEnabled1 = sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default));
                boolean warningLowEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_low_enabled), context.getResources().getBoolean(R.bool.pref_warning_low_enabled_default));
                boolean isCharging1 = batteryStatus1.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
                boolean alreadyNotified1 = sharedPreferences.getBoolean(context.getString(R.string.pref_already_notified), context.getResources().getBoolean(R.bool.pref_already_notified_default));

                if (alreadyNotified1 || isCharging1 || !isEnabled1 || !warningLowEnabled) {
                    return; // return if disabled in settings or charging
                }
                if (sharedPreferences.getBoolean(context.getString(R.string.pref_already_notified), context.getResources().getBoolean(R.bool.pref_already_notified_default))) {
                    return;
                }
                int warningLow = sharedPreferences.getInt(context.getString(R.string.pref_warning_low), context.getResources().getInteger(R.integer.pref_warning_low_default));
                showNotification(
                        context,
                        String.format(Locale.getDefault(), "%s %d%%!", context.getString(R.string.warning_low), warningLow),
                        notificationID,
                        getNotificationSound(context),
                        null,
                        null
                );
                sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), true).apply();
                break;
            case ID_SILENT_MODE:
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                int ringerMode = audioManager.getRingerMode();
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                if (!notificationManager.areNotificationsEnabled()
                        || ringerMode == AudioManager.RINGER_MODE_SILENT
                        || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    showNotification(
                            context,
                            context.getString(R.string.notifications_are_off),
                            notificationID,
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            null,
                            null
                    );
                }
                break;
            case ID_STOP_CHARGING:
                boolean stopChargingEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
                if (stopChargingEnabled) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (!RootChecker.isChargingEnabled()) {
                                    PendingIntent pendingIntent = PendingIntent.getService(context, 0, new Intent(
                                            context, EnableChargingService.class), FLAG_UPDATE_CURRENT);
                                    showNotification(
                                            context,
                                            context.getString(R.string.dismiss_if_unplugged),
                                            notificationID,
                                            null,
                                            pendingIntent,
                                            pendingIntent
                                    );
                                }
                            } catch (RootChecker.NotRootedException e) {
                                e.printStackTrace();
                                showNotification(context, ID_NOT_ROOTED);
                            } catch (RootChecker.BatteryFileNotFoundException e) {
                                showNotification(context, ID_STOP_CHARGING_NOT_WORKING);
                            }
                        }
                    });
                }
                break;
            case ID_STOP_CHARGING_NOT_WORKING:
                showNotification(
                        context,
                        context.getString(R.string.stop_charging_not_working),
                        0,
                        null,
                        PendingIntent.getActivity(
                                context,
                                notificationID,
                                new Intent(context, SettingsActivity.class),
                                FLAG_UPDATE_CURRENT
                        ),
                        null
                );
                break;
            case ID_GRANT_ROOT:
                boolean stopCharging1 = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
                boolean usbDisabled = sharedPreferences.getBoolean(context.getString(R.string.pref_usb_charging_disabled), context.getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
                if (stopCharging1 || usbDisabled) {
                    showNotification(
                            context,
                            context.getString(R.string.grant_root_again),
                            0,
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            PendingIntent.getService(
                                    context, 0, new Intent(context, GrantRootService.class),
                                    FLAG_UPDATE_CURRENT
                            ),
                            PendingIntent.getService(context, 0,
                                    new Intent(context, DisableRootFeaturesService.class), FLAG_UPDATE_CURRENT)
                    );
                }
                break;
            case ID_NOT_ROOTED:
                showNotification(
                        context,
                        context.getString(R.string.not_rooted_notification),
                        notificationID,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        PendingIntent.getService(
                                context, 0, new Intent(context, GrantRootService.class),
                                FLAG_UPDATE_CURRENT
                        ),
                        PendingIntent.getService(context, 0,
                                new Intent(context, DisableRootFeaturesService.class), FLAG_UPDATE_CURRENT)
                );
                break;
        }
    }

    private static void showNotification(Context context, String contentText, int id, Uri sound, PendingIntent clickIntent, PendingIntent dismissIntent) {
        Log.d("NotificationBuilder", contentText);
        if (clickIntent == null) {
            clickIntent = PendingIntent.getActivity(
                    context, 0, new Intent(context, MainActivity.class), FLAG_UPDATE_CURRENT);
        }
        long[] vibratePattern = null;
        if (sound != null) {
            vibratePattern = new long[]{0, 300, 300, 300};
        }
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.bigText(contentText);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(sound)
                .setVibrate(vibratePattern)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText)
                .setStyle(bigTextStyle)
                .setContentIntent(clickIntent)
                .setAutoCancel(true)
                .setDeleteIntent(dismissIntent);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    private static Uri getNotificationSound(Context context) {
        String uri = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_sound_uri), "");
        if (!uri.equals("")) {
            return Uri.parse(uri); // saved URI
        } else {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // default URI
        }
    }
}
