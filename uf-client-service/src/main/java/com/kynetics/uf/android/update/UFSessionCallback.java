package com.kynetics.uf.android.update;

import android.content.pm.PackageInstaller;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author Daniele Sergio
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
final class UFSessionCallback extends PackageInstaller.SessionCallback{
    private static final String TAG = UFSessionCallback.class.getSimpleName();

    private final int sessionId;
    private final CountDownLatch countDownLatch;
    private final AtomicBoolean installWithoutErrors;
     UFSessionCallback(int sessionId, CountDownLatch countDownLatch, AtomicBoolean installWithoutErrors) {
        this.sessionId = sessionId;
        this.countDownLatch = countDownLatch;
        this.installWithoutErrors = installWithoutErrors;
    }

    @Override
    public void onCreated(int sessionId) {
        Log.d(TAG, String.format("onCreated: %s",sessionId));

    }

    @Override
    public void onBadgingChanged(int sessionId) {
        Log.d(TAG, String.format("onBadgingChanged: %s",sessionId));

    }

    @Override
    public void onActiveChanged(int sessionId, boolean active) {
        Log.d(TAG, String.format("onActiveChanged: %s, %s",sessionId, active));

    }

    @Override
    public void onProgressChanged(int sessionId, float progress) {
        Log.d(TAG, String.format("onProgressChanged: %s, %s",sessionId, progress));

    }

    @Override
    public void onFinished(int sessionId, boolean success) {
        Log.d(TAG, String.format("Installation result: %s", success ? "OK" : "KO"));
        if(!success){
            installWithoutErrors.set(false);
        }
        if(this.sessionId == sessionId ){
            countDownLatch.countDown();
        }
    }
}
