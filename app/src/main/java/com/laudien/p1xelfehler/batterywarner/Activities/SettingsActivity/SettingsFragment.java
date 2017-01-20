package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.preference.TwoStatePreference;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    // private static final String TAG = "SettingsFragment";
    private static final int REQUEST_AUTO_SAVE = 70;
    TwoStatePreference pref_autoSave;
    private SwitchPreference switch_darkTheme;
    private RingtonePreference ringtonePreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        SeekBarPreference slider_warningHigh = (SeekBarPreference) findPreference(getString(R.string.pref_warning_high_enabled));
        slider_warningHigh.setOnPreferenceChangeListener(this);
        if (getActivity() instanceof SettingsActivity) {
            switch_darkTheme = (SwitchPreference) findPreference(getString(R.string.pref_dark_theme_enabled));
            switch_darkTheme.setOnPreferenceChangeListener(this);
        }
        ringtonePreference = (RingtonePreference) findPreference(getString(R.string.pref_sound_uri));
        ringtonePreference.setOnPreferenceChangeListener(this);
        pref_autoSave = (TwoStatePreference) findPreference(getString(R.string.pref_graph_autosave));
        pref_autoSave.setOnPreferenceChangeListener(this);

        Context context = getActivity();
        if (context != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String sound = sharedPreferences.getString(getString(R.string.pref_sound_uri), "");
            Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(sound));
            ringtonePreference.setSummary(ringtone.getTitle(context));
        }

        if (!Contract.IS_PRO) {
            Preference pref_graphEnabled = findPreference(getString(R.string.pref_graph_enabled));
            pref_graphEnabled.setEnabled(false);
            Preference pref_timeFormat = findPreference(getString(R.string.pref_time_format));
            pref_timeFormat.setEnabled(false);
            pref_autoSave.setEnabled(false);
            PreferenceCategory category_graph = (PreferenceCategory) findPreference("stats");
            category_graph.setTitle(String.format(Locale.getDefault(),
                    "%s (%s)", getString(R.string.stats), getString(R.string.pro_only_short)));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // restart discharging alarm and charging service
        Context context = getActivity();
        if (context != null) {
            BatteryAlarmManager batteryAlarmManager = BatteryAlarmManager.getInstance(context);
            batteryAlarmManager.cancelDischargingAlarm(context);
            batteryAlarmManager.setDischargingAlarm(context);
            context.startService(new Intent(context, ChargingService.class));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == switch_darkTheme) {
            Context context = getActivity();
            if (context != null) {
                Toast.makeText(
                        context,
                        getString(R.string.theme_activated_toast),
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else if (preference == ringtonePreference) {
            Context context = getActivity();
            if (context != null) {
                Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(o.toString()));
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
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
}
