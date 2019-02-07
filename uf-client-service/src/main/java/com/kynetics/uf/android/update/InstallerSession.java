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

package com.kynetics.uf.android.update;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class InstallerSession {

    private static final String ACTION_INSTALL_COMPLETE = "com.kynetics.aciont.INSTALL_COMPLETED";

    public static InstallerSession newInstance(Context context,
                                        CountDownLatch countDownLatch,
                                        String packageName,
                                        AtomicBoolean installWithoutErrors) throws IOException {
        final PackageInstaller packageInstaller = context.getPackageManager()
                .getPackageInstaller();
        final PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(packageName);
        final int sessionId = packageInstaller.createSession(params);
        final UFSessionCallback ufSessionCallback = new UFSessionCallback(sessionId,
                countDownLatch,
                installWithoutErrors);
        packageInstaller.registerSessionCallback(ufSessionCallback);
        return new InstallerSession(context, packageInstaller, sessionId);
    }

    public void  writeSession(File file, String name) {
        final long sizeBytes = file.length();
        Log.v(TAG, "apk size :" + sizeBytes);
        try (PackageInstaller.Session session = packageInstaller.openSession(sessionId);
             InputStream in = new FileInputStream(file);
             OutputStream out = session.openWrite(name, 0, sizeBytes)) {
            final byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
        } catch (IOException e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    public void commitSession() {
        try (PackageInstaller.Session  session = packageInstaller.openSession(sessionId) ) {
            session.commit(createIntentSender());
        } catch (IOException e) {
            Log.d(TAG, e.getMessage() , e);
        }
    }

    private IntentSender createIntentSender() {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(ACTION_INSTALL_COMPLETE),
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent.getIntentSender();
    }

    private InstallerSession(Context context,
                             PackageInstaller packageInstaller,
                             int sessionId) {
        this.context = context;
        this.packageInstaller = packageInstaller;
        this.sessionId = sessionId;
    }

    private static final String TAG = InstallerSession.class.getSimpleName();

    private final Context context;
    private final PackageInstaller packageInstaller;
    // TODO: 07/02/19 unregister sessionCallback
    private final int sessionId;
}
