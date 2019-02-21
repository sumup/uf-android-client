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

import com.kynetics.uf.android.update.CurrentUpdateState;
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
    private final CurrentUpdateState currentUpdateState;

    AndroidSystemOperation(Context context, boolean isOtaUpdateExecuted) {
        this.updateStatus = UpdateStatus.newPendingStatus();
        this.context = context;
        this.isOtaUpdateExecuted = isOtaUpdateExecuted;
        this.currentUpdateState = new CurrentUpdateState(context);
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
        currentUpdateState.setOtaIsFoundToTrue();
        return UpdateSystem.copyFile(inputStream);
    }

    private boolean copyApkFile(InputStream inputStream, String fileName) {
        currentUpdateState.setApkIsFoundTrue();
        return UpdateSystem.copyApkFile(context, inputStream, fileName);
    }

    @Override
    public void executeUpdate(long updateId) {
        if(currentUpdateState.getApkIsFound() && currentUpdateState.getOtaIsFound()){
            updateStatus = UpdateStatus.newFailureStatus(new String[]{"Update os with application is not yet supported"});
        } else if(currentUpdateState.isUfServiceUpdated()){
            currentUpdateState.incrementApkAlreadyInstalled();
            currentUpdateState.setUpdateUpdated(false);
            updateApp();
        } if(UpdateSystem.apkToInstall(context)) {
            updateApp();
        } else {
            updateOta(updateId);
        }

        currentUpdateState.clearState();

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
            updateStatus = UpdateSystem.installApk(context, currentUpdateState);
        } catch (InterruptedException e) {
            Log.i(TAG, e.getMessage(), e);
            updateStatus = UpdateStatus.newFailureStatus(new String[]{e.getMessage()});
        }
    }

    @Override
    public UpdateStatus updateStatus() {
        return isOtaUpdateExecuted ? UpdateSystem.successInstallation() : updateStatus;
    }
}
