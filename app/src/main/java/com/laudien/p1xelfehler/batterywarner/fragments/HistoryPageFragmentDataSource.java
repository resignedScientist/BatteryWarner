package com.laudien.p1xelfehler.batterywarner.fragments;

import android.support.annotation.Nullable;

import java.io.File;

public interface HistoryPageFragmentDataSource {
    @Nullable
    File getFile(int index);

    boolean isCurrentItem(int index);
}
