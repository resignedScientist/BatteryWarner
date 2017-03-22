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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.Activities.BaseActivity;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

import static android.view.View.GONE;
import static android.widget.Toast.LENGTH_SHORT;

/**
 * Fragment that shows some information about the current battery status. Refreshes automatically.
 * Contains a button for toggling all warnings or logging of the app.
 */
public class BatteryInfoFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int NO_STATE = -1;
    private final String KEY_COLOR = "key_color";
    private int COLOR_RED, COLOR_ORANGE, COLOR_GREEN;
    private SharedPreferences sharedPreferences;
    private Context context;
    private TextView textView_technology, textView_temp, textView_health, textView_batteryLevel,
            textView_voltage, textView_current, textView_screenOn, textView_screenOff;
    private ImageView img_battery;
    private int warningLow, warningHigh, currentColor;
    private boolean isCharging, dischargingServiceEnabled;
    private BatteryManager batteryManager;
    private long screenOnTime, screenOffTime;
    private int screenOnDrain, screenOffDrain;

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

            if (dischargingServiceEnabled) {
                double screenOnTimeInHours = (double) screenOnTime / 3600000;
                double screenOffTimeInHours = (double) screenOffTime / 3600000;
                double screenOnPercentPerHour = screenOnDrain / screenOnTimeInHours;
                double screenOffPercentPerHour = screenOffDrain / screenOffTimeInHours;
                if (screenOnPercentPerHour != 0.0 && !Double.isInfinite(screenOnPercentPerHour) && !Double.isNaN(screenOnPercentPerHour)
                        && screenOffPercentPerHour != 0.0 && !Double.isInfinite(screenOffPercentPerHour) && !Double.isNaN(screenOffPercentPerHour)) {
                    textView_screenOn.setText(String.format(Locale.getDefault(), "%s: %.2f %%/h",
                            getString(R.string.screen_on), screenOnPercentPerHour));
                    textView_screenOff.setText(String.format(Locale.getDefault(), "%s: %.2f %%/h",
                            getString(R.string.screen_off), screenOffPercentPerHour));
                } else {
                    showNoData();
                }
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
            } else if (batteryLevel < warningHigh) { // battery ok
                nextColor = COLOR_GREEN;
            } else { // battery high
                nextColor = COLOR_ORANGE;
            }
            if (nextColor != currentColor) {
                currentColor = nextColor;
                setImageColor(nextColor, img_battery);
                if (!dischargingServiceEnabled && nextColor == COLOR_RED) {
                    NotificationBuilder.showNotification(context, NotificationBuilder.ID_WARNING_LOW);
                }
            }
        }
    };

    /**
     * Helper method for setting a color filter to an image.
     *
     * @param color     The color the filter should have.
     * @param imageView The ImageView the filter should be set to.
     */
    public static void setImageColor(int color, ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        imageView.setImageDrawable(drawable);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        dischargingServiceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
        setHasOptionsMenu(dischargingServiceEnabled);
        COLOR_GREEN = context.getResources().getColor(R.color.colorBatteryOk);
        COLOR_RED = context.getResources().getColor(R.color.colorBatteryLow);
        COLOR_ORANGE = context.getResources().getColor(R.color.colorBatteryHigh);
        View view = inflater.inflate(R.layout.fragment_battery_infos, container, false);

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
            textView_current.setVisibility(GONE);
        }

        if (!dischargingServiceEnabled) {
            textView_screenOn.setVisibility(GONE);
            textView_screenOff.setVisibility(GONE);
        }

        // load last color (so that the battery is not white!)
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_COLOR)) {
                currentColor = savedInstanceState.getInt(KEY_COLOR);
                setImageColor(currentColor, img_battery);
            }
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.on_off_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_reset) {
            // reset screen on/off percentages and times in sharedPreferences
            sharedPreferences.edit()
                    .putInt(getString(R.string.value_drain_screen_on), 0)
                    .putInt(getString(R.string.value_drain_screen_off), 0)
                    .putLong(getString(R.string.value_time_screen_on), 0)
                    .putLong(getString(R.string.value_time_screen_off), 0)
                    .apply();
            showNoData();
            ((BaseActivity) getActivity()).showToast(R.string.toast_reset_data_success, LENGTH_SHORT);
        }
        return super.onOptionsItemSelected(item);
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
        getActivity().registerReceiver(batteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        getActivity().unregisterReceiver(batteryChangedReceiver);
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_COLOR, currentColor);
    }

    private void showNoData() {
        textView_screenOn.setText(String.format(Locale.getDefault(), "%s: %s %%/h",
                getString(R.string.screen_on), "N/A"));
        textView_screenOff.setText(String.format(Locale.getDefault(), "%s: %s %%/h",
                getString(R.string.screen_off), "N/A"));
    }
}
