package com.laudien.p1xelfehler.batterywarner.Activities;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;

public class HistoryActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setToolbarTitle("History");
    }
}
