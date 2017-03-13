package com.laudien.p1xelfehler.batterywarner.Services;

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
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.Contract;
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
import static android.os.BatteryManager.BATTERY_PLUGGED_USB;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static com.laudien.p1xelfehler.batterywarner.Contract.IS_PRO;
import static com.laudien.p1xelfehler.batterywarner.Contract.NO_STATE;
import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_NOT_ROOTED;
import static java.text.DateFormat.SHORT;

/**
 * Background service that runs while charging. It records the charging curve with the GraphDbHelper class
 * and shows a notification if the battery level is above X% as defined in settings.
 * It stops automatically after the device is fully charged, not charging anymore
 * or nothing in the settings is enabled that need this service.
 * If the pro version is used, it saves the graph using the static method in the GraphFragment.
 */
public class ChargingService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = getClass().getSimpleName();
    private SharedPreferences sharedPreferences;
    private int lastPercentage = NO_STATE, warningHigh, lastChargingType, smartChargingPercentage;
    private boolean graphEnabled, isEnabled, warningHighEnabled, acEnabled, usbEnabled, wirelessEnabled,
            usbDisabled, smartChargingEnabled, chargingPaused = false;
    private long timeResumeCharging, timeBefore;
    private String timeString = null;

    private BroadcastReceiver ringerModeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int ringerMode = audioManager.getRingerMode();

            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(NotificationBuilder.ID_SILENT_MODE);
            }
        }
    };

    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent batteryStatus) {
            int chargingType = batteryStatus.getIntExtra(EXTRA_PLUGGED, NO_STATE);
            boolean isCharging = chargingType != 0;
            if ((!isCharging && !(smartChargingEnabled && chargingPaused)) // if not charging and not smartCharging AND chargingPaused
                    || !isEnabled // if not enabled
                    || !isChargingTypeEnabled(batteryStatus)
                    || (!graphEnabled && !warningHighEnabled)) // if not charging AND warningHigh is disabled
            {
                stopSelf();
                return;
            }
            long timeNow = Calendar.getInstance().getTimeInMillis();
            if (!chargingPaused) { // log only if charging is not paused
                int batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, NO_STATE);
                if (warningHighEnabled && batteryLevel >= warningHigh) { // warning high
                    NotificationBuilder.showNotification(context, NotificationBuilder.ID_WARNING_HIGH);
                    if (smartChargingEnabled) { // if smart charging is enabled --> pause charging
                        chargingPaused = true;
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    RootChecker.enableCharging(context);
                                } catch (RootChecker.NotRootedException e) {
                                    e.printStackTrace();
                                    NotificationBuilder.showNotification(context, ID_NOT_ROOTED);
                                }
                            }
                        });
                    } else if (!IS_PRO) {
                        stopSelf();
                        return;
                    }
                }
                if (batteryLevel != lastPercentage) {
                    // log in database
                    int temperature = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, NO_STATE);
                    if (Contract.IS_PRO && graphEnabled && temperature != NO_STATE) {
                        GraphDbHelper graphDbHelper = GraphDbHelper.getInstance(context);
                        graphDbHelper.addValue(timeNow, batteryLevel, temperature);
                        lastPercentage = batteryLevel;
                    }
                    // stop service if battery is full or reached smartChargingPercentage if enabled
                    if ((!smartChargingEnabled && batteryLevel == 100) || smartChargingEnabled && batteryLevel >= smartChargingPercentage) {
                        stopSelf();
                    }
                }
            } else { // charging is paused
                if (timeNow >= timeResumeCharging) { // if time resume is reached => resume charging
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                RootChecker.enableCharging(context);
                            } catch (RootChecker.NotRootedException e) {
                                e.printStackTrace();
                                NotificationBuilder.showNotification(context, ID_NOT_ROOTED);
                            }
                        }
                    });
                    chargingPaused = false;
                }
            }
        }
    };

    /**
     * Resets the graph if the graph is enabled and starts the ChargingService if the device is charging.
     *
     * @param context An instance of the Context class.
     */
    public static void startService(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean graphEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));
        if (graphEnabled) {
            GraphDbHelper dbHelper = GraphDbHelper.getInstance(context);
            dbHelper.resetTable();
            sharedPreferences.edit()
                    .putLong(context.getString(R.string.pref_graph_time), Calendar.getInstance().getTimeInMillis())
                    .putInt(context.getString(R.string.pref_last_percentage), -1)
                    .apply();
        }
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            boolean isCharging = batteryStatus.getIntExtra(EXTRA_PLUGGED, NO_STATE) != 0;
            if (isCharging) {
                context.startService(new Intent(context, ChargingService.class));
            }
        }
    }

    /**
     * Restarts the service with resetting the graph.
     *
     * @param context An instance of the Context class.
     */
    public static void restartService(Context context) {
        context.stopService(new Intent(context, ChargingService.class));
        startService(context);
    }

    public static boolean isChargingTypeEnabled(Context context, Intent batteryStatus) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int chargingType = batteryStatus.getIntExtra(EXTRA_PLUGGED, NO_STATE);
        boolean acEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_ac_enabled), context.getResources().getBoolean(R.bool.pref_ac_enabled_default));
        boolean usbEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_usb_enabled), context.getResources().getBoolean(R.bool.pref_usb_enabled_default));
        boolean wirelessEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_wireless_enabled), context.getResources().getBoolean(R.bool.pref_wireless_enabled_default));
        return isChargingTypeEnabled(chargingType, acEnabled, usbEnabled, wirelessEnabled);
    }

    public static boolean isChargingTypeEnabled(int chargingType, boolean acEnabled, boolean usbEnabled, boolean wirelessEnabled) {
        switch (chargingType) {
            case BatteryManager.BATTERY_PLUGGED_AC: // ac charging
                if (!acEnabled) {
                    return false;
                }
                break;
            case BatteryManager.BATTERY_PLUGGED_USB: // usb charging
                if (!usbEnabled) {
                    return false;
                }
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: // wireless charging
                if (!wirelessEnabled) {
                    return false;
                }
        }
        return true;
    }

    public boolean isChargingTypeEnabled(Intent batteryStatus) {
        int chargingType = batteryStatus.getIntExtra(EXTRA_PLUGGED, NO_STATE);
        if (chargingType != lastChargingType) {
            lastChargingType = chargingType;
            sharedPreferences.edit().putInt(getString(R.string.pref_last_chargingType), chargingType).apply();
        }
        return isChargingTypeEnabled(chargingType, acEnabled, usbEnabled, wirelessEnabled);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started!");
        // load from sharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        warningHigh = sharedPreferences.getInt(getString(R.string.pref_warning_high), getResources().getInteger(R.integer.pref_warning_high_default));
        graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
        isEnabled = sharedPreferences.getBoolean(getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default));
        warningHighEnabled = sharedPreferences.getBoolean(getString(R.string.pref_warning_high_enabled), getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
        acEnabled = sharedPreferences.getBoolean(getString(R.string.pref_ac_enabled), getResources().getBoolean(R.bool.pref_ac_enabled_default));
        usbEnabled = sharedPreferences.getBoolean(getString(R.string.pref_usb_enabled), getResources().getBoolean(R.bool.pref_usb_enabled_default));
        wirelessEnabled = sharedPreferences.getBoolean(getString(R.string.pref_wireless_enabled), getResources().getBoolean(R.bool.pref_wireless_enabled_default));
        lastChargingType = sharedPreferences.getInt(getString(R.string.pref_last_chargingType), NO_STATE);
        usbDisabled = sharedPreferences.getBoolean(getString(R.string.pref_usb_charging_disabled), getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
        smartChargingEnabled = sharedPreferences.getBoolean(getString(R.string.pref_smart_charging_enabled), getResources().getBoolean(R.bool.pref_smart_charging_enabled_default));
        timeBefore = sharedPreferences.getLong(getString(R.string.pref_smart_charging_time_before), getResources().getInteger(R.integer.pref_smart_charging_time_before_default));
        timeString = "";//sharedPreferences.getString(getString(R.string.pref_smart_charging_time), "");
        smartChargingPercentage = sharedPreferences.getInt(getString(R.string.pref_smart_charging_limit), getResources().getInteger(R.integer.pref_smart_charging_limit_default));
        calcSmartChargingTimes();

        registerReceiver(
                ringerModeChangedReceiver,
                new IntentFilter(RINGER_MODE_CHANGED_ACTION)
        );
        registerReceiver(
                batteryChangedReceiver,
                new IntentFilter(ACTION_BATTERY_CHANGED)
        );
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(ringerModeChangedReceiver);
        unregisterReceiver(batteryChangedReceiver);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        if (lastPercentage != NO_STATE) {
            sharedPreferences.edit()
                    .putInt(getString(R.string.pref_last_percentage), lastPercentage)
                    .apply();
        }
        // auto save if pro and last charging type was enabled
        if (IS_PRO && isChargingTypeEnabled(lastChargingType, acEnabled, usbEnabled, wirelessEnabled)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    GraphFragment.saveGraph(getApplicationContext());
                }
            });
        }
        Log.d(TAG, "Service destroyed!");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // not used, because i won't bind it to anything!
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preference) {
        this.sharedPreferences = sharedPreferences;
        if (preference.equals(getString(R.string.warning_high))) {
            warningHigh = sharedPreferences.getInt(preference, getResources().getInteger(R.integer.pref_warning_high_default));
        } else if (preference.equals(getString(R.string.pref_warning_high_enabled))) {
            warningHighEnabled = sharedPreferences.getBoolean(preference, getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
        } else if (preference.equals(getString(R.string.pref_graph_enabled))) {
            graphEnabled = sharedPreferences.getBoolean(preference, getResources().getBoolean(R.bool.pref_graph_enabled_default));
        } else if (preference.equals(getString(R.string.pref_is_enabled))) {
            isEnabled = sharedPreferences.getBoolean(preference, getResources().getBoolean(R.bool.pref_is_enabled_default));
        } else if (preference.equals(getString(R.string.pref_ac_enabled))) {
            acEnabled = sharedPreferences.getBoolean(preference, getResources().getBoolean(R.bool.pref_ac_enabled_default));
        } else if (preference.equals(getString(R.string.pref_usb_enabled))) {
            usbEnabled = sharedPreferences.getBoolean(preference, getResources().getBoolean(R.bool.pref_usb_enabled_default));
        } else if (preference.equals(getString(R.string.pref_wireless_enabled))) {
            wirelessEnabled = sharedPreferences.getBoolean(preference, getResources().getBoolean(R.bool.pref_wireless_enabled_default));
        } else if (preference.equals(getString(R.string.pref_usb_charging_disabled)) && lastChargingType == BATTERY_PLUGGED_USB) {
            // disable charging if usb charging was disabled while the service is running
            usbDisabled = sharedPreferences.getBoolean(preference, getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
            if (usbDisabled) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            RootChecker.disableCharging(ChargingService.this);
                            stopSelf();
                        } catch (RootChecker.NotRootedException e) {
                            e.printStackTrace();
                            NotificationBuilder.showNotification(ChargingService.this, ID_NOT_ROOTED);
                        }
                    }
                });
            }
        } else if (preference.equals(getString(R.string.pref_smart_charging_enabled))) {
            smartChargingEnabled = sharedPreferences.getBoolean(preference, getResources().getBoolean(R.bool.pref_smart_charging_enabled_default));
        } else if (preference.equals(getString(R.string.pref_smart_charging_time))) {
            timeString = sharedPreferences.getString(preference, null);
            calcSmartChargingTimes();
        } else if (preference.equals(getString(R.string.pref_smart_charging_time_before))) {
            timeBefore = sharedPreferences.getLong(preference, getResources().getInteger(R.integer.pref_smart_charging_time_before_default));
            calcSmartChargingTimes();
        } else if (preference.equals(getString(R.string.pref_smart_charging_limit))) {
            smartChargingPercentage = sharedPreferences.getInt(preference, getResources().getInteger(R.integer.pref_smart_charging_limit_default));
        }
    }

    private void calcSmartChargingTimes() {
        if (timeString != null && timeBefore > 0 && !timeString.equals("")) {
            DateFormat dateFormat = DateFormat.getTimeInstance(SHORT, Locale.getDefault());
            try {
                Date date = dateFormat.parse(timeString);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                long endTime = calendar.getTimeInMillis();
                timeResumeCharging = endTime - timeBefore;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
