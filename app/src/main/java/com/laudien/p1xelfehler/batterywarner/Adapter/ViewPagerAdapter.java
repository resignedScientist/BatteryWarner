package com.laudien.p1xelfehler.batterywarner.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.laudien.p1xelfehler.batterywarner.Fragments.GraphFragment;
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
        switch (position){
            case 0:
                title = "Hauptseite";
                break;
            case 1:
                title = "Statistiken";
                break;
        }
        return title;
    }
}
