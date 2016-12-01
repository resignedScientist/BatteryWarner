package com.laudien.p1xelfehler.batterywarner.Fragments;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import java.util.ArrayList;
import java.util.List;

public class GraphFragment extends Fragment {
    private static final String TAG = "GraphFragment";
    private LineGraphSeries<DataPoint> series_chargeCurve;
    private GraphView graph_chargeCurve;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        graph_chargeCurve = (GraphView) view.findViewById(R.id.graph_chargeCurve);
        Viewport viewport_chargeCurve = graph_chargeCurve.getViewport();

        viewport_chargeCurve.setYAxisBoundsManual(true);
        viewport_chargeCurve.setMinY(0);
        viewport_chargeCurve.setMaxY(100);

        graph_chargeCurve.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX)
                    return super.formatLabel(value, true) + "min";
                else
                    return super.formatLabel(value, false) + "%";
            }
        });

        series_chargeCurve = new LineGraphSeries<>();
        series_chargeCurve.setDrawBackground(true);
        graph_chargeCurve.addSeries(series_chargeCurve);
        addChargeCurve();

        Button btn_refresh = (Button) view.findViewById(R.id.btn_refresh);
        btn_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addChargeCurve();
            }
        });

        return view;
    }

    private void addChargeCurve(){
        GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String[] columns = {GraphChargeDbHelper.TABLE_COLUMN_TIME, GraphChargeDbHelper.TABLE_COLUMN_PERCENTAGE};
        Cursor cursor = database.query(GraphChargeDbHelper.TABLE_NAME, columns, null, null, null, null,
                "length(" + GraphChargeDbHelper.TABLE_COLUMN_TIME + "), " + GraphChargeDbHelper.TABLE_COLUMN_TIME);

        if(cursor.moveToFirst()){
            do{ // while the cursor has data
                int time = cursor.getInt(0);
                int percentage = cursor.getInt(1);
                Log.i(TAG, "Data read: time = " + time + "; percentage = " + percentage);
                try {
                    series_chargeCurve.appendData(new DataPoint(time, percentage), false, 1000);
                }catch (Exception e){
                    series_chargeCurve.resetData(new DataPoint[]{new DataPoint(time, percentage)});
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
    }
}
