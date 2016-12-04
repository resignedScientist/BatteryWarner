package com.laudien.p1xelfehler.batterywarner.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.Adapter.ViewPagerAdapter;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Fragments.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.R;

import static com.laudien.p1xelfehler.batterywarner.Contract.PREF_FIRST_START;
import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {
    private static final String TAG = "MainActivity";
    private Toolbar toolbar;
    private ViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        final SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        boolean firstStart = sharedPreferences.getBoolean(PREF_FIRST_START, true);

        if (firstStart) {
            startActivity(new Intent(this, IntroActivity.class));
        }

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(this);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            GraphFragment graphFragment = (GraphFragment) viewPagerAdapter.getItem(1);
            graphFragment.reloadChargeCurve();
            if (getSharedPreferences(Contract.SHARED_PREFS, MODE_PRIVATE)
                    .getBoolean(Contract.PREF_GRAPH_ENABLED, true))
                Toast.makeText(getApplicationContext(), getString(R.string.graph_reloaded), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        toolbar.getMenu().clear();
        switch (position) {
            case 1:
                toolbar.inflateMenu(R.menu.reload_menu);
                break;
        }
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
