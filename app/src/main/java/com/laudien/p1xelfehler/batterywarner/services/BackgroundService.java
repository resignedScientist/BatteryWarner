package com.laudien.p1xelfehler.batterywarner.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseController;
import com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;
import com.laudien.p1xelfehler.batterywarner.preferences.infoNotificationActivity.InfoNotificationActivity;

import java.util.Locale;

import static android.app.Notification.PRIORITY_HIGH;
import static android.app.Notification.PRIORITY_LOW;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.os.BatteryManager.EXTRA_TEMPERATURE;
import static android.os.BatteryManager.EXTRA_VOLTAGE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.O;
import static android.view.View.GONE;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_NOT_ROOTED;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_STOP_CHARGING_NOT_WORKING;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.LARGE_ICON_RESOURCE;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.SMALL_ICON_RESOURCE;

public class BackgroundService extends Service {
    public static final String ACTION_ENABLE_CHARGING = "enableCharging";
    public static final String ACTION_ENABLE_USB_CHARGING = "enableUsbCharging";
    public static final String ACTION_ENABLE_CHARGING_AND_SAVE_GRAPH = "enableChargingAndSaveGraph";
    public static final String ACTION_DISABLE_CHARGING = "disableCharging";
    public static final String ACTION_RESET_ALL = "resetService";
    public static final String ACTION_CHANGE_PREFERENCE = "changePreference";
    public static final String EXTRA_PREFERENCE_KEY = "preferenceKey";
    public static final String EXTRA_PREFERENCE_VALUE = "preferenceValue";
    public static final int NOTIFICATION_ID_WARNING_HIGH = 2001;
    public static final int NOTIFICATION_ID_WARNING_LOW = 2002;
    private static final int NOTIFICATION_ID_INFO = 2003;
    private static final int NOTIFICATION_LED_ON_TIME = 500;
    private static final int NOTIFICATION_LED_OFF_TIME = 2000;
    private boolean chargingPausedBySmartCharging = false;
    private boolean chargingResumedBySmartCharging = false;
    private boolean chargingResumedByAutoResume = false;
    private boolean chargingPausedByIllegalUsbCharging = false;
    private boolean alreadyNotified = false;
    private boolean screenOn = true;
    private boolean chargingDisabledInFile = false;
    private boolean charging = false;
    private int lastBatteryLevel = -1;
    private long smartChargingResumeTime;
    private String infoNotificationMessage;
    private NotificationCompat.Builder infoNotificationBuilder;
    private BroadcastReceiver screenOnOffReceiver;
    private BatteryChangedReceiver batteryChangedReceiver;
    private NotificationManager notificationManager;
    @RequiresApi(LOLLIPOP)
    private BatteryManager batteryManager;
    private SharedPreferences sharedPreferences;
    private RemoteViews infoNotificationContent;
    private BatteryHelper.BatteryData batteryData;
    private DatabaseController databaseController;

