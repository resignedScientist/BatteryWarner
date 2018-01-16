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
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseContract;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseModel;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseValue;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;
import com.laudien.p1xelfehler.batterywarner.helper.TemperatureConverter;
import com.laudien.p1xelfehler.batterywarner.preferences.infoNotificationActivity.InfoNotificationActivity;

import java.util.Locale;

import static android.app.Notification.PRIORITY_HIGH;
import static android.app.Notification.PRIORITY_LOW;
import static android.os.BatteryManager.BATTERY_HEALTH_COLD;
import static android.os.BatteryManager.BATTERY_HEALTH_DEAD;
import static android.os.BatteryManager.BATTERY_HEALTH_GOOD;
import static android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT;
import static android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE;
import static android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE;
import static android.os.BatteryManager.EXTRA_HEALTH;
import static android.os.BatteryManager.EXTRA_LEVEL;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.os.BatteryManager.EXTRA_TECHNOLOGY;
import static android.os.BatteryManager.EXTRA_TEMPERATURE;
import static android.os.BatteryManager.EXTRA_VOLTAGE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.N;
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
    private Notification.Builder infoNotificationBuilder;
    private BroadcastReceiver screenOnOffReceiver;
    private BatteryChangedReceiver batteryChangedReceiver;
    private NotificationManager notificationManager;
    @RequiresApi(LOLLIPOP)
    private BatteryManager batteryManager;
    private SharedPreferences sharedPreferences;
    private RemoteViews infoNotificationContent;
    private BatteryData batteryData;
    private BatteryValueChangedListener listener;
    private DatabaseModel databaseModel;
    private BackgroundServiceBinder binder = new BackgroundServiceBinder();
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getString(R.string.pref_smart_charging_time_before))
                    || key.equals(getString(R.string.pref_smart_charging_time))
                    || key.equals(getString(R.string.pref_smart_charging_use_alarm_clock_time))) {
                smartChargingResumeTime = getSmartChargingResumeTime();
            } else if (batteryData != null && key.equals(getString(R.string.pref_temp_unit))) {
                batteryData.onTemperatureUnitChanged();
            }
        }
    };

    /**
     * Checks if the given charging method is enabled in preferences.
     *
     * @param context           The app context.
     * @param chargingType      The extra EXTRA_PLUGGED from the intent with the action ACTION_BATTERY_CHANGED.
     * @param sharedPreferences The shared preferences containing this preference.
     *                          If it is null, the default shared preferences are loaded using the context.
     * @return Returns true if the charging method is enabled in preferences, false if not.
     */
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

    /**
     * Not used by this class but has to be there.
     *
     * @param intent The intent containing one of the actions used or none.
     * @return Returns always null.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Executed every time the service was started and not already running.
     * <p>
     * It initializes some instance variables and registers all used receivers.
     * <p>
     * Also, it checks if the charging has been stopped and shows the stop charging notification if it is.
     * That causes the first root check of the app.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        onRestoreState();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (SDK_INT >= LOLLIPOP) {
            batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        }
        // battery changed receiver
        batteryChangedReceiver = new BatteryChangedReceiver();
        databaseModel = DatabaseModel.getInstance(this);
        databaseModel.registerDatabaseListener(batteryChangedReceiver);
        final Intent batteryChangedIntent = registerReceiver(batteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        batteryData = new BatteryData(batteryChangedIntent, this);
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

    /**
     * Executed every time when the Context.startService() method was called (if the service is already running or not).
     * <p>
     * It can work with the following actions:
     * - ACTION_ENABLE_USB_CHARGING to enable usb charging in preferences
     * - ACTION_ENABLE_CHARGING to enable charging
     * - ACTION_ENABLE_CHARGING_AND_SAVE_GRAPH to enable charging and save the charging graph
     * - ACTION_DISABLE_CHARGING to disable charging
     * - ACTION_RESET_ALL to just reset the service
     * - ACTION_CHANGE_PREFERENCE to change any preference in the default shared preferences.
     * <p>
     * Every time such an action is triggered, the service will reset to the starting state without
     * loading the last state.
     * <p>
     * If an action is given or not - it will build a battery info notification if enabled
     * (it has to be there on android Oreo) and start the service in the foreground.
     * <p>
     * If the battery info notification is disabled, the service will not start in the foreground.
     *
     * @param intent  The intent containing one of the actions or none. A RuntimeException will be thrown for an unknown action.
     * @param flags   Some flags that will not be used by this class but may be from any super class.
     * @param startId The start id that will not be used by this class but may be from any super class.
     * @return Returns START_STICKY, that means that the service restarts automatically if it was stopped.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
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
        boolean infoNotificationEnabled = SDK_INT >= O || sharedPreferences.getBoolean(getString(R.string.pref_info_notification_enabled), getResources().getBoolean(R.bool.pref_info_notification_enabled_default));
        if (infoNotificationEnabled) {
            String[] data = batteryData.getEnabledOnly(this, sharedPreferences);
            infoNotificationContent = buildInfoNotificationContent(data);
            String message = buildInfoNotificationMessage(data);
            Notification infoNotification = buildInfoNotification(infoNotificationContent, message);
            startForeground(NOTIFICATION_ID_INFO, infoNotification);
        }
        return START_STICKY;
    }

    /**
     * Executed every time the service will be stopped. It unregisters all receivers
     * and saves the service state to shared preferences with the file name @string/prefs_background_service.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        unregisterReceiver(batteryChangedReceiver);
        unregisterReceiver(screenOnOffReceiver);
        databaseModel.unregisterDatabaseListener(batteryChangedReceiver);
        onSaveState();
    }

    /**
     * Method that is triggered from outside the service using ACTION_CHANGE_PREFERENCE
     * to change a preference inside the default shared preferences.
     * It is useful for Tasker especially and can work with Boolean, Integer and Long values.
     * Keep attention that the value has the correct type before using this method!
     *
     * @param key   The preference key.
     * @param value The value where to set the preference.
     */
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

    /**
     * Saves the service state using shared preferences with the file name @string/prefs_background_service.
     */
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

    /**
     * Restores the saved service state using shared preferences with the file name @string/prefs_background_service.
     */
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

    /**
     * Resets the service to the first start state.
     */
    private void resetService() {
        chargingPausedBySmartCharging = false;
        chargingResumedBySmartCharging = false;
        chargingResumedByAutoResume = false;
        chargingPausedByIllegalUsbCharging = false;
        alreadyNotified = false;
        lastBatteryLevel = -1;
    }

    /**
     * Stops the charging or shows a notification if it did not work.
     *
     * @param enableSound Determines if the sound should be enabled (true) or not (false).
     */
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

    /**
     * Resumes the charging or shows a notification if it did not work.
     */
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

    /**
     * Saves the graph into the database if graph recording and auto save are enabled in preferences.
     */
    private void saveGraph() {
        boolean autoSaveGraphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_autosave), getResources().getBoolean(R.bool.pref_graph_autosave_default));
        if (autoSaveGraphEnabled) {
            DatabaseUtils.saveGraph(BackgroundService.this, null);
        }
    }

    /**
     * Builds and shows the high battery warning notification.
     */
    private void showWarningHighNotification() {
        Notification notification = buildWarningHighNotification();
        notificationManager.notify(NOTIFICATION_ID_WARNING_HIGH, notification);
    }

    /**
     * Builds the battery info notification and returns it.
     *
     * @param content The big notification content that is shown if there is enough room for it.
     * @param message The notification message that is shown if there is not enough room
     *                for the big notification content.
     * @return The battery info notification.
     */
    private Notification buildInfoNotification(RemoteViews content, String message) {
        Intent clickIntent = new Intent(this, InfoNotificationActivity.class);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID_INFO, clickIntent, 0);
        if (SDK_INT >= O) {
            infoNotificationBuilder = new Notification.Builder(this, getString(R.string.channel_battery_info));
        } else {
            infoNotificationBuilder = new Notification.Builder(this);
        }
        infoNotificationBuilder
                .setSmallIcon(SMALL_ICON_RESOURCE)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), LARGE_ICON_RESOURCE))
                .setOngoing(true)
                .setContentIntent(clickPendingIntent)
                .setContentTitle(getString(R.string.title_info_notification))
                .setContentText(message);
        if (SDK_INT >= N) {
            infoNotificationBuilder.setCustomBigContentView(content);
        } else {
            infoNotificationBuilder.setContent(content);
        }
        if (SDK_INT < O) {
            infoNotificationBuilder.setPriority(Notification.PRIORITY_MIN);
        }
        return infoNotificationBuilder.build();
    }

    /**
     * Builds the notification content that is shown if there is enough room for it.
     *
     * @param data An array of localised strings that should be shown in the notification.
     * @return RemoteViews object that represents the content of the battery info notification.
     */
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

    /**
     * Builds the message text of the battery info notification that is shown
     * if the notification is folded and there is not enough room for the regular notification.
     *
     * @param data An array of localised strings that should be shown in the notification.
     * @return The notification message.
     */
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

    /**
     * Builds the stop charging notification and returns it.
     * It does only play a sound if it is enabled in preferences below android Oreo
     * and uses the @strings/channel_warning_high notification channel with android Oreo and above.
     *
     * @param enableSound True if the sound should be enabled (below Oreo only), false if not.
     * @return The stop charging notification.
     */
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
            boolean soundEnabled = sharedPreferences.getBoolean(getString(R.string.pref_warning_high_sound_enabled), getResources().getBoolean(R.bool.pref_waring_high_sound_enabled_default));
            if (enableSound && soundEnabled) {
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

    /**
     * Builds the high battery warning notification and returns it.
     * It does only play a sound if it is enabled in preferences below android Oreo
     * and uses the @strings/channel_warning_high notification channel with android Oreo and above.
     * <p>
     * Only used if stop charging is disabled!
     * If the charging has been stopped, use the buildStopChargingNotification() method instead.
     *
     * @return The low battery warning notification.
     */
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
                .setLights(Color.GREEN, NOTIFICATION_LED_ON_TIME, NOTIFICATION_LED_OFF_TIME)
                .setVibrate(NotificationHelper.VIBRATE_PATTERN);
        if (Build.VERSION.SDK_INT >= O) {
            builder.setChannelId(getString(R.string.channel_warning_high));
        } else {
            builder.setPriority(PRIORITY_HIGH);
            boolean soundEnabled = sharedPreferences.getBoolean(getString(R.string.pref_warning_high_sound_enabled), getResources().getBoolean(R.bool.pref_waring_high_sound_enabled_default));
            if (soundEnabled) {
                builder.setSound(NotificationHelper.getWarningSound(this, sharedPreferences, true));
            }
        }
        return builder.build();
    }

    /**
     * Builds the low battery warning notification and returns it.
     * It uses the @strings/channel_warning_low notification channel with android Oreo and above.
     *
     * @param warningLow      The percentage at which the low battery warning triggers.
     *                        This is the percentage that is actually shown in the notification.
     * @param showPowerSaving Determines if the "Toggle power saving" button should be shown (true) or not (false).
     * @return The low battery warning notification.
     */
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
                .setVibrate(NotificationHelper.VIBRATE_PATTERN);
        if (Build.VERSION.SDK_INT >= O) {
            builder.setChannelId(getString(R.string.channel_warning_low));
        } else {
            builder.setPriority(PRIORITY_HIGH)
                    .setSound(NotificationHelper.getWarningSound(BackgroundService.this, sharedPreferences, false));
        }
        if (showPowerSaving) {
            Intent exitPowerSaveIntent = new Intent(this, TogglePowerSavingService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, exitPowerSaveIntent, 0);
            builder.addAction(R.drawable.ic_battery_charging_full_white_24dp, getString(R.string.notification_button_toggle_power_saving), pendingIntent);
        }
        return builder.build();
    }

    /**
     * Reads the time when smart charging should have finished (the alarm time),
     * calculates the time when the charging should resume (using the minutes before preference)
     * and returns it in milliseconds (UTC unix time).
     *
     * @return The time the charging should resume using smart charging in milliseconds (UTC unix time)
     */
    private long getSmartChargingResumeTime() {
        long alarmTime; // the target time the device should be charged to the defined maximum
        boolean smartChargingUseClock = SDK_INT >= LOLLIPOP && sharedPreferences.getBoolean(getString(R.string.pref_smart_charging_use_alarm_clock_time), getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        int smartChargingMinutes = sharedPreferences.getInt(getString(R.string.pref_smart_charging_time_before), getResources().getInteger(R.integer.pref_smart_charging_time_before_default));
        if (SDK_INT >= LOLLIPOP && smartChargingUseClock) { // use alarm clock
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager != null ? alarmManager.getNextAlarmClock() : null;
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
        // prevent that the alarm time is in the past
        if (alarmTime <= timeNow) {
            long day = 1000 * 60 * 60 * 24;
            long daysToAdd = (timeNow - alarmTime) / day + 1;
            alarmTime += daysToAdd * day;
            // save the new alarm time in the shared preferences
            sharedPreferences.edit().putLong(getString(R.string.pref_smart_charging_time), alarmTime).apply();
        }
        return alarmTime - timeBefore;
    }

    public interface BatteryValueChangedListener {
        void onBatteryValueChanged(BatteryData batteryData, int index);
    }

    /**
     * A broadcast receiver that handles all charging and discharging functionalities of the app.
     * It uses the ACTION_BATTERY_CHANGED intent action.
     */
    private class BatteryChangedReceiver extends BroadcastReceiver implements DatabaseContract.DatabaseListener {
        private long graphCreationTime = 0;

        /**
         * This method receives the ACTION_BATTERY_CHANGED action and triggers the correct methods.
         *
         * @param context An app context.
         * @param intent  An intent with ACTION_BATTERY_CHANGED.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null || !intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                return;
            }
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
         * Executed every time the charging starts or stops.
         */
        private void onChargingStateChanged() {
            if (!chargingDisabledInFile) {
                lastBatteryLevel = -1;
                alreadyNotified = false;
            }
        }

        /**
         * Handles graph recording, the high battery warning, stop charging, smart charging and more.
         *
         * @param intent       The intent received with ACTION_BATTERY_CHANGED.
         * @param graphEnabled True if charging graph recording is enabled in preferences, false if not.
         */
        private void handleCharging(Intent intent, boolean graphEnabled) {
            long timeNow = System.currentTimeMillis();
            int batteryLevel = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int temperature = intent.getIntExtra(EXTRA_TEMPERATURE, 0);
            int voltage = intent.getIntExtra(EXTRA_VOLTAGE, 0);
            int current = SDK_INT >= LOLLIPOP ? batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) : 0;
            int warningHigh = sharedPreferences.getInt(getString(R.string.pref_warning_high), getResources().getInteger(R.integer.pref_warning_high_default));
            if (graphCreationTime == 0) {
                graphCreationTime = databaseModel.getCreationTime();
                if (graphCreationTime == 0) {
                    graphCreationTime = timeNow;
                }
            }
            DatabaseValue databaseValue = new DatabaseValue(batteryLevel, temperature, voltage, current, timeNow, graphCreationTime);

            // add a value to the database
            if (graphEnabled) {
                databaseModel.addValue(databaseValue);
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
                                    // repeat warning roughly every 60 seconds
                                    boolean repeatWarning = sharedPreferences.getBoolean(getString(R.string.pref_repeat_warning), getResources().getBoolean(R.bool.pref_repeat_warning_default));
                                    if (repeatWarning) {
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                alreadyNotified = false;
                                            }
                                        }, 60 * 1000);
                                    }
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
                                databaseModel.addValue(databaseValue);
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

        /**
         * Handles the low battery warning.
         *
         * @param intent The intent received with ACTION_BATTERY_CHANGED.
         */
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
         * @param chargingType The extra EXTRA_PLUGGED from the intent with the action ACTION_BATTERY_CHANGED.
         * @return Returns true, if the charging is allowed and false if not.
         */
        private boolean isChargingAllowed(int chargingType) {
            boolean usbChargingDisabled = sharedPreferences.getBoolean(getString(R.string.pref_usb_charging_disabled), getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
            boolean usbCharging = chargingType == BatteryManager.BATTERY_PLUGGED_USB;
            return !(usbCharging && usbChargingDisabled);
        }

        /**
         * Updates the battery info notification and the BatteryData instance with new data.
         *
         * @param intent The intent received with ACTION_BATTERY_CHANGED.
         */
        private void refreshInfoNotification(@Nullable Intent intent) {
            if (intent != null) {
                batteryData.update(intent, BackgroundService.this);
            }
            if (infoNotificationBuilder != null) {
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
        }

        /**
         * Resets the android os battery stats.
         */
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

        /**
         * Clears the app internal database to make room for a new charging graph.
         * Only does that, if the charging graph is enabled in preferences.
         */
        private void resetGraph() {
            boolean graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
            if (graphEnabled) {
                databaseModel.resetTable();
            }
        }

        /**
         * Shows the low battery warning.
         *
         * @param warningLow The percentage at which the low battery warning triggers.
         *                   This is the percentage that is actually shown in the notification.
         */
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

        /**
         * Checks if the warning sound is enabled with the current charging method.
         *
         * @param batteryIntent The intent received with ACTION_BATTERY_CHANGED.
         * @return True if the warning sound is enabled, false if not. Returns false if the device is discharging.
         */
        private boolean isWarningSoundEnabled(Intent batteryIntent) {
            boolean warningHighEnabled = sharedPreferences.getBoolean(getString(R.string.pref_warning_high_enabled), getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
            if (!warningHighEnabled) {
                return false;
            }
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

        @Override
        public void onValueAdded(DatabaseValue value, long totalNumberOfRows) {

        }

        @Override
        public void onTableReset() {
            graphCreationTime = 0;
        }
    }

    /**
     * Receiver that is responsible for screen on and screen off actions.
     * It just updates the screenOn variable that is responsible
     * for updating the info notification only if the screen is on.
     */
    private class ScreenOnOffReceiver extends BroadcastReceiver {

        /**
         * Listens for the actions ACTION_SCREEN_ON and ACTION_SCREEN_OFF
         * and executes the onScreenTurnedOn() and onScreenTurnedOff() methods.
         *
         * @param context The BackgroundService context.
         * @param intent  An intent with either ACTION_SCREEN_ON or ACTION_SCREEN_OFF.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                onScreenTurnedOn();
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) { // screen turned off
                onScreenTurnedOff();
            }
        }

        /**
         * The screen has been turned on.
         */
        private void onScreenTurnedOn() {
            screenOn = true;
        }

        /**
         * The screen has been turned off.
         */
        private void onScreenTurnedOff() {
            screenOn = false;
        }
    }

    public class BackgroundServiceBinder extends Binder {
        public BatteryData getBatteryData() {
            return BackgroundService.this.batteryData;
        }

        public void setBatteryValueChangedListener(BatteryValueChangedListener listener) {
            BackgroundService.this.listener = listener;
            // trigger listener for all values for initialization
            for (byte i = 0; i < BatteryData.NUMBER_OF_ITEMS; i++) {
                listener.onBatteryValueChanged(batteryData, i);
            }
        }

        public void removeBatteryValueChangedListener() {
            listener = null;
        }
    }

    /**
     * This class holds all the data that can be shown in the MainPageFragment or the info
     * notification (or else where if needed). You can set an BatteryValueChangedListener
     * that notifies when the data was changed with one of the setters. It will only be called
     * if the new data is actually different from the old data. The data can be updated with the
     * update() method.
     */
    public class BatteryData {

        public static final int INDEX_TECHNOLOGY = 0;
        public static final int INDEX_TEMPERATURE = 1;
        public static final int INDEX_HEALTH = 2;
        public static final int INDEX_BATTERY_LEVEL = 3;
        public static final int INDEX_VOLTAGE = 4;
        public static final int INDEX_CURRENT = 5;
        private static final int NUMBER_OF_ITEMS = 6;
        private final String[] values = new String[NUMBER_OF_ITEMS];
        private String technology;
        private int health, batteryLevel;
        private int current;
        private double temperature, voltage;

        private BatteryData(Intent batteryStatus, Context context) {
            update(batteryStatus, context);
        }

        /**
         * Updates all the data that is in the batteryStatus intent.
         *
         * @param batteryStatus Intent that is provided by a receiver with the action ACTION_BATTERY_CHANGED.
         * @param context       An instance of the Context class.
         */
        void update(Intent batteryStatus, Context context) {
            setTechnology(batteryStatus.getStringExtra(EXTRA_TECHNOLOGY));
            setTemperature((double) batteryStatus.getIntExtra(EXTRA_TEMPERATURE, -1) / 10, false);
            setHealth(batteryStatus.getIntExtra(EXTRA_HEALTH, -1));
            setBatteryLevel(batteryStatus.getIntExtra(EXTRA_LEVEL, -1));
            setVoltage((double) batteryStatus.getIntExtra(EXTRA_VOLTAGE, -1) / 1000);
            if (SDK_INT >= LOLLIPOP) {
                BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                if (batteryManager != null) {
                    setCurrent(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
                }
            }
        }

        /**
         * This method does the same as the getAsArray() method, but only returns the data that is
         * enabled to be shown in the info notification.
         * Caution: The indexes are not correct here!
         *
         * @param context           An instance of the Context class.
         * @param sharedPreferences An instance of the SharedPreferences class.
         * @return Returns enabled data as String array with correct formats to show to the user.
         */
        private String[] getEnabledOnly(Context context, SharedPreferences sharedPreferences) {
            boolean[] enabledBooleans = new boolean[NUMBER_OF_ITEMS];
            enabledBooleans[INDEX_TECHNOLOGY] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_technology), context.getResources().getBoolean(R.bool.pref_info_technology_default));
            enabledBooleans[INDEX_TEMPERATURE] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_temperature), context.getResources().getBoolean(R.bool.pref_info_temperature_default));
            enabledBooleans[INDEX_HEALTH] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_health), context.getResources().getBoolean(R.bool.pref_info_health_default));
            enabledBooleans[INDEX_BATTERY_LEVEL] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_battery_level), context.getResources().getBoolean(R.bool.pref_info_battery_level_default));
            enabledBooleans[INDEX_VOLTAGE] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_voltage), context.getResources().getBoolean(R.bool.pref_info_voltage_default));
            enabledBooleans[INDEX_CURRENT] = sharedPreferences.getBoolean(context.getString(R.string.pref_info_current), context.getResources().getBoolean(R.bool.pref_info_current_default));
            // add enabled strings to array
            String[] enabledValues = new String[NUMBER_OF_ITEMS];
            byte count = 0;
            for (byte i = 0; i < NUMBER_OF_ITEMS; i++) {
                if (enabledBooleans[i]) {
                    enabledValues[i] = values[i];
                    count++;
                }
            }
            // remove null values from array
            String[] cleanedValues = new String[count];
            byte j = 0;
            for (String s : enabledValues) {
                if (s != null) {
                    cleanedValues[j++] = s;
                }
            }
            return cleanedValues;
        }

        /**
         * Get a specific value with the given index as correctly formatted String.
         *
         * @param index One of the INDEX attributes that determine which value should be returned.
         * @return Returns the value with the given index as correctly formatted String.
         */
        public String getValueString(int index) {
            return values[index];
        }


        /**
         * Get the value with the given index as object.
         *
         * @param index One of the INDEX attributes that determine which value should be returned.
         * @return Returns the value with the given index as object or null if there is no object with that index.
         */
        public Object getValue(int index) {
            switch (index) {
                case INDEX_TECHNOLOGY:
                    return technology;
                case INDEX_TEMPERATURE:
                    return temperature;
                case INDEX_HEALTH:
                    return health;
                case INDEX_BATTERY_LEVEL:
                    return batteryLevel;
                case INDEX_VOLTAGE:
                    return voltage;
                case INDEX_CURRENT:
                    return current;
                default:
                    return null;
            }
        }

        private void setTechnology(String technology) {
            if (this.technology == null || !this.technology.equals(technology)) {
                this.technology = technology;
                values[INDEX_TECHNOLOGY] = getString(R.string.info_technology) + ": " + technology;
                notifyListener(INDEX_TECHNOLOGY);
            }
        }

        private void setHealth(int health) {
            if (this.health != health || values[INDEX_HEALTH] == null) {
                this.health = health;
                values[INDEX_HEALTH] = getString(R.string.info_health) + ": " + getHealthString(health);
                notifyListener(INDEX_HEALTH);
            }
        }

        private void setBatteryLevel(int batteryLevel) {
            if (this.batteryLevel != batteryLevel || values[INDEX_BATTERY_LEVEL] == null) {
                this.batteryLevel = batteryLevel;
                values[INDEX_BATTERY_LEVEL] = String.format(getString(R.string.info_battery_level) + ": %d%%", batteryLevel);
                notifyListener(INDEX_BATTERY_LEVEL);
            }
        }

        @RequiresApi(api = LOLLIPOP)
        private void setCurrent(int current) {
            if (this.current != current || values[INDEX_CURRENT] == null) {
                this.current = current;
                boolean reverseCurrent = PreferenceManager.getDefaultSharedPreferences(BackgroundService.this).getBoolean(getString(R.string.pref_reverse_current), getResources().getBoolean(R.bool.pref_reverse_current_default));
                values[INDEX_CURRENT] = String.format(Locale.getDefault(), "%s: %d mA", getString(R.string.info_current), current / (reverseCurrent ? 1000 : -1000));
                notifyListener(INDEX_CURRENT);
            }
        }

        private void setTemperature(double temperature, boolean forceUpdate) {
            if (forceUpdate || this.temperature != temperature || values[INDEX_TEMPERATURE] == null) {
                this.temperature = temperature;
                values[INDEX_TEMPERATURE] = String.format(
                        Locale.getDefault(),
                        "%s: %s",
                        getString(R.string.info_temperature),
                        TemperatureConverter.getCorrectTemperatureString(BackgroundService.this, temperature)
                );
                notifyListener(INDEX_TEMPERATURE);
            }
        }

        private void setVoltage(double voltage) {
            if (this.voltage != voltage || values[INDEX_VOLTAGE] == null) {
                this.voltage = voltage;
                values[INDEX_VOLTAGE] = String.format(Locale.getDefault(), getString(R.string.info_voltage) + ": %.3f V", voltage);
                notifyListener(INDEX_VOLTAGE);
            }
        }

        private String getHealthString(int health) {
            Context context = BackgroundService.this;
            switch (health) {
                case BATTERY_HEALTH_COLD:
                    return context.getString(R.string.health_cold);
                case BATTERY_HEALTH_DEAD:
                    return context.getString(R.string.health_dead);
                case BATTERY_HEALTH_GOOD:
                    return context.getString(R.string.health_good);
                case BATTERY_HEALTH_OVER_VOLTAGE:
                    return context.getString(R.string.health_overvoltage);
                case BATTERY_HEALTH_OVERHEAT:
                    return context.getString(R.string.health_overheat);
                case BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                    return context.getString(R.string.health_unspecified_failure);
                default:
                    return context.getString(R.string.health_unknown);
            }
        }

        private void onTemperatureUnitChanged() {
            setTemperature(temperature, true);
            batteryChangedReceiver.refreshInfoNotification(null);
        }

        private void notifyListener(int index) {
            if (listener != null) {
                listener.onBatteryValueChanged(this, index);
            }
        }
    }
}
