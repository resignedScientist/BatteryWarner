package com.laudien.p1xelfehler.batterywarner

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Dialog
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast.LENGTH_SHORT
import com.laudien.p1xelfehler.batterywarner.database.DatabaseModel
import com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils
import com.laudien.p1xelfehler.batterywarner.fragments.HistoryPageFragment
import com.laudien.p1xelfehler.batterywarner.fragments.HistoryPageFragmentDataSource
import com.laudien.p1xelfehler.batterywarner.helper.KeyboardHelper
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper
import kotlinx.android.synthetic.main.activity_history.*
import java.io.File
import java.util.*

/**
 * Activity that shows all the charging curves that were saved.
 */
class HistoryActivity : BaseActivity(), ViewPager.OnPageChangeListener {
    private val PERMISSION_REQUEST_CODE = 60
    private val adapter = HistoryPagerAdapter(supportFragmentManager)
    private val handleButtonsHandler = Handler()
    private val handleButtonsRunnable = Runnable {
        val pos = viewPager.currentItem
        if (adapter.count - 1 <= pos) {
            slideOut(btn_next)
        } else {
            slideIn(btn_next)
        }
        if (pos <= 0) {
            slideOut(btn_prev)
        } else {
            slideIn(btn_prev)
        }
    }
    private var lastPage = 0