    public static boolean isChargingTypeEnabled(Context context, int chargingType, @Nullable SharedPreferences sharedPreferences) {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        switch (chargingType) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_ac_enabled), context.getResources().getBoolean(R.bool.pref_ac_enabled_default));
            case BatteryManager.BATTERY_PLUGGED_USB:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_usb_enabled), context.getResources().getBoolean(R.bool.pref_usb_enabled_default));
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_wireless_enabled), context.getResources().getBoolean(R.bool.pref_wireless_enabled_default));
            default: // discharging or unknown charging type
                return false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        onRestoreState();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (s.equals(getString(R.string.pref_smart_charging_time_before))
                        || s.equals(getString(R.string.pref_smart_charging_time))
                        || s.equals(getString(R.string.pref_smart_charging_use_alarm_clock_time))) {
                    smartChargingResumeTime = getSmartChargingResumeTime();
                }
            }
        });
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (SDK_INT >= LOLLIPOP) {
            batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        }
        // battery changed receiver
        batteryChangedReceiver = new BatteryChangedReceiver();
        databaseController = DatabaseController.getInstance(this);
        final Intent batteryChangedIntent = registerReceiver(batteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        batteryData = BatteryHelper.getBatteryData(batteryChangedIntent, this);
        // screen on/off receiver
        screenOnOffReceiver = new ScreenOnOffReceiver();
        IntentFilter onOffFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        onOffFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOnOffReceiver, onOffFilter);
        // check if charging was disabled in file and show the notification for enabling it again
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    chargingDisabledInFile = !RootHelper.isChargingEnabled();
                    notificationManager.cancel(NotificationHelper.ID_GRANT_ROOT); // prevent double root check when updated
                    if (chargingDisabledInFile) {
                        NotificationHelper.cancelNotification(BackgroundService.this,
                                NOTIFICATION_ID_WARNING_HIGH, NOTIFICATION_ID_WARNING_LOW);
                        Notification notification = buildStopChargingNotification(false);
                        notificationManager.notify(NOTIFICATION_ID_WARNING_HIGH, notification);
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            resetService(); // reset service on any valid action
            switch (intent.getAction()) {
                case ACTION_ENABLE_USB_CHARGING:
                    sharedPreferences.edit()
                            .putBoolean(getString(R.string.pref_usb_charging_disabled), false)
                            .apply();
                case ACTION_ENABLE_CHARGING: // enable charging action by Tasker or 'Enable charging' button after not allowed usb charging
                    resumeCharging();
                    break;
                case ACTION_ENABLE_CHARGING_AND_SAVE_GRAPH: // 'Enable charging' button on the stop charging notification
                    saveGraph();
                    resumeCharging();
                    break;
                case ACTION_DISABLE_CHARGING: // disable charging action by Tasker
                    stopCharging(false);
                    break;
                case ACTION_RESET_ALL: // just reset the service
                    break;
                case ACTION_CHANGE_PREFERENCE:
                    Bundle extras = intent.getExtras();
                    if (extras == null || !extras.containsKey(EXTRA_PREFERENCE_KEY) || !extras.containsKey(EXTRA_PREFERENCE_VALUE)) {
                        throw new RuntimeException("Missing intent extras!");
                    }
                    String key = getString(extras.getInt(EXTRA_PREFERENCE_KEY));
                    Object value = extras.get(EXTRA_PREFERENCE_VALUE);
                    changePreference(key, value);
                    break;
                default:
                    throw new RuntimeException("Unknown action!");
            }
        }
        // battery info notification
        boolean infoNotificationEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_notification_enabled), getResources().getBoolean(R.bool.pref_info_notification_enabled_default));
        if (infoNotificationEnabled) {
            String[] data = batteryData.getEnabledOnly(this, sharedPreferences);
            infoNotificationContent = buildInfoNotificationContent(data);
            String message = buildInfoNotificationMessage(data);
            Notification infoNotification = buildInfoNotification(infoNotificationContent, message);
            startForeground(NOTIFICATION_ID_INFO, infoNotification);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryChangedReceiver);
        unregisterReceiver(screenOnOffReceiver);
        onSaveState();
    }

    private void changePreference(String key, Object value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        }
        editor.apply();
        Log.d(getClass().getSimpleName(), "Preference changed by tasker: " + key + " to value: " + value);
        Toast.makeText(this, getString(R.string.toast_preference_changed) + value + " !", Toast.LENGTH_SHORT).show();
    }

    private void onSaveState() {
        SharedPreferences backgroundServicePrefs = getSharedPreferences(getString(R.string.prefs_background_service), MODE_PRIVATE);
        backgroundServicePrefs.edit()
                .putBoolean("chargingPausedBySmartCharging", chargingPausedBySmartCharging)
                .putBoolean("chargingResumedBySmartCharging", chargingResumedBySmartCharging)
                .putBoolean("chargingResumedByAutoResume", chargingResumedByAutoResume)
                .putBoolean("chargingPausedByIllegalUsbCharging", chargingPausedByIllegalUsbCharging)
                .putBoolean("alreadyNotified", alreadyNotified)
                .putBoolean("charging", charging)
                .putInt("lastBatteryLevel", lastBatteryLevel)
                .apply();
    }

    private void onRestoreState() {
        SharedPreferences backgroundServicePrefs = getSharedPreferences(getString(R.string.prefs_background_service), MODE_PRIVATE);
        chargingPausedBySmartCharging = backgroundServicePrefs.getBoolean("chargingPausedBySmartCharging", chargingPausedBySmartCharging);
        chargingResumedBySmartCharging = backgroundServicePrefs.getBoolean("chargingResumedBySmartCharging", chargingResumedBySmartCharging);
        chargingResumedByAutoResume = backgroundServicePrefs.getBoolean("chargingResumedByAutoResume", chargingResumedByAutoResume);
        chargingPausedByIllegalUsbCharging = backgroundServicePrefs.getBoolean("chargingPausedByIllegalUsbCharging", chargingPausedByIllegalUsbCharging);
        alreadyNotified = backgroundServicePrefs.getBoolean("alreadyNotified", alreadyNotified);
        charging = backgroundServicePrefs.getBoolean("charging", charging);
        lastBatteryLevel = backgroundServicePrefs.getInt("lastBatteryLevel", lastBatteryLevel);
    }

    private void resetService() {
        chargingPausedBySmartCharging = false;
        chargingResumedBySmartCharging = false;
        chargingResumedByAutoResume = false;
        chargingPausedByIllegalUsbCharging = false;
        alreadyNotified = false;
        lastBatteryLevel = -1;
    }

    private void stopCharging(final boolean enableSound) {
        chargingDisabledInFile = true;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RootHelper.disableCharging();
                    notificationManager.cancel(NOTIFICATION_ID_WARNING_HIGH);
                    Notification stopChargingNotification = buildStopChargingNotification(enableSound);
                    notificationManager.notify(NOTIFICATION_ID_WARNING_HIGH, stopChargingNotification);
                } catch (RootHelper.NotRootedException e) {
                    NotificationHelper.showNotification(BackgroundService.this, ID_NOT_ROOTED);
                    showWarningHighNotification();
                    chargingDisabledInFile = false;
                } catch (RootHelper.NoBatteryFileFoundException e) {
                    NotificationHelper.showNotification(BackgroundService.this, ID_STOP_CHARGING_NOT_WORKING);
                    chargingDisabledInFile = false;
                }
            }
        });
    }

    private void resumeCharging() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RootHelper.enableCharging();
                    chargingDisabledInFile = false;
                    notificationManager.cancel(NOTIFICATION_ID_WARNING_HIGH); // cancel stop charging notification
                } catch (RootHelper.NotRootedException e) {
                    chargingDisabledInFile = true;
                    NotificationHelper.showNotification(BackgroundService.this, ID_NOT_ROOTED);
                } catch (RootHelper.NoBatteryFileFoundException e) {
                    chargingDisabledInFile = true;
                    NotificationHelper.showNotification(BackgroundService.this, ID_STOP_CHARGING_NOT_WORKING);
                }
            }
        });
    }

    private void saveGraph() {
        boolean graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
        boolean autoSaveGraphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_autosave), getResources().getBoolean(R.bool.pref_graph_autosave_default));
        if (graphEnabled && autoSaveGraphEnabled) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    databaseController.saveGraph(BackgroundService.this);
                }
            });
        }
    }

    private void showWarningHighNotification() {
        Notification notification = buildWarningHighNotification();
        notificationManager.notify(NOTIFICATION_ID_WARNING_HIGH, notification);
    }

    private Notification buildInfoNotification(RemoteViews content, String message) {
        Intent clickIntent = new Intent(this, InfoNotificationActivity.class);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID_INFO, clickIntent, 0);
        infoNotificationBuilder = new NotificationCompat.Builder(this, getString(R.string.channel_battery_info))
                .setSmallIcon(SMALL_ICON_RESOURCE)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), LARGE_ICON_RESOURCE))
                .setOngoing(true)
                .setContentIntent(clickPendingIntent)
                .setContentTitle(getString(R.string.title_info_notification))
                .setCustomBigContentView(content)
                .setContentText(message);
        if (SDK_INT < O) {
            infoNotificationBuilder.setPriority(Notification.PRIORITY_MIN);
        }
        return infoNotificationBuilder.build();
    }

    private RemoteViews buildInfoNotificationContent(String[] data) {
        if (infoNotificationContent == null) {
            boolean darkThemeEnabled = sharedPreferences.getBoolean(getString(R.string.pref_dark_info_notification), getResources().getBoolean(R.bool.pref_dark_info_notification_default));
            if (darkThemeEnabled) { // dark theme
                infoNotificationContent = new RemoteViews(getPackageName(), R.layout.notification_battery_info_dark);
            } else { // default theme
                infoNotificationContent = new RemoteViews(getPackageName(), R.layout.notification_battery_info);
            }
        }
        // set text sizes
        int textSize = sharedPreferences.getInt(getString(R.string.pref_info_text_size), getResources().getInteger(R.integer.pref_info_text_size_default));
        infoNotificationContent.setTextViewTextSize(R.id.textView_message_left, TypedValue.COMPLEX_UNIT_SP, textSize);
        infoNotificationContent.setTextViewTextSize(R.id.textView_message_right, TypedValue.COMPLEX_UNIT_SP, textSize);
        // generate info text
        if (data != null && data.length > 0) {
            if (data.length <= 3) {
                infoNotificationContent.setViewVisibility(R.id.textView_message_right, GONE);
                infoNotificationContent.setViewVisibility(R.id.view_middleLine, GONE);
                String message = data[0];
                if (data.length > 1) {
                    for (byte i = 1; i < data.length; i++) {
                        message = message.concat("\n").concat(data[i]);
                    }
                }
                infoNotificationContent.setTextViewText(R.id.textView_message_left, message);
            } else { // more then 3 items
                String message_left = data[0], message_right = data[1];
                for (byte i = 2; i < data.length; i++) {
                    if (i % 2 == 0) {
                        message_left = message_left.concat("\n").concat(data[i]);
                    } else {
                        message_right = message_right.concat("\n").concat(data[i]);
                    }
                }
                infoNotificationContent.setTextViewText(R.id.textView_message_left, message_left);
                infoNotificationContent.setTextViewText(R.id.textView_message_right, message_right);
            }
        } else {
            infoNotificationContent.setViewVisibility(R.id.view_middleLine, GONE);
            infoNotificationContent.setViewVisibility(R.id.textView_message_right, GONE);
            infoNotificationContent.setTextViewText(R.id.textView_message_left,
                    getString(R.string.notification_no_items_enabled));
        }
        return infoNotificationContent;
    }

    private String buildInfoNotificationMessage(String[] data) {
        if (data != null && data.length > 0) {
            infoNotificationMessage = data[0];
            if (data.length > 1) {
                int i = 1;
                do {
                    infoNotificationMessage = infoNotificationMessage.concat(", ").concat(data[i]);
                    i++;
                } while (i < data.length);
            }
            return infoNotificationMessage;
        } else {
            return getString(R.string.notification_no_items_enabled);
        }
    }

    private Notification buildStopChargingNotification(boolean enableSound) {
        String messageText = getString(R.string.notification_charging_disabled);
        Intent enableChargingIntent = new Intent(this, BackgroundService.class);
        enableChargingIntent.setAction(chargingPausedByIllegalUsbCharging ?
                ACTION_ENABLE_CHARGING : ACTION_ENABLE_CHARGING_AND_SAVE_GRAPH);
        PendingIntent enableChargingPendingIntent = PendingIntent.getService(this, NOTIFICATION_ID_WARNING_HIGH,
                enableChargingIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        // create base notification builder
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(SMALL_ICON_RESOURCE)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), LARGE_ICON_RESOURCE))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(messageText)
                .setStyle(NotificationHelper.getBigTextStyle(messageText))
                .setLights(Color.GREEN, NOTIFICATION_LED_ON_TIME, NOTIFICATION_LED_OFF_TIME)
                .setOngoing(true)
                .setContentIntent(enableChargingPendingIntent)
                .addAction(R.drawable.ic_battery_charging_full_white_24dp, getString(R.string.notification_button_enable_charging), enableChargingPendingIntent);
        if (SDK_INT >= O) {
            builder.setChannelId(getString(R.string.channel_warning_high));
        } else { // API lower than 26 (Android Oreo)
            builder.setPriority(PRIORITY_LOW);
            if (enableSound) {
                builder.setSound(NotificationHelper.getWarningSound(this, sharedPreferences, true));
            }
        }
        // add 'Enable Usb Charging' button if needed
        if (chargingPausedByIllegalUsbCharging) {
            Intent usbIntent = new Intent(this, BackgroundService.class);
            usbIntent.setAction(ACTION_ENABLE_USB_CHARGING);
            PendingIntent usbPendingIntent = PendingIntent.getService(this,
                    NOTIFICATION_ID_WARNING_HIGH, usbIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            builder.addAction(R.drawable.ic_battery_charging_full_white_24dp, getString(R.string.notification_button_enable_usb_charging), usbPendingIntent);
        }
        return builder.build();
    }

    private Notification buildWarningHighNotification() {
        int warningHigh = sharedPreferences.getInt(getString(R.string.pref_warning_high), getResources().getInteger(R.integer.pref_warning_high_default));
        String messageText = String.format(Locale.getDefault(), "%s %d%%!", getString(R.string.notification_warning_high), warningHigh);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(SMALL_ICON_RESOURCE)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), LARGE_ICON_RESOURCE))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(messageText)
                .setStyle(NotificationHelper.getBigTextStyle(messageText))
                .setContentIntent(NotificationHelper.getDefaultClickIntent(this))
                .setAutoCancel(true)
                .setSound(NotificationHelper.getWarningSound(this, sharedPreferences, true))
                .setLights(Color.GREEN, NOTIFICATION_LED_ON_TIME, NOTIFICATION_LED_OFF_TIME)
                .setVibrate(NotificationHelper.VIBRATE_PATTERN);
        if (Build.VERSION.SDK_INT >= O) {
            builder.setChannelId(getString(R.string.channel_warning_high));
        } else {
            builder.setPriority(PRIORITY_HIGH);
        }
        return builder.build();
    }

    private Notification buildWarningLowNotification(int warningLow, boolean showPowerSaving) {
        String messageText = String.format(Locale.getDefault(), "%s %d%%!", getString(R.string.notification_warning_low), warningLow);
        Notification.Builder builder = new Notification.Builder(BackgroundService.this)
                .setSmallIcon(SMALL_ICON_RESOURCE)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), LARGE_ICON_RESOURCE))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(messageText)
                .setStyle(NotificationHelper.getBigTextStyle(messageText))
                .setContentIntent(NotificationHelper.getDefaultClickIntent(BackgroundService.this))
                .setAutoCancel(true)
                .setLights(Color.RED, NOTIFICATION_LED_ON_TIME, NOTIFICATION_LED_OFF_TIME)
                .setSound(NotificationHelper.getWarningSound(BackgroundService.this, sharedPreferences, false))
                .setVibrate(NotificationHelper.VIBRATE_PATTERN);
        if (Build.VERSION.SDK_INT >= O) {
            builder.setChannelId(getString(R.string.channel_warning_low));
        } else {
            builder.setPriority(PRIORITY_HIGH);
        }
        if (showPowerSaving) {
            Intent exitPowerSaveIntent = new Intent(this, TogglePowerSavingService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, exitPowerSaveIntent, 0);
            builder.addAction(R.drawable.ic_battery_charging_full_white_24dp, getString(R.string.notification_button_toggle_power_saving), pendingIntent);
        }
        return builder.build();
    }

    private long getSmartChargingResumeTime() {
        long alarmTime; // the target time the device should be charged to the defined maximum
        boolean smartChargingUseClock = SDK_INT >= LOLLIPOP && sharedPreferences.getBoolean(getString(R.string.pref_smart_charging_use_alarm_clock_time), getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        int smartChargingMinutes = sharedPreferences.getInt(getString(R.string.pref_smart_charging_time_before), getResources().getInteger(R.integer.pref_smart_charging_time_before_default));
        if (SDK_INT >= LOLLIPOP && smartChargingUseClock) { // use alarm clock
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
            if (alarmClockInfo != null) {
                alarmTime = alarmClockInfo.getTriggerTime();
            } else { // no alarm time is set in the alarm app
                NotificationHelper.showNotification(this, NotificationHelper.ID_NO_ALARM_TIME_FOUND);
                return 0;
            }
        } else { // use time in shared preferences (smartChargingTime)
            long smartChargingTime = sharedPreferences.getLong(getString(R.string.pref_smart_charging_time), -1);
            if (smartChargingTime != -1) {
                alarmTime = smartChargingTime;
            } else { // there is no time saved in shared preferences
                return 0;
            }
        }
        long timeBefore = smartChargingMinutes * 60 * 1000;
        long timeNow = System.currentTimeMillis();
        long resumeTime = alarmTime - timeBefore;
        while (alarmTime <= timeNow) {
            alarmTime += 1000 * 60 * 60 * 24; // add a day if time is in the past
            resumeTime = alarmTime - timeBefore;
            // save the new time in the shared preferences
            sharedPreferences.edit().putLong(getString(R.string.pref_smart_charging_time), alarmTime).apply();
        }
        return resumeTime;
    }

    private class BatteryChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                // check if charging changed
                int chargingType = intent.getIntExtra(EXTRA_PLUGGED, 0);
                boolean isCharging = chargingType != 0;
                // stop charging if it is not allowed to charge
                if (!chargingPausedByIllegalUsbCharging && isCharging && !isChargingAllowed(chargingType)) {
                    chargingPausedByIllegalUsbCharging = true;
                    stopCharging(false);
                    return;
                }
                // handle change in charging state
                if (charging != isCharging) {
                    charging = isCharging;
                    onChargingStateChanged();
                    if (charging) { // started charging
                        onPowerConnected();
                    } else if (!chargingPausedByIllegalUsbCharging) { // started discharging
                        onPowerDisconnected();
                    }
                }
                // handle charging/discharging
                boolean graphEnabled = !chargingPausedByIllegalUsbCharging && sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
                if (isCharging || chargingDisabledInFile && (chargingPausedBySmartCharging || graphEnabled)) {
                    handleCharging(intent, graphEnabled);
                } else if (!chargingPausedByIllegalUsbCharging) { // discharging
                    handleDischarging(intent);
                }
                // refresh batteryData and info notification
                if (screenOn) {
                    refreshInfoNotification(intent);
                }
            }
        }

        /**
         * Charging was resumed by the app or the user connects the charger.
         */
        private void onPowerConnected() {
            if (!chargingDisabledInFile) {
                notificationManager.cancel(NOTIFICATION_ID_WARNING_LOW);
                if (!chargingResumedBySmartCharging && !chargingResumedByAutoResume) {
                    resetGraph();
                }
            }
        }

        /**
         * Charging was stopped by the app or the user disconnects the charger.
         */
        private void onPowerDisconnected() {
            if (!chargingDisabledInFile) {
                saveGraph();
                notificationManager.cancel(NOTIFICATION_ID_WARNING_HIGH);
                resetService();
            }
        }

        /**
         * Method to prevent duplicate code for things that would be written in
         * onPowerConnected() AND onPowerDisconnected() otherwise.
         */
        private void onChargingStateChanged() {
            if (!chargingDisabledInFile) {
                lastBatteryLevel = -1;
                alreadyNotified = false;
            }
        }

        private void handleCharging(Intent intent, boolean graphEnabled) {
            long timeNow = System.currentTimeMillis();
            int batteryLevel = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int temperature = intent.getIntExtra(EXTRA_TEMPERATURE, 0);
            int voltage = intent.getIntExtra(EXTRA_VOLTAGE, 0);
            int current = SDK_INT >= LOLLIPOP ? batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) : 0;
            int warningHigh = sharedPreferences.getInt(getString(R.string.pref_warning_high), getResources().getInteger(R.integer.pref_warning_high_default));

            // add a value to the database
            if (graphEnabled) {
                databaseController.addValue(batteryLevel, temperature, voltage, current, timeNow);
            }

            // handle warnings, Stop Charging and Smart Charging
            if (charging || chargingDisabledInFile && chargingPausedBySmartCharging) {
                boolean smartChargingEnabled = sharedPreferences.getBoolean(getString(R.string.pref_smart_charging_enabled), getResources().getBoolean(R.bool.pref_smart_charging_enabled_default));
                // battery level changed
                if (batteryLevel != lastBatteryLevel) {
                    lastBatteryLevel = batteryLevel;
                    if (batteryLevel >= warningHigh) {
                        if (!chargingResumedBySmartCharging) {
                            boolean stopChargingEnabled = sharedPreferences.getBoolean(getString(R.string.pref_stop_charging), getResources().getBoolean(R.bool.pref_stop_charging_default));
                            if (!alreadyNotified) {
                                alreadyNotified = true;
                                boolean warningEnabled = isWarningSoundEnabled(intent);
                                boolean shouldResetBatteryStats = sharedPreferences.getBoolean(getString(R.string.pref_reset_battery_stats), getResources().getBoolean(R.bool.pref_reset_battery_stats_default));
                                // reset android battery stats
                                if (shouldResetBatteryStats) {
                                    resetBatteryStats();
                                }
                                // stop charging
                                if (stopChargingEnabled) {
                                    if (smartChargingEnabled) {
                                        chargingPausedBySmartCharging = true;
                                    }
                                    boolean enableSound = warningEnabled && !chargingResumedByAutoResume;
                                    stopCharging(enableSound);
                                    chargingResumedByAutoResume = false;
                                } else if (warningEnabled) { // stop charging is disabled
                                    showWarningHighNotification();
                                }
                            }
                        }
                    }
                }
                // check smart charging resume time
                if (chargingPausedBySmartCharging) {
                    if (!chargingResumedBySmartCharging) {
                        if (smartChargingResumeTime == 0) {
                            smartChargingResumeTime = getSmartChargingResumeTime();
                        }
                        if (timeNow >= smartChargingResumeTime) {
                            // add a graph point for optics/correctness
                            if (graphEnabled) {
                                databaseController.addValue(batteryLevel, temperature, voltage, current, timeNow);
                            }
                            chargingResumedBySmartCharging = true;
                            resumeCharging();
                        } else if (!chargingResumedByAutoResume) { // resume time not reached yet and not resumed
                            boolean autoResumeEnabled = sharedPreferences.getBoolean(getString(R.string.pref_smart_charging_auto_resume), getResources().getBoolean(R.bool.pref_smart_charging_auto_resume_default));
                            int autoResumePercentage = sharedPreferences.getInt(getString(R.string.pref_smart_charging_auto_resume_percentage), getResources().getInteger(R.integer.pref_smart_charging_auto_resume_percentage_default));
                            // resume charging if the auto resume percentage limit was reached
                            if (autoResumeEnabled && batteryLevel <= warningHigh - autoResumePercentage) {
                                chargingResumedByAutoResume = true;
                                alreadyNotified = false;
                                resumeCharging();
                            }
                        }
                    } else { // charging already resumed
                        int smartChargingLimit = sharedPreferences.getInt(getString(R.string.pref_smart_charging_limit), getResources().getInteger(R.integer.pref_smart_charging_limit_default));
                        if (batteryLevel >= smartChargingLimit) {
                            chargingPausedBySmartCharging = false;
                            chargingResumedBySmartCharging = false;
                            stopCharging(true);
                        }
                    }
                }
            }
        }

        private void handleDischarging(Intent intent) {
            int batteryLevel = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            if (batteryLevel != lastBatteryLevel) {
                lastBatteryLevel = batteryLevel;
                // show warning low notification
                if (!alreadyNotified) {
                    int warningLow = sharedPreferences.getInt(getString(R.string.pref_warning_low), getResources().getInteger(R.integer.pref_warning_low_default));
                    if (batteryLevel <= warningLow) {
                        alreadyNotified = true;
                        showWarningLowNotification(warningLow);
                    }
                }
            }
        }

        /**
         * Method which returns if the current charging type is allowed.
         *
         * @param chargingType The extra EXTRA_PLUGGED in the battery intent.
         * @return Returns true, if the charging is allowed and false if not.
         */
        private boolean isChargingAllowed(int chargingType) {
            boolean usbChargingDisabled = sharedPreferences.getBoolean(getString(R.string.pref_usb_charging_disabled), getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
            boolean usbCharging = chargingType == BatteryManager.BATTERY_PLUGGED_USB;
            return !(usbCharging && usbChargingDisabled);
        }

        private void refreshInfoNotification(Intent intent) {
            batteryData.update(intent, BackgroundService.this);
            if (infoNotificationBuilder != null) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String[] data = batteryData.getEnabledOnly(BackgroundService.this, sharedPreferences);
                            infoNotificationContent = buildInfoNotificationContent(data);
                            infoNotificationMessage = buildInfoNotificationMessage(data);
                            infoNotificationBuilder.setContentText(infoNotificationMessage);
                            notificationManager.notify(NOTIFICATION_ID_INFO, infoNotificationBuilder.build());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        }

        private void resetBatteryStats() {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        RootHelper.resetBatteryStats();
                    } catch (RootHelper.NotRootedException e) {
                        NotificationHelper.showNotification(BackgroundService.this, NotificationHelper.ID_NOT_ROOTED);
                    }
                }
            });
        }

        private void resetGraph() {
            boolean graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
            if (graphEnabled) {
                databaseController.resetTable();
            }
        }

        private void showWarningLowNotification(final int warningLow) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    boolean showPowerSaving = SDK_INT >= LOLLIPOP && RootHelper.isRootAvailable();
                    Notification notification = buildWarningLowNotification(warningLow, showPowerSaving);
                    notificationManager.notify(NOTIFICATION_ID_WARNING_LOW, notification);
                    // enable power saving mode
                    if (showPowerSaving) {
                        boolean enablePowerSaving = sharedPreferences.getBoolean(getString(R.string.pref_power_saving_mode), getResources().getBoolean(R.bool.pref_power_saving_mode_default));
                        if (enablePowerSaving) {
                            try {
                                RootHelper.togglePowerSavingMode(true);
                            } catch (RootHelper.NotRootedException ignored) {
                            } // cannot happen!
                        }
                    }
                }
            });
        }

        private boolean isWarningSoundEnabled(Intent batteryIntent) {
            boolean warningHighEnabled = sharedPreferences.getBoolean(getString(R.string.pref_warning_high_enabled), getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
            if (!warningHighEnabled) {
                return false;
            } else {
                int chargingType = batteryIntent.getIntExtra(EXTRA_PLUGGED, -1);
                switch (chargingType) {
                    case BatteryManager.BATTERY_PLUGGED_AC:
                        return sharedPreferences.getBoolean(getString(R.string.pref_ac_enabled), getResources().getBoolean(R.bool.pref_ac_enabled_default));
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        return sharedPreferences.getBoolean(getString(R.string.pref_usb_enabled), getResources().getBoolean(R.bool.pref_usb_enabled_default));
                    case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                        return sharedPreferences.getBoolean(getString(R.string.pref_wireless_enabled), getResources().getBoolean(R.bool.pref_wireless_enabled_default));
                    default: // discharging or unknown charging type
                        return false;
                }
            }
        }
    }

    private class ScreenOnOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                onScreenTurnedOn();
            } else { // screen turned off
                onScreenTurnedOff();
            }
        }

        private void onScreenTurnedOn() {
            screenOn = true;
        }

        private void onScreenTurnedOff() {
            screenOn = false;
        }
    }
}
