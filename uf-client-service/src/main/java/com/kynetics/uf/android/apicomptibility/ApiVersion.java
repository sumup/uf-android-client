/*
 * Copyright © 2017-2020  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.apicomptibility;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 * @author Daniele Sergio
 */
public enum ApiVersion {
    PRE_O, POST_O{
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void startService(Context context, Intent intent) {
            context.startForegroundService(intent);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void configureChannel(String channelId, String channelName, NotificationManager notificationManager) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            channel.setSound(null, null);

            notificationManager.createNotificationChannel(channel);
        }
    };

    public static ApiVersion fromVersionCode(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? POST_O : PRE_O;
    }

    public void startService(Context context, Intent intent){
        context.startService(intent);
    }

    public void configureChannel(String channelId, String channelName, NotificationManager notificationManager){

    }
}
