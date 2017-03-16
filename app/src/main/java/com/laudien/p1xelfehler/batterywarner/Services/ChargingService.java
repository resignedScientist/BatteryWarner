package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.AlarmManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.RootChecker;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.media.AudioManager.RINGER_MODE_CHANGED_ACTION;
import static android.os.BatteryManager.BATTERY_PLUGGED_AC;
import static android.os.BatteryManager.BATTERY_PLUGGED_USB;
import static android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.os.BatteryManager.EXTRA_TEMPERATURE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.laudien.p1xelfehler.batterywarner.Contract.IS_PRO;
import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_NOT_ROOTED;
import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_NO_ALARM_TIME_FOUND;
import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_WARNING_HIGH;
import static java.text.DateFormat.SHORT;

/**
 * Background service that runs while charging. It records the charging curve with the GraphDbHelper class
 * and shows a notification if the battery level is above X% as defined in settings.
 * It stops automatically after the device is fully charged, not charging anymore
 * or nothing in the settings is enabled that needs this service.
 * If the pro version is used, it saves the graph using the static method in the GraphFragment.
 */
public class ChargingService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = getClass().getSimpleName();
    private boolean warningHighEnabled, isGraphEnabled, acEnabled, usbEnabled, wirelessEnabled,
            stopChargingEnabled, smartChargingEnabled, smartChargingUseClock, graphChanged, usbChargingDisabled,
            isChargingPaused = false, isChargingResumed = false, alreadyNotified = false;
    private int warningHigh, smartChargingLimit, smartChargingMinutes, chargingType, lastBatteryLevel = -1;
    private long smartChargingResumeTime;
    private String smartChargingTimeString;
    private DateFormat dateFormat = DateFormat.getTimeInstance(SHORT, Locale.getDefault());
    private GraphDbHelper graphDbHelper = GraphDbHelper.getInstance(this);
    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            int batteryLevel = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int temperature = intent.getIntExtra(EXTRA_TEMPERATURE, 0);
            long timeNow = Calendar.getInstance().getTimeInMillis();
            chargingType = intent.getIntExtra(EXTRA_PLUGGED, -1);
            boolean isCharging = chargingType != 0;
            boolean isChargingTypeEnabled = isChargingTypeEnabled(chargingType);
            // stop service if usb charging but disabled in settings..
            // or not charging and not paused by the service
            if (usbChargingDisabled && chargingType == BATTERY_PLUGGED_USB || !isCharging && !isChargingPaused) {
                stopSelf();
                return;
            }
            // stop charging again if the user dismisses the notification while charging is paused
            if (isCharging && isChargingPaused) {
                isCharging = false;
                stopCharging();
            }
            if (batteryLevel != lastBatteryLevel) { // if battery level changed
                // add a value to the database
                if (isGraphEnabled && IS_PRO) {
                    lastBatteryLevel = batteryLevel;
                    if (!graphChanged) { // reset table if it is the first value
                        graphChanged = true;
                        graphDbHelper.resetTable();
                    }
                    graphDbHelper.addValue(timeNow, batteryLevel, temperature);
                }
                if (batteryLevel >= warningHigh) {
                    // show the warning high notification if the battery level reached it
                    if (warningHighEnabled && isChargingTypeEnabled && !alreadyNotified) {
                        alreadyNotified = true;
                        NotificationBuilder.showNotification(context, ID_WARNING_HIGH);
                    }
                    // stop charging if enabled
                    if (stopChargingEnabled) {
                        if (!isChargingPaused && !isChargingResumed) { // stop only if not paused and not resumed!
                            stopCharging();
                        }
                        if (smartChargingEnabled) {
                            // stop charging and this service if the smart charging limit is reached
                            if (batteryLevel >= smartChargingLimit) {
                                stopCharging();
                                stopSelf();
                            } else {
                                // check if resume time is reached and charging is not resumed yet
                                if (!isChargingResumed && timeNow >= smartChargingResumeTime) {
                                    // resume charging
                                    resumeCharging();
                                }
                            }
                        } else { // stop service if smart charging is disabled
                            stopSelf();
                        }

                    }
                }
                // stop service if everything is turned off or the device is fully charged
                if ((!isCharging && !(smartChargingEnabled && isChargingPaused)) || batteryLevel == 100
                        || (!isGraphEnabled && (!warningHighEnabled || !isChargingTypeEnabled) && !stopChargingEnabled && !smartChargingEnabled)) {
                    stopSelf();
                }
            }
        }
    };
    private BroadcastReceiver ringerModeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int ringerMode = audioManager.getRingerMode();
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(NotificationBuilder.ID_SILENT_MODE);
                unregisterReceiver(this);
            }
        }
    };

    /**
     * Checks if the given charging type is enabled in settings.
     *
     * @param context           An instance of the Context class.
     * @param chargingType      The charging type you can receive from the BatteryManager.
     * @param sharedPreferences An instance of the SharedPreferences class. If it is null, the default preferences from the Context are used.
     * @return Returns true, if the charging type is enabled in settings, false if not. It also returns false, if the type is discharging.
     */
    public static boolean isChargingTypeEnabled(Context context, int chargingType, @Nullable SharedPreferences sharedPreferences) {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        switch (chargingType) {
            case BATTERY_PLUGGED_AC:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_ac_enabled), context.getResources().getBoolean(R.bool.pref_ac_enabled_default));
            case BATTERY_PLUGGED_USB:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_usb_enabled), context.getResources().getBoolean(R.bool.pref_usb_enabled_default));
            case BATTERY_PLUGGED_WIRELESS:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_wireless_enabled), context.getResources().getBoolean(R.bool.pref_wireless_enabled_default));
            default: // discharging
                return false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // read the variables from the shared preferences
        warningHighEnabled = sharedPreferences.getBoolean(getString(R.string.pref_warning_high_enabled), getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
        warningHigh = sharedPreferences.getInt(getString(R.string.pref_warning_high), getResources().getInteger(R.integer.pref_warning_high_default));
        isGraphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
        stopChargingEnabled = sharedPreferences.getBoolean(getString(R.string.pref_stop_charging), getResources().getBoolean(R.bool.pref_stop_charging_default));
        smartChargingEnabled = sharedPreferences.getBoolean(getString(R.string.pref_smart_charging_enabled), getResources().getBoolean(R.bool.pref_smart_charging_enabled_default));
        smartChargingLimit = sharedPreferences.getInt(getString(R.string.pref_smart_charging_limit), getResources().getInteger(R.integer.pref_smart_charging_limit_default));
        acEnabled = sharedPreferences.getBoolean(getString(R.string.pref_ac_enabled), getResources().getBoolean(R.bool.pref_ac_enabled_default));
        usbEnabled = sharedPreferences.getBoolean(getString(R.string.pref_usb_enabled), getResources().getBoolean(R.bool.pref_usb_enabled_default));
        wirelessEnabled = sharedPreferences.getBoolean(getString(R.string.pref_wireless_enabled), getResources().getBoolean(R.bool.pref_wireless_enabled_default));
        usbChargingDisabled = sharedPreferences.getBoolean(getString(R.string.pref_usb_charging_disabled), getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
        smartChargingUseClock = sharedPreferences.getBoolean(getString(R.string.pref_smart_charging_use_alarm_clock_time), getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        smartChargingMinutes = sharedPreferences.getInt(getString(R.string.pref_smart_charging_time_before), getResources().getInteger(R.integer.pref_smart_charging_time_before_default));
        smartChargingTimeString = sharedPreferences.getString(getString(R.string.pref_smart_charging_time), null);
        Log.d(TAG, "smartChargingTimeString = " + smartChargingTimeString);
        smartChargingResumeTime = getSmartChargingResumeTime();
        if (smartChargingLimit < warningHigh) {
            smartChargingLimit = warningHigh;
        }
        // register all receivers
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        registerReceiver(batteryChangedReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
        registerReceiver(ringerModeChangedReceiver, new IntentFilter(RINGER_MODE_CHANGED_ACTION));
        Log.d(TAG, "Service started!");
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // unregister all receivers
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(batteryChangedReceiver);
        try { // try and catch because receiver might already be unregistered
            unregisterReceiver(ringerModeChangedReceiver);
        } catch (Exception ignored) {
        }
        // auto save
        boolean autoSaveEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_autosave), getResources().getBoolean(R.bool.pref_graph_autosave_default));
        if (isGraphEnabled && autoSaveEnabled && graphChanged) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    GraphFragment.saveGraph(ChargingService.this);
                }
            });
        }
        // put last battery level in the shared preferences (it might be used by the discharging service)
        if (lastBatteryLevel != -1) {
            sharedPreferences.edit()
                    .putInt(getString(R.string.pref_last_percentage), lastBatteryLevel)
                    .apply();
        }
        Log.d(TAG, "Service destroyed!");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_warning_high_enabled))) {
            warningHighEnabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
        } else if (key.equals(getString(R.string.pref_warning_high))) {
            warningHigh = sharedPreferences.getInt(key, getResources().getInteger(R.integer.pref_warning_high_default));
        } else if (key.equals(getString(R.string.pref_graph_enabled))) {
            isGraphEnabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_graph_enabled_default));
        } else if (key.equals(getString(R.string.pref_stop_charging))) {
            stopChargingEnabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_stop_charging_default));
        } else if (key.equals(getString(R.string.pref_smart_charging_enabled))) {
            smartChargingEnabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_smart_charging_enabled_default));
        } else if (key.equals(getString(R.string.pref_smart_charging_limit))) {
            smartChargingLimit = sharedPreferences.getInt(key, getResources().getInteger(R.integer.pref_smart_charging_limit_default));
            if (smartChargingLimit < warningHigh) {
                smartChargingLimit = warningHigh;
            }
        } else if (key.equals(getString(R.string.pref_smart_charging_time_before))) {
            smartChargingMinutes = sharedPreferences.getInt(key, getResources().getInteger(R.integer.pref_smart_charging_time_before_default));
            smartChargingResumeTime = getSmartChargingResumeTime();
        } else if (key.equals(getString(R.string.pref_smart_charging_use_alarm_clock_time))) {
            smartChargingUseClock = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
            smartChargingResumeTime = getSmartChargingResumeTime();
        } else if (key.equals(getString(R.string.pref_ac_enabled))) {
            acEnabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_ac_enabled_default));
        } else if (key.equals(getString(R.string.pref_usb_enabled))) {
            usbEnabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_usb_enabled_default));
        } else if (key.equals(getString(R.string.pref_wireless_enabled))) {
            wirelessEnabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_wireless_enabled_default));
        } else if (key.equals(getString(R.string.pref_usb_charging_disabled))) {
            usbChargingDisabled = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
            // stop service if usb charging, but usb charging was disabled
            if (usbChargingDisabled && chargingType == BATTERY_PLUGGED_USB) {
                stopSelf();
            }
        }
    }

    private boolean isChargingTypeEnabled(int chargingType) {
        switch (chargingType) {
            case BATTERY_PLUGGED_AC:
                return acEnabled;
            case BATTERY_PLUGGED_USB:
                return usbEnabled;
            case BATTERY_PLUGGED_WIRELESS:
                return wirelessEnabled;
            default: // discharging
                return false;
        }
    }

    private void stopCharging() {
        isChargingPaused = true;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RootChecker.disableCharging(ChargingService.this);
                } catch (RootChecker.NotRootedException e) {
                    e.printStackTrace();
                    NotificationBuilder.showNotification(ChargingService.this, ID_NOT_ROOTED);
                    stopSelf(); // stop service if not rooted!
                }
            }
        });
    }

    private void resumeCharging() {
        isChargingResumed = true;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RootChecker.enableCharging(ChargingService.this);
                } catch (RootChecker.NotRootedException e) {
                    e.printStackTrace();
                    NotificationBuilder.showNotification(ChargingService.this, ID_NOT_ROOTED);
                    stopSelf(); // stop service if not rooted!
                }
            }
        });
    }

    private long getSmartChargingResumeTime() {
        long alarmTime;
        if (smartChargingUseClock && SDK_INT >= LOLLIPOP) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
            if (alarmClockInfo != null) {
                alarmTime = alarmClockInfo.getTriggerTime();
            } else {// the smart charging feature cannot be used, because no alarm time is set in the alarm app
                if (smartChargingEnabled) {
                    smartChargingEnabled = false; // disable the feature just for the service
                    NotificationBuilder.showNotification(this, ID_NO_ALARM_TIME_FOUND); // show the notification
                }
                return 0;
            }
        } else { // KitKat devices
            if (smartChargingEnabled) {
                try {
                    Date date = dateFormat.parse(smartChargingTimeString);
                    Calendar calendar = Calendar.getInstance();
                    long timeNow = calendar.getTimeInMillis();
                    calendar.setTime(date);
                    int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);
                    calendar.setTimeInMillis(timeNow);
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    alarmTime = calendar.getTimeInMillis();
                    if (alarmTime <= timeNow) {
                        alarmTime += 1000 * 60 * 60 * 24; // add a day if time is in the past
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    stopSelf();
                    return 0;
                }
            } else {
                return 0;
            }
        }
        // Now we have the time the alarm is ringing (alarmTime). Now we calculate the resume time:
        long timeBefore = (long) smartChargingMinutes * 60 * 1000;
        return alarmTime - timeBefore;
    }
}
