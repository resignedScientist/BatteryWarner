package com.laudien.p1xelfehler.batterywarner.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.MainActivity;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.SettingsActivity;
import com.laudien.p1xelfehler.batterywarner.preferences.smartChargingActivity.SmartChargingActivity;
import com.laudien.p1xelfehler.batterywarner.services.GrantRootService;

import static android.app.Notification.PRIORITY_HIGH;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.media.RingtoneManager.TYPE_NOTIFICATION;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;

/**
 * Helper class to show a notification with the given type. All notifications used in the app are listed here.
 */
public final class NotificationHelper {
    public static final int ID_GRANT_ROOT = 1001;
    /**
     * Notification id of the notification that tells the user that the stop charging feature is not working
     * on this device.
     */
    public static final int ID_STOP_CHARGING_NOT_WORKING = 1002;
    /**
     * Notification id of the notification that asks for root again if app has no root rights anymore.
     */
    public static final int ID_NOT_ROOTED = 1003;
    /**
     * Notification id of the notification that tells the user that no alarm was found in the alarm app
     **/
    public static final int ID_NO_ALARM_TIME_FOUND = 1004;
    /**
     * Notification id that is shown on certain special events like sales.
     * It is the only notification that cannot be used with showNotification().
     * Use showEventNotification() instead!
     */

    public static final long[] VIBRATE_PATTERN = {0, 300, 300, 300};

    public static final int SMALL_ICON_RESOURCE = R.drawable.ic_stat_battery_full_white;

    public static final int LARGE_ICON_RESOURCE = R.drawable.ic_battery_status_full_green_48dp;

    private NotificationHelper() {
    }

