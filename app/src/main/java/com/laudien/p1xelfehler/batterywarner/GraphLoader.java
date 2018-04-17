package com.laudien.p1xelfehler.batterywarner;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import com.laudien.p1xelfehler.batterywarner.database.Data;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseContract;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseModel;

import java.io.File;

public class GraphLoader extends AsyncTaskLoader<Data> {
    @Nullable
    private File databaseFile;

    public GraphLoader(Context context, @Nullable File databaseFile) {
        super(context);
        this.databaseFile = databaseFile;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public Data loadInBackground() {
        Context context = getContext();
        DatabaseContract.Model model = DatabaseModel.getInstance(context);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useFahrenheit = sharedPreferences.getString(context.getString(R.string.pref_temp_unit), context.getString(R.string.pref_temp_unit_default)).equals("1");
        boolean reverseCurrent = sharedPreferences.getBoolean(context.getString(R.string.pref_reverse_current), context.getResources().getBoolean(R.bool.pref_reverse_current_default));
        return model.readData(databaseFile, useFahrenheit, reverseCurrent);
    }
}
