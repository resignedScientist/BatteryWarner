package com.laudien.p1xelfehler.batterywarner.preferences;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.laudien.p1xelfehler.batterywarner.AppInfoHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.SettingsActivity;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;
import com.laudien.p1xelfehler.batterywarner.receivers.RootCheckFinishedReceiver;

import java.util.ArrayList;
import java.util.Locale;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.O;
import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper.ID_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper.ID_DISCHARGING;
import static com.laudien.p1xelfehler.batterywarner.receivers.RootCheckFinishedReceiver.ACTION_ROOT_CHECK_FINISHED;

/**
 * A Fragment that shows the default settings and adds some functionality to some settings when
 * they are changed.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int REQUEST_AUTO_SAVE = 70;
    private TwoStatePreference pref_autoSave, pref_warningHighEnabled, pref_usb, pref_ac,
            pref_wireless, pref_graphEnabled, switch_darkTheme, pref_infoNotificationEnabled,
            pref_usb_disabled, pref_stopCharging, pref_power_saving_mode, pref_measureBatteryDrain,
            pref_reset_battery_stats, pref_chargingService, pref_darkInfoNotification;
    private RingtonePreference ringtonePreference_high, ringtonePreference_low;
    private Preference pref_smart_charging, pref_info_notification_items;
    private final RootCheckFinishedReceiver rootCheckFinishedReceiver = new RootCheckFinishedReceiver() {
        @Override
        protected void disablePreferences(String preferenceKey) {
            if (preferenceKey.equals(getString(R.string.pref_stop_charging))) {
                pref_stopCharging.setChecked(false);
            } else if (preferenceKey.equals(getString(R.string.pref_smart_charging_enabled))) {
                SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
                sharedPreferences.edit()
                        .putBoolean(getString(R.string.pref_smart_charging_enabled), false)
                        .apply();
                setSmartChargingSummary(sharedPreferences);
            } else if (preferenceKey.equals(getString(R.string.pref_usb_charging_disabled))) {
                pref_usb_disabled.setChecked(false);
            } else if (preferenceKey.equals(getString(R.string.pref_power_saving_mode))) {
                pref_power_saving_mode.setChecked(false);
            } else if (preferenceKey.equals(getString(R.string.pref_reset_battery_stats))) {
                pref_reset_battery_stats.setChecked(false);
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        switch_darkTheme = (TwoStatePreference) findPreference(getString(R.string.pref_dark_theme_enabled));
        ringtonePreference_high = (RingtonePreference) findPreference(getString(R.string.pref_sound_uri_high));
        ringtonePreference_low = (RingtonePreference) findPreference(getString(R.string.pref_sound_uri_low));
        pref_autoSave = (TwoStatePreference) findPreference(getString(R.string.pref_graph_autosave));
        pref_graphEnabled = (TwoStatePreference) findPreference(getString(R.string.pref_graph_enabled));
        pref_warningHighEnabled = (TwoStatePreference) findPreference(getString(R.string.pref_warning_high_enabled));
        pref_usb = (TwoStatePreference) findPreference(getString(R.string.pref_usb_enabled));
        pref_ac = (TwoStatePreference) findPreference(getString(R.string.pref_ac_enabled));
        pref_wireless = (TwoStatePreference) findPreference(getString(R.string.pref_wireless_enabled));
        pref_stopCharging = (TwoStatePreference) findPreference(getString(R.string.pref_stop_charging));
        pref_chargingService = (TwoStatePreference) findPreference(getString(R.string.pref_charging_service_enabled));
        pref_usb_disabled = (TwoStatePreference) findPreference(getString(R.string.pref_usb_charging_disabled));
        pref_smart_charging = findPreference(getString(R.string.pref_smart_charging_enabled));
        pref_info_notification_items = findPreference(getString(R.string.pref_info_notification_items));
        pref_power_saving_mode = (TwoStatePreference) findPreference(getString(R.string.pref_power_saving_mode));
        pref_reset_battery_stats = (TwoStatePreference) findPreference(getString(R.string.pref_reset_battery_stats));
        pref_darkInfoNotification = (TwoStatePreference) findPreference(getString(R.string.pref_dark_info_notification));
        pref_infoNotificationEnabled = (TwoStatePreference) findPreference(getString(R.string.pref_info_notification_enabled));
        pref_measureBatteryDrain = (TwoStatePreference) findPreference(getString(R.string.pref_measure_battery_drain));

        Context context = getContext();
        if (context != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            setRingtoneSummary(true); // warning high sound
            setRingtoneSummary(false); // warning low sound
            // register receivers
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            context.registerReceiver(rootCheckFinishedReceiver, new IntentFilter(ACTION_ROOT_CHECK_FINISHED));
        }

        if (!AppInfoHelper.isPro()) {
            pref_graphEnabled.setEnabled(false);
            Preference pref_timeFormat = findPreference(getString(R.string.pref_time_format));
            pref_timeFormat.setEnabled(false);
            pref_autoSave.setEnabled(false);
            PreferenceCategory category_graph = (PreferenceCategory) findPreference("stats");
            category_graph.setTitle(String.format(Locale.getDefault(),
                    "%s (%s)", getString(R.string.title_stats), getString(R.string.toast_not_pro_short)));
            Preference pref_pro = new Preference(context);
            pref_pro.setTitle(getString(R.string.title_get_pro));
            pref_pro.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + AppInfoHelper.PACKAGE_NAME_PRO)));
                    } catch (android.content.ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + AppInfoHelper.PACKAGE_NAME_PRO)));
                    }
                    return true;
                }
            });
            category_graph.addPreference(pref_pro);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        pref_smart_charging.setEnabled(pref_stopCharging.isChecked());
        pref_usb.setEnabled(!pref_usb_disabled.isChecked());
        if (AppInfoHelper.isPro()) {
            pref_autoSave.setEnabled(pref_graphEnabled.isChecked());
        }
        Context context = getContext();
        if (context != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            setInfoNotificationSubtitle(sharedPreferences);
            setSmartChargingSummary(sharedPreferences);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Context context = getContext();
        if (context != null) {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            context.unregisterReceiver(rootCheckFinishedReceiver);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUTO_SAVE && grantResults[0] != PERMISSION_GRANTED) {
            pref_autoSave.setChecked(false); // uncheck preference again if permission was not granted
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference == switch_darkTheme) {
            Context context = getContext();
            if (context != null && context instanceof SettingsActivity) {
                ToastHelper.sendToast(context, R.string.toast_theme_changed, LENGTH_SHORT);
            }
        } else if (preference == ringtonePreference_high) {
            setRingtoneSummary(true);
        } else if (preference == ringtonePreference_low) {
            setRingtoneSummary(false);
        } else if (preference == pref_autoSave && pref_autoSave.isChecked()) {
            // check for permission
            if (SDK_INT >= M
                    && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_AUTO_SAVE);
            }
        } else if (preference == pref_warningHighEnabled) {
            boolean highChecked = pref_warningHighEnabled.isChecked();
            if (!highChecked) {
                pref_stopCharging.setChecked(false);
                sharedPreferences.edit().putBoolean(getString(R.string.pref_smart_charging_enabled), false).apply();
                pref_smart_charging.setSummary(getString(R.string.summary_disabled));
            }
            if (highChecked) {
                Context context = getContext();
                if (context != null) {
                    ServiceHelper.startService(context, sharedPreferences, ID_CHARGING);
                }
            }
        } else if (preference == pref_graphEnabled) {
            boolean checked = pref_graphEnabled.isChecked();
            pref_autoSave.setEnabled(checked);
            Context context = getContext();
            if (context != null && checked) {
                ServiceHelper.startService(context, sharedPreferences, ID_CHARGING);
            }
        } else if (preference == pref_stopCharging || preference == pref_usb_disabled || preference == pref_power_saving_mode || preference == pref_reset_battery_stats) { // root features
            TwoStatePreference twoStatePreference = (TwoStatePreference) preference;
            if (twoStatePreference != null && twoStatePreference.isChecked()) {
                RootHelper.handleRootDependingPreference(getActivity(), preference.getKey());
            }
            if (preference == pref_stopCharging) {
                pref_smart_charging.setEnabled(pref_stopCharging.isChecked());
                sharedPreferences.edit().putBoolean(getString(R.string.pref_smart_charging_enabled), false).apply();
                pref_smart_charging.setSummary(getString(R.string.summary_disabled));
            } else if (preference == pref_usb_disabled) {
                boolean checked = pref_usb_disabled.isChecked();
                pref_usb.setEnabled(!checked);
            }
        } else if (preference == pref_chargingService) {
            Context context = getContext();
            if (context != null) {
                if (pref_chargingService.isChecked()) {
                    ServiceHelper.startService(context, sharedPreferences, ID_CHARGING);
                }
                setInfoNotificationSubtitle(sharedPreferences);
            }
        } else if ((preference == pref_ac && pref_ac.isChecked())
                || (preference == pref_usb && pref_usb.isChecked())
                || (preference == pref_wireless && pref_wireless.isChecked())) {
            Context context = getContext();
            if (context != null) {
                ServiceHelper.startService(context, sharedPreferences, ID_CHARGING);
            }
        } else if (preference == pref_darkInfoNotification || preference == pref_infoNotificationEnabled || preference == pref_measureBatteryDrain) {
            Context context = getContext();
            if (context != null) {
                ServiceHelper.restartService(getContext(), sharedPreferences, ID_DISCHARGING);
            }
        }
    }

    @Override
    public Context getContext() {
        return SDK_INT >= M ? super.getContext() : getActivity();
    }

    private void setInfoNotificationSubtitle(SharedPreferences sharedPreferences) {
        ArrayList<String> enabledItems = new ArrayList<>();
        if (sharedPreferences.getBoolean(getString(R.string.pref_info_technology), getResources().getBoolean(R.bool.pref_info_technology_default)))
            enabledItems.add(getString(R.string.info_technology));
        if (sharedPreferences.getBoolean(getString(R.string.pref_info_temperature), getResources().getBoolean(R.bool.pref_info_temperature_default)))
            enabledItems.add(getString(R.string.info_temperature));
        if (sharedPreferences.getBoolean(getString(R.string.pref_info_health), getResources().getBoolean(R.bool.pref_info_health_default)))
            enabledItems.add(getString(R.string.info_health));
        if (sharedPreferences.getBoolean(getString(R.string.pref_info_battery_level), getResources().getBoolean(R.bool.pref_info_battery_level_default)))
            enabledItems.add(getString(R.string.info_battery_level));
        if (sharedPreferences.getBoolean(getString(R.string.pref_info_voltage), getResources().getBoolean(R.bool.pref_info_voltage_default)))
            enabledItems.add(getString(R.string.info_voltage));
        if (sharedPreferences.getBoolean(getString(R.string.pref_info_current), getResources().getBoolean(R.bool.pref_info_current_default)))
            enabledItems.add(getString(R.string.info_current));
        if (sharedPreferences.getBoolean(getString(R.string.pref_measure_battery_drain), getResources().getBoolean(R.bool.pref_measure_battery_drain_default))) {
            if (sharedPreferences.getBoolean(getString(R.string.pref_info_screen_on), getResources().getBoolean(R.bool.pref_info_screen_on_default)))
                enabledItems.add(getString(R.string.info_screen_on));
            if (sharedPreferences.getBoolean(getString(R.string.pref_info_screen_off), getResources().getBoolean(R.bool.pref_info_screen_off_default)))
                enabledItems.add(getString(R.string.info_screen_off));
        }
        if (!enabledItems.isEmpty()) {
            String summary = enabledItems.get(0);
            for (byte i = 0; i < enabledItems.size(); i++) {
                if (i == 0) {
                    continue;
                }
                summary = summary.concat(", ").concat(enabledItems.get(i));
            }
            pref_info_notification_items.setSummary(summary);
        } else { // no items selected
            pref_info_notification_items.setSummary(getString(R.string.notification_no_items_enabled));
        }
    }

    private void setSmartChargingSummary(SharedPreferences sharedPreferences) {
        boolean smartChargingEnabled = sharedPreferences.getBoolean(getString(R.string.pref_smart_charging_enabled), getResources().getBoolean(R.bool.pref_smart_charging_enabled_default));
        pref_smart_charging.setSummary(smartChargingEnabled ? R.string.title_enabled : R.string.summary_disabled);
    }

    private void setRingtoneSummary(boolean warningHigh) {
        if (SDK_INT < O) {
            Context context = getActivity();
            if (context != null) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                String sound = sharedPreferences.getString(getString(warningHigh ? R.string.pref_sound_uri_high : R.string.pref_sound_uri_low), "");
                Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(sound));
                Preference preference = warningHigh ? ringtonePreference_high : ringtonePreference_low;
                preference.setSummary(ringtone.getTitle(context));
            }
        }
    }
}
