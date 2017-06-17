package com.laudien.p1xelfehler.batterywarner;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.fragments.HistoryPageFragment;
import com.laudien.p1xelfehler.batterywarner.helper.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.helper.KeyboardHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.AppInfoHelper.DATABASE_HISTORY_PATH;
import static com.laudien.p1xelfehler.batterywarner.fragments.HistoryPageFragment.EXTRA_FILE_PATH;

/**
 * Activity that shows all the charging curves that were saved.
 * It is the frame for the HistoryFragment and only loads the fragment if the external storage
 * permission is given. If not, it asks for the permission and loads the fragment if the
 * user allows it.
 */
public class HistoryActivity extends BaseActivity implements ViewPager.OnPageChangeListener, View.OnClickListener {
    private static final int PERMISSION_REQUEST_CODE = 60;
    private ImageButton btn_next, btn_prev;
    private ViewPager viewPager;
    private HistoryPagerAdapter adapter;
    private TextView textView_nothingSaved, textView_fileName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        setToolbarTitle(getString(R.string.title_history));
        textView_nothingSaved = (TextView) findViewById(R.id.textView_nothingSaved);
        textView_fileName = (TextView) findViewById(R.id.textView_fileName);
        btn_next = (ImageButton) findViewById(R.id.btn_next);
        btn_next.setOnClickListener(this);
        btn_prev = (ImageButton) findViewById(R.id.btn_prev);
        btn_prev.setOnClickListener(this);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        // check for permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE
            );
        } else {
            loadGraph();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_rename:
                showRenameDialog();
                return true;
            case R.id.menu_delete:
                showDeleteDialog();
                return true;
            case R.id.menu_delete_all:
                showDeleteAllDialog();
                return true;
            case R.id.menu_info:
                showGraphInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    finish();
                    return;
                }
            }
            recreate();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        File file = adapter.getFile(position);
        if (file != null) {
            textView_fileName.setText(file.getName());
        }
        if (adapter.getCount() - 1 <= position) {
            slideOut(btn_next);
        } else {
            slideIn(btn_next);
        }
        if (position <= 0) {
            slideOut(btn_prev);
        } else {
            slideIn(btn_prev);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onClick(View v) {
        viewPager.setCurrentItem(viewPager.getCurrentItem() + (v == btn_next ? 1 : -1), true);
    }

    private void loadGraph() {
        adapter = new HistoryPagerAdapter(getSupportFragmentManager(), readGraphs());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(this);
        onPageSelected(viewPager.getCurrentItem());
    }

    private ArrayList<File> readGraphs() {
        File path = new File(DATABASE_HISTORY_PATH);
        File[] files = path.listFiles();
        ArrayList<File> fileList = new ArrayList<>();
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(this);
        if (files != null) { // there are files in the database folder
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (SDK_INT >= KITKAT) {
                        return -Long.compare(o1.lastModified(), o2.lastModified());
                    } else { // before KitKat
                        if (o1.lastModified() == o2.lastModified()) {
                            return 0;
                        }
                        if (o1.lastModified() > o2.lastModified()) {
                            return -1;
                        } else { // o1.lastModified() < o2.lastModified()
                            return 1;
                        }
                    }
                }
            });
            for (File file : files) {
                // check if the file is a valid database file
                if (dbHelper.isValidDatabase(file.getPath())) {
                    fileList.add(file); // add the file path to the fileList
                }
            }
            if (fileList.isEmpty()) { // if no readable graph is in the database folder
                textView_nothingSaved.setVisibility(VISIBLE); // show "nothing saved"
            } else { // min 1 readable graph in the database folder
                textView_fileName.setText(fileList.get(0).getName()); // set the name TextView
            }
        } else { // no files in the database folder
            textView_nothingSaved.setVisibility(VISIBLE); // show "nothing saved"
        }
        return fileList;
    }

    private void showDeleteDialog() {
        if (adapter.getCount() != 0) {
            new AlertDialog.Builder(this).setCancelable(true)
                    .setTitle(R.string.dialog_title_are_you_sure)
                    .setMessage(R.string.dialog_message_delete_graph)
                    .setIcon(R.mipmap.ic_launcher)
                    .setPositiveButton(getString(R.string.dialog_button_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            int currentPosition = viewPager.getCurrentItem();
                            if (adapter.removeItem(currentPosition)) {
                                ToastHelper.sendToast(HistoryActivity.this, R.string.toast_success_delete_graph, LENGTH_SHORT);
                                if (adapter.getCount() == 0) {
                                    textView_nothingSaved.setVisibility(VISIBLE);
                                }
                            } else {
                                ToastHelper.sendToast(HistoryActivity.this, R.string.toast_error_deleting, LENGTH_SHORT);
                            }
                        }
                    }).setNegativeButton(getString(R.string.dialog_button_cancel), null)
                    .create().show();
        } else {
            ToastHelper.sendToast(this, R.string.toast_no_graphs_saved, LENGTH_SHORT);
        }
    }

    private void showDeleteAllDialog() {
        if (adapter.getCount() != 0) {
            new AlertDialog.Builder(this).setCancelable(true)
                    .setTitle(R.string.dialog_title_are_you_sure)
                    .setMessage(R.string.dialog_message_delete_all_graphs)
                    .setIcon(R.mipmap.ic_launcher)
                    .setPositiveButton(getString(R.string.dialog_button_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (adapter.removeAllItems()) {
                                ToastHelper.sendToast(HistoryActivity.this, R.string.toast_success_delete_all_graphs, LENGTH_SHORT);
                                textView_nothingSaved.setVisibility(VISIBLE);
                                onPageSelected(-1);
                            } else {
                                ToastHelper.sendToast(HistoryActivity.this, R.string.toast_error_deleting, LENGTH_SHORT);
                            }
                        }
                    }).setNegativeButton(getString(R.string.dialog_button_cancel), null)
                    .create().show();
        } else {
            ToastHelper.sendToast(this, R.string.toast_no_graphs_saved, LENGTH_SHORT);
        }
    }

    private void showGraphInfo() {
        if (adapter.getCount() != 0) {
            adapter.getCurrentFragment().showInfo();
        } else {
            ToastHelper.sendToast(this, R.string.toast_no_graphs_saved, LENGTH_SHORT);
        }
    }

    private void showRenameDialog() {
        if (adapter.getCount() > 0) {
            final String oldName = adapter.getFile(viewPager.getCurrentItem()).getName();
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_rename);
            final EditText editText = dialog.findViewById(R.id.editText);
            editText.setText(oldName);
            Button btn_ok = dialog.findViewById(R.id.btn_ok);
            btn_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newName = editText.getText().toString();
                    if (!newName.equals(oldName)) {
                        if (newName.contains("/")) {
                            ToastHelper.sendToast(HistoryActivity.this, R.string.toast_error_renaming_wrong_characters, LENGTH_SHORT);
                        } else {
                            File newFile = new File(DATABASE_HISTORY_PATH + "/" + newName);
                            if (newFile.exists()) {
                                ToastHelper.sendToast(HistoryActivity.this, String.format(Locale.getDefault(), "%s '%s'!",
                                        getString(R.string.toast_graph_name_already_exists), newName));
                            } else if (adapter.renameFile(viewPager.getCurrentItem(), newFile)) {
                                textView_fileName.setText(newName);
                                ToastHelper.sendToast(HistoryActivity.this, R.string.toast_success_renaming, LENGTH_SHORT);
                            } else {
                                ToastHelper.sendToast(HistoryActivity.this, R.string.toast_error_renaming, LENGTH_SHORT);
                            }
                        }
                    }
                    dialog.dismiss();
                }
            });
            Button btn_cancel = dialog.findViewById(R.id.btn_cancel);
            btn_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.show();
            // show keyboard
            KeyboardHelper.showKeyboard(dialog.getWindow());
        } else { // no graphs saved
            ToastHelper.sendToast(this, R.string.toast_no_graphs_saved, LENGTH_SHORT);
        }
    }

    private void slideIn(final View view) {
        if (view.getVisibility() != VISIBLE) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    view.setVisibility(VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.startAnimation(animation);
        }
    }

    private void slideOut(final View view) {
        if (view.getVisibility() == VISIBLE) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.startAnimation(animation);
        }
    }

    /**
     * A FragmentStatePagerAdapter that loads HistoryPageFragments into the ViewPager.
     */
    private class HistoryPagerAdapter extends FragmentStatePagerAdapter {
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

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            if (getCount() == 0) {
                textView_fileName.setText("");
            } else {
                textView_fileName.setText(files.get(viewPager.getCurrentItem()).getName());
            }
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
}
