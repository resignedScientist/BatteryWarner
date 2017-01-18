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

public class HistoryFragment extends Fragment implements View.OnClickListener, ViewPager.OnPageChangeListener {
    private static final String TAG = "HistoryFragment";
    ImageButton btn_next, btn_prev;
    private ViewPager viewPager;
    private HistoryPagerAdapter adapter;
    private TextView textView_nothingSaved, textView_fileName;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        viewPager = (ViewPager) view.findViewById(R.id.viewPager);
        adapter = new HistoryPagerAdapter(getFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(this);
        textView_nothingSaved = (TextView) view.findViewById(R.id.textView_nothingSaved);
        textView_fileName = (TextView) view.findViewById(R.id.textView_fileName);
        btn_next = (ImageButton) view.findViewById(R.id.btn_next);
        btn_next.setOnClickListener(this);
        btn_prev = (ImageButton) view.findViewById(R.id.btn_prev);
        btn_prev.setOnClickListener(this);

        readGraphs();

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
                HistoryPageFragment fragment = (HistoryPageFragment) adapter.getItem(viewPager.getCurrentItem());
                if (fragment != null) {
                    fragment.showInfo();
                } else {
                    Toast.makeText(getContext(), getString(R.string.no_graphs_saved), Toast.LENGTH_SHORT).show();
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
        HistoryPageFragment currentFragment = (HistoryPageFragment) adapter.getItem(position);
        textView_fileName.setText(currentFragment.getFileName());
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
        final HistoryPageFragment fragment = (HistoryPageFragment) adapter.getItem(currentPosition);
        if (fragment == null) {
            Toast.makeText(getContext(), getString(R.string.no_graphs_saved), Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(getContext()).setCancelable(true)
                .setTitle("Are you sure?")
                .setMessage("Do you really want to delete that graph?")
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (!fragment.deleteFile()) {
                            return;
                        }
                        adapter.removeItem(currentPosition);
                        if (adapter.getCount() == 0) {
                            viewPager.setVisibility(View.INVISIBLE);
                            textView_nothingSaved.setVisibility(View.VISIBLE);
                            textView_fileName.setText("");
                        } else { // min 1 item is there
                            HistoryPageFragment newFragment = (HistoryPageFragment) adapter.getItem(viewPager.getCurrentItem());
                            textView_fileName.setText(newFragment.getFileName());
                        }
                        if (adapter.getCount() < 2) {
                            btn_next.setEnabled(false);
                            btn_prev.setEnabled(false);
                        }
                    }
                }).setNegativeButton("Cancel", null)
                .create().show();
    }

    private void showRenameDialog() {
        final HistoryPageFragment fragment = (HistoryPageFragment) adapter.getItem(viewPager.getCurrentItem());
        if (fragment == null) {
            Toast.makeText(getContext(), getString(R.string.no_graphs_saved), Toast.LENGTH_SHORT).show();
            return;
        }
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_rename, null);
        final EditText editText = (EditText) view.findViewById(R.id.editText);
        editText.setText(fragment.getFileName());
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(getString(R.string.rename_graph))
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String newName = editText.getText().toString();
                        if (!fragment.renameFile(newName)) {
                            return;
                        }
                        textView_fileName.setText(fragment.getFileName());
                    }
                })
                .create();
        Window window = dialog.getWindow();
        if (window != null) { // show keyboard
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.show();
    }

    private void readGraphs() {
        // do the job
        File path = new File(Contract.DATABASE_HISTORY_PATH);
        File[] files = path.listFiles();
        int goodFiles = 0;
        int firstGoodFile = 0;
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        if (files != null) {
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
                pageFragment.addGraphsFromFile(file);
                adapter.addItem(pageFragment);
            }
            if (goodFiles == 0) {
                textView_nothingSaved.setVisibility(View.VISIBLE);
            } else {
                textView_fileName.setText(files[firstGoodFile].getName());
            }
        } else {
            textView_nothingSaved.setVisibility(View.VISIBLE);
        }
        if (adapter.getCount() < 2) {
            btn_next.setEnabled(false);
            btn_prev.setEnabled(false);
        }
    }
}
