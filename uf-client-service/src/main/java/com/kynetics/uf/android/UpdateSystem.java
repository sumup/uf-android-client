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
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.kynetics.uf.android.update.CurrentUpdateState;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

import static com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation.UpdateStatus.newFailureStatus;
import static com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation.UpdateStatus.newSuccessStatus;

/**
 * @author Daniele Sergio
 */
class UpdateSystem {
    private static final String TAG = UpdateSystem.class.getSimpleName();

    private static final String OTA_FILE_NAME = "update.zip";
    private static final String UPDATE_PENDING_FILE_NAME = "update_pending";
    private static final String UPDATE_APK_FOLDER = "updateApplication";

    static boolean copyOtaFile(InputStream inputStream){
        clearOtaUpdate();
        return write(inputStream, new File(getPath(OTA_FILE_NAME)));
    }

    static void clearOtaUpdate(){
        logWarnIfDeletionFails(new File(getPath(OTA_FILE_NAME)));
    }

    static void clearApkUpdate(Context context){
        final File updateDirectory = getUpdateApkFolder(context);
        if(!updateDirectory.exists()){
            return;
        }

        for(File file : updateDirectory.listFiles()) {
            logWarnIfDeletionFails(file);
        }
    }

    static boolean copyApkFile(Context context, InputStream inputStream, String fileName){
        final File updateFolder = getUpdateApkFolder(context);
        if(!updateFolder.exists() && !updateFolder.mkdir()){
            return false;
        }

        final File destinationFile = new File(updateFolder, fileName);
        if(!deleteFileIfExists(destinationFile)){
            return false;
        }

        return write(inputStream, destinationFile);
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
            Log.d(TAG, e.getMessage(), e);
        }
        logWarnIfDeletionFails(file);
        return updatePendingId;
    }

    static boolean verify(){
        try {
            File packageFile = new File(getPath(OTA_FILE_NAME));
            RecoverySystem.verifyPackage(packageFile, null, null);
            return true;
        }
        catch (Exception e) {
            Log.e(TAG, "Corrupted package", e);
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

    // TODO: 22/02/19 refactor
    static SystemOperation.UpdateStatus installApplications(Context context, CurrentUpdateState currentUpdateState) throws InterruptedException {
        if(android.os.Build.VERSION.SDK_INT <  Build.VERSION_CODES.LOLLIPOP){
            return newFailureStatus(new String[]{String.format("Installation of apk is not supported from device with android system api lower than %s (current is %s)",Build.VERSION_CODES.LOLLIPOP, android.os.Build.VERSION.SDK_INT) });
        }

        final File updateDirectory = getUpdateApkFolder(context);

        if(!updateDirectory.exists()){
            return newFailureStatus(new String[]{"Apk not found"});
        }

        final File[] files = updateDirectory.listFiles();
        final int numberOfApkToInstall = files.length - currentUpdateState.getApkAlreadyInstalled();
        if(numberOfApkToInstall < 1){
            return  getDistributionInstalletionResponse(currentUpdateState);
        }

        long memoryNeeded = 0;
        for(int i =0; i< files.length; i++){
            if(i>=currentUpdateState.getApkAlreadyInstalled()){
                memoryNeeded += files[i].length();
            }
        }

        final long freeSpace = getFreeSpace( Environment.getDataDirectory());
        Log.i(TAG, String.format("FreeSpace: %s; Space needed: %s", freeSpace, memoryNeeded));
        if(memoryNeeded * 2 > freeSpace){
            return newFailureStatus(new String[]{"Not enough space available"});
        }


        final CountDownLatch countDownLatch = new CountDownLatch(numberOfApkToInstall);
        final TreeSet<File> fileOrdered = new TreeSet<>(Arrays.asList(files));
        final Iterator<File> fileIterator = fileOrdered.iterator();
        for(int i=0; i<currentUpdateState.getApkAlreadyInstalled(); i++){
            if(fileIterator.hasNext()){
                fileIterator.next();
            }
        }
        while (fileIterator.hasNext()){
            final File file = fileIterator.next();
            if(file.getName().endsWith("apk")){
                try {
                    Log.d(TAG, String.format("installing apk named %s", file.getName()));
                    installApk(context, file, getPackageFromApk(context, file.getAbsolutePath()), countDownLatch, currentUpdateState);
                } catch (IOException | IllegalArgumentException e) {
                    addErrorMessage(currentUpdateState, String.format("%s installation fails with error %s", file.getName(), e.getMessage()));
                    currentUpdateState.incrementApkAlreadyInstalled();
                    countDownLatch.countDown();
                    Log.d(TAG, String.format("Failed to install %s", file.getName()));
                    Log.d(TAG, e.getMessage(), e);
                }
            } else {
                currentUpdateState.incrementApkAlreadyInstalled();
                countDownLatch.countDown();
            }
        }

        for(File file : files) {
            logWarnIfDeletionFails(file);
        }

        final UpdateConfirmationTimeoutProvider.Timeout timeout = UpdateConfirmationTimeoutProvider
                .FixedTimeProvider.ofSeconds(1800).getTimeout(null);

        if(!countDownLatch.await(timeout.value, timeout.timeUnit)){
            addErrorMessage(currentUpdateState,"Time to update exceeds the timeout");
        }

        return  getDistributionInstalletionResponse(currentUpdateState);

    }

    static boolean existApkToInstall(Context context){
        final File updateDirectory = getUpdateApkFolder(context);
        return updateDirectory.exists() && updateDirectory.listFiles().length > 0;
    }

    static long getFreeSpace(File path){
        final StatFs stat = new StatFs(path.getPath());
        return stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
    }

    private static SystemOperation.UpdateStatus getDistributionInstalletionResponse(CurrentUpdateState currentUpdateState) {
        final Set<String> errors = currentUpdateState.getDistributionReportError();
        final Set<String> success = currentUpdateState.getDistributionReportSuccess();
        final Set<String> allReport = new HashSet<>();
        allReport.addAll(errors);
        allReport.addAll(success);
        return errors.size() == 0 ? newSuccessStatus(allReport.toArray(new String[0])) : newFailureStatus(allReport.toArray(new String[0]));
    }

    private static void addErrorMessage(CurrentUpdateState currentUpdateState, String newErrorMessage){
        final Set<String> errorMessages = currentUpdateState.getDistributionReportError();
        errorMessages.add(newErrorMessage);
        currentUpdateState.setDistributionReportError(errorMessages);
    }

    private static String getPath(String fileName){
        return String.format("%s/%s",Environment.getDownloadCacheDirectory(), fileName);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void installApk(Context context, File file, String packageName, CountDownLatch countDownLatch, CurrentUpdateState currentUpdateState)
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
                currentUpdateState
        );
        installerSession.writeSession(file,  packageName);
        installerSession.commitSession();
    }

    private static String getPackageFromApk(Context context, String apkPath){
        PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(apkPath,PackageManager.GET_ACTIVITIES);
        if(packageInfo != null) {
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            return appInfo.packageName;
        }
        return null;
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

    private static boolean deleteFileIfExists(File file){
        try {
            if (file == null || !file.exists()) {
                return true;
            }
            return file.delete();
        } catch (SecurityException e){
            Log.d(TAG, String.format("Delete file named %s throw exception",file.getName()), e);
            return false;
        }
    }

    private static void logWarnIfDeletionFails(File file){
        if(!deleteFileIfExists(file)){
            Log.w(TAG, String.format("Deletion of %s is failed", file.getName()));
        }
    }

    @NonNull
    private static File getUpdateApkFolder(Context context) {
        return new File(context.getFilesDir(), UPDATE_APK_FOLDER);
    }
}
