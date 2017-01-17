package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.ViewPagerAdapter;

import java.util.ArrayList;

class HistoryPagerAdapter extends ViewPagerAdapter {
    private ArrayList<Fragment> fragments;

    HistoryPagerAdapter(Context context, FragmentManager fm) {
        super(context, fm);
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
}
