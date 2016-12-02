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
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receiver.BatteryAlarmReceiver;

public class GraphFragment extends Fragment {
    private static final String TAG = "GraphFragment";
    private LineGraphSeries<DataPoint> series_chargeCurve;
    private TextView textView_chargingTime;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        GraphView graph_chargeCurve = (GraphView) view.findViewById(R.id.graph_chargeCurve);
        Viewport viewport_chargeCurve = graph_chargeCurve.getViewport();
        textView_chargingTime = (TextView) view.findViewById(R.id.textView_chargingTime);

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

        Button btn_refresh = (Button) view.findViewById(R.id.btn_refresh);
        btn_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addChargeCurve();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        addChargeCurve();
    }

    private void addChargeCurve() {
        long time = 0;
        GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String[] columns = {GraphChargeDbHelper.TABLE_COLUMN_TIME, GraphChargeDbHelper.TABLE_COLUMN_PERCENTAGE};
        Cursor cursor = database.query(GraphChargeDbHelper.TABLE_NAME, columns, null, null, null, null,
                "length(" + GraphChargeDbHelper.TABLE_COLUMN_TIME + "), " + GraphChargeDbHelper.TABLE_COLUMN_TIME);

        if (cursor.moveToFirst()) { // if the cursor has data
            do { // while the cursor has data
                time = cursor.getLong(0);
                int percentage = cursor.getInt(1);
                Log.i(TAG, "Data read: time = " + time + "; percentage = " + percentage);
                try {
                    series_chargeCurve.appendData(new DataPoint(time / 60000, percentage), false, 1000);
                } catch (Exception e) {
                    series_chargeCurve.resetData(new DataPoint[]{new DataPoint(time / 60000, percentage)});
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        dbHelper.close();
        if (!BatteryAlarmReceiver.isCharging(getContext())) {
            long minutes = time/60000;
            long seconds = (time - minutes*60000)/1000;
            textView_chargingTime.setText("Ladezeit: " + minutes + "min, " + seconds + "s");
            textView_chargingTime.setVisibility(View.VISIBLE);
        } else {
            textView_chargingTime.setVisibility(View.INVISIBLE);
        }
    }
}
