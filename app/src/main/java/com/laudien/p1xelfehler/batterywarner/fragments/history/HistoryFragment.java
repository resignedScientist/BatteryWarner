package com.laudien.p1xelfehler.batterywarner.fragments.history;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;
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

/**
 * Fragment that shows the history of the charging curves.
 * It loads each database file in the external storage directory of the app
 * in a separate HistoryPageFragment after checking if each database file in the directory
 * is actually a SQLite database. The HistoryPageFragments are organized in a ViewPager using
 * an instance of the HistoryPagerAdapter.
 */
public class HistoryFragment extends Fragment implements View.OnClickListener, ViewPager.OnPageChangeListener {

    private ImageButton btn_next, btn_prev;
    private ViewPager viewPager;
    private HistoryPagerAdapter adapter;
    private TextView textView_nothingSaved, textView_fileName;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        textView_nothingSaved = (TextView) view.findViewById(R.id.textView_nothingSaved);
        textView_fileName = (TextView) view.findViewById(R.id.textView_fileName);
        btn_next = (ImageButton) view.findViewById(R.id.btn_next);
        btn_next.setOnClickListener(this);
        btn_prev = (ImageButton) view.findViewById(R.id.btn_prev);
        btn_prev.setOnClickListener(this);
        viewPager = (ViewPager) view.findViewById(R.id.viewPager);
        adapter = new HistoryPagerAdapter(getFragmentManager(), readGraphs());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(this);
        onPageSelected(viewPager.getCurrentItem());
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_rename:
                showRenameDialog();
                break;
            case R.id.menu_delete:
                showDeleteDialog();
                break;
            case R.id.menu_delete_all:
                showDeleteAllDialog();
                break;
            case R.id.menu_info:
                showGraphInfo();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(final View v) {
        viewPager.setCurrentItem(viewPager.getCurrentItem() + (v == btn_next ? 1 : -1), true);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.history_menu, menu);
    }

    private void showDeleteDialog() {
        if (adapter.getCount() != 0) {
            new AlertDialog.Builder(getContext()).setCancelable(true)
                    .setTitle(R.string.dialog_title_are_you_sure)
                    .setMessage(R.string.dialog_message_delete_graph)
                    .setIcon(R.mipmap.ic_launcher)
                    .setPositiveButton(getString(R.string.dialog_button_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            int currentPosition = viewPager.getCurrentItem();
                            if (adapter.removeItem(currentPosition)) {
                                ToastHelper.sendToast(getContext(), R.string.toast_success_delete_graph, LENGTH_SHORT);
                                if (adapter.getCount() == 0) {
                                    textView_nothingSaved.setVisibility(VISIBLE);
                                    textView_fileName.setText("");
                                }
                            } else {
                                ToastHelper.sendToast(getContext(), R.string.toast_error_deleting, LENGTH_SHORT);
                            }
                        }
                    }).setNegativeButton(getString(R.string.dialog_button_cancel), null)
                    .create().show();
        } else {
            ToastHelper.sendToast(getContext(), R.string.toast_no_graphs_saved, LENGTH_SHORT);
        }
    }

    private void showDeleteAllDialog() {
        if (adapter.getCount() != 0) {
            new AlertDialog.Builder(getContext()).setCancelable(true)
                    .setTitle(R.string.dialog_title_are_you_sure)
                    .setMessage(R.string.dialog_message_delete_all_graphs)
                    .setIcon(R.mipmap.ic_launcher)
                    .setPositiveButton(getString(R.string.dialog_button_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (adapter.removeAllItems()) {
                                ToastHelper.sendToast(getContext(), R.string.toast_success_delete_all_graphs, LENGTH_SHORT);
                                textView_nothingSaved.setVisibility(VISIBLE);
                                textView_fileName.setText("");
                                onPageSelected(-1);
                            } else {
                                ToastHelper.sendToast(getContext(), R.string.toast_error_deleting, LENGTH_SHORT);
                            }
                        }
                    }).setNegativeButton(getString(R.string.dialog_button_cancel), null)
                    .create().show();
        } else {
            ToastHelper.sendToast(getContext(), R.string.toast_no_graphs_saved, LENGTH_SHORT);
        }
    }

    private void showGraphInfo() {
        if (adapter.getCount() != 0) {
            adapter.getCurrentFragment().showInfo();
        } else {
            ToastHelper.sendToast(getContext(), R.string.toast_no_graphs_saved, LENGTH_SHORT);
        }
    }

    private void showRenameDialog() {
        if (adapter.getCount() > 0) {
            final String oldName = adapter.getFile(viewPager.getCurrentItem()).getName();
            final Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.dialog_rename);
            final EditText editText = (EditText) dialog.findViewById(R.id.editText);
            editText.setText(oldName);
            Button btn_ok = (Button) dialog.findViewById(R.id.btn_ok);
            btn_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newName = editText.getText().toString();
                    if (!newName.equals(oldName)) {
                        if (newName.contains("/")) {
                            ToastHelper.sendToast(getContext(), R.string.toast_error_renaming_wrong_characters, LENGTH_SHORT);
                        } else {
                            File newFile = new File(DATABASE_HISTORY_PATH + "/" + newName);
                            if (newFile.exists()) {
                                ToastHelper.sendToast(getContext(), String.format(Locale.getDefault(), "%s '%s'!",
                                        getString(R.string.toast_graph_name_already_exists), newName), LENGTH_SHORT);
                            } else if (adapter.renameFile(viewPager.getCurrentItem(), newFile)) {
                                textView_fileName.setText(newName);
                                ToastHelper.sendToast(getContext(), R.string.toast_success_renaming, LENGTH_SHORT);
                            } else {
                                ToastHelper.sendToast(getContext(), R.string.toast_error_renaming, LENGTH_SHORT);
                            }
                        }
                    }
                    dialog.dismiss();
                }
            });
            Button btn_cancel = (Button) dialog.findViewById(R.id.btn_cancel);
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
            ToastHelper.sendToast(getContext(), R.string.toast_no_graphs_saved, LENGTH_SHORT);
        }
    }

    private ArrayList<File> readGraphs() {
        File path = new File(DATABASE_HISTORY_PATH);
        File[] files = path.listFiles();
        ArrayList<File> fileList = new ArrayList<>();
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
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

    private void slideIn(final View view) {
        if (view.getVisibility() != VISIBLE) {
            Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_bottom);
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
            Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_bottom);
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
}
