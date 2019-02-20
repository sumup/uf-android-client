/*
 *
 *  Copyright © 2017-2019  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.uf.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.kynetics.updatefactory.ddiclient.core.model.FileInfo;
import com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation;

import java.io.InputStream;

import static com.kynetics.uf.android.UpdateSystem.clearApkUpdate;
import static com.kynetics.uf.android.UpdateSystem.clearOtaUpdate;


/**
 * @author Daniele Sergio
 */

public class AndroidSystemOperation implements SystemOperation {
    private static final String TAG = AndroidSystemOperation.class.getSimpleName();

    private UpdateStatus updateStatus;
    private boolean isOtaUpdateExecuted;
    private final Context context;

    private static final String OTA_IS_FOUND_KEY = "OTA_IS_FOUND";
    private static final String APK_IS_FOUND_KEY = "APK_IS_FOUND";

    private final SharedPreferences sharedPreferences;

    AndroidSystemOperation(Context context, boolean isOtaUpdateExecuted, SharedPreferences sharedPreferences) {
        this.updateStatus = UpdateStatus.newPendingStatus();
        this.context = context;
        this.isOtaUpdateExecuted = isOtaUpdateExecuted;
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public boolean savingFile(InputStream inputStream, FileInfo fileInfo) {
        isOtaUpdateExecuted = false;
        this.updateStatus = UpdateStatus.newPendingStatus();
        final String fileName = fileInfo.getLinkInfo().getFileName();
        return fileName.endsWith("apk") ?
                copyApkFile(inputStream, fileName)
                : copyOsFile(inputStream);
    }

    private boolean copyOsFile(InputStream inputStream) {
        setOtaIsFoundToTrue();
        return UpdateSystem.copyFile(inputStream);
    }

    private boolean copyApkFile(InputStream inputStream, String fileName) {
        setApkIsFoundTrue();
        return UpdateSystem.copyApkFile(context, inputStream, fileName);
    }

    @Override
    public void executeUpdate(long updateId) {
        if(getApkIsFound() && getOtaIsFound()){
            resetApkOsFlag();
            updateStatus = UpdateStatus.newFailureStatus(new String[]{"Update os with application is not yet supported"});
        } else if(UpdateSystem.apkToInstall(context)){
            resetApkOsFlag();
            updateApp();
        } else {
            resetApkOsFlag();
            updateOta(updateId);
        }

        clearOtaUpdate();
        clearApkUpdate(context);
    }

    private void updateOta(long updateId) {
        if(UpdateSystem.verify()){
            UpdateSystem.install(context, updateId);
        } else {
            updateStatus = UpdateStatus.newFailureStatus(new String[]{"Wrong ota signature"});
        }
    }

    private void updateApp(){
        try {
            updateStatus = UpdateSystem.installApk(context);
        } catch (InterruptedException e) {
            Log.i(TAG, e.getMessage(), e);
            updateStatus = UpdateStatus.newFailureStatus(new String[]{"Time to update exceeds the timeout"});
        }
    }

    @Override
    public UpdateStatus updateStatus() {
        return isOtaUpdateExecuted ? UpdateSystem.successInstallation() : updateStatus;
    }

    private boolean getOtaIsFound(){
        return sharedPreferences.getBoolean(OTA_IS_FOUND_KEY, false);
    }

    private void setOtaIsFoundToTrue(){
        sharedPreferences.edit().putBoolean(OTA_IS_FOUND_KEY, true).apply();
    }

    private boolean getApkIsFound(){
        return sharedPreferences.getBoolean(APK_IS_FOUND_KEY, false);
    }

    private void setApkIsFoundTrue(){
        sharedPreferences.edit().putBoolean(APK_IS_FOUND_KEY, true).apply();
    }

    private void resetApkOsFlag(){
        sharedPreferences.edit()
                .remove(OTA_IS_FOUND_KEY)
                .remove(APK_IS_FOUND_KEY)
                .apply();
    }
}
