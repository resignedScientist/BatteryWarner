package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Calendar;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.media.AudioManager.RINGER_MODE_CHANGED_ACTION;
import static com.laudien.p1xelfehler.batterywarner.Contract.NO_STATE;

public class ChargingService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static String TAG = "ChargingService";
    private SharedPreferences sharedPreferences;
    private int lastPercentage = NO_STATE, warningHigh, lastChargingType;
    private boolean graphEnabled, isEnabled, warningHighEnabled, acEnabled, usbEnabled, wirelessEnabled;

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
        public void onReceive(Context context, Intent batteryStatus) {
            boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE) != 0;
            if (!isCharging // if not charging
                    || !isEnabled // if not enabled
                    || !isChargingTypeEnabled(batteryStatus)
                    || (!graphEnabled && !warningHighEnabled)) // if not charging AND warningHigh is disabled
            {
                stopSelf();
                return;
            }

            int batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, NO_STATE);
            if (warningHighEnabled && batteryLevel >= warningHigh) { // warning high
                NotificationBuilder.showNotification(context, NotificationBuilder.ID_WARNING_HIGH);
            }

            // log in database
            int temperature = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, NO_STATE);
            if (Contract.IS_PRO && graphEnabled && batteryLevel != lastPercentage && temperature != NO_STATE) {
                GraphDbHelper graphDbHelper = GraphDbHelper.getInstance(context);
                long timeNow = Calendar.getInstance().getTimeInMillis();
                graphDbHelper.addValue(timeNow, batteryLevel, temperature);
                lastPercentage = batteryLevel;
            }

            // stop service if battery is full
            if (batteryLevel == 100) {
                stopSelf();
            }
        }
    };

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
            boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE) != 0;
            if (isCharging) {
                context.startService(new Intent(context, ChargingService.class));
            }
        }
    }

    public static void stopService(Context context) {
        context.stopService(new Intent(context, ChargingService.class));
    }

    public static void restartService(Context context) {
        stopService(context);
        startService(context);
    }

    public static boolean isChargingTypeEnabled(Context context, Intent batteryStatus) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int chargingType = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE);
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

    public static boolean isChargingTypeEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int chargingType = sharedPreferences.getInt(context.getString(R.string.pref_last_chargingType), NO_STATE);
        boolean acEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_ac_enabled), context.getResources().getBoolean(R.bool.pref_ac_enabled_default));
        boolean usbEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_usb_enabled), context.getResources().getBoolean(R.bool.pref_usb_enabled_default));
        boolean wirelessEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_wireless_enabled), context.getResources().getBoolean(R.bool.pref_wireless_enabled_default));
        return isChargingTypeEnabled(chargingType, acEnabled, usbEnabled, wirelessEnabled);
    }

    public boolean isChargingTypeEnabled(Intent batteryStatus) {
        int chargingType = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE);
        if (chargingType != lastChargingType) {
            lastChargingType = chargingType;
            sharedPreferences.edit().putInt(getString(R.string.pref_last_chargingType), chargingType).apply();
        }
        return isChargingTypeEnabled(chargingType, acEnabled, usbEnabled, wirelessEnabled);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // not used, because i won't bind it to anything!
        return null;
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
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
        }
    }
}
