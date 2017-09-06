package com.laudien.p1xelfehler.batterywarner;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper;
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;
import com.twofortyfouram.log.Lumberjack;

public class TaskerEditActivity extends AbstractAppCompatPluginActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasker_edit);

        /*
         * To help the user keep context, the title shows the host's name and the subtitle
         * shows the plug-in's name.
         */
        CharSequence callingApplicationLabel = null;
        try {
            callingApplicationLabel =
                    getPackageManager().getApplicationLabel(
                            getPackageManager().getApplicationInfo(getCallingPackage(),
                                    0));
        } catch (final PackageManager.NameNotFoundException e) {
            Lumberjack.e("Calling package couldn't be found%s", e); //$NON-NLS-1$
        }
        if (null != callingApplicationLabel) {
            setTitle(callingApplicationLabel);
        }

        getSupportActionBar().setSubtitle("Neat battery warner plugin");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean isBundleValid(@NonNull Bundle bundle) {
        return TaskerHelper.isBundleValid(bundle);
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull Bundle bundle, @NonNull String s) {

    }

    @Nullable
    @Override
    public Bundle getResultBundle() {
        return null;
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull Bundle bundle) {
        return null;
    }
}
