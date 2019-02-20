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
import com.kynetics.uf.android.update.UpdateConfirmationTimeoutProvider;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import static com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation.UpdateStatus.newFailureStatus;
import static com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation.UpdateStatus.newSuccessStatus;

/**
 * @author Daniele Sergio
 */
public class UpdateSystem {
    private static final String TAG = UpdateSystem.class.getSimpleName();

    private static final String OTA_FILE_NAME = "update.zip";
    private static final String UPDATE_PENDING_FILE_NAME = "update_pending";
    private static final String UPDATE_APK_FOLDER = "updateApplication";

    static boolean copyFile(InputStream inputStream){
        clearOtaUpdate();
        return write(inputStream, new File(getPath(OTA_FILE_NAME)));
    }

    public static void clearOtaUpdate(){
        final File packageFile = new File(getPath(OTA_FILE_NAME));
        if (packageFile.exists()) {
            packageFile.delete();
        }
    }

    public static void clearApkUpdate(Context context){
        final File updateDirectory = new File(context.getFilesDir(), UPDATE_APK_FOLDER);
        for(File file : updateDirectory.listFiles()) {
            file.delete();
        }
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
            final int resultCode = Integer.parseInt(fileInputStream.readLine());
            return resultCode  == 1 ? SystemOperation.UpdateStatus.newSuccessStatus(null) :
                    newFailureStatus(new String[] {String.format("last_install result code: %s",resultCode)});
        }catch (IOException exception){
            Log.e(TAG, "installation error", exception);
            return newFailureStatus(new String[] {
                    String.format("Installation fails with exception: %s", exception.getMessage())});
        }
    }


    static SystemOperation.UpdateStatus installApk(Context context) throws InterruptedException {
        if(android.os.Build.VERSION.SDK_INT <  Build.VERSION_CODES.LOLLIPOP){
            return newFailureStatus(new String[]{String.format("Installation of apk is not supported from device with android system api lower than %s (current is %s)",Build.VERSION_CODES.LOLLIPOP, android.os.Build.VERSION.SDK_INT) });
        }

        final File updateDirectory = new File(context.getFilesDir(), UPDATE_APK_FOLDER);

        if(!updateDirectory.exists()){
            return newFailureStatus(new String[]{"Apk not found"});
        }

        final List<String> errorMessages = new ArrayList<>();

        final CountDownLatch countDownLatch = new CountDownLatch(updateDirectory.listFiles().length);
        for(File file : updateDirectory.listFiles()){
            if(file.getName().endsWith("apk")){
                try {
                    Log.d(TAG, String.format("installing apk named %s", file.getName()));
                    installPackage(context, file, getPackageFromApk(context, file.getAbsolutePath()), countDownLatch, errorMessages);
                } catch (IOException | IllegalArgumentException e) {
                    errorMessages.add(String.format("%s installation fails with error %s", file.getName(), e.getMessage()));
                    countDownLatch.countDown();
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

        final UpdateConfirmationTimeoutProvider.Timeout timeout = UpdateConfirmationTimeoutProvider
                .FixedTimeProvider./**/ofSeconds(1800).getTimeout(null); // TODO: 20/02/19 make seconds configurables
        countDownLatch.await(timeout.value, timeout.timeUnit);

        return errorMessages.size() == 0 ? newSuccessStatus(null) : newFailureStatus(errorMessages.toArray(new String[0]));
    }

    public static boolean apkToInstall(Context context){
        final File updateDirectory = new File(context.getFilesDir(), UPDATE_APK_FOLDER);
        return updateDirectory.exists() && updateDirectory.listFiles().length > 0;
    }

    private static String getPath(String fileName){
        return String.format("%s/%s",Environment.getDownloadCacheDirectory(), fileName);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static void installPackage(Context context, File file, String packageName, CountDownLatch countDownLatch, List<String> errorMessages)
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
                errorMessages
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
