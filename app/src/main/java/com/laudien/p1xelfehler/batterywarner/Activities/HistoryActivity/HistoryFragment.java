package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.laudien.p1xelfehler.batterywarner.R;

public class HistoryFragment extends Fragment implements View.OnClickListener {
    private ViewPager viewPager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        viewPager = (ViewPager) view.findViewById(R.id.viewPager);
        HistoryPagerAdapter adapter = new HistoryPagerAdapter(getContext(), getFragmentManager());
        viewPager.setAdapter(adapter);

        adapter.addItem(new HistoryPageFragment());
        adapter.addItem(new HistoryPageFragment());
        adapter.addItem(new HistoryPageFragment());
        adapter.addItem(new HistoryPageFragment());
        adapter.addItem(new HistoryPageFragment());

        ImageButton btn_next = (ImageButton) view.findViewById(R.id.btn_next);
        btn_next.setOnClickListener(this);
        ImageButton btn_prev = (ImageButton) view.findViewById(R.id.btn_prev);
        btn_prev.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        int currentPosition = viewPager.getCurrentItem();
        switch (v.getId()) {
            case R.id.btn_next:
                viewPager.setCurrentItem(currentPosition + 1, true);
                break;
            case R.id.btn_prev:
                viewPager.setCurrentItem(currentPosition - 1, true);
                break;
        }
    }
}
