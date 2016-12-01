package com.laudien.p1xelfehler.batterywarner.Fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.laudien.p1xelfehler.batterywarner.R;

public class GraphFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        GraphView graph_chargeCurve = (GraphView) view.findViewById(R.id.graph_chargeCurve);
        Viewport viewport_chargeCurve = graph_chargeCurve.getViewport();

        viewport_chargeCurve.setYAxisBoundsManual(true);
        viewport_chargeCurve.setMinY(0);
        viewport_chargeCurve.setMaxY(100);

        graph_chargeCurve.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX)
                    return super.formatLabel(value, isValueX) + "min";
                else
                    return super.formatLabel(value, isValueX) + "%";
            }
        });

        return view;
    }
}
