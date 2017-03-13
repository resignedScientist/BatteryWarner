package com.laudien.p1xelfehler.batterywarner.Activities.SmartChargingActivity;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;

public class SmartChargingPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.smart_charging);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.smart_charging_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_info){
            openInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openInfoDialog() {
        Context context = getActivity();
        if (context != null) {
            new AlertDialog.Builder(context)
                    .setTitle("Information")
                    .setView(R.layout.fragment_smart_charging)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, null)
                    .setIcon(R.mipmap.ic_launcher)
                    .create()
                    .show();
        }
    }
}
