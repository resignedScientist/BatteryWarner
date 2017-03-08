package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.Activities.BaseActivity;
import com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity.SettingsActivity;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;

/**
 * The main activity that is shown to the user after opening the app (if he/she has finished the intro).
 */
public class MainActivity extends BaseActivity {
    private boolean backPressed = false;
    private int clickCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setToolbarTitle();

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAppInstalled();
    }

    @Override
    public void onBackPressed() {
        if (!backPressed) {
            Toast.makeText(getApplicationContext(), getString(R.string.click_to_exit), Toast.LENGTH_SHORT).show();
            backPressed = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    backPressed = false;
                }
            }, 3000);
        } else {
            finishAffinity();
        }
    }

    private void isAppInstalled() {
        // checks if 2 versions (free + pro) are installed and tells you that you have to uninstall the free one
        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo(Contract.PACKAGE_NAME_FREE, PackageManager.GET_ACTIVITIES);
            packageManager.getPackageInfo(Contract.PACKAGE_NAME_PRO, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            return; // one of the app is not installed
        }
        // both apps are installed:
        // check for alex mode:
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(getString(R.string.pref_alex_mode), getResources().getBoolean(R.bool.pref_alex_mode_default))) {
            return;
        }
        // disable the free application
        if (!Contract.IS_PRO) {
            sharedPreferences.edit().putBoolean(getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default)).apply();
        } else {
            sendBroadcast(new Intent(Contract.BROADCAST_BOTH_APPS_INSTALLED));
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView title = new TextView(this);
        float scale = getResources().getDisplayMetrics().density;
        int dp = (int) (scale + 0.5f);
        title.setPadding(24 * dp, 8 * dp, 16 * dp, 0);
        title.setText(getString(R.string.uninstall_title));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        final AlertDialog dialog = builder.setCancelable(false)
                .setCustomTitle(title)
                .setMessage(getString(R.string.uninstall_text))
                .setNegativeButton(getString(R.string.uninstall_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setPositiveButton(getString(R.string.uninstall_go), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Uri uri = Uri.parse("package:" + Contract.PACKAGE_NAME_FREE);
                        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
                        startActivity(uninstallIntent);
                    }
                })
                .setIcon(R.mipmap.ic_launcher)
                .create();
        dialog.show();
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (clickCounter < 4) {
                    clickCounter++;
                } else {
                    Toast.makeText(getApplicationContext(), "Hallo Alex, du Cheater!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    sharedPreferences
                            .edit()
                            .putBoolean(getString(R.string.pref_alex_mode), true)
                            .apply();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
