package com.laudien.p1xelfehler.batterywarner.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receiver.BatteryAlarmReceiver;

public class GraphFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "GraphFragment";
    private SharedPreferences sharedPreferences;
    private GraphView graph_chargeCurve;
    private LineGraphSeries<DataPoint> series_chargeCurve;
    private Viewport viewport_chargeCurve;
    private TextView textView_chargingTime;
    Button btn_refresh;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        sharedPreferences = getContext().getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        graph_chargeCurve = (GraphView) view.findViewById(R.id.graph_chargeCurve);
        viewport_chargeCurve = graph_chargeCurve.getViewport();
        textView_chargingTime = (TextView) view.findViewById(R.id.textView_chargingTime);

        // y bounds
        viewport_chargeCurve.setYAxisBoundsManual(true);
        viewport_chargeCurve.setMinY(0);
        viewport_chargeCurve.setMaxY(100);

        // x bounds
        viewport_chargeCurve.setXAxisBoundsManual(true);
        viewport_chargeCurve.setMinX(0);
        viewport_chargeCurve.setMaxX(1);

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

        btn_refresh = (Button) view.findViewById(R.id.btn_refresh);
        btn_refresh.setOnClickListener(this);

        CheckBox checkBox_chargeCurve = (CheckBox) view.findViewById(R.id.checkBox_chargeCurve);
        checkBox_chargeCurve.setOnCheckedChangeListener(this);
        checkBox_chargeCurve.setChecked(sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        addChargeCurve();
    }

    private void addChargeCurve() {
        if (!sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true)) return;
        long time = 0;
        int percentage = 0;
        GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String[] columns = {GraphChargeDbHelper.TABLE_COLUMN_TIME, GraphChargeDbHelper.TABLE_COLUMN_PERCENTAGE};
        Cursor cursor = database.query(GraphChargeDbHelper.TABLE_NAME, columns, null, null, null, null,
                "length(" + GraphChargeDbHelper.TABLE_COLUMN_TIME + "), " + GraphChargeDbHelper.TABLE_COLUMN_TIME);

        if (cursor.moveToFirst()) { // if the cursor has data
            do { // while the cursor has data
                time = cursor.getLong(0);
                percentage = cursor.getInt(1);
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
        if (!BatteryAlarmReceiver.isCharging(getContext()) || percentage == 100) {
            String timeString = "Ladezeit: ";
            if (time > 3600000) {
                long hours = time / 3600000;
                timeString += hours + " h, ";
            }
            long minutes = time / 60000;
            textView_chargingTime.setText(timeString + minutes + " min");
            textView_chargingTime.setVisibility(View.VISIBLE);
        } else {
            textView_chargingTime.setVisibility(View.INVISIBLE);
        }
        if (time == 0)
            viewport_chargeCurve.setMaxX(1);
        else
            viewport_chargeCurve.setMaxX(time / 60000);
    }

    @Override
    public void onClick(View view) {
        addChargeCurve();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        sharedPreferences.edit().putBoolean(Contract.PREF_GRAPH_ENABLED, b).apply();
        btn_refresh.setEnabled(b);
        if (b) {
            graph_chargeCurve.addSeries(series_chargeCurve);
            addChargeCurve();
        } else {
            graph_chargeCurve.removeAllSeries();
            viewport_chargeCurve.setMaxX(1);
            textView_chargingTime.setVisibility(View.INVISIBLE);
        }
    }
}