    companion object {
        val DATABASE_HISTORY_PATH = "${Environment.getExternalStorageDirectory()}/BatteryWarner"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        setToolbarTitle(getString(R.string.title_history))
        btn_next.setOnClickListener {
            viewPager.setCurrentItem(viewPager.currentItem + 1, true)
        }
        btn_prev.setOnClickListener {
            viewPager.setCurrentItem(viewPager.currentItem - 1, true)
        }
        viewPager.offscreenPageLimit = 2
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(this)
        if (savedInstanceState == null) {
            lastPage = -1
        }
        // check for permission
        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
            )
            return
        }
        // permissions are granted at this point
        loadGraphs()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_rename -> showRenameDialog()
            R.id.menu_delete -> showDeleteDialog()
            R.id.menu_delete_all -> showDeleteAllDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        DatabaseModel.getInstance(this).closeAllExternalFiles()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    finish()
                    return
                }
            }
            loadGraphs()
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        val file = adapter.getFile(position)
        textView_fileName.text = file?.name
        if (adapter.count == 0) {
            slideOut(btn_next)
            slideOut(btn_prev)
            return
        }
        handleButtonsHandler.removeCallbacks(handleButtonsRunnable)
        if (position != lastPage) {
            lastPage = position
            handleButtonsRunnable.run()
        } else {
            handleButtonsHandler.postDelayed(handleButtonsRunnable, 1000)
        }
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    private fun loadGraphs() {
        val fileList = DatabaseUtils.getBatteryFiles()
        adapter.loadFiles(fileList)
        if (fileList.isEmpty()) {
            textView_nothingSaved.visibility = VISIBLE
            textView_fileName.visibility = INVISIBLE
        }
    }

    private fun showDeleteDialog() {
        if (adapter.count != 0) {
            AlertDialog.Builder(this).setCancelable(true)
                    .setTitle(R.string.dialog_title_are_you_sure)
                    .setMessage(R.string.dialog_message_delete_graph)
                    .setIcon(R.drawable.ic_battery_status_full_green_48dp)
                    .setPositiveButton(getString(R.string.dialog_button_yes)) { _, _ ->
                        val currentPosition = viewPager.currentItem
                        if (adapter.deleteFile(currentPosition)) {
                            ToastHelper.sendToast(this@HistoryActivity, R.string.toast_success_delete_graph, LENGTH_SHORT)
                            if (adapter.count == 0) {
                                textView_nothingSaved.visibility = VISIBLE
                            }
                        } else {
                            ToastHelper.sendToast(this@HistoryActivity, R.string.toast_error_deleting, LENGTH_SHORT)
                        }
                    }.setNegativeButton(getString(R.string.dialog_button_cancel), null)
                    .create().show()
        } else {
            ToastHelper.sendToast(this, R.string.toast_no_graphs_saved, LENGTH_SHORT)
        }
    }

    private fun showDeleteAllDialog() {
        if (adapter.count != 0) {
            AlertDialog.Builder(this).setCancelable(true)
                    .setTitle(R.string.dialog_title_are_you_sure)
                    .setMessage(R.string.dialog_message_delete_all_graphs)
                    .setIcon(R.drawable.ic_battery_status_full_green_48dp)
                    .setPositiveButton(getString(R.string.dialog_button_yes)) { _, _ ->
                        if (adapter.deleteAllFiles()) {
                            ToastHelper.sendToast(this@HistoryActivity, R.string.toast_success_delete_all_graphs, LENGTH_SHORT)
                            textView_nothingSaved.visibility = VISIBLE
                        } else {
                            ToastHelper.sendToast(this@HistoryActivity, R.string.toast_error_deleting, LENGTH_SHORT)
                        }
                    }.setNegativeButton(getString(R.string.dialog_button_cancel), null)
                    .create().show()
        } else {
            ToastHelper.sendToast(this, R.string.toast_no_graphs_saved, LENGTH_SHORT)
        }
    }

    private fun showRenameDialog() {
        if (adapter.count > 0) {
            val oldName = adapter.getFile(viewPager.currentItem)?.name
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_rename)
            val editText = dialog.findViewById<EditText>(R.id.editText)
            editText.setText(oldName)
            val btn_ok = dialog.findViewById<Button>(R.id.btn_ok)
            btn_ok.setOnClickListener {
                val newName = editText.text.toString()
                if (newName != oldName) {
                    if (newName.contains("/")) {
                        ToastHelper.sendToast(this@HistoryActivity, R.string.toast_error_renaming_wrong_characters, LENGTH_SHORT)
                    } else {
                        val newFile = File(DATABASE_HISTORY_PATH + "/" + newName)
                        if (newFile.exists()) {
                            ToastHelper.sendToast(this@HistoryActivity, String.format(Locale.getDefault(), "%s '%s'!",
                                    getString(R.string.toast_graph_name_already_exists), newName))
                        } else if (adapter.renameFile(viewPager.currentItem, newFile)) {
                            textView_fileName.text = newName
                            ToastHelper.sendToast(this@HistoryActivity, R.string.toast_success_renaming, LENGTH_SHORT)
                        } else {
                            ToastHelper.sendToast(this@HistoryActivity, R.string.toast_error_renaming, LENGTH_SHORT)
                        }
                    }
                }
                dialog.dismiss()
            }
            val btn_cancel = dialog.findViewById<Button>(R.id.btn_cancel)
            btn_cancel.setOnClickListener { dialog.dismiss() }
            dialog.show()
            // show keyboard
            KeyboardHelper.showKeyboard(dialog.window)
        } else { // no graphs saved
            ToastHelper.sendToast(this, R.string.toast_no_graphs_saved, LENGTH_SHORT)
        }
    }

    private fun slideIn(view: View) {
        if (view.visibility != VISIBLE) {
            val animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    view.visibility = VISIBLE
                }

                override fun onAnimationEnd(animation: Animation) {

                }

                override fun onAnimationRepeat(animation: Animation) {

                }
            })
            view.startAnimation(animation)
        }
    }

    private fun slideOut(view: View) {
        if (view.visibility == VISIBLE) {
            val animation = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {

                }

                override fun onAnimationEnd(animation: Animation) {
                    view.visibility = INVISIBLE
                }

                override fun onAnimationRepeat(animation: Animation) {

                }
            })
            view.startAnimation(animation)
        }
    }

    /**
     * A FragmentStatePagerAdapter that loads HistoryPageFragments into the ViewPager.
     */
    private inner class HistoryPagerAdapter (fm: FragmentManager) : FragmentStatePagerAdapter(fm), HistoryPageFragmentDataSource {
        private var files: ArrayList<File>? = null

        override fun getItem(position: Int): Fragment {
            val fragment = HistoryPageFragment()
            fragment.dataSource = this
            fragment.index = position
            return fragment
        }

        override fun getCount(): Int {
            return files?.size ?: 0
        }

        override fun getItemPosition(`object`: Any): Int {
            return PagerAdapter.POSITION_NONE
        }

        override fun notifyDataSetChanged() {
            super.notifyDataSetChanged()
            onPageSelected(viewPager.currentItem)
        }

        override fun isCurrentItem(index: Int): Boolean {
            return index == viewPager.currentItem
        }

        /**
         * Removes the database file of the fragment at the given position.
         *
         * @param position The position of the fragment in the ViewPager.
         * @return Returns true, if the file was successfully removed, false if not.
         */
        internal fun deleteFile(position: Int): Boolean {
            if (position <= count) {
                val file = files?.get(position)
                if (file?.delete() == true) {
                    files?.removeAt(position)
                    notifyDataSetChanged()
                    return true
                }
            }
            return false
        }

        internal fun deleteAllFiles(): Boolean {
            if (count > 0) {
                for (f in files!!) {
                    if (!f.delete()) {
                        loadGraphs()
                        return false
                    }
                }
                files?.clear()
                notifyDataSetChanged()
                return true
            } else {
                return false
            }
        }

        /**
         * Get the database file of the fragment at the given position.
         *
         * @param position The position of the fragment in the ViewPager.
         * @return Returns the database file of the fragment at the given position.
         */
        override fun getFile(position: Int): File? {
            if (count <= position) {
                return null
            }
            return files?.get(position)
        }

        /**
         * Rename the history file of a given position.
         *
         * @param position The position of the fragment in the ViewPager.
         * @param newFile  The new file with the new name to which the file should be renamed.
         * @return Returns true, if the renaming was successful, false if not.
         */
        internal fun renameFile(position: Int, newFile: File): Boolean {
            if (position <= count) {
                val oldFile = files!!.get(position)
                if (oldFile.renameTo(newFile)) {
                    textView_fileName.text = newFile.name
                    files!![position] = newFile
                    return true
                }
            }
            return false
        }

        internal fun loadFiles(files: ArrayList<File>?) {
            this.files = files
            notifyDataSetChanged()
        }
    }
}
