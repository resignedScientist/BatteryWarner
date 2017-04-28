package com.laudien.p1xelfehler.batterywarner.Preferences.SmartChargingActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.BaseActivity;
import com.laudien.p1xelfehler.batterywarner.R;

public class SmartChargingActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.frame_layout);
        setToolbarTitle(getString(R.string.title_smart_charging));
        // replace container layout with SmartChargingFragment
        getFragmentManager().beginTransaction().replace(R.id.container_layout, new SmartChargingFragment()).commit();
    }
}
