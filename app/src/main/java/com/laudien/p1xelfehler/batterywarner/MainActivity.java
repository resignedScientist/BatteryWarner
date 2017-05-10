package com.laudien.p1xelfehler.batterywarner;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;

import com.laudien.p1xelfehler.batterywarner.appIntro.IntroActivity;
import com.laudien.p1xelfehler.batterywarner.fragments.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.fragments.MainPageFragment;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * The main activity that is shown to the user after opening the app if the intro is already finished.
 * It also checks if both apps are installed and sends a broadcast or disables app functionality
 * depending on if this is the pro version or not. It tells the user to uninstall the free version.
 */
public class MainActivity extends BaseActivity {
    private boolean backPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstStart = sharedPreferences.getBoolean(getString(R.string.pref_first_start), getResources().getBoolean(R.bool.pref_first_start_default));
        if (firstStart) {
            startActivity(new Intent(this, IntroActivity.class));
            finish();
        } else {
            setContentView(R.layout.activity_main);
            setToolbarTitle();
            ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
            if (viewPager != null) { // phones only
                ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
                viewPager.setAdapter(viewPagerAdapter);
                TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
                tabLayout.setupWithViewPager(viewPager);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAppInstalled();
    }

    @Override
    public void onBackPressed() {
        if (!backPressed) {
            showToast(R.string.toast_click_to_exit, LENGTH_SHORT);
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

    /**
     * Checks if 2 versions (free + pro) are installed and tells the user to uninstall the free one.
     */
    private void isAppInstalled() {
        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo(AppInfoHelper.PACKAGE_NAME_FREE, PackageManager.GET_ACTIVITIES);
            packageManager.getPackageInfo(AppInfoHelper.PACKAGE_NAME_PRO, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) { // one of the apps is not installed
            return;
        }
        // both apps are installed:
        // disable the free application
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!AppInfoHelper.IS_PRO) {
            sharedPreferences.edit().putBoolean(getString(R.string.pref_is_enabled), false).apply();
        } else {
            sendBroadcast(new Intent().setPackage(AppInfoHelper.PACKAGE_NAME_FREE).setClassName(AppInfoHelper.PACKAGE_NAME_FREE, "BothAppsInstalledReceiver"));
        }
        // show the dialog
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.uninstall_title)
                .setMessage(getString(R.string.uninstall_text))
                .setNegativeButton(getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setPositiveButton(getString(R.string.uninstall_go), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Uri uri = Uri.parse("package:" + AppInfoHelper.PACKAGE_NAME_FREE);
                        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
                        startActivity(uninstallIntent);
                    }
                })
                .setIcon(R.mipmap.ic_launcher)
                .create()
                .show();
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

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new MainPageFragment();
                case 1:
                    return new GraphFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.title_main_page);
                case 1:
                    return getString(R.string.title_stats);
                default:
                    return null;
            }
        }
    }
}
