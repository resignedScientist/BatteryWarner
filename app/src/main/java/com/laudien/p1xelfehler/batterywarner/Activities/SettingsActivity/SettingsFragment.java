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
    private TwoStatePreference pref_autoSave, pref_warningLow, pref_warningHigh;
    private Preference pref_usb, pref_ac, pref_wireless;
    private SwitchPreference switch_darkTheme;
    private RingtonePreference ringtonePreference;
    private SeekBarPreference pref_seekBarLow, pref_seekBarHigh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        if (getActivity() instanceof SettingsActivity) {
            switch_darkTheme = (SwitchPreference) findPreference(getString(R.string.pref_dark_theme_enabled));
            switch_darkTheme.setOnPreferenceChangeListener(this);
        }
        ringtonePreference = (RingtonePreference) findPreference(getString(R.string.pref_sound_uri));
        ringtonePreference.setOnPreferenceChangeListener(this);
        pref_autoSave = (TwoStatePreference) findPreference(getString(R.string.pref_graph_autosave));
        pref_autoSave.setOnPreferenceChangeListener(this);
        pref_warningLow = (TwoStatePreference) findPreference(getString(R.string.pref_warning_low_enabled));
        pref_warningLow.setOnPreferenceChangeListener(this);
        pref_warningHigh = (TwoStatePreference) findPreference(getString(R.string.pref_warning_high_enabled));
        pref_warningHigh.setOnPreferenceChangeListener(this);
        pref_seekBarLow = (SeekBarPreference) findPreference(getString(R.string.pref_warning_low));
        pref_seekBarHigh = (SeekBarPreference) findPreference(getString(R.string.pref_warning_high));
        pref_usb = findPreference(getString(R.string.pref_usb_enabled));
        pref_ac = findPreference(getString(R.string.pref_ac_enabled));
        pref_wireless = findPreference(getString(R.string.pref_wireless_enabled));

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
    }

    @Override
    public void onResume() {
        super.onResume();
        pref_seekBarLow.setEnabled(pref_warningLow.isChecked());
        boolean highChecked = pref_warningHigh.isChecked();
        pref_seekBarHigh.setEnabled(highChecked);
        pref_usb.setEnabled(highChecked);
        pref_ac.setEnabled(highChecked);
        pref_wireless.setEnabled(highChecked);
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
        } else if (preference == pref_warningLow) {
            pref_seekBarLow.setEnabled((boolean) o);
        } else if (preference == pref_warningHigh) {
            boolean highChecked = (boolean) o;
            pref_seekBarHigh.setEnabled(highChecked);
            pref_usb.setEnabled(highChecked);
            pref_ac.setEnabled(highChecked);
            pref_wireless.setEnabled(highChecked);
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
