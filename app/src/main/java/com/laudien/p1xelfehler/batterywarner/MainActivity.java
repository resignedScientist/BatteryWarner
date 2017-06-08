package com.laudien.p1xelfehler.batterywarner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.laudien.p1xelfehler.batterywarner.appIntro.IntroActivity;
import com.laudien.p1xelfehler.batterywarner.fragments.BatteryInfoFragment;
import com.laudien.p1xelfehler.batterywarner.fragments.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.fragments.MainPageFragment;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;
import com.laudien.p1xelfehler.batterywarner.views.BatteryView;

import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.fragments.BatteryInfoFragment.COLOR_HIGH;
import static com.laudien.p1xelfehler.batterywarner.fragments.BatteryInfoFragment.COLOR_LOW;
import static com.laudien.p1xelfehler.batterywarner.fragments.BatteryInfoFragment.COLOR_OK;

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
            } else { // tablets only
                final BatteryView batteryView = (BatteryView) findViewById(R.id.img_battery);
                BatteryInfoFragment batteryInfoFragment = (BatteryInfoFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_battery_info);
                batteryInfoFragment.setOnBatteryColorChangedListener(new BatteryInfoFragment.OnBatteryColorChangedListener() {
                    @Override
                    public void onColorChanged(byte colorID) {
                        switch (colorID) {
                            case COLOR_LOW:
                                batteryView.setColor(getResources().getColor(R.color.colorBatteryLow));
                                break;
                            case COLOR_OK:
                                batteryView.setColor(getResources().getColor(R.color.colorBatteryOk));
                                break;
                            case COLOR_HIGH:
                                batteryView.setColor(getResources().getColor(R.color.colorBatteryHigh));
                                break;
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!backPressed) {
            ToastHelper.sendToast(this, R.string.toast_click_to_exit, LENGTH_SHORT);
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
