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

import com.kynetics.updatefactory.ddiclient.core.model.FileInfo;
import com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation;

import java.io.InputStream;

/**
 * @author Daniele Sergio
 */

public class AndroidSystemOperation implements SystemOperation {
    private UpdateStatus updateStatus;
    private boolean isOtaUpdateExecuted;
    private final Context context;
    private boolean apkFound;
    private boolean osFound;

    AndroidSystemOperation(Context context, boolean isOtaUpdateExecuted) {
        this.updateStatus = UpdateStatus.NOT_APPLIED;
        this.context = context;
        this.isOtaUpdateExecuted = isOtaUpdateExecuted;
        resetApkOsFlag();
    }

    @Override
    public boolean savingFile(InputStream inputStream, FileInfo fileInfo) {
        isOtaUpdateExecuted = false;
        this.updateStatus = UpdateStatus.NOT_APPLIED;
        final String fileName = fileInfo.getLinkInfo().getFileName();
        return fileName.endsWith("apk") ?
                copyApkFile(inputStream, fileName)
                : copyOsFile(inputStream);
    }

    private boolean copyOsFile(InputStream inputStream) {
        apkFound = true;
        return UpdateSystem.copyFile(inputStream);
    }

    private boolean copyApkFile(InputStream inputStream, String fileName) {
        osFound = true;
        return UpdateSystem.copyApkFile(context, inputStream, fileName);
    }

    @Override
    public void executeUpdate(long updateId) {
        if(apkFound && osFound){
            resetApkOsFlag();
            updateStatus = UpdateStatus.APPLIED_WITH_ERROR;
        } else if(UpdateSystem.apkToInstall(context)){
            resetApkOsFlag();
            updateApp();
        } else {
            resetApkOsFlag();
            updateOta(updateId);
        }
    }

    private void updateOta(long updateId) {
        if(UpdateSystem.verify()){
            UpdateSystem.install(context, updateId);
        } else {
            updateStatus = UpdateStatus.APPLIED_WITH_ERROR;
        }
    }

    private void updateApp(){
        if(UpdateSystem.installApk(context)){
            updateStatus = UpdateStatus.SUCCESSFULLY_APPLIED;
        } else {
            updateStatus = UpdateStatus.APPLIED_WITH_ERROR;
        }
    }

    @Override
    public UpdateStatus updateStatus() {
        return isOtaUpdateExecuted ? UpdateSystem.successInstallation() : updateStatus;
    }

    private void resetApkOsFlag(){
        apkFound = false;
        osFound = false;
    }
}
