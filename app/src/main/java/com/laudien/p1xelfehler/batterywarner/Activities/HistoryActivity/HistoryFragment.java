package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

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
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.Activities.BaseActivity;
import com.laudien.p1xelfehler.batterywarner.HelperClasses.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.Contract.DATABASE_HISTORY_PATH;

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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_next:
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
                break;
            case R.id.btn_prev:
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        textView_fileName.setText(adapter.getFile(position).getName());
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
                                ((BaseActivity) (getActivity())).showToast(R.string.toast_success_delete_graph, LENGTH_SHORT);
                                if (adapter.getCount() == 0) {
                                    textView_nothingSaved.setVisibility(VISIBLE);
                                    textView_fileName.setText("");
                                } else { // min 1 item is there
                                    textView_fileName.setText(adapter.getFile(currentPosition).getName());
                                }
                                if (adapter.getCount() < 2) {
                                    disableButtons();
                                }
                            } else {
                                ((BaseActivity) (getActivity())).showToast(R.string.toast_error_deleting, LENGTH_SHORT);
                            }
                        }
                    }).setNegativeButton(getString(R.string.dialog_button_cancel), null)
                    .create().show();
        } else {
            ((BaseActivity) (getActivity())).showToast(R.string.toast_no_graphs_saved, LENGTH_SHORT);
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
                                ((BaseActivity) (getActivity())).showToast(R.string.toast_success_delete_all_graphs, LENGTH_SHORT);
                                textView_nothingSaved.setVisibility(VISIBLE);
                                textView_fileName.setText("");
                                disableButtons();
                            } else {
                                ((BaseActivity) (getActivity())).showToast(R.string.toast_error_deleting, LENGTH_SHORT);
                            }
                        }
                    }).setNegativeButton(getString(R.string.dialog_button_cancel), null)
                    .create().show();
        } else {
            ((BaseActivity) (getActivity())).showToast(R.string.toast_no_graphs_saved, LENGTH_SHORT);
        }
    }

    private void showGraphInfo() {
        if (adapter.getCount() != 0) {
            adapter.getCurrentFragment().showInfo();
        } else {
            ((BaseActivity) (getActivity())).showToast(R.string.toast_no_graphs_saved, LENGTH_SHORT);
        }
    }

    private void disableButtons() {
        btn_next.setEnabled(false);
        btn_prev.setEnabled(false);
    }

    private void showRenameDialog() {
        if (adapter.getCount() == 0) {
            ((BaseActivity) (getActivity())).showToast(R.string.toast_no_graphs_saved, LENGTH_SHORT);
            return;
        }
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_rename, null);
        final EditText editText = (EditText) view.findViewById(R.id.editText);
        final String oldName = adapter.getFile(viewPager.getCurrentItem()).getName();
        editText.setText(oldName);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(getString(R.string.menu_rename_graph))
                .setView(view)
                .setNegativeButton(getString(R.string.dialog_button_cancel), null)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String newName = editText.getText().toString();
                        if (!newName.equals(oldName)) {
                            if (newName.contains("/")) {
                                ((BaseActivity) (getActivity())).showToast(R.string.toast_error_renaming_wrong_characters, LENGTH_SHORT);
                            } else {
                                File newFile = new File(DATABASE_HISTORY_PATH + "/" + newName);
                                if (newFile.exists()) {
                                    ((BaseActivity) (getActivity())).showToast(String.format(Locale.getDefault(), "%s '%s'!",
                                            getString(R.string.toast_graph_name_already_exists), newName), LENGTH_SHORT);
                                } else if (adapter.renameFile(viewPager.getCurrentItem(), newFile)) {
                                    textView_fileName.setText(newName);
                                    ((BaseActivity) (getActivity())).showToast(R.string.toast_success_renaming, LENGTH_SHORT);
                                } else {
                                    ((BaseActivity) (getActivity())).showToast(R.string.toast_error_renaming, LENGTH_SHORT);
                                }
                            }
                        }
                    }
                })
                .create();
        Window window = dialog.getWindow();
        if (window != null) { // show keyboard
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.show();
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
                    return -Long.compare(o1.lastModified(), o2.lastModified());
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
        if (fileList.size() < 2) { // disable buttons if not needed
            disableButtons();
        }
        return fileList;
    }
}
