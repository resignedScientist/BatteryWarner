package com.laudien.p1xelfehler.batterywarner.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper;
import com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.preferences.infoNotificationActivity.InfoNotificationActivity;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;
import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;
import static android.view.View.GONE;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_HEALTH;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_TECHNOLOGY;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_SILENT_MODE;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_WARNING_HIGH;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_WARNING_LOW;
import static com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper.ID_CHARGING;

/**
 * Background service that runs while discharging. It logs the percentage loss and times
 * for screen on and off and saves it in the default shared preferences.
 * It stops automatically if the user starts to charge or it is disabled in the settings.
 */
public class DischargingService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int NOTIFICATION_ID = 3001;
    private SharedPreferences sharedPreferences;
    private SharedPreferences temporaryPrefs;
    private BatteryChangedReceiver batteryChangedReceiver;
    private ChargingStateChangedReceiver chargingStateChangedReceiver;
    private MyOnBatteryValueChangedListener onBatteryValueChangedListener;
    private ScreenStateChangedReceiver screenStateChangedReceiver;
    private BatteryData batteryData;
    private RemoteViews notificationContent;
    private NotificationCompat.Builder compatBuilder;
    private Notification.Builder builder;
    private NotificationManager notificationManager;
    private boolean alreadyNotified = false;
    private boolean warningLowEnabled;
    private boolean infoNotificationEnabled;
    private int warningLow;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(getClass().getSimpleName(), "Starting service...");
        if (batteryChangedReceiver == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            temporaryPrefs = getSharedPreferences(getString(R.string.prefs_temporary), MODE_PRIVATE);
            warningLowEnabled = sharedPreferences.getBoolean(getString(R.string.pref_warning_low_enabled), getResources().getBoolean(R.bool.pref_warning_low_enabled_default));
            warningLow = sharedPreferences.getInt(getString(R.string.pref_warning_low), getResources().getInteger(R.integer.pref_warning_low_default));
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            batteryChangedReceiver = new BatteryChangedReceiver();
            onBatteryValueChangedListener = new MyOnBatteryValueChangedListener();
            Intent batteryStatus = registerReceiver(batteryChangedReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
            batteryData = BatteryHelper.getBatteryData(batteryStatus, this);
            chargingStateChangedReceiver = new ChargingStateChangedReceiver();
            IntentFilter chargingStateChangedFilter = new IntentFilter(ACTION_POWER_CONNECTED);
            chargingStateChangedFilter.addAction(ACTION_POWER_DISCONNECTED);
            registerReceiver(chargingStateChangedReceiver, chargingStateChangedFilter);
            screenStateChangedReceiver = new ScreenStateChangedReceiver();
            IntentFilter screenStateFilter = new IntentFilter(ACTION_SCREEN_ON);
            screenStateFilter.addAction(ACTION_SCREEN_OFF);
            registerReceiver(screenStateChangedReceiver, screenStateFilter);
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            temporaryPrefs.registerOnSharedPreferenceChangeListener(this);
            Log.d(getClass().getSimpleName(), "Service started!");
        } else {
            Log.d(getClass().getSimpleName(), "Service already running!");
        }
        // bind in info notification (if O or enabled)
        infoNotificationEnabled = SDK_INT >= O || sharedPreferences.getBoolean(getString(R.string.pref_info_notification_enabled), getResources().getBoolean(R.bool.pref_info_notification_enabled_default));
        if (infoNotificationEnabled) {
            batteryData.registerOnBatteryValueChangedListener(onBatteryValueChangedListener);
            notificationContent = createNotificationContent();
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(getClass().getSimpleName(), "Destroying service...");
        unregisterReceiver(batteryChangedReceiver);
        unregisterReceiver(chargingStateChangedReceiver);
        unregisterReceiver(screenStateChangedReceiver);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        temporaryPrefs.unregisterOnSharedPreferenceChangeListener(this);
        batteryData.unregisterOnBatteryValueChangedListener(onBatteryValueChangedListener);
        Log.d(getClass().getSimpleName(), "Service destroyed!");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_warning_low))) {
            warningLow = sharedPreferences.getInt(key, getResources().getInteger(R.integer.pref_warning_low_default));
        } else if (key.equals(getString(R.string.pref_warning_low_enabled))) {
            warningLowEnabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_warning_low_enabled_default));
            if (warningLowEnabled) {
                alreadyNotified = false;
            } else {
                NotificationHelper.cancelNotification(this, ID_WARNING_LOW);
            }
        } else if (key.equals(getString(R.string.pref_info_notification_enabled))) {
            infoNotificationEnabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_info_notification_enabled_default));
        } else if (key.equals(getString(R.string.pref_is_enabled))) {
            if (!sharedPreferences.getBoolean(key, true)) {
                stopSelf();
            }
        }
    }

    private Notification createNotification() {
        // generate message
        String[] data = batteryData.getEnabledOnly(this, sharedPreferences);
        String message = updateNotificationContent(data);
        // set text sizes
        int textSize = sharedPreferences.getInt(getString(R.string.pref_info_text_size), getResources().getInteger(R.integer.pref_info_text_size_default));
        notificationContent.setTextViewTextSize(R.id.textView_message_left, TypedValue.COMPLEX_UNIT_SP, textSize);
        notificationContent.setTextViewTextSize(R.id.textView_message_right, TypedValue.COMPLEX_UNIT_SP, textSize);
        // click intent
        Intent clickIntent = new Intent(this, InfoNotificationActivity.class);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID, clickIntent, 0);
        // build notification
        if (SDK_INT >= N) {
            builder = new Notification.Builder(this)
                    .setOngoing(true)
                    .setContentIntent(clickPendingIntent)
                    .setContentTitle(getString(R.string.title_info_notification))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentText(message)
                    .setCustomBigContentView(notificationContent);
            if (SDK_INT >= O) {
                builder.setChannelId(getString(R.string.channel_battery_info));
            } else { // Android N (API 24/25)
                builder.setPriority(Notification.PRIORITY_LOW);
            }
            return builder.build();
        } else { // API lower than N
            compatBuilder = new NotificationCompat.Builder(this)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setContentIntent(clickPendingIntent)
                    .setContentTitle(getString(R.string.title_info_notification))
                    .setCustomBigContentView(notificationContent)
                    .setContentText(message)
                    .setSmallIcon(R.mipmap.ic_launcher);
            return compatBuilder.build();
        }
    }

    private RemoteViews createNotificationContent() {
        boolean darkThemeEnabled = sharedPreferences.getBoolean(getString(R.string.pref_dark_info_notification), getResources().getBoolean(R.bool.pref_dark_info_notification_default));
        RemoteViews notificationContent;
        if (darkThemeEnabled) { // dark theme
            notificationContent = new RemoteViews(getPackageName(), R.layout.notification_battery_info_dark);
        } else { // default theme
            notificationContent = new RemoteViews(getPackageName(), R.layout.notification_battery_info);
        }
        if (SDK_INT == LOLLIPOP || SDK_INT == LOLLIPOP_MR1) {
            notificationContent.setImageViewResource(R.id.img_battery, R.mipmap.ic_launcher_round);
        }
        return notificationContent;
    }

    private void updateNotification() {
        if (infoNotificationEnabled) {
            String[] data = batteryData.getEnabledOnly(this, sharedPreferences);
            String message = updateNotificationContent(data);
            Notification notification = SDK_INT >= N ? builder.setContentText(message).build() : compatBuilder.setContentText(message).build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private String updateNotificationContent(String[] data) {
        if (data.length > 0) {
            if (data.length <= 3) {
                notificationContent.setViewVisibility(R.id.textView_message_right, GONE);
                notificationContent.setViewVisibility(R.id.view_middleLine, GONE);
                String message = data[0];
                for (byte i = 1; i < data.length; i++) {
                    message = message.concat("\n").concat(data[i]);
                }
                notificationContent.setTextViewText(R.id.textView_message_left, message);
                return message;
            } else { // more then 3 items
                String message_left = data[0], message_right = data[1];
                for (byte i = 2; i < data.length; i++) {
                    if (i % 2 == 0) {
                        message_left = message_left.concat("\n").concat(data[i]);
                    } else {
                        message_right = message_right.concat("\n").concat(data[i]);
                    }
                }
                notificationContent.setTextViewText(R.id.textView_message_left, message_left);
                notificationContent.setTextViewText(R.id.textView_message_right, message_right);
                return message_left;
            }
        } else { // no items enabled
            notificationContent.setViewVisibility(R.id.view_middleLine, GONE);
            notificationContent.setViewVisibility(R.id.textView_message_right, GONE);
            String message = getString(R.string.notification_no_items_enabled);
            notificationContent.setTextViewText(R.id.textView_message_left, message);
            return message;
        }
    }

    private class BatteryChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent batteryStatus) {
            batteryData.update(batteryStatus, DischargingService.this);
            boolean isCharging = BatteryHelper.isCharging(batteryStatus);
            int batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0);
            // show battery low warning
            if (!alreadyNotified && warningLowEnabled) {
                if (!isCharging) {
                    if (batteryLevel <= warningLow) {
                        alreadyNotified = true;
                        NotificationHelper.showNotification(getApplicationContext(), ID_WARNING_LOW);
                    }
                }
            }
        }
    }

    private class MyOnBatteryValueChangedListener implements BatteryData.OnBatteryValueChangedListener {
        @Override
        public void onBatteryValueChanged(int index) {
            switch (index) {
                case INDEX_TECHNOLOGY:
                    if (!sharedPreferences.getBoolean(getString(R.string.pref_info_technology), getResources().getBoolean(R.bool.pref_info_technology_default)))
                        return;
                    break;
                case INDEX_TEMPERATURE:
                    if (!sharedPreferences.getBoolean(getString(R.string.pref_info_temperature), getResources().getBoolean(R.bool.pref_info_temperature_default)))
                        return;
                    break;
                case INDEX_HEALTH:
                    if (!sharedPreferences.getBoolean(getString(R.string.pref_info_health), getResources().getBoolean(R.bool.pref_info_health_default)))
                        return;
                    break;
                case INDEX_BATTERY_LEVEL:
                    if (!sharedPreferences.getBoolean(getString(R.string.pref_info_battery_level), getResources().getBoolean(R.bool.pref_info_battery_level_default)))
                        return;
                    break;
                case INDEX_VOLTAGE:
                    if (!sharedPreferences.getBoolean(getString(R.string.pref_info_voltage), getResources().getBoolean(R.bool.pref_info_voltage_default)))
                        return;
                    break;
                case INDEX_CURRENT:
                    if (SDK_INT >= LOLLIPOP && !sharedPreferences.getBoolean(getString(R.string.pref_info_current), getResources().getBoolean(R.bool.pref_info_current_default)))
                        return;
                    break;
            }
            updateNotification();
        }
    }

    private class ChargingStateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_POWER_CONNECTED:
                    NotificationHelper.cancelNotification(context, ID_WARNING_LOW);
                    ServiceHelper.startService(context, sharedPreferences, ID_CHARGING);
                    break;
                case ACTION_POWER_DISCONNECTED:
                    boolean stopChargingEnabled = sharedPreferences.getBoolean(getString(R.string.pref_stop_charging), getResources().getBoolean(R.bool.pref_stop_charging_default));
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            NotificationHelper.cancelNotification(getApplicationContext(), ID_WARNING_HIGH, ID_SILENT_MODE);
                        }
                    }, stopChargingEnabled ? 5000 : 0);
                    break;
                default:
                    return;
            }
            alreadyNotified = false;
        }
    }

    private class ScreenStateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_SCREEN_ON)) {  // screen on
                Log.d(DischargingService.class.getSimpleName(), "screen on received!");
                registerReceiver(batteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            } else if (action.equals(ACTION_SCREEN_OFF)) { // screen off
                Log.d(DischargingService.class.getSimpleName(), "screen off received!");
                try { // crash workaround for some devices
                    unregisterReceiver(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
