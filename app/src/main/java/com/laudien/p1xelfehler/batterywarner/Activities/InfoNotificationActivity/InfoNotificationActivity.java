package com.laudien.p1xelfehler.batterywarner.Activities.InfoNotificationActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.Activities.BaseActivity;
import com.laudien.p1xelfehler.batterywarner.R;

public class InfoNotificationActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.frame_layout);
        setToolbarTitle(getString(R.string.shown_in_notification));
        // replace container layout with InfoNotificationFragment
        getFragmentManager().beginTransaction().replace(R.id.container_layout, new InfoNotificationFragment()).commit();
    }
}
