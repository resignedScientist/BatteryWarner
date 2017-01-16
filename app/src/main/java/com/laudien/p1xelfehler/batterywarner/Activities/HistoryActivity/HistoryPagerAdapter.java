package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.ViewPagerAdapter;

import java.util.ArrayList;

public class HistoryPagerAdapter extends ViewPagerAdapter {
    ArrayList<Fragment> fragments;

    public HistoryPagerAdapter(Context context, FragmentManager fm) {
        super(context, fm);
        fragments = new ArrayList<>();
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    public void addItem(Fragment fragment) {
        fragments.add(fragment);
        notifyDataSetChanged();
    }
}
