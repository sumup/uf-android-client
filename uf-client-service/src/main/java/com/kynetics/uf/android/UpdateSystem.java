/*
 * Copyright © 2017-2018  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.RecoverySystem;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * @author Daniele Sergio
 */
public class UpdateSystem {
    private static final String TAG = UpdateSystem.class.getSimpleName();

    private static final String OTA_FILE_NAME = "update.zip";
    private static final String UPDATE_PENDING_FILE_NAME = "update_pending";
    private static final String UPDATE_APK_FOLDER = "updateApplication";
    static final String ACTION_INSTALL_COMPLETE = "com.kynetics.aciont.INSTALL_COMPLETED";

    static boolean copyFile(InputStream inputStream){
        final File packageFile = new File(getPath(OTA_FILE_NAME));
        if (packageFile.exists()) {
            packageFile.delete();
        }

        return write(inputStream, packageFile);
    }

    static boolean copyApkFile(Context context, InputStream inputStream, String fileName){
        final File updateFolder = new File(context.getFilesDir(),  UPDATE_APK_FOLDER);
        if(!updateFolder.exists()){
            updateFolder.mkdir();
        }


        final File apkFile = new File(updateFolder,  fileName);
        if(apkFile.exists()){
            apkFile.delete();
        }

        return write(inputStream, apkFile);
    }

    private static boolean write(InputStream inputStream, File outputStream) {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        try (FileChannel dest = (new FileOutputStream(outputStream)).getChannel();
             InputStream src = inputStream;
             ReadableByteChannel source = Channels.newChannel(src)
        ){
            while (source.read(buffer) != -1) {
                buffer.flip();
                dest.write(buffer);
                buffer.compact();
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                dest.write(buffer);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy update file into internal storage ", e);
        }
        return false;
    }

    static Long getUpdatePendingId(){
        final File file = new File(getPath(UPDATE_PENDING_FILE_NAME));
        if(!file.exists()){
            return null;
        }
        Long updatePendingId = null;
        try (final Scanner scan = new Scanner(file)) {
            if(scan.hasNextLine()){
                final String line = scan.nextLine();
                updatePendingId = Long.decode(line);
            }
        } catch (FileNotFoundException | NumberFormatException e) {
            e.printStackTrace();
        }
        file.delete();
        return updatePendingId;
    }

    static boolean verify(){
        try {
            File packageFile = new File(getPath(OTA_FILE_NAME));
            RecoverySystem.verifyPackage(packageFile, null, null);
            return true;
        }
        catch (Exception e) {
            Log.e(TAG, "Corrupted package: " + e);
            return false;
        }
    }

    static void install(Context context, long updateId){
        try {
            File packageFile = new File(getPath(OTA_FILE_NAME));
            File updatePendingFile = new File(getPath(UPDATE_PENDING_FILE_NAME));
            write(new ByteArrayInputStream(String.valueOf(updateId).getBytes()),updatePendingFile);
            RecoverySystem.installPackage(context, packageFile);
        }
        catch (Exception e) {
            Log.e(TAG, "Error install package: " + e);
        }
    }

    static SystemOperation.UpdateStatus successInstallation() {
        try (BufferedReader fileInputStream = new BufferedReader(new FileReader(new File("/cache/recovery", "last_install")))) {
            fileInputStream.readLine();
            return Integer.parseInt(fileInputStream.readLine()) == 1 ?
                    SystemOperation.UpdateStatus.SUCCESSFULLY_APPLIED :
                    SystemOperation.UpdateStatus.APPLIED_WITH_ERROR;
        }catch (IOException exception){
            Log.e(TAG, "installation error", exception);
            return SystemOperation.UpdateStatus.APPLIED_WITH_ERROR;
        }
    }


    static boolean installApk(Context context){
        if(android.os.Build.VERSION.SDK_INT <  Build.VERSION_CODES.LOLLIPOP){
            return false;
        }

        final File updateDirectory = new File(context.getFilesDir(), UPDATE_APK_FOLDER);

        if(!updateDirectory.exists()){
            return true;
        }

        boolean flag = true;

        final CountDownLatch countDownLatch = new CountDownLatch(updateDirectory.listFiles().length);
        for(File file : updateDirectory.listFiles()){
            if(file.getName().endsWith("apk")){
                try {
                    installPackage(context, new FileInputStream(file),getPakcageFromApk(context, file.getAbsolutePath()), countDownLatch);
                } catch (IOException e) {
                    flag = false;
                }
            } else {
                countDownLatch.countDown();
            }
        }

        for(File file : updateDirectory.listFiles()) {
            file.delete();
        }

        return flag;
    }

    public static boolean apkToInstall(Context context){
        final File updateDirectory = new File(context.getFilesDir(), UPDATE_APK_FOLDER);
        return updateDirectory.exists() && updateDirectory.listFiles().length > 0;
    }

    private static String getPath(String fileName){
        return String.format("%s/%s",Environment.getDownloadCacheDirectory(), fileName);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static final class UFSessionCallback extends PackageInstaller.SessionCallback{
        private final int sessionId;
        private final CountDownLatch countDownLatch;

        public UFSessionCallback(int sessionId, CountDownLatch countDownLatch) {
            this.sessionId = sessionId;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void onCreated(int sessionId) {

        }

        @Override
        public void onBadgingChanged(int sessionId) {

        }

        @Override
        public void onActiveChanged(int sessionId, boolean active) {

        }

        @Override
        public void onProgressChanged(int sessionId, float progress) {

        }

        @Override
        public void onFinished(int sessionId, boolean success) {
            if(this.sessionId == sessionId ){
                countDownLatch.countDown();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static void installPackage(Context context, InputStream in, String packageName, CountDownLatch countDownLatch)
            throws IOException {
        try{
            Looper.prepare();
        } catch (RuntimeException r){
            Log.d(TAG, r.getMessage());
        }
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(packageName);
        // set params
        final int sessionId = packageInstaller.createSession(params);
        packageInstaller.registerSessionCallback(new UFSessionCallback(sessionId, countDownLatch));
        PackageInstaller.Session session = packageInstaller.openSession(sessionId);
        OutputStream out = session.openWrite(packageName, 0, -1);
        byte[] buffer = new byte[65536];
        int c;
        while ((c = in.read(buffer)) != -1) {
            out.write(buffer, 0, c);
        }
        session.fsync(out);
        in.close();
        out.close();

        session.commit(createIntentSender(context, sessionId));

    }



    private static String getPakcageFromApk(Context context, String apkPath){
        PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(apkPath,PackageManager.GET_ACTIVITIES);
        if(packageInfo != null) {
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            return appInfo.packageName;
        }
        return null;
    }

    private static IntentSender createIntentSender(Context context, int sessionId) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(ACTION_INSTALL_COMPLETE),
                0);
        return pendingIntent.getIntentSender();
    }
}
