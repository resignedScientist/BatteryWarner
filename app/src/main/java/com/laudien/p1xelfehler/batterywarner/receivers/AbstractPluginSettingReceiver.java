package com.laudien.p1xelfehler.batterywarner.receivers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.twofortyfouram.log.Lumberjack;
import com.twofortyfouram.spackle.AndroidSdkVersion;
import com.twofortyfouram.spackle.ThreadUtil;
import com.twofortyfouram.spackle.bundle.BundleScrubber;

import net.jcip.annotations.ThreadSafe;

import static com.twofortyfouram.assertion.Assertions.assertInRangeInclusive;
import static com.twofortyfouram.assertion.Assertions.assertNotNull;

// copied and slightly changed from: https://github.com/twofortyfouram/android-plugin-client-sdk-for-locale
@ThreadSafe
public abstract class AbstractPluginSettingReceiver extends BroadcastReceiver {

    /*
     * The multiple return statements in this method are a little gross, but the
     * alternative of nested if statements is even worse :/
     */
    @Override
    public final void onReceive(final Context context, final Intent intent) {
        if (BundleScrubber.scrub(intent)) {
            return;
        }
        Lumberjack.v("Received %s", intent); //$NON-NLS-1$

        /*
         * Note: It is OK if a host sends an ordered broadcast for plug-in
         * settings. Such a behavior would allow the host to optionally block until the
         * plug-in setting finishes.
         */

        if (!com.twofortyfouram.locale.api.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            Lumberjack
                    .e("Intent action is not %s",
                            com.twofortyfouram.locale.api.Intent.ACTION_FIRE_SETTING); //$NON-NLS-1$
            return;
        }

        /*
         * Ignore implicit intents, because they are not valid. It would be
         * meaningless if ALL plug-in setting BroadcastReceivers installed were
         * asked to handle queries not intended for them. Ideally this
         * implementation here would also explicitly assert the class name as
         * well, but then the unit tests would have trouble. In the end,
         * asserting the package is probably good enough.
         */
        if (!context.getPackageName().equals(intent.getPackage())
                && !new ComponentName(context, this.getClass().getName()).equals(intent
                .getComponent())) {
            Lumberjack.e("Intent is not explicit"); //$NON-NLS-1$
            return;
        }

        final Bundle bundle = intent
                .getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        if (BundleScrubber.scrub(intent)) {
            return;
        }

        if (null == bundle) {
            Lumberjack.e("%s is missing",
                    com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE); //$NON-NLS-1$
            return;
        }

        if (!isBundleValid(context, bundle)) {
            Lumberjack.e("%s is invalid",
                    com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE); //$NON-NLS-1$
            return;
        }

        if (isAsync() && AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.HONEYCOMB)) {
            final AsyncCallback callback = new AsyncCallback() {

                @NonNull
                private final Context mContext = context;

                @NonNull
                private final Bundle mBundle = bundle;

                @Override
                public int runAsync() {
                    firePluginSetting(mContext, mBundle);
                    return Activity.RESULT_OK;
                }

            };

            goAsyncWithCallback(callback, isOrderedBroadcast());
        } else {
            firePluginSetting(context, bundle);
        }
    }

    /**
     * <p>Gives the plug-in receiver an opportunity to validate the Bundle, to
     * ensure that a malicious application isn't attempting to pass
     * an invalid Bundle.</p>
     * <p>
     * This method will be called on the BroadcastReceiver's Looper (normatively the main thread)
     * </p>
     *
     * @param bundle The plug-in's Bundle previously returned by the edit
     *               Activity.  {@code bundle} should not be mutated by this method.
     * @return true if {@code bundle} appears to be valid.  false if {@code bundle} appears to be
     * invalid.
     */
    protected abstract boolean isBundleValid(@NonNull final Context context, @NonNull final Bundle bundle);

    /**
     * Configures the receiver whether it should process the Intent in a
     * background thread. Plug-ins should return true if their
     * {@link #firePluginSetting(android.content.Context, android.os.Bundle)} method performs any
     * sort of disk IO (ContentProvider query, reading SharedPreferences, etc.).
     * or other work that may be slow.
     * <p>
     * Asynchronous BroadcastReceivers are not supported prior to Honeycomb, so
     * with older platforms broadcasts will always be processed on the BroadcastReceiver's Looper
     * (which for Manifest registered receivers will be the main thread).
     *
     * @return True if the receiver should process the Intent in a background
     * thread. False if the plug-in should process the Intent on the
     * BroadcastReceiver's Looper (normatively the main thread).
     */
    protected abstract boolean isAsync();

    /**
     * If {@link #isAsync()} returns true, this method will be called on a
     * background thread. If {@link #isAsync()} returns false, this method will
     * be called on the main thread. Regardless of which thread this method is
     * called on, this method MUST return within 10 seconds per the requirements
     * for BroadcastReceivers.
     *
     * @param context BroadcastReceiver context.
     * @param bundle  The plug-in's Bundle previously returned by the edit
     *                Activity.
     */
    protected abstract void firePluginSetting(@NonNull final Context context,
                                              @NonNull final Bundle bundle);


    /*
     * This method is package visible rather than protected so that it will be
     * obfuscated by ProGuard.
     *
     * @param callback Callback to execute on a background thread.
     * @param isOrdered Indicates whether an ordered broadcast is being processed.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    /* package */ final void goAsyncWithCallback(@NonNull final AsyncCallback callback,
                                                 final boolean isOrdered) {
        assertNotNull(callback, "callback"); //$NON-NLS-1$

        final PendingResult pendingResult = goAsync();
        if (null == pendingResult) {
            throw new AssertionError(
                    "PendingResult was null.  Was goAsync() called previously?"); //$NON-NLS-1$
        }

        final Handler.Callback handlerCallback = new AsyncHandlerCallback();
        final HandlerThread thread = ThreadUtil.newHandlerThread(getClass().getName(),
                ThreadUtil.ThreadPriority.BACKGROUND);
        final Handler handler = new Handler(thread.getLooper(), handlerCallback);

        final Object obj = new Pair<PendingResult, AsyncCallback>(pendingResult, callback);
        final int isOrderedInt = isOrdered ? 1 : 0;
        final Message msg = handler
                .obtainMessage(AsyncHandlerCallback.MESSAGE_HANDLE_CALLBACK, isOrderedInt, 0, obj);

        final boolean isMessageSent = handler.sendMessage(msg);
        if (!isMessageSent) {
            throw new AssertionError();
        }
    }

    /* package */ interface AsyncCallback {

        /**
         * @return The result code to be set if this is an ordered broadcast.
         */
        int runAsync();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static final class AsyncHandlerCallback implements Handler.Callback {

        /**
         * Message MUST contain a {@code Pair<PendingResult, AsyncCallback>} as the {@code msg.obj}
         * and a boolean encoded in the {@code msg.arg1} to indicate whether the broadcast was
         * ordered.
         */
        public static final int MESSAGE_HANDLE_CALLBACK = 0;

        @NonNull
        @SuppressWarnings("unchecked")
        private static Pair<PendingResult, AsyncCallback> castObj(@NonNull final Object o) {
            return (Pair<PendingResult, AsyncCallback>) o;
        }

        private static void quit() {
            if (AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.JELLY_BEAN_MR2)) {
                quitJellybeanMr2();
            } else {
                Looper.myLooper().quit();
            }
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        private static void quitJellybeanMr2() {
            Looper.myLooper().quitSafely();
        }

        @Override
        public boolean handleMessage(final Message msg) {
            assertNotNull(msg, "msg"); //$NON-NLS-1$
            switch (msg.what) {
                case MESSAGE_HANDLE_CALLBACK: {
                    assertNotNull(msg.obj, "msg.obj"); //$NON-NLS-1$
                    assertInRangeInclusive(msg.arg1, 0, 1, "msg.arg1");  //$NON-NLS-1$

                    final Pair<PendingResult, AsyncCallback> pair = castObj(msg.obj);
                    final boolean isOrdered = 0 != msg.arg1;

                    final PendingResult pendingResult = pair.first;
                    final AsyncCallback asyncCallback = pair.second;

                    try {
                        final int resultCode = asyncCallback.runAsync();

                        if (isOrdered) {
                            pendingResult.setResultCode(resultCode);
                        }
                    } finally {
                        pendingResult.finish();
                    }

                    quit();

                    break;
                }
            }
            return true;
        }
    }
}
