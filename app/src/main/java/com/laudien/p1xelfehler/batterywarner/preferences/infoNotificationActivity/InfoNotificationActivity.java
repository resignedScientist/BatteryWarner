package com.laudien.p1xelfehler.batterywarner.preferences.infoNotificationActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.laudien.p1xelfehler.batterywarner.BaseActivity;
import com.laudien.p1xelfehler.batterywarner.MainActivity;
import com.laudien.p1xelfehler.batterywarner.R;

public class InfoNotificationActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_base_layout);
        setToolbarTitle(getString(R.string.title_info_notification_items));
        // replace container layout with InfoNotificationFragment
        getFragmentManager().beginTransaction().replace(R.id.container_layout, new InfoNotificationFragment()).commit();
    }

    @Override
    public void onBackPressed() {
        if (isTaskRoot()) {
            backStackBack();
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && isTaskRoot()) {
            backStackBack();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method will be executed if you click back in navigation keys or toolbar.
     * It starts the main activity.
     */
    private void backStackBack() {
        startActivity(new Intent(this, MainActivity.class));
    }
}
