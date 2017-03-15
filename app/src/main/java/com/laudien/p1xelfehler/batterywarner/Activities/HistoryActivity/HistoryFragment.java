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
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity.HistoryPageFragment.EXTRA_FILE_PATH;

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
            case R.id.menu_info:
                if (adapter.getCount() != 0) {
                    adapter.getCurrentFragment().showInfo();
                } else {
                    Toast.makeText(getContext(), R.string.no_graphs_saved, Toast.LENGTH_SHORT).show();
                }
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
        final int currentPosition = viewPager.getCurrentItem();
        if (adapter.getCount() == 0) {
            Toast.makeText(getContext(), getString(R.string.no_graphs_saved), Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(getContext()).setCancelable(true)
                .setTitle(getString(R.string.are_you_sure))
                .setMessage(getString(R.string.question_delete_graph))
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        adapter.removeItem(currentPosition);
                        if (adapter.getCount() == 0) {
                            viewPager.setVisibility(View.INVISIBLE);
                            textView_nothingSaved.setVisibility(View.VISIBLE);
                            textView_fileName.setText("");
                        } else { // min 1 item is there
                            textView_fileName.setText(adapter.getFile(currentPosition).getName());
                        }
                        if (adapter.getCount() < 2) {
                            disableButtons();
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel), null)
                .create().show();
    }

    private void disableButtons() {
        btn_next.setEnabled(false);
        btn_prev.setEnabled(false);
    }

    private void showRenameDialog() {
        if (adapter.getCount() == 0) {
            Toast.makeText(getContext(), getString(R.string.no_graphs_saved), Toast.LENGTH_SHORT).show();
            return;
        }
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_rename, null);
        final EditText editText = (EditText) view.findViewById(R.id.editText);
        final String oldName = adapter.getFile(viewPager.getCurrentItem()).getName();
        editText.setText(oldName);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(getString(R.string.rename_graph))
                .setView(view)
                .setNegativeButton(getString(R.string.cancel), null)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String newName = editText.getText().toString();
                        if (!newName.equals(oldName)) {
                            if (newName.contains("/")) {
                                Toast.makeText(getContext(), R.string.unallowed_character_renaming, Toast.LENGTH_SHORT).show();
                            } else {
                                File newFile = new File(newName);
                                if (newFile.exists()) {
                                    Toast.makeText(getContext(), "There already is a graph named '" + newName + "'!", Toast.LENGTH_SHORT).show();
                                } else if (adapter.renameFile(viewPager.getCurrentItem(), new File(newName))) {
                                    textView_fileName.setText(newName);
                                    Toast.makeText(getContext(), R.string.success_renaming, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), R.string.error_renaming, Toast.LENGTH_SHORT).show();
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
        File path = new File(Contract.DATABASE_HISTORY_PATH);
        File[] files = path.listFiles();
        ArrayList<File> fileList = new ArrayList<>();
        int goodFiles = 0;
        int firstGoodFile = 0;
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return -Long.compare(o1.lastModified(), o2.lastModified());
                }
            });
            for (File file : files) {
                // check if the file is a valid database file
                if (!dbHelper.isValidDatabase(file.getPath())) {
                    if (file == files[firstGoodFile]) {
                        firstGoodFile++;
                    }
                    continue;
                }
                goodFiles++;
                HistoryPageFragment pageFragment = new HistoryPageFragment();
                Bundle bundle = new Bundle();
                bundle.putString(EXTRA_FILE_PATH, file.getPath());
                pageFragment.setArguments(bundle);
                fileList.add(file);
            }
            if (goodFiles == 0) {
                textView_nothingSaved.setVisibility(View.VISIBLE);
            } else {
                textView_fileName.setText(files[firstGoodFile].getName());
            }
        } else {
            textView_nothingSaved.setVisibility(View.VISIBLE);
        }
        if (fileList.size() < 2) {
            disableButtons();
        }
        return fileList;
    }
}
