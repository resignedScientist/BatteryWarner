package com.laudien.p1xelfehler.batterywarner.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper;
import com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.view.View.GONE;
import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_HEALTH;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_SCREEN_OFF;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_SCREEN_ON;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_TECHNOLOGY;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_WARNING_LOW;

public class BatteryInfoFragment extends Fragment implements BatteryData.OnBatteryValueChangedListener {

    public static final byte COLOR_LOW = 1;
    public static final byte COLOR_HIGH = 2;
    public static final byte COLOR_OK = 3;
    private boolean isCharging;
    private boolean infoNotificationEnabled;
    private byte currentColor = 0;
    private int warningLow, warningHigh;
    private SharedPreferences sharedPreferences;
    private TextView textView_screenOn, textView_screenOff, textView_current, textView_technology,
            textView_temp, textView_health, textView_batteryLevel, textView_voltage;
    private BatteryData batteryData;
    private OnBatteryColorChangedListener onBatteryColorChangedListener;
    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent batteryStatus) {
            isCharging = BatteryHelper.isCharging(batteryStatus);
            if (!infoNotificationEnabled) {
                batteryData.update(batteryStatus, context, sharedPreferences);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean dischargingServiceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
        View view = inflater.inflate(R.layout.fragment_battery_infos, container, false);

        textView_technology = view.findViewById(R.id.textView_technology);
        textView_temp = view.findViewById(R.id.textView_temp);
        textView_health = view.findViewById(R.id.textView_health);
        textView_batteryLevel = view.findViewById(R.id.textView_batteryLevel);
        textView_voltage = view.findViewById(R.id.textView_voltage);
        textView_current = view.findViewById(R.id.textView_current);
        textView_screenOn = view.findViewById(R.id.textView_screenOn);
        textView_screenOff = view.findViewById(R.id.textView_screenOff);

        if (SDK_INT < LOLLIPOP) {
            textView_current.setVisibility(GONE);
        }
        // hide the screen on/off TextViews if discharging service is disabled
        if (!dischargingServiceEnabled) {
            textView_screenOn.setVisibility(GONE);
            textView_screenOff.setVisibility(GONE);
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        warningLow = sharedPreferences.getInt(getString(R.string.pref_warning_low), getResources().getInteger(R.integer.pref_warning_low_default));
        warningHigh = sharedPreferences.getInt(getString(R.string.pref_warning_high), getResources().getInteger(R.integer.pref_warning_high_default));
        infoNotificationEnabled = sharedPreferences.getBoolean(getString(R.string.pref_info_notification_enabled), getResources().getBoolean(R.bool.pref_info_notification_enabled_default));
        Context context = getActivity();
        if (context != null) {
            // register receivers
            Intent batteryStatus = context.registerReceiver(batteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            batteryData = BatteryHelper.getBatteryData(batteryStatus, context, sharedPreferences);
            batteryData.registerOnBatteryValueChangedListener(this);
            // refresh TextViews
            for (byte i = 0; i < batteryData.getAsArray().length; i++) {
                onBatteryValueChanged(i);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(batteryChangedReceiver);
        batteryData.unregisterOnBatteryValueChangedListener(this);
    }

    public void resetDischargingStats() {
        // reset screen on/off percentages and times in sharedPreferences
        SharedPreferences temporaryPrefs = getContext().getSharedPreferences(getString(R.string.prefs_temporary), MODE_PRIVATE);
        temporaryPrefs.edit()
                .putInt(getString(R.string.value_drain_screen_on), 0)
                .putInt(getString(R.string.value_drain_screen_off), 0)
                .putLong(getString(R.string.value_time_screen_on), 0)
                .putLong(getString(R.string.value_time_screen_off), 0)
                .apply();
        showNoData();
        ToastHelper.sendToast(getContext(), R.string.toast_reset_data_success, LENGTH_SHORT);
    }

    public void setOnBatteryColorChangedListener(OnBatteryColorChangedListener onBatteryColorChangedListener) {
        this.onBatteryColorChangedListener = onBatteryColorChangedListener;
        if (currentColor != 0) {
            onBatteryColorChangedListener.onColorChanged(currentColor);
        }
    }

    private void showNoData() {
        textView_screenOn.setText(String.format(Locale.getDefault(), "%s: %s %%/h",
                getString(R.string.info_screen_on), "N/A"));
        textView_screenOff.setText(String.format(Locale.getDefault(), "%s: %s %%/h",
                getString(R.string.info_screen_off), "N/A"));
    }

    private void setBatteryColor() {
        byte nextColor;
        if (batteryData.getBatteryLevel() <= warningLow) { // battery low
            nextColor = COLOR_LOW;
        } else if (batteryData.getBatteryLevel() < warningHigh) { // battery ok
            nextColor = COLOR_OK;
        } else { // battery high
            nextColor = COLOR_HIGH;
        }
        if (nextColor != currentColor) {
            currentColor = nextColor;
            if (onBatteryColorChangedListener != null) {
                onBatteryColorChangedListener.onColorChanged(nextColor);
            }
            if (nextColor == COLOR_LOW) {
                if (!isCharging) {
                    Context context = getActivity();
                    if (context != null) {
                        NotificationHelper.showNotification(context, ID_WARNING_LOW);
                    }
                }
            }
        }
    }

    @Override
    public void onBatteryValueChanged(int index) {
        switch (index) {
            case INDEX_TECHNOLOGY:
                textView_technology.setText(batteryData.getValueString(index));
                break;
            case INDEX_TEMPERATURE:
                textView_temp.setText(batteryData.getValueString(index));
                break;
            case INDEX_HEALTH:
                textView_health.setText(batteryData.getValueString(index));
                break;
            case INDEX_BATTERY_LEVEL:
                textView_batteryLevel.setText(batteryData.getValueString(index));
                setBatteryColor();
                break;
            case INDEX_VOLTAGE:
                textView_voltage.setText(batteryData.getValueString(index));
                break;
            case INDEX_CURRENT:
                textView_current.setText(batteryData.getValueString(index));
                break;
            case INDEX_SCREEN_ON:
                textView_screenOn.setText(batteryData.getValueString(index));
                break;
            case INDEX_SCREEN_OFF:
                textView_screenOff.setText(batteryData.getValueString(index));
                break;
        }
    }

    public interface OnBatteryColorChangedListener {
        void onColorChanged(byte colorID);
    }
}
