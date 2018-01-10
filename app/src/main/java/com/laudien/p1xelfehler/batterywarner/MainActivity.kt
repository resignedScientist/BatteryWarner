package com.laudien.p1xelfehler.batterywarner

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.laudien.p1xelfehler.batterywarner.appIntro.IntroActivity
import com.laudien.p1xelfehler.batterywarner.fragments.GraphFragment
import com.laudien.p1xelfehler.batterywarner.fragments.MainPageFragment
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {
    private var backPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createNotificationChannels(this)
        }
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val firstStart = sharedPreferences.getBoolean(getString(R.string.pref_first_start), true)
        if (firstStart) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        } else {
            setContentView(R.layout.activity_main)
            setSupportActionBar(toolbar)
            if (viewPager != null) { // phones only
                val adapter = ViewPagerAdapter(supportFragmentManager)
                viewPager?.adapter = adapter
                tab_layout?.setupWithViewPager(viewPager)
            }
        }
        // start services just in case
        ServiceHelper.startService(this)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (backPressed) {
            finishAffinity()
        } else {
            ToastHelper.sendToast(this, R.string.toast_click_to_exit, Toast.LENGTH_SHORT)
            backPressed = true
            Handler().postDelayed(Runnable {
                backPressed = false
            }, 3000)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.menu_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> MainPageFragment()
                1 -> GraphFragment()
                else -> return Fragment()
            }
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> getString(R.string.title_main_page)
                1 -> getString(R.string.title_stats)
                else -> null
            }
        }
    }
}
