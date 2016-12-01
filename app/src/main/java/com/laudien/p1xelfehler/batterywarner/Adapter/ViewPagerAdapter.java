package com.laudien.p1xelfehler.batterywarner.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.laudien.p1xelfehler.batterywarner.Fragments.OnOffFragment;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    public ViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        switch (position){
            case 0:
                fragment = new OnOffFragment();
                break;
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return 1;
    }
}
