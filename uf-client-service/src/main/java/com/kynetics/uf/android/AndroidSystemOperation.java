/*
 * Copyright © 2017-2018  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android;

import android.content.Context;

import com.kynetics.updatefactory.ddiclient.core.UFService;
import com.kynetics.updatefactory.ddiclient.core.model.FileInfo;
import com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation;

import java.io.InputStream;

/**
 * @author Daniele Sergio
 */

public class AndroidSystemOperation implements SystemOperation {
    private UpdateStatus updateStatus;
    private boolean updateExecuted;
    private final Context context;

    public AndroidSystemOperation(Context context, boolean updateExecuted) {
        this.updateStatus = UpdateStatus.NOT_APPLIED;
        this.context = context;
        this.updateExecuted = updateExecuted;
    }

    @Override
    public boolean savingFile(InputStream inputStream, FileInfo fileInfo) {
        updateExecuted = false;
        this.updateStatus = UpdateStatus.NOT_APPLIED;
        return UpdateSystem.copyFile(inputStream);
    }

    @Override
    public void executeUpdate(long updateId) {
        if(UpdateSystem.verify()){
            UpdateSystem.install(context, updateId);
        } else {
            updateStatus = UpdateStatus.APPLIED_WITH_ERROR;
        }
    }

    @Override
    public UpdateStatus updateStatus() {
        return updateExecuted ? UpdateSystem.successInstallation() : updateStatus;
    }
}
