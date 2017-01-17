package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;

import java.io.File;

public class HistoryFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "HistoryFragment";
    private ViewPager viewPager;
    private HistoryPagerAdapter adapter;
    private TextView textView_nothingSaved;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        viewPager = (ViewPager) view.findViewById(R.id.viewPager);
        adapter = new HistoryPagerAdapter(getContext(), getFragmentManager());
        viewPager.setAdapter(adapter);
        textView_nothingSaved = (TextView) view.findViewById(R.id.textView_nothingSaved);

        readGraphs();

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

    private void readGraphs() {
        // check for permission
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[]{
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    Contract.PERMISSION_STORAGE_WRITE
            );
            return;
        }

        // do the job
        File path = new File(Contract.DATABASE_HISTORY_PATH);
        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                HistoryPageFragment pageFragment = new HistoryPageFragment();
                pageFragment.addGraphsFromFile(file);
                adapter.addItem(pageFragment);
            }
        } else {
            textView_nothingSaved.setVisibility(View.VISIBLE);
        }
    }
}
