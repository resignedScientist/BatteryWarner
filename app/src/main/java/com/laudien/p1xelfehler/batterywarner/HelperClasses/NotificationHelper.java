package com.laudien.p1xelfehler.batterywarner.HelperClasses;

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
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.RemoteViews;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.MainActivity;
import com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity.SettingsActivity;
import com.laudien.p1xelfehler.batterywarner.Activities.SmartChargingActivity.SmartChargingActivity;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.DisableRootFeaturesService;
import com.laudien.p1xelfehler.batterywarner.Services.EnableChargingService;
import com.laudien.p1xelfehler.batterywarner.Services.GrantRootService;
import com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryHelper.BatteryData;

import java.util.Locale;

import static android.app.Notification.PRIORITY_HIGH;
import static android.app.Notification.PRIORITY_LOW;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;
import static android.view.View.GONE;

/**
 * Helper class to show a notification with the given type. All notifications used in the app are listed here.
 */
public final class NotificationHelper {
    /**
     * Notification id of the notification that warns if silent/vibrate mode is turned on.
     */
    public static final int ID_SILENT_MODE = 1337;
    /**
     * Notification id of the notification that warns that the battery is above X%.
     */
    public static final int ID_WARNING_HIGH = 1338;
    /**
     * Notification id of the notification that warns that the battery is below Y%.
     */
    public static final int ID_WARNING_LOW = 1339;
    /**
     * Notification id of the notification that the user has to click/dismiss after the device is unplugged.
     * Only shown if the stop charging feature is enabled.
     */
    public static final int ID_STOP_CHARGING = 1340;
    /**
     * Notification id of the notification that asks the user for root again after the app was updated.
     */
    public static final int ID_GRANT_ROOT = 1341;
    /**
     * Notification id of the notification that tells the user that the stop charging feature is not working
     * on this device.
     */
    public static final int ID_STOP_CHARGING_NOT_WORKING = 1342;
    /**
     * Notification id of the notification that asks for root again if app has no root rights anymore.
     */
    public static final int ID_NOT_ROOTED = 1343;
    /**
     * Notification id of the notification that tells the user that no alarm was found in the alarm app
     **/
    public static final int ID_NO_ALARM_TIME_FOUND = 1344;
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static final int ID_BATTERY_INFO = 1345;
    private static final long[] VIBRATE_PATTERN = {0, 300, 300, 300};

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
        boolean isEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default));
        if (isEnabled) {
            switch (notificationID) {
                case ID_WARNING_HIGH:
                    showWarningHighNotification(context, sharedPreferences);
                    break;
                case ID_WARNING_LOW:
                    showWarningLowNotification(context, sharedPreferences);
                    break;
                case ID_SILENT_MODE:
                    showSilentModeNotification(context, sharedPreferences);
                    break;
                case ID_STOP_CHARGING:
                    showStopChargingNotification(context, sharedPreferences);
                    break;
                case ID_STOP_CHARGING_NOT_WORKING:
                    showStopChargingNotWorkingNotification(context, sharedPreferences);
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

    private static void showWarningHighNotification(Context context, SharedPreferences sharedPreferences) {
        boolean warningHighEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
        boolean alreadyNotified = sharedPreferences.getBoolean(context.getString(R.string.pref_already_notified), context.getResources().getBoolean(R.bool.pref_already_notified_default));
        // show notification
        if (!alreadyNotified && warningHighEnabled) {
            sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), true).apply();
            int warningHigh = sharedPreferences.getInt(context.getString(R.string.pref_warning_high), context.getResources().getInteger(R.integer.pref_warning_high_default));
            String messageText = String.format(Locale.getDefault(), "%s %d%%!", context.getString(R.string.warning_high), warningHigh);
            Notification.Builder builder = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setSound(getWarningSound(context, sharedPreferences))
                    .setVibrate(getWarningVibratePattern(context, sharedPreferences))
                    .setPriority(PRIORITY_HIGH)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(messageText)
                    .setStyle(getBigTextStyle(messageText))
                    .setContentIntent(getDefaultClickIntent(context))
                    .setAutoCancel(true);
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(ID_WARNING_HIGH, builder.build());
        }
    }

    private static void showWarningLowNotification(Context context, SharedPreferences sharedPreferences) {
        boolean warningLowEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_low_enabled), context.getResources().getBoolean(R.bool.pref_warning_low_enabled_default));
        boolean alreadyNotified = sharedPreferences.getBoolean(context.getString(R.string.pref_already_notified), context.getResources().getBoolean(R.bool.pref_already_notified_default));
        if (!alreadyNotified && warningLowEnabled) {
            sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), true).apply();
            int warningLow = sharedPreferences.getInt(context.getString(R.string.pref_warning_low), context.getResources().getInteger(R.integer.pref_warning_low_default));
            String messageText = String.format(Locale.getDefault(), "%s %d%%!", context.getString(R.string.warning_low), warningLow);
            Notification.Builder builder = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setSound(getWarningSound(context, sharedPreferences))
                    .setVibrate(getWarningVibratePattern(context, sharedPreferences))
                    .setPriority(PRIORITY_HIGH)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(messageText)
                    .setStyle(getBigTextStyle(messageText))
                    .setContentIntent(getDefaultClickIntent(context))
                    .setAutoCancel(true);
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(ID_WARNING_LOW, builder.build());
        }
    }

    private static void showSilentModeNotification(Context context, SharedPreferences sharedPreferences) {
        boolean silentNotificationEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_notifications_off_warning), context.getResources().getBoolean(R.bool.pref_notifications_off_warning_default));
        boolean isSoundEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_enable_sound), context.getResources().getBoolean(R.bool.pref_enable_sound_default));
        if (silentNotificationEnabled && isSoundEnabled) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int ringerMode = audioManager.getRingerMode();
            boolean areNotificationsEnabled;
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            if (SDK_INT >= N) {
                areNotificationsEnabled = notificationManager.areNotificationsEnabled();
            } else {
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
                areNotificationsEnabled = notificationManagerCompat.areNotificationsEnabled();
            }
            if (!areNotificationsEnabled || ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                String messageText = context.getString(R.string.notifications_are_off);
                Notification.Builder builder = new Notification.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setSound(getDefaultSound())
                        .setVibrate(VIBRATE_PATTERN)
                        .setPriority(PRIORITY_HIGH)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(messageText)
                        .setStyle(getBigTextStyle(messageText))
                        .setContentIntent(getDefaultClickIntent(context))
                        .setAutoCancel(true);
                notificationManager.notify(ID_SILENT_MODE, builder.build());
            }
        }
    }

    private static void showStopChargingNotification(final Context context, SharedPreferences sharedPreferences) {
        boolean stopChargingEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
        if (stopChargingEnabled) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!RootHelper.isChargingEnabled()) {
                            PendingIntent pendingIntent = PendingIntent.getService(context, ID_STOP_CHARGING,
                                    new Intent(context, EnableChargingService.class), PendingIntent.FLAG_CANCEL_CURRENT);
                            String messageText = context.getString(R.string.dismiss_if_unplugged);
                            Notification.Builder builder = new Notification.Builder(context)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setPriority(PRIORITY_LOW)
                                    .setContentTitle(context.getString(R.string.app_name))
                                    .setContentText(messageText)
                                    .setStyle(getBigTextStyle(messageText))
                                    .setContentIntent(getDefaultClickIntent(context))
                                    .addAction(R.drawable.ic_battery_charging_full_white_48dp, context.getString(R.string.enable_charging), pendingIntent)
                                    .setOngoing(true);
                            NotificationManager notificationManager = (NotificationManager)
                                    context.getSystemService(NOTIFICATION_SERVICE);
                            notificationManager.notify(ID_STOP_CHARGING, builder.build());
                        }
                    } catch (RootHelper.NotRootedException e) {
                        e.printStackTrace();
                        showNotification(context, ID_NOT_ROOTED);
                    } catch (RootHelper.BatteryFileNotFoundException e) {
                        e.printStackTrace();
                        showNotification(context, ID_STOP_CHARGING_NOT_WORKING);
                    }
                }
            });
        }
    }

    private static void showStopChargingNotWorkingNotification(Context context, SharedPreferences sharedPreferences) {
        boolean stopChargingEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
        boolean usbChargingDisabled = sharedPreferences.getBoolean(context.getString(R.string.pref_usb_charging_disabled), context.getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
        if (stopChargingEnabled || usbChargingDisabled) {
            String messageText = context.getString(R.string.stop_charging_not_working);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    new Intent(context, SettingsActivity.class), FLAG_UPDATE_CURRENT);
            Notification.Builder builder = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setSound(getDefaultSound())
                    .setVibrate(VIBRATE_PATTERN)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(messageText)
                    .setStyle(getBigTextStyle(messageText))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(ID_STOP_CHARGING_NOT_WORKING, builder.build());
        }
    }

    private static void showGrantRootNotification(Context context, SharedPreferences sharedPreferences) {
        boolean stopChargingEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
        boolean usbChargingDisabled = sharedPreferences.getBoolean(context.getString(R.string.pref_usb_charging_disabled), context.getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
        if (stopChargingEnabled || usbChargingDisabled) {
            String messageText = context.getString(R.string.grant_root_again);
            PendingIntent clickIntent = PendingIntent.getService(context, 0, new Intent(context, GrantRootService.class), FLAG_UPDATE_CURRENT);
            PendingIntent deleteIntent = PendingIntent.getService(context, 0, new Intent(context, DisableRootFeaturesService.class), FLAG_UPDATE_CURRENT);
            Notification.Builder builder = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setSound(getDefaultSound())
                    .setVibrate(VIBRATE_PATTERN)
                    .setPriority(PRIORITY_HIGH)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(messageText)
                    .setStyle(getBigTextStyle(messageText))
                    .setContentIntent(clickIntent)
                    .addAction(R.drawable.ic_done_white_48dp, context.getString(R.string.grant_root), clickIntent)
                    .setDeleteIntent(deleteIntent)
                    .setAutoCancel(true);
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(ID_GRANT_ROOT, builder.build());
        }
    }

    private static void showNotRootedNotification(Context context) {
        String messageText = context.getString(R.string.not_rooted_notification);
        PendingIntent clickIntent = PendingIntent.getService(context, 0, new Intent(context, GrantRootService.class), FLAG_UPDATE_CURRENT);
        PendingIntent deleteIntent = PendingIntent.getService(context, 0, new Intent(context, DisableRootFeaturesService.class), FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(getDefaultSound())
                .setVibrate(VIBRATE_PATTERN)
                .setPriority(PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(messageText)
                .setStyle(getBigTextStyle(messageText))
                .setContentIntent(clickIntent)
                .setDeleteIntent(deleteIntent)
                .addAction(R.drawable.ic_done_white_48dp, context.getString(R.string.grant_root), clickIntent)
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(ID_NOT_ROOTED, builder.build());
    }

    private static void showNoAlarmTimeFoundNotification(Context context) {
        String messageText = context.getString(R.string.no_alarm_time_found);
        PendingIntent clickIntent = PendingIntent.getActivity(context, 0, new Intent(context, SmartChargingActivity.class), FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(getDefaultSound())
                .setVibrate(VIBRATE_PATTERN)
                .setPriority(PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(messageText)
                .setStyle(getBigTextStyle(messageText))
                .setContentIntent(clickIntent)
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(ID_NO_ALARM_TIME_FOUND, builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void showBatteryInfoNotification(Context context, SharedPreferences sharedPreferences, BatteryData batteryData){
        if (batteryData != null) {
            RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_battery_info);
            contentView.setImageViewResource(R.id.img_battery, R.mipmap.ic_launcher);

            // unload not needed TextViews
            contentView.setViewVisibility(R.id.textView_technology, GONE);
            contentView.setViewVisibility(R.id.textView_health, GONE);
            contentView.setViewVisibility(R.id.textView_batteryLevel, GONE);

            // set TextView texts
            contentView.setTextViewText(R.id.textView_temp, batteryData.getTemperature(context));
            contentView.setTextViewText(R.id.textView_voltage, batteryData.getVoltage(context));
            contentView.setTextViewText(R.id.textView_current, batteryData.getCurrent(context));
            String screenOn = batteryData.getScreenOn(context, sharedPreferences);
            String screenOff = batteryData.getScreenOff(context, sharedPreferences);
            if (screenOn != null && screenOff != null) {
                contentView.setTextViewText(R.id.textView_screenOn, screenOn);
                contentView.setTextViewText(R.id.textView_screenOff, screenOff);
            } else {
                contentView.setViewVisibility(R.id.textView_screenOn, GONE);
                contentView.setViewVisibility(R.id.textView_screenOff, GONE);
            }
            Notification notification = new Notification.Builder(context)
                    .setOngoing(true)
                    .setContentIntent(getDefaultClickIntent(context))
                    .setCustomBigContentView(contentView)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setSmallIcon(android.R.color.transparent)
                    .build();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(ID_BATTERY_INFO, notification);
        }
    }

    private static Uri getWarningSound(Context context, SharedPreferences sharedPreferences) {
        boolean soundEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_enable_sound), context.getResources().getBoolean(R.bool.pref_enable_sound_default));
        if (soundEnabled) {
            String uri = sharedPreferences.getString(context.getString(R.string.pref_sound_uri), "");
            if (!uri.equals("")) {
                return Uri.parse(uri); // saved URI
            } else {
                return getDefaultSound(); // default URI
            }
        } else {
            return null;
        }
    }

    private static long[] getWarningVibratePattern(Context context, SharedPreferences sharedPreferences) {
        boolean isSoundEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_enable_sound), context.getResources().getBoolean(R.bool.pref_enable_sound_default));
        if (isSoundEnabled) {
            return VIBRATE_PATTERN;
        } else {
            return null;
        }
    }

    private static Uri getDefaultSound() {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    private static Notification.BigTextStyle getBigTextStyle(String messageText) {
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.bigText(messageText);
        return bigTextStyle;
    }

    private static PendingIntent getDefaultClickIntent(Context context) {
        return PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), FLAG_UPDATE_CURRENT);
    }

    private static class IdNotFoundException extends RuntimeException {
        private IdNotFoundException() {
            super("The given notification id does not exist!");
        }
    }
}
