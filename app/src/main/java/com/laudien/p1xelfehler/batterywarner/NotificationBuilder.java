package com.laudien.p1xelfehler.batterywarner;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.MainActivity;
import com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity.SettingsFragment;

public class NotificationBuilder {
    public static final int NOTIFICATION_WARNING_HIGH = 0;
    public static final int NOTIFICATION_WARNING_LOW = 1;
    public static final int NOTIFICATION_SILENT_MODE = 2;
    public static final int NOTIFICATION_ID_SILENT_MODE = 1337;
    public static final int NOTIFICATION_ID_BATTERY_WARNING = 1338;
    private Context context;

    public NotificationBuilder(Context context) {
        this.context = context;
    }

    public void showNotification(int type) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        switch (type) {
            case NOTIFICATION_WARNING_HIGH:
                if (sharedPreferences.getBoolean(Contract.PREF_ALREADY_NOTIFIED, false)) return;
                int warningHigh = sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_HIGH);
                showNotification(
                        String.format("%s %d%%!", context.getString(R.string.warning_high), warningHigh),
                        NOTIFICATION_ID_BATTERY_WARNING
                );
                break;
            case NOTIFICATION_WARNING_LOW:
                if (sharedPreferences.getBoolean(Contract.PREF_ALREADY_NOTIFIED, false)) return;
                int warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_LOW);
                showNotification(
                        String.format("%s %d%%!", context.getString(R.string.warning_low), warningLow),
                        NOTIFICATION_ID_BATTERY_WARNING
                );
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
                            NOTIFICATION_ID_SILENT_MODE
                    );
                }
                break;
        }
    }

    private void showNotification(String contentText, int id) {
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(SettingsFragment.getNotificationSound(context))
                .setVibrate(new long[]{0, 300, 300, 300})
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }
}
