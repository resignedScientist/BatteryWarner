package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.laudien.p1xelfehler.batterywarner.R;

/**
 * A FragmentPagerAdapter that is used by the MainActivity with the ViewPager.
 */
class ViewPagerAdapter extends FragmentPagerAdapter {

    private Context context;

    ViewPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
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
