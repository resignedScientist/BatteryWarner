package com.laudien.p1xelfehler.batterywarner.MainActivity;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.laudien.p1xelfehler.batterywarner.R;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    private Context context;

    public ViewPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = new OnOffFragment();
                break;
            case 1:
                fragment = new GraphFragment();
                break;
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title = "";
        switch (position) {
            case 0:
                title = context.getString(R.string.main_page);
                break;
            case 1:
                title = context.getString(R.string.stats);
                break;
        }
        return title;
    }
}
