package com.laudien.p1xelfehler.batterywarner.preferences.infoNotificationActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.BaseActivity;
import com.laudien.p1xelfehler.batterywarner.R;

public class InfoNotificationActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_base_layout);
        setToolbarTitle(getString(R.string.title_info_notification_items));
        // replace container layout with InfoNotificationFragment
        getFragmentManager().beginTransaction().replace(R.id.container_layout, new InfoNotificationFragment()).commit();
    }
}
