package com.laudien.p1xelfehler.batterywarner.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.views.BatteryView;

import static com.laudien.p1xelfehler.batterywarner.fragments.BatteryInfoFragment.COLOR_HIGH;
import static com.laudien.p1xelfehler.batterywarner.fragments.BatteryInfoFragment.COLOR_LOW;
import static com.laudien.p1xelfehler.batterywarner.fragments.BatteryInfoFragment.COLOR_OK;

/**
 * Fragment that shows some information about the current battery status. Refreshes automatically.
 * Contains a button for toggling all warnings or logging of the app.
 */
public class MainPageFragment extends Fragment {

    BatteryInfoFragment infoFragment;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean dischargingServiceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
        setHasOptionsMenu(dischargingServiceEnabled);
        View view = inflater.inflate(R.layout.fragment_main_page, container, false);
        final BatteryView img_battery = (BatteryView) view.findViewById(R.id.img_battery);
        infoFragment = (BatteryInfoFragment) getChildFragmentManager().findFragmentById(R.id.fragment_battery_info);
        infoFragment.setOnBatteryColorChangedListener(new BatteryInfoFragment.OnBatteryColorChangedListener() {
            @Override
            public void onColorChanged(byte colorID) {
                switch (colorID) {
                    case COLOR_LOW:
                        img_battery.setColor(getContext().getResources().getColor(R.color.colorBatteryLow));
                        break;
                    case COLOR_OK:
                        img_battery.setColor(getContext().getResources().getColor(R.color.colorBatteryOk));
                        break;
                    case COLOR_HIGH:
                        img_battery.setColor(getContext().getResources().getColor(R.color.colorBatteryHigh));
                        break;
                }
            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.on_off_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_reset){
            infoFragment.resetDischargingStats();
        }
        return super.onOptionsItemSelected(item);
    }
}
