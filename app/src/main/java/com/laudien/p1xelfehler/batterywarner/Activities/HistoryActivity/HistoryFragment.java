package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.laudien.p1xelfehler.batterywarner.R;

public class HistoryFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewPager);
        HistoryPagerAdapter adapter = new HistoryPagerAdapter(getContext(), getFragmentManager());
        viewPager.setAdapter(adapter);

        adapter.addItem(new HistoryPageFragment());
        adapter.addItem(new HistoryPageFragment());
        return view;
    }
}
