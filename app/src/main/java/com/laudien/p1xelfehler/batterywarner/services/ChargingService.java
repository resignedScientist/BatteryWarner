package com.laudien.p1xelfehler.batterywarner.services;

import android.app.AlarmManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.fragments.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.helper.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.media.AudioManager.RINGER_MODE_CHANGED_ACTION;
import static android.os.BatteryManager.BATTERY_PLUGGED_AC;
import static android.os.BatteryManager.BATTERY_PLUGGED_USB;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.os.BatteryManager.EXTRA_TEMPERATURE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.laudien.p1xelfehler.batterywarner.AppInfoHelper.IS_PRO;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_NOT_ROOTED;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_NO_ALARM_TIME_FOUND;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_SILENT_MODE;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_STOP_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_STOP_CHARGING_NOT_WORKING;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_WARNING_HIGH;

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
    private long smartChargingResumeTime, smartChargingTime;
    private GraphDbHelper graphDbHelper = GraphDbHelper.getInstance(this);
    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            int batteryLevel = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int temperature = intent.getIntExtra(EXTRA_TEMPERATURE, 0);
            long timeNow = System.currentTimeMillis();
            chargingType = intent.getIntExtra(EXTRA_PLUGGED, -1);
            boolean isCharging = chargingType != 0;
            boolean isChargingTypeEnabled = isChargingTypeEnabled(chargingType);
            // stop service if user unplugs the device before the smart charging limit is reached and after the smart charging time is reached
            if (!isCharging && isChargingResumed) {
                stopSelf();
                return;
            }
            // stop charging and stop self if plugged in via usb but usb charging is disabled
            if (usbChargingDisabled && chargingType == BATTERY_PLUGGED_USB) {
                stopCharging();
                stopSelf();
                return;
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
                        // make sure that already notified is false in the shared preferences before notifying
                        getSharedPreferences(getString(R.string.prefs_temporary), MODE_PRIVATE).edit()
                                .putBoolean(getString(R.string.pref_already_notified), false)
                                .apply();
                        // show warning high notification
                        NotificationHelper.showNotification(context, ID_WARNING_HIGH);
                    }
                    // stop charging if enabled
                    if (stopChargingEnabled) {
                        if (!isChargingPaused && !isChargingResumed) { // stop only if not paused and not resumed!
                            pauseCharging();
                        }
                        if (smartChargingEnabled) {
                            // stop charging and this service if the smart charging limit is reached
                            if (batteryLevel >= smartChargingLimit) {
                                stopCharging();
                                stopSelf();
                                return;
                            }
                        } else { // stop service if smart charging is disabled
                            stopSelf();
                            return;
                        }
                    }
                }
            }
            // check if resume time is reached and charging is paused and not resumed yet
            if (!isCharging && stopChargingEnabled && smartChargingEnabled && isChargingPaused && !isChargingResumed && timeNow >= smartChargingResumeTime) {
                if (isGraphEnabled && IS_PRO) { // add a graph point for optics/correctness
                    graphDbHelper.addValue(timeNow, batteryLevel, temperature);
                }
                resumeCharging();
            }
            // stop service if everything is turned off or the device is fully charged
            if ((!isCharging && !(smartChargingEnabled && isChargingPaused)) || batteryLevel == 100
                    || (!isGraphEnabled && (!warningHighEnabled || !isChargingTypeEnabled) && !stopChargingEnabled && !smartChargingEnabled)) {
                stopSelf();
            }
        }
    };
    private BroadcastReceiver ringerModeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int ringerMode = audioManager.getRingerMode();
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                NotificationHelper.cancelNotification(context, ID_SILENT_MODE);
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
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return sharedPreferences.getBoolean(context.getString(R.string.pref_wireless_enabled), context.getResources().getBoolean(R.bool.pref_wireless_enabled_default));
            default: // discharging or unknown charging type
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
        smartChargingTime = sharedPreferences.getLong(getString(R.string.pref_smart_charging_time), -1);
        smartChargingResumeTime = getSmartChargingResumeTime(sharedPreferences);
        if (smartChargingLimit < warningHigh) {
            smartChargingLimit = warningHigh;
        }
        Log.d(TAG, "warningHighEnabled = " + warningHighEnabled);
        Log.d(TAG, "warningHigh = " + warningHigh);
        Log.d(TAG, "isGraphEnabled = " + isGraphEnabled);
        Log.d(TAG, "stopChargingEnabled = " + stopChargingEnabled);
        Log.d(TAG, "smartChargingEnabled = " + smartChargingEnabled);
        Log.d(TAG, "smartChargingLimit = " + smartChargingLimit);
        Log.d(TAG, "acEnabled = " + acEnabled);
        Log.d(TAG, "usbEnabled = " + usbEnabled);
        Log.d(TAG, "wirelessEnabled = " + wirelessEnabled);
        Log.d(TAG, "usbChargingDisabled = " + usbChargingDisabled);
        Log.d(TAG, "smartChargingUseClock = " + smartChargingUseClock);
        Log.d(TAG, "smartChargingMinutes = " + smartChargingMinutes);
        Log.d(TAG, "smartChargingTime = " + smartChargingTime);
        Log.d(TAG, "smartChargingResumeTime = " + smartChargingResumeTime);
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
            SharedPreferences temporaryPreferences = getSharedPreferences(getString(R.string.prefs_temporary), MODE_PRIVATE);
            temporaryPreferences.edit()
                    .putInt(getString(R.string.pref_last_chargingType), chargingType)
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
            smartChargingResumeTime = getSmartChargingResumeTime(sharedPreferences);
        } else if (key.equals(getString(R.string.pref_smart_charging_limit))) {
            smartChargingLimit = sharedPreferences.getInt(key, getResources().getInteger(R.integer.pref_smart_charging_limit_default));
            if (smartChargingLimit < warningHigh) {
                smartChargingLimit = warningHigh;
            }
        } else if (key.equals(getString(R.string.pref_smart_charging_time_before))) {
            smartChargingMinutes = sharedPreferences.getInt(key, getResources().getInteger(R.integer.pref_smart_charging_time_before_default));
            smartChargingResumeTime = getSmartChargingResumeTime(sharedPreferences);
        } else if (key.equals(getString(R.string.pref_smart_charging_time))) {
            smartChargingTime = sharedPreferences.getLong(key, -1);
            smartChargingResumeTime = getSmartChargingResumeTime(sharedPreferences);
        } else if (key.equals(getString(R.string.pref_smart_charging_use_alarm_clock_time))) {
            smartChargingUseClock = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
            smartChargingResumeTime = getSmartChargingResumeTime(sharedPreferences);
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
                stopCharging();
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
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return wirelessEnabled;
            default: // discharging
                return false;
        }
    }

    private void pauseCharging() {
        Log.d(TAG, "Pausing charging...");
        isChargingPaused = true;
        stopCharging();
    }

    private void stopCharging() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RootHelper.disableCharging();
                    NotificationHelper.showNotification(ChargingService.this, ID_STOP_CHARGING);
                } catch (RootHelper.NotRootedException e) {
                    e.printStackTrace();
                    NotificationHelper.showNotification(ChargingService.this, ID_NOT_ROOTED);
                } catch (RootHelper.NoBatteryFileFoundException e) {
                    e.printStackTrace();
                    NotificationHelper.showNotification(ChargingService.this, ID_STOP_CHARGING_NOT_WORKING);
                }
            }
        });
    }

    private void resumeCharging() {
        Log.d(TAG, "Resuming charging...");
        isChargingResumed = true;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RootHelper.enableCharging();
                    NotificationHelper.cancelNotification(ChargingService.this, ID_STOP_CHARGING);
                } catch (RootHelper.NotRootedException e) {
                    e.printStackTrace();
                    NotificationHelper.showNotification(ChargingService.this, ID_NOT_ROOTED);
                    stopSelf(); // stop service if not rooted!
                } catch (RootHelper.NoBatteryFileFoundException e) {
                    e.printStackTrace();
                    NotificationHelper.showNotification(ChargingService.this, ID_STOP_CHARGING_NOT_WORKING);
                }
            }
        });
    }

    private long getSmartChargingResumeTime(SharedPreferences sharedPreferences) {
        if (smartChargingEnabled) {
            long alarmTime; // the target time the device should be charged to the defined maximum
            if (SDK_INT >= LOLLIPOP && smartChargingUseClock) { // use alarm clock
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
                if (alarmClockInfo != null) {
                    alarmTime = alarmClockInfo.getTriggerTime();
                } else { // no alarm time is set in the alarm app
                    NotificationHelper.showNotification(this, ID_NO_ALARM_TIME_FOUND);
                    sharedPreferences.edit().putBoolean(getString(R.string.pref_smart_charging_use_alarm_clock_time), false).apply();
                    return 0;
                }
            } else { // use time in shared preferences (smartChargingTime)
                if (smartChargingTime != -1) {
                    alarmTime = smartChargingTime;
                } else { // there is no time saved in shared preferences
                    return 0;
                }
            }
            long timeBefore = (long) smartChargingMinutes * 60 * 1000;
            long timeNow = System.currentTimeMillis();
            long resumeTime = alarmTime - timeBefore;
            while (resumeTime <= timeNow) {
                alarmTime += 1000 * 60 * 60 * 24; // add a day if time is in the past
                resumeTime = alarmTime - timeBefore;
                // save the new time in the shared preferences
                sharedPreferences.edit().putLong(getString(R.string.pref_smart_charging_time), alarmTime).apply();
                Log.d(TAG, "added a day to the time!");
            }
            // => Smart charging notification (only for test purposes!)
            /*DateFormat formatter = DateFormat.getDateTimeInstance(SHORT, SHORT, Locale.getDefault());
            String message = String.format(Locale.getDefault(),
                    "%s: %d%%\n%s: %s\n%s: %d%%\n%s: %s\n%s: %d\n%s: %b",
                    "Charge to", warningHigh,
                    "Resume charging at", formatter.format(new Date(alarmTime - timeBefore)),
                    "Then charge to", smartChargingLimit,
                    "Target time", formatter.format(new Date(alarmTime)),
                    "Minutes before", smartChargingMinutes,
                    "Use next alarm clock", smartChargingUseClock
            );
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1346, new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Smart Charging")
                    .setContentText(message)
                    .setStyle(NotificationHelper.getBigTextStyle(message))
                    .build()
            );*/
            // <= Smart charging notification (only for test purposes!)
            return resumeTime; // return the resume time
        } else { // smart charging is disabled
            return 0;
        }
    }
}
