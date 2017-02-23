package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jjoe64.graphview.series.Series;
import com.laudien.p1xelfehler.batterywarner.Activities.BasicGraphFragment;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;

public class GraphFragment extends BasicGraphFragment {

    private static final String TAG = "GraphFragment";
    private boolean graphEnabled;
    private SharedPreferences sharedPreferences;

    public static void notify(Context context) {

    }

    public static void saveGraph(Context context) {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected Series[] getSeries() {
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        return dbHelper.getGraphs(getContext());
    }

    @Override
    protected long getEndDate() {
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        return GraphDbHelper.getEndTime(dbHelper.getReadableDatabase());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.reload_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (!Contract.IS_PRO && id != R.id.menu_open_history && id != R.id.menu_settings) {
            Toast.makeText(getContext(), getString(R.string.pro_only_short), Toast.LENGTH_SHORT).show();
            return false;
        }
        switch (id) {
            case R.id.menu_refresh:
                if (graphEnabled) {
                    reloadChargeCurve();
                    Toast.makeText(getContext(), getString(R.string.graph_reloaded), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), getString(R.string.disabled_in_settings), Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.menu_reset:
                if (sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default))) {
                    showResetDialog();
                }
                break;
            case R.id.menu_open_history:
                openHistory();
                return true;
            case R.id.menu_save_to_history:
                saveGraph();
                return true;
            case R.id.menu_info:
                showInfo();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveGraph() {

    }

    public void reloadChargeCurve() {
        for (Series s : series) {
            graphView.removeSeries(s);
        }
        series = getSeries();
        loadSeries();
    }

    public void showResetDialog() {

    }

    public void openHistory() {

    }
}
