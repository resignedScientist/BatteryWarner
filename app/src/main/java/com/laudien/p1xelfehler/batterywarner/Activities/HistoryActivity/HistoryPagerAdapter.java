package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

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

    void addItem(Fragment fragment) {
        fragments.add(fragment);
        notifyDataSetChanged();
    }

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
