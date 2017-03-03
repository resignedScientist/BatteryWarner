package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receivers.DischargingAlarmReceiver;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;
import com.laudien.p1xelfehler.batterywarner.Services.DischargingService;

import java.util.Locale;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;

public class OnOffFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "OnOffFragment";
    private static final int COLOR_RED = 0, COLOR_ORANGE = 1, COLOR_GREEN = 2;
    private static final int NO_STATE = -1;
    private SharedPreferences sharedPreferences;
    private Context context;
    private TextView textView_technology, textView_temp, textView_health, textView_batteryLevel,
            textView_voltage, textView_current, textView_screenOn, textView_screenOff;
    private ToggleButton toggleButton;
    private ImageView img_battery;
    private int warningLow, warningHigh, currentColor;
    private boolean isCharging;
    private BatteryManager batteryManager;
    private long screenOnTime, screenOffTime;
    private int screenOnDrain, screenOffDrain;

    private BroadcastReceiver onOffChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            toggleButton.setOnCheckedChangeListener(null); // disable the toasts
            toggleButton.setChecked(sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default)));
            toggleButton.setOnCheckedChangeListener(OnOffFragment.this); // enable the toasts
        }
    };

    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            String technology = intent.getStringExtra(android.os.BatteryManager.EXTRA_TECHNOLOGY);
            double temperature = (double) intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, NO_STATE) / 10;
            int health = intent.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, NO_STATE);
            String healthString;
            int batteryLevel = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, NO_STATE);
            double voltage = (double) intent.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, NO_STATE) / 1000;
            isCharging = intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
            boolean dischargingServiceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));

            if (dischargingServiceEnabled) {
                double screenOnTimeInHours = (double) screenOnTime / 3600000;
                double screenOffTimeInHours = (double) screenOffTime / 3600000;
                double screenOnPercentPerHour = screenOnDrain / screenOnTimeInHours;
                double screenOffPercentPerHour = screenOffDrain / screenOffTimeInHours;
                Log.d(TAG, "screenOnDrain = " + screenOnDrain);
                Log.d(TAG, "screenOffDrain = " + screenOffDrain);
                Log.d(TAG, "screenOnTimeInHours = " + screenOnTimeInHours);
                Log.d(TAG, "screenOffTimeInHours = " + screenOffTimeInHours);
                Log.d(TAG, "screenOnPercentPerHour = " + screenOnPercentPerHour);
                Log.d(TAG, "screenOffPercentPerHour = " + screenOffPercentPerHour);
                if (screenOnPercentPerHour != 0.0 && !Double.isInfinite(screenOnPercentPerHour) && !Double.isNaN(screenOnPercentPerHour)
                        && screenOffPercentPerHour != 0.0 && !Double.isInfinite(screenOffPercentPerHour) && !Double.isNaN(screenOffPercentPerHour)) {
                    textView_screenOn.setText(String.format(Locale.getDefault(), "%s: %.2f %%/h",
                            getString(R.string.screen_on), screenOnPercentPerHour));
                    textView_screenOff.setText(String.format(Locale.getDefault(), "%s: %.2f %%/h",
                            getString(R.string.screen_off), screenOffPercentPerHour));
                    showPercentPerHour();
                } else {
                    textView_screenOn.setText(String.format(Locale.getDefault(), "%s: %s",
                            getString(R.string.screen_on), getString(R.string.not_enough_data)));
                    textView_screenOff.setText(String.format(Locale.getDefault(), "%s: %s",
                            getString(R.string.screen_off), getString(R.string.not_enough_data)));
                }
            } else {
                hidePercentPerHour();
            }

            if (batteryManager != null) {
                long currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                textView_current.setText(String.format(
                        Locale.getDefault(),
                        "%s: %d mA",
                        getString(R.string.current),
                        currentNow / -1000)
                );
            }

            switch (health) {
                case android.os.BatteryManager.BATTERY_HEALTH_COLD:
                    healthString = getString(R.string.health_cold);
                    break;
                case android.os.BatteryManager.BATTERY_HEALTH_DEAD:
                    healthString = getString(R.string.health_dead);
                    break;
                case android.os.BatteryManager.BATTERY_HEALTH_GOOD:
                    healthString = getString(R.string.health_good);
                    break;
                case android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                    healthString = getString(R.string.health_overvoltage);
                    break;
                case android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT:
                    healthString = getString(R.string.health_overheat);
                    break;
                case android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                    healthString = getString(R.string.health_unspecified_failure);
                    break;
                default:
                    healthString = getString(R.string.health_unknown);
                    break;
            }

            textView_technology.setText(getString(R.string.technology) + ": " + technology);
            textView_temp.setText(String.format(Locale.getDefault(),
                    getString(R.string.temperature) + ": %.1f °C", temperature));
            textView_health.setText(getString(R.string.health) + ": " + healthString);
            textView_batteryLevel.setText(String.format(getString(R.string.battery_level) + ": %d%%", batteryLevel));
            textView_voltage.setText(String.format(Locale.getDefault(),
                    getString(R.string.voltage) + ": %.3f V", voltage));

            // Image color
            int nextColor;
            if (batteryLevel <= warningLow) { // battery low
                nextColor = COLOR_RED;
                setImageColor(context.getResources().getColor(R.color.colorBatteryLow));
            } else if (batteryLevel < warningHigh) { // battery ok
                nextColor = COLOR_GREEN;
                setImageColor(context.getResources().getColor(R.color.colorBatteryOk));
            } else { // battery high
                nextColor = COLOR_ORANGE;
                setImageColor(context.getResources().getColor(R.color.colorBatteryHigh));
            }
            if (nextColor != currentColor) {
                if (nextColor == COLOR_RED) {
                    NotificationBuilder.showNotification(context, NotificationBuilder.ID_WARNING_LOW);
                } else if (nextColor == COLOR_ORANGE) {
                    NotificationBuilder.showNotification(context, NotificationBuilder.ID_WARNING_HIGH);
                }
                currentColor = nextColor;
            }
        }
    };

    public static void setImageColor(int color, ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        imageView.setImageDrawable(drawable);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_on_off, container, false);
        context = getContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        toggleButton = (ToggleButton) view.findViewById(R.id.toggleButton);

        boolean isChecked = sharedPreferences.getBoolean(getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default));
        toggleButton.setChecked(isChecked);
        toggleButton.setOnCheckedChangeListener(this);

        textView_technology = (TextView) view.findViewById(R.id.textView_technology);
        textView_temp = (TextView) view.findViewById(R.id.textView_temp);
        textView_health = (TextView) view.findViewById(R.id.textView_health);
        textView_batteryLevel = (TextView) view.findViewById(R.id.textView_batteryLevel);
        textView_voltage = (TextView) view.findViewById(R.id.textView_voltage);
        textView_current = (TextView) view.findViewById(R.id.textView_current);
        textView_screenOn = (TextView) view.findViewById(R.id.textView_screenOn);
        textView_screenOff = (TextView) view.findViewById(R.id.textView_screenOff);
        img_battery = (ImageView) view.findViewById(R.id.img_battery);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager = (BatteryManager) getActivity().getSystemService(Context.BATTERY_SERVICE);
        } else {
            textView_current.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        screenOnTime = sharedPreferences.getLong(getString(R.string.value_time_screen_on), 0);
        screenOffTime = sharedPreferences.getLong(getString(R.string.value_time_screen_off), 0);
        warningLow = sharedPreferences.getInt(getString(R.string.pref_warning_low), getResources().getInteger(R.integer.pref_warning_low_default));
        warningHigh = sharedPreferences.getInt(getString(R.string.pref_warning_high), getResources().getInteger(R.integer.pref_warning_high_default));
        screenOnDrain = sharedPreferences.getInt(getString(R.string.value_drain_screen_on), 0);
        screenOffDrain = sharedPreferences.getInt(getString(R.string.value_drain_screen_off), 0);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        getActivity().registerReceiver(onOffChangedReceiver, new IntentFilter(Contract.BROADCAST_ON_OFF_CHANGED));
        getActivity().registerReceiver(batteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        onOffChangedReceiver.onReceive(context, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        getActivity().unregisterReceiver(onOffChangedReceiver);
        getActivity().unregisterReceiver(batteryChangedReceiver);
    }

    private void setImageColor(int color) {
        setImageColor(color, img_battery);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        sharedPreferences.edit().putBoolean(getString(R.string.pref_is_enabled), isChecked).apply();
        if (isChecked) { // turned on
            sharedPreferences.edit().putBoolean(getString(R.string.pref_already_notified), false).apply();
            if (isCharging) {
                context.startService(new Intent(context, ChargingService.class));
            } else {
                context.sendBroadcast(new Intent(Contract.BROADCAST_DISCHARGING_ALARM));
                context.startService(new Intent(context, DischargingService.class));
            }
            Toast.makeText(context, getString(R.string.enabled_info), LENGTH_SHORT).show();
        } else if (!isCharging) { // turned off and discharging
            DischargingAlarmReceiver.cancelDischargingAlarm(context);
            Toast.makeText(context, getString(R.string.disabled_info), LENGTH_SHORT).show();
        }
        // send broadcast
        context.sendBroadcast(new Intent(Contract.BROADCAST_ON_OFF_CHANGED));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(getString(R.string.value_time_screen_on))) {
            screenOnTime = sharedPreferences.getLong(s, 0);
        } else if (s.equals(getString(R.string.value_time_screen_off))) {
            screenOffTime = sharedPreferences.getLong(s, 0);
        } else if (s.equals(getString(R.string.value_drain_screen_on))) {
            screenOnDrain = sharedPreferences.getInt(s, 0);
        } else if (s.equals(getString(R.string.value_drain_screen_off))) {
            screenOffDrain = sharedPreferences.getInt(s, 0);
        }
    }

    private void showPercentPerHour() {
        textView_screenOn.setVisibility(VISIBLE);
        textView_screenOff.setVisibility(VISIBLE);
    }

    private void hidePercentPerHour() {
        textView_screenOn.setVisibility(INVISIBLE);
        textView_screenOff.setVisibility(INVISIBLE);
    }
}
