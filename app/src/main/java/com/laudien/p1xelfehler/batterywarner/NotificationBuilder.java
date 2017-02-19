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
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.MainActivity;
import com.laudien.p1xelfehler.batterywarner.Receivers.NotificationDismissReceiver;

import java.util.Locale;

public class NotificationBuilder {
    public static final int NOTIFICATION_SILENT_MODE = 2;
    public static final int NOTIFICATION_ID_SILENT_MODE = 1337;
    public static final int NOTIFICATION_ID_BATTERY_WARNING = 1338;
    public static final int NOTIFICATION_ID_STOP_CHARGING = 1339;
    static final int NOTIFICATION_WARNING_HIGH = 0;
    static final int NOTIFICATION_WARNING_LOW = 1;
    private Context context;

    public NotificationBuilder(Context context) {
        this.context = context;
    }

    public void showNotification(int type) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        switch (type) {
            case NOTIFICATION_WARNING_HIGH:
                if (!BatteryAlarmManager.isChargingNotificationEnabled(context, sharedPreferences)) {
                    return; // return if disabled in settings or not charging
                }
                if (sharedPreferences.getBoolean(context.getString(R.string.pref_already_notified), false)) {
                    return;
                }
                int warningHigh = sharedPreferences.getInt(context.getString(R.string.pref_warning_high), Contract.DEF_WARNING_HIGH);
                RootChecker.disableCharging(context);
                showNotification(
                        String.format(Locale.getDefault(), "%s %d%%!", context.getString(R.string.warning_high), warningHigh),
                        NOTIFICATION_ID_BATTERY_WARNING,
                        true,
                        false
                );
                sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), true).apply();
                break;
            case NOTIFICATION_WARNING_LOW:
                if (!BatteryAlarmManager.isDischargingNotificationEnabled(context, sharedPreferences)) {
                    return; // return if disabled in settings or charging
                }
                if (sharedPreferences.getBoolean(context.getString(R.string.pref_already_notified), false))
                    return;
                int warningLow = sharedPreferences.getInt(context.getString(R.string.pref_warning_low), Contract.DEF_WARNING_LOW);
                showNotification(
                        String.format(Locale.getDefault(), "%s %d%%!", context.getString(R.string.warning_low), warningLow),
                        NOTIFICATION_ID_BATTERY_WARNING,
                        true,
                        false
                );
                sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), true).apply();
                break;
            case NOTIFICATION_SILENT_MODE:
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                int ringerMode = audioManager.getRingerMode();
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                if (!notificationManager.areNotificationsEnabled()
                        || ringerMode == AudioManager.RINGER_MODE_SILENT
                        || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    showNotification(
                            context.getString(R.string.notifications_are_off),
                            NOTIFICATION_ID_SILENT_MODE,
                            true,
                            false
                    );
                }
                break;
            case NOTIFICATION_ID_STOP_CHARGING:
                try {
                    if (!RootChecker.isChargingEnabled()) {
                        showNotification(
                                "Dismiss the notification if the device is unplugged!",
                                NOTIFICATION_ID_STOP_CHARGING,
                                false,
                                true
                        );
                    }
                    break;
                } catch (NotRootedException e) {
                    e.printStackTrace();
                }
        }
    }

    private void showNotification(String contentText, int id, boolean sound, boolean dismissIntentEnabled) {
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent dismissIntent = null;
        if (dismissIntentEnabled) {
            dismissIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                    context, NotificationDismissReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        }
        Uri soundUri = null;
        long[] vibratePattern = null;
        if (sound) {
            soundUri = getNotificationSound();
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

    private Uri getNotificationSound() {
        String uri = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_sound_uri), "");
        if (!uri.equals("")) {
            return Uri.parse(uri); // saved URI
        } else {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // default URI
        }
    }
}
