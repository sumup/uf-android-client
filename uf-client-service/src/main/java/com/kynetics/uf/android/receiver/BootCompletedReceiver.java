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

package com.kynetics.uf.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kynetics.uf.android.UpdateFactoryService;
import com.kynetics.uf.android.apicomptibility.ApiVersion;

/**
 * @author Daniele Sergio
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            final Intent myIntent = new Intent(context, UpdateFactoryService.class);
            ApiVersion.fromVersionCode().startService(context, myIntent);
        }
    }
}
