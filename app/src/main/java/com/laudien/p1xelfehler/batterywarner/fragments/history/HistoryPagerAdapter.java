package com.laudien.p1xelfehler.batterywarner.fragments.history;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.io.File;
import java.util.ArrayList;

import static com.laudien.p1xelfehler.batterywarner.fragments.history.HistoryPageFragment.EXTRA_FILE_PATH;

/**
 * A FragmentStatePagerAdapter that is used by the HistoryFragment to load HistoryPageFragments
 * into a ViewPager.
 */
class HistoryPagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<File> files;
    private HistoryPageFragment currentFragment; // the current item

    HistoryPagerAdapter(FragmentManager fm, ArrayList<File> files) {
        super(fm);
        this.files = files;
    }

    @Override
    public Fragment getItem(int position) {
        HistoryPageFragment fragment = new HistoryPageFragment();
        Bundle bundle = new Bundle(1);
        bundle.putString(EXTRA_FILE_PATH, files.get(position).getPath());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (currentFragment != object) {
            currentFragment = (HistoryPageFragment) object;
        }
        super.setPrimaryItem(container, position, object);
    }

    /**
     * Returns the fragment that is currently shown in the foreground.
     *
     * @return Returns the fragment that is currently shown in the foreground.
     */
    HistoryPageFragment getCurrentFragment() {
        return currentFragment;
    }

    /**
     * Removes the database file of the fragment at the given position.
     *
     * @param position The position of the fragment in the ViewPager.
     * @return Returns true, if the file was successfully removed, false if not.
     */
    boolean removeItem(int position) {
        if (position < files.size() && position >= 0) {
            File file = files.get(position);
            if (file.delete()) {
                files.remove(file);
                notifyDataSetChanged();
                return true;
            }
        }
        return false;
    }

    boolean removeAllItems() {
        if (!files.isEmpty()) {
            for (File f : files) {
                if (!f.delete()) {
                    return false;
                }
            }
            files = new ArrayList<>();
            notifyDataSetChanged();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the database file of the fragment at the given position.
     *
     * @param position The position of the fragment in the ViewPager.
     * @return Returns the database file of the fragment at the given position.
     */
    File getFile(int position) {
        if (!files.isEmpty()) {
            return files.get(position);
        } else {
            return null;
        }
    }

    /**
     * Rename the history file of a given position.
     *
     * @param position The position of the fragment in the ViewPager.
     * @param newFile  The new file with the new name to which the file should be renamed.
     * @return Returns true, if the renaming was successful, false if not.
     */
    boolean renameFile(int position, File newFile) {
        if (position < files.size() && position >= 0) {
            File oldFile = files.get(position);
            if (oldFile.renameTo(newFile)) {
                files.set(position, newFile);
                notifyDataSetChanged();
                return true;
            }
        }
        return false;
    }
}
