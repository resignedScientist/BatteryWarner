package com.laudien.p1xelfehler.batterywarner;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;

public class FragmentUtilActivity extends AppCompatActivity {
    public int layoutId = 1337;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout view = new LinearLayout(this);
        view.setId(layoutId);

        setContentView(view);
    }
}