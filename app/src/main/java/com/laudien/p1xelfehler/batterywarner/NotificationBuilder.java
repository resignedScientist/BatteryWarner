package com.laudien.p1xelfehler.batterywarner;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.MainActivity;
import com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity.SettingsActivity;
import com.laudien.p1xelfehler.batterywarner.Receivers.GrantRootReceiver;
import com.laudien.p1xelfehler.batterywarner.Receivers.NotificationDismissReceiver;

import java.util.Locale;

public final class NotificationBuilder {
    public static final int NOTIFICATION_SILENT_MODE = 2;
    public static final int NOTIFICATION_ID_SILENT_MODE = 1337;
    public static final int NOTIFICATION_ID_BATTERY_WARNING = 1338;
    public static final int NOTIFICATION_ID_STOP_CHARGING = 1339;
    public static final int NOTIFICATION_ID_GRANT_ROOT = 1340;
    public static final int NOTIFICATION_ID_STOP_CHARGING_NOT_WORKING = 1341;
    static final int NOTIFICATION_WARNING_HIGH = 0;
    static final int NOTIFICATION_WARNING_LOW = 1;

    private NotificationBuilder() {
    }

    public static void showNotification(final Context context, final int type) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        switch (type) {
            case NOTIFICATION_WARNING_HIGH:
                if (!BatteryAlarmManager.isChargingNotificationEnabled(context, sharedPreferences)) {
                    return; // return if disabled in settings or not charging
                }
                if (sharedPreferences.getBoolean(context.getString(R.string.pref_already_notified), context.getResources().getBoolean(R.bool.pref_already_notified_default))) {
                    return;
                }
                int warningHigh = sharedPreferences.getInt(context.getString(R.string.pref_warning_high), context.getResources().getInteger(R.integer.pref_warning_high_default));
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            RootChecker.disableCharging(context);
                        } catch (RootChecker.NotRootedException e) {
                            e.printStackTrace();
                        }
                    }
                });

                showNotification(
                        context,
                        String.format(Locale.getDefault(), "%s %d%%!", context.getString(R.string.warning_high), warningHigh),
                        NOTIFICATION_ID_BATTERY_WARNING,
                        true,
                        null,
                        null
                );
                sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), true).apply();
                break;
            case NOTIFICATION_WARNING_LOW:
                if (!BatteryAlarmManager.isDischargingNotificationEnabled(context, sharedPreferences)) {
                    return; // return if disabled in settings or charging
                }
                if (sharedPreferences.getBoolean(context.getString(R.string.pref_already_notified), context.getResources().getBoolean(R.bool.pref_already_notified_default))) {
                    return;
                }
                int warningLow = sharedPreferences.getInt(context.getString(R.string.pref_warning_low), context.getResources().getInteger(R.integer.pref_warning_low_default));
                showNotification(
                        context,
                        String.format(Locale.getDefault(), "%s %d%%!", context.getString(R.string.warning_low), warningLow),
                        NOTIFICATION_ID_BATTERY_WARNING,
                        true,
                        null,
                        null
                );
                sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), context.getResources().getBoolean(R.bool.pref_already_notified_default)).apply();
                break;
            case NOTIFICATION_SILENT_MODE:
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                int ringerMode = audioManager.getRingerMode();
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                if (!notificationManager.areNotificationsEnabled()
                        || ringerMode == AudioManager.RINGER_MODE_SILENT
                        || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    showNotification(
                            context,
                            context.getString(R.string.notifications_are_off),
                            type,
                            true,
                            null,
                            null
                    );
                }
                break;
            case NOTIFICATION_ID_STOP_CHARGING:
                if (sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default))) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (!RootChecker.isChargingEnabled()) {
                                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                                            context, NotificationDismissReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
                                    showNotification(
                                            context,
                                            context.getString(R.string.dismiss_if_unplugged),
                                            type,
                                            false,
                                            pendingIntent,
                                            pendingIntent
                                    );
                                }
                            } catch (RootChecker.NotRootedException e) {
                                e.printStackTrace();
                            } catch (RootChecker.BatteryFileNotFoundException e) {
                                showNotification(context, NOTIFICATION_ID_STOP_CHARGING_NOT_WORKING);
                            }
                        }
                    });
                    break;
                }
            case NOTIFICATION_ID_STOP_CHARGING_NOT_WORKING:
                showNotification(
                        context,
                        context.getString(R.string.stop_charging_not_working),
                        0,
                        false,
                        PendingIntent.getActivity(
                                context,
                                type,
                                new Intent(context, SettingsActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        ),
                        null
                );
                break;
            case NOTIFICATION_ID_GRANT_ROOT:
                final String pref_stop_charging = context.getString(R.string.pref_stop_charging);
                if (sharedPreferences.getBoolean(pref_stop_charging, context.getResources().getBoolean(R.bool.pref_stop_charging_default))) {
                    AsyncTask.execute(new Runnable() { // run in another thread
                        @Override
                        public void run() {
                            sharedPreferences.edit().putBoolean(pref_stop_charging, false).apply();
                            showNotification(
                                    context,
                                    context.getString(R.string.grant_root_again),
                                    0,
                                    true,
                                    PendingIntent.getBroadcast(
                                            context, 0, new Intent(context, GrantRootReceiver.class),
                                            PendingIntent.FLAG_UPDATE_CURRENT
                                    ),
                                    null
                            );
                        }
                    });

                }
                break;
        }
    }

    public static void showNotification(Context context, String contentText, int id, boolean sound, PendingIntent contentIntent, PendingIntent dismissIntent) {
        if (contentIntent == null) {
            contentIntent = PendingIntent.getActivity(
                    context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        }
        Uri soundUri = null;
        long[] vibratePattern = null;
        if (sound) {
            soundUri = getNotificationSound(context);
            vibratePattern = new long[]{0, 300, 300, 300};
        }
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.bigText(contentText);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(soundUri)
                .setVibrate(vibratePattern)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText)
                .setStyle(bigTextStyle)
                .setContentIntent(contentIntent)
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
