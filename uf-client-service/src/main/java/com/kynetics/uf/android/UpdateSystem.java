/*
 * Copyright © 2017   LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android;

import android.content.Context;
import android.os.Environment;
import android.os.RecoverySystem;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * @author Daniele Sergio
 */
public class UpdateSystem {
    private static final String TAG = UpdateSystem.class.getSimpleName();

    private static String OTA_FILE_NAME = "update.zip";

    public static boolean copyFile(InputStream inputStream){
        final File packageFile = new File(getOtaPath());
        if (packageFile.exists()) {
            packageFile.delete();
        }

        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        try (FileChannel dest = (new FileOutputStream(packageFile)).getChannel();
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
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to copy update file into internal storage: " + e);
            return false;
        }
        return true;
    }

    public static boolean verify(){
        try {
            File packageFile = new File(getOtaPath());
            RecoverySystem.verifyPackage(packageFile, null, null);
            return true;
        }
        catch (Exception e) {
            Log.e(TAG, "Corrupted package: " + e);
            return false;
        }
    }

    public static void install(Context context){
        try {
            File packageFile = new File(getOtaPath());
            RecoverySystem.installPackage(context, packageFile);
        }
        catch (Exception e) {
            Log.e(TAG, "Error install package: " + e);
        }
    }

    public static boolean successInstallation() {
        try (BufferedReader fileInputStream = new BufferedReader(new FileReader(new File("/cache/recovery", "last_install")))) {
            fileInputStream.readLine();
            return Integer.parseInt(fileInputStream.readLine()) == 1;
        }catch (IOException exception){
            Log.e(TAG, "installation error", exception);
            return false;
        }
    }

    private static String getOtaPath(){
        return String.format("%s/%s",Environment.getDownloadCacheDirectory(), OTA_FILE_NAME);
    }


}
