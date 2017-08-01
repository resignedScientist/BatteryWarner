package com.laudien.p1xelfehler.batterywarner.fragments;

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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.view.View.GONE;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_HEALTH;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_TECHNOLOGY;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData.INDEX_VOLTAGE;

/**
 * Fragment that shows some battery information in TextViews.
 */
public class BatteryInfoFragment extends Fragment implements BatteryData.OnBatteryValueChangedListener {

    public static final byte COLOR_LOW = 1;
    public static final byte COLOR_HIGH = 2;
    public static final byte COLOR_OK = 3;
    private byte currentColor = 0;
    private int warningLow, warningHigh;
    private SharedPreferences sharedPreferences;
    private TextView textView_current, textView_technology,
            textView_temp, textView_health, textView_batteryLevel, textView_voltage;
    private BatteryData batteryData;
    private OnBatteryColorChangedListener onBatteryColorChangedListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        View view = inflater.inflate(R.layout.fragment_battery_infos, container, false);

        textView_technology = view.findViewById(R.id.textView_technology);
        textView_temp = view.findViewById(R.id.textView_temp);
        textView_health = view.findViewById(R.id.textView_health);
        textView_batteryLevel = view.findViewById(R.id.textView_batteryLevel);
        textView_voltage = view.findViewById(R.id.textView_voltage);
        textView_current = view.findViewById(R.id.textView_current);

        if (SDK_INT < LOLLIPOP) {
            textView_current.setVisibility(GONE);
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        warningLow = sharedPreferences.getInt(getString(R.string.pref_warning_low), getResources().getInteger(R.integer.pref_warning_low_default));
        warningHigh = sharedPreferences.getInt(getString(R.string.pref_warning_high), getResources().getInteger(R.integer.pref_warning_high_default));
        Context context = getActivity();
        if (context != null) {
            // register receivers
            Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            batteryData = BatteryHelper.getBatteryData(batteryStatus, context);
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
        batteryData.unregisterOnBatteryValueChangedListener(this);
    }

    /**
     * Method that updates the view when the {@link com.laudien.p1xelfehler.batterywarner.helper.BatteryHelper.BatteryData}
     * has changed.
     *
     * @param index The index of the data that has changed.
     */
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
        }
    }

    public void setOnBatteryColorChangedListener(OnBatteryColorChangedListener onBatteryColorChangedListener) {
        this.onBatteryColorChangedListener = onBatteryColorChangedListener;
        if (currentColor != 0) {
            onBatteryColorChangedListener.onColorChanged(currentColor);
        }
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
        }
    }

    public interface OnBatteryColorChangedListener {
        void onColorChanged(byte colorID);
    }
}
