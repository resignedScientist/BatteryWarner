package com.example.laudien.batterywarner.Activities;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.laudien.batterywarner.Contract;
import com.example.laudien.batterywarner.Fragments.SettingsFragment;
import com.example.laudien.batterywarner.R;

public class SettingsActivity extends AppCompatActivity {
    private SettingsFragment settingsFragment;
    Uri sound;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.settings));
        setSupportActionBar(toolbar);

        settingsFragment = new SettingsFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.container_layout, settingsFragment).commit();
        sound = SettingsFragment.getNotificationSound(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.settings));
        }
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_done:
                settingsFragment.saveAll();
                getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE).edit()
                        .putString(Contract.PREF_SOUND_URI, sound.toString())
                        .apply();
                Toast.makeText(getApplicationContext(), getString(R.string.settings_saved), Toast.LENGTH_SHORT).show();
                finish(); // close the settings
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        switch (requestCode) {
            case Contract.PICK_SOUND_REQUEST: // notification sound picker
                sound = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        }
    }
}
