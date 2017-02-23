package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receivers.DischargingAlarmReceiver;
import com.laudien.p1xelfehler.batterywarner.RootChecker;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";
    private static final int REQUEST_AUTO_SAVE = 70;
    private TwoStatePreference pref_autoSave, pref_warningLow, pref_warningHigh, pref_graphEnabled,
            pref_usb, pref_ac, pref_wireless, pref_stopCharging, switch_darkTheme;
    private RingtonePreference ringtonePreference;
    private SeekBarPreference pref_seekBarLow, pref_seekBarHigh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        switch_darkTheme = (TwoStatePreference) findPreference(getString(R.string.pref_dark_theme_enabled));
        ringtonePreference = (RingtonePreference) findPreference(getString(R.string.pref_sound_uri));
        pref_autoSave = (TwoStatePreference) findPreference(getString(R.string.pref_graph_autosave));
        pref_graphEnabled = (TwoStatePreference) findPreference(getString(R.string.pref_graph_enabled));
        pref_warningLow = (TwoStatePreference) findPreference(getString(R.string.pref_warning_low_enabled));
        pref_warningHigh = (TwoStatePreference) findPreference(getString(R.string.pref_warning_high_enabled));
        pref_seekBarLow = (SeekBarPreference) findPreference(getString(R.string.pref_warning_low));
        pref_seekBarHigh = (SeekBarPreference) findPreference(getString(R.string.pref_warning_high));
        pref_usb = (TwoStatePreference) findPreference(getString(R.string.pref_usb_enabled));
        pref_ac = (TwoStatePreference) findPreference(getString(R.string.pref_ac_enabled));
        pref_wireless = (TwoStatePreference) findPreference(getString(R.string.pref_wireless_enabled));
        pref_stopCharging = (TwoStatePreference) findPreference(getString(R.string.pref_stop_charging));

        Context context = getActivity();
        if (context != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String sound = sharedPreferences.getString(getString(R.string.pref_sound_uri), "");
            Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(sound));
            ringtonePreference.setSummary(ringtone.getTitle(context));
        }

        if (!Contract.IS_PRO) {
            pref_graphEnabled.setEnabled(false);
            Preference pref_timeFormat = findPreference(getString(R.string.pref_time_format));
            pref_timeFormat.setEnabled(false);
            pref_autoSave.setEnabled(false);
            PreferenceCategory category_graph = (PreferenceCategory) findPreference("stats");
            category_graph.setTitle(String.format(Locale.getDefault(),
                    "%s (%s)", getString(R.string.stats), getString(R.string.pro_only_short)));
            Preference pref_pro = new Preference(context);
            pref_pro.setTitle(getString(R.string.get_pro));
            pref_pro.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Contract.PACKAGE_NAME_PRO)));
                    } catch (android.content.ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + Contract.PACKAGE_NAME_PRO)));
                    }
                    return true;
                }
            });
            category_graph.addPreference(pref_pro);
        }

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        pref_seekBarLow.setEnabled(pref_warningLow.isChecked());
        boolean highChecked = pref_warningHigh.isChecked();
        pref_seekBarHigh.setEnabled(highChecked);
        pref_stopCharging.setEnabled(highChecked);
        pref_usb.setEnabled(highChecked);
        pref_ac.setEnabled(highChecked);
        pref_wireless.setEnabled(highChecked);
        if (Contract.IS_PRO) {
            pref_autoSave.setEnabled(pref_graphEnabled.isChecked());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUTO_SAVE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    pref_autoSave.setChecked(false);
                    return;
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference == switch_darkTheme) {
            Context context = getActivity();
            if (context != null && context instanceof SettingsActivity) {
                Toast.makeText(
                        context,
                        getString(R.string.theme_activated_toast),
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else if (preference == ringtonePreference) {
            Context context = getActivity();
            if (context != null) {
                String chosenRingtone = sharedPreferences.getString(key, "");
                Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(chosenRingtone));
                ringtonePreference.setSummary(ringtone.getTitle(context));
            }
        } else if (preference == pref_autoSave && !pref_autoSave.isChecked()) {
            // check for permission
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                            },
                            REQUEST_AUTO_SAVE
                    );
                }
            }
        } else if (preference == pref_warningLow) {
            pref_seekBarLow.setEnabled(pref_warningLow.isChecked());
            Context context = getActivity();
            if (context != null) {
                DischargingAlarmReceiver.cancelDischargingAlarm(context);
                context.sendBroadcast(new Intent(Contract.BROADCAST_DISCHARGING_ALARM));
            }
        } else if (preference == pref_warningHigh) {
            boolean highChecked = pref_warningHigh.isChecked();
            pref_seekBarHigh.setEnabled(highChecked);
            pref_usb.setEnabled(highChecked);
            pref_ac.setEnabled(highChecked);
            pref_wireless.setEnabled(highChecked);
            pref_stopCharging.setEnabled(highChecked);
            if (!highChecked) {
                pref_stopCharging.setChecked(false);
            }
            Context context = getActivity();
            if (context != null && highChecked) {
                // start service without resetting the graph
                context.startService(new Intent(context, ChargingService.class));
            }
        } else if (preference == pref_graphEnabled) {
            boolean checked = pref_graphEnabled.isChecked();
            pref_autoSave.setEnabled(checked);
            Context context = getActivity();
            if (context != null && checked) {
                Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batteryStatus != null) {
                    boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
                    if (isCharging) {
                        ChargingService.startService(getActivity());
                    }
                }
            }
        } else if (preference == pref_stopCharging) {
            boolean checked = pref_stopCharging.isChecked();
            if (checked) {
                new AsyncTask<Void, Void, Boolean>() {
                    // returns false if not rooted
                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        try {
                            RootChecker.isChargingEnabled();
                        } catch (RootChecker.NotRootedException e) {
                            return false;
                        } catch (RootChecker.BatteryFileNotFoundException e) {
                            Context context = getActivity();
                            if (context != null) {
                                NotificationBuilder.showNotification(context,
                                        NotificationBuilder.NOTIFICATION_ID_STOP_CHARGING_NOT_WORKING);
                            }
                        }
                        return true;
                    }

                    @Override
                    protected void onPostExecute(Boolean aBoolean) {
                        super.onPostExecute(aBoolean);
                        if (!aBoolean) { // show a toast if not rooted
                            Toast.makeText(getActivity(), getString(R.string.toast_not_rooted), Toast.LENGTH_SHORT).show();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    pref_stopCharging.setChecked(false);
                                }
                            }, 500);
                        }
                    }
                }.execute();
            }
        }
    }
}
