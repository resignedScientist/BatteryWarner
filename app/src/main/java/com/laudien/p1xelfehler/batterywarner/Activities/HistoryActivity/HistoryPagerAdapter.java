package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

/**
 * A FragmentStatePagerAdapter that is used by the HistoryFragment to load HistoryPageFragments
 * into a ViewPager.
 */
class HistoryPagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<Fragment> fragments;

    HistoryPagerAdapter(FragmentManager fm) {
        super(fm);
        fragments = new ArrayList<>();
    }

    @Override
    public Fragment getItem(int position) {
        if (fragments.size() < position + 1 || position < 0) {
            return null;
        }
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    /**
     * Adds a fragment to the ViewPager.
     *
     * @param fragment The fragment that should be added.
     */
    void addItem(Fragment fragment) {
        fragments.add(fragment);
        notifyDataSetChanged();
    }

    /**
     * Removes a fragment at the given position in the ViewPager.
     * @param position The position of the fragment that should be removed.
     */
    void removeItem(int position) {
        fragments.remove(position);
        notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        // refresh all fragments when data set changed
        return POSITION_NONE;
    }
}