    /**
     * Shows the notification with the given id.
     *
     * @param context        An instance of the Context class.
     * @param notificationID The id of the notification - usually one of the id constants.
     */
    public static void showNotification(final Context context, final int notificationID) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        switch (notificationID) {
            case ID_STOP_CHARGING_NOT_WORKING:
                showStopChargingNotWorkingNotification(context);
                break;
            case ID_GRANT_ROOT:
                showGrantRootNotification(context, sharedPreferences);
                break;
            case ID_NOT_ROOTED:
                showNotRootedNotification(context);
                break;
            case ID_NO_ALARM_TIME_FOUND:
                showNoAlarmTimeFoundNotification(context);
                break;
            default:
                throw new IdNotFoundException();
        }
    }

    /**
     * Cancels the notification with the given notification id.
     *
     * @param context        An instance of the Context class.
     * @param notificationID The id of the notification - usually one of the id constants.
     */
    public static void cancelNotification(Context context, int... notificationID) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        for (int id : notificationID) {
            notificationManager.cancel(id);
        }
    }

    private static void showStopChargingNotWorkingNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        String messageText = context.getString(R.string.notification_stop_charging_not_working);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, SettingsActivity.class), FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(SMALL_ICON_RESOURCE)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), LARGE_ICON_RESOURCE))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(messageText)
                .setStyle(getBigTextStyle(messageText))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(getDefaultSound())
                .setVibrate(VIBRATE_PATTERN);
        if (SDK_INT >= O) {
            builder.setChannelId(context.getString(R.string.channel_other_warnings));
        } else {
            builder.setPriority(PRIORITY_HIGH);
        }
        notificationManager.notify(ID_STOP_CHARGING_NOT_WORKING, builder.build());
    }

    private static void showGrantRootNotification(Context context, SharedPreferences sharedPreferences) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        // check if one of the root preferences is enabled
        boolean oneRootPermissionIsEnabled = RootHelper.isAnyRootPreferenceEnabled(context, sharedPreferences);
        // send the grant root notification only if one of the root preferences is enabled
        if (oneRootPermissionIsEnabled) {
            String messageText = context.getString(R.string.notification_grant_root);
            PendingIntent clickIntent = PendingIntent.getService(context, 0, new Intent(context, GrantRootService.class), FLAG_UPDATE_CURRENT);
            Notification.Builder builder = new Notification.Builder(context)
                    .setSmallIcon(SMALL_ICON_RESOURCE)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), LARGE_ICON_RESOURCE))
                    .setOngoing(true)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(messageText)
                    .setStyle(getBigTextStyle(messageText))
                    .setContentIntent(clickIntent)
                    .addAction(R.drawable.ic_done_white_24dp, context.getString(R.string.notification_button_grant_root), clickIntent)
                    .setAutoCancel(true)
                    .setSound(getDefaultSound())
                    .setVibrate(VIBRATE_PATTERN);
            if (SDK_INT >= O) {
                builder.setChannelId(context.getString(R.string.channel_other_warnings));
            } else {
                builder.setPriority(PRIORITY_HIGH);
            }
            notificationManager.notify(ID_GRANT_ROOT, builder.build());
        }
    }

    private static void showNotRootedNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        String messageText = context.getString(R.string.notification_not_rooted);
        PendingIntent clickIntent = PendingIntent.getService(context, 0, new Intent(context, GrantRootService.class), FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setOngoing(true)
                .setSmallIcon(SMALL_ICON_RESOURCE)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), LARGE_ICON_RESOURCE))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(messageText)
                .setStyle(getBigTextStyle(messageText))
                .setContentIntent(clickIntent)
                .addAction(R.drawable.ic_done_white_24dp, context.getString(R.string.notification_button_grant_root), clickIntent)
                .setAutoCancel(true)
                .setSound(getDefaultSound())
                .setVibrate(VIBRATE_PATTERN);
        if (SDK_INT >= O) {
            builder.setChannelId(context.getString(R.string.channel_other_warnings));
        } else {
            builder.setPriority(PRIORITY_HIGH);
        }
        notificationManager.notify(ID_NOT_ROOTED, builder.build());
    }

    private static void showNoAlarmTimeFoundNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        String messageText = context.getString(R.string.toast_no_alarm_time_found);
        PendingIntent clickIntent = PendingIntent.getActivity(context, 0, new Intent(context, SmartChargingActivity.class), FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(SMALL_ICON_RESOURCE)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), LARGE_ICON_RESOURCE))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(messageText)
                .setStyle(getBigTextStyle(messageText))
                .setContentIntent(clickIntent)
                .setAutoCancel(true)
                .setSound(getDefaultSound())
                .setVibrate(VIBRATE_PATTERN);
        if (SDK_INT >= O) {
            builder.setChannelId(context.getString(R.string.channel_other_warnings));
        } else {
            builder.setPriority(PRIORITY_HIGH);
        }
        notificationManager.notify(ID_NO_ALARM_TIME_FOUND, builder.build());
    }

    public static Uri getWarningSound(Context context, SharedPreferences sharedPreferences, boolean warningHigh) {
        int pref_id = warningHigh ? R.string.pref_sound_uri_high : R.string.pref_sound_uri_low;
        String uri = sharedPreferences.getString(context.getString(pref_id), "");
        if (uri.equals("")) {
            return getDefaultSound();
        } else {
            return Uri.parse(uri);
        }
    }

    private static Uri getDefaultSound() {
        return RingtoneManager.getDefaultUri(TYPE_NOTIFICATION);
    }

    public static Notification.BigTextStyle getBigTextStyle(String messageText) {
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.bigText(messageText);
        return bigTextStyle;
    }

    public static PendingIntent getDefaultClickIntent(Context context) {
        return PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), FLAG_UPDATE_CURRENT);
    }

    @RequiresApi(api = O)
    public static void createNotificationChannels(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        // high battery warning
        NotificationChannel channel = new NotificationChannel(
                context.getString(R.string.channel_warning_high),
                context.getString(R.string.channel_title_warning_high),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.channel_description_warning_high));
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setVibrationPattern(VIBRATE_PATTERN);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setShowBadge(true);
        notificationManager.createNotificationChannel(channel);
        // low battery warning
        channel = new NotificationChannel(
                context.getString(R.string.channel_warning_low),
                context.getString(R.string.channel_title_warning_low),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.channel_description_warning_low));
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setVibrationPattern(VIBRATE_PATTERN);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setShowBadge(true);
        notificationManager.createNotificationChannel(channel);
        // info notification
        channel = new NotificationChannel(
                context.getString(R.string.channel_battery_info),
                context.getString(R.string.channel_title_battery_info),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(context.getString(R.string.channel_description_battery_info));
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        channel.setShowBadge(false);
        notificationManager.createNotificationChannel(channel);
        // other warnings
        channel = new NotificationChannel(
                context.getString(R.string.channel_other_warnings),
                context.getString(R.string.channel_title_other_warnings),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.channel_description_other_warnings));
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setVibrationPattern(VIBRATE_PATTERN);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setShowBadge(true);
        notificationManager.createNotificationChannel(channel);
    }

    private static class IdNotFoundException extends RuntimeException {
        private IdNotFoundException() {
            super("The given notification id does not exist!");
        }
    }
}
