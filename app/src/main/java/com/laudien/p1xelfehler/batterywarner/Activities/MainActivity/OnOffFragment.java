package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity.SettingsActivity;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receivers.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import java.util.Locale;

import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.Contract.PREF_IS_ENABLED;
import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class OnOffFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    //private static final String TAG = "OnOffFragment";
    private static final int COLOR_RED = 0, COLOR_ORANGE = 1, COLOR_GREEN = 2;
    private static final int NO_STATE = -1;
    private SharedPreferences sharedPreferences;
    private Context context;
    private BatteryAlarmManager batteryAlarmManager;
    private TextView textView_technology, textView_temp, textView_health, textView_batteryLevel, textView_voltage;
    private ToggleButton toggleButton;
    private ImageView img_battery;
    private int warningLow, warningHigh, currentColor;
    private IntentFilter onOffChangedFilter, batteryChangedFilter;
    private boolean isCharging;

    private BroadcastReceiver onOffChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
            toggleButton.setOnCheckedChangeListener(null); // disable the toasts
            toggleButton.setChecked(sharedPreferences.getBoolean(PREF_IS_ENABLED, true));
            toggleButton.setOnCheckedChangeListener(OnOffFragment.this); // enable the toasts
        }
    };

    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String technology = intent.getStringExtra(android.os.BatteryManager.EXTRA_TECHNOLOGY);
            double temperature = (double) intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, NO_STATE) / 10;
            int health = intent.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, NO_STATE);
            String healthString;
            int batteryLevel = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, NO_STATE);
            double voltage = (double) intent.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, NO_STATE) / 1000;
            isCharging = intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;

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
                batteryAlarmManager.checkBattery(false);
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
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        batteryAlarmManager = new BatteryAlarmManager(context);
        Button btn_settings = (Button) view.findViewById(R.id.btn_settings);
        btn_settings.setOnClickListener(this);
        toggleButton = (ToggleButton) view.findViewById(R.id.toggleButton);
        warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
        warningHigh = sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_HIGH);

        boolean isChecked = sharedPreferences.getBoolean(PREF_IS_ENABLED, true);
        toggleButton.setChecked(isChecked);
        toggleButton.setOnCheckedChangeListener(this);

        textView_technology = (TextView) view.findViewById(R.id.textView_technology);
        textView_temp = (TextView) view.findViewById(R.id.textView_temp);
        textView_health = (TextView) view.findViewById(R.id.textView_health);
        textView_batteryLevel = (TextView) view.findViewById(R.id.textView_batteryLevel);
        textView_voltage = (TextView) view.findViewById(R.id.textView_voltage);
        img_battery = (ImageView) view.findViewById(R.id.img_battery);

        onOffChangedFilter = new IntentFilter(Contract.BROADCAST_ON_OFF_CHANGED);
        batteryChangedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(onOffChangedReceiver, onOffChangedFilter);
        getActivity().registerReceiver(batteryChangedReceiver, batteryChangedFilter);
        onOffChangedReceiver.onReceive(context, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(onOffChangedReceiver);
        getActivity().unregisterReceiver(batteryChangedReceiver);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_settings:
                startActivity(new Intent(context, SettingsActivity.class));
                break;
        }
    }

    private void setImageColor(int color) {
        setImageColor(color, img_battery);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        //Log.i(TAG, "User changed status to " + isChecked);
        sharedPreferences.edit().putBoolean(PREF_IS_ENABLED, isChecked).apply();
        if (isChecked) {
            sharedPreferences.edit().putBoolean(Contract.PREF_ALREADY_NOTIFIED, false).apply();
            if (isCharging)
                context.startService(new Intent(context, ChargingService.class));
            else
                batteryAlarmManager.checkBattery(true);
            Toast.makeText(context, getString(R.string.enabled_info), LENGTH_SHORT).show();
        } else {
            if (isCharging)
                context.stopService(new Intent(context, ChargingService.class));
            else
                BatteryAlarmManager.cancelExistingAlarm(context);
            Toast.makeText(context, getString(R.string.disabled_info), LENGTH_SHORT).show();
        }
    }
}
