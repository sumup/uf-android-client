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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.RecoverySystem;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.kynetics.uf.android.update.InstallerSession;
import com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Daniele Sergio
 */
public class UpdateSystem {
    private static final String TAG = UpdateSystem.class.getSimpleName();

    private static final String OTA_FILE_NAME = "update.zip";
    private static final String UPDATE_PENDING_FILE_NAME = "update_pending";
    private static final String UPDATE_APK_FOLDER = "updateApplication";

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

        final AtomicBoolean installWithoutErrors = new AtomicBoolean(true);

        final CountDownLatch countDownLatch = new CountDownLatch(updateDirectory.listFiles().length);
        for(File file : updateDirectory.listFiles()){
            if(file.getName().endsWith("apk")){
                try {
                    Log.d(TAG, String.format("installing apk named %s", file.getName()));
                    installPackage(context, file, getPackageFromApk(context, file.getAbsolutePath()), countDownLatch, installWithoutErrors);
                } catch (IOException | IllegalArgumentException e) {
                    installWithoutErrors.set(false);
                    Log.d(TAG, String.format("Failed to install %s", file.getName()));
                    Log.d(TAG, e.getMessage(), e);
                }
            } else {
                countDownLatch.countDown();
            }
        }

        for(File file : updateDirectory.listFiles()) {
            file.delete();
        }

        return installWithoutErrors.get();
    }

    public static boolean apkToInstall(Context context){
        final File updateDirectory = new File(context.getFilesDir(), UPDATE_APK_FOLDER);
        return updateDirectory.exists() && updateDirectory.listFiles().length > 0;
    }

    private static String getPath(String fileName){
        return String.format("%s/%s",Environment.getDownloadCacheDirectory(), fileName);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static void installPackage(Context context, File file, String packageName, CountDownLatch countDownLatch, AtomicBoolean installWithoutErrors)
            throws IOException {
        try{
            Looper.prepare();
        } catch (RuntimeException r){
            Log.d(TAG, r.getMessage());
        }

        final InstallerSession installerSession = InstallerSession.newInstance(
                context,
                countDownLatch,
                packageName,
                installWithoutErrors
        );
        installerSession.writeSession(file,  packageName);
        installerSession.commitSession();
        /**/
    }

    private static String getPackageFromApk(Context context, String apkPath){
        PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(apkPath,PackageManager.GET_ACTIVITIES);
        if(packageInfo != null) {
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            return appInfo.packageName;
        }
        return null;
    }

}
