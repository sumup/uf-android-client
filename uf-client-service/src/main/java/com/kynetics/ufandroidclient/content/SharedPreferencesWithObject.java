/*
 * Copyright © 2017   LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.ufandroidclient.content;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniele Sergio
 */
public class SharedPreferencesWithObject implements SharedPreferences{
    private final static String TAG = SharedPreferencesWithObject.class.getSimpleName();

    public Map<String, ?> getAll() {
        return sharedPreferences.getAll();
    }

    public String getString(String s, @Nullable String s1) {
        return sharedPreferences.getString(s, s1);
    }

    public Set<String> getStringSet(String s, @Nullable Set<String> set) {
        return sharedPreferences.getStringSet(s, set);
    }

    public int getInt(String s, int i) {
        return sharedPreferences.getInt(s, i);
    }

    public long getLong(String s, long l) {
        return sharedPreferences.getLong(s, l);
    }

    public float getFloat(String s, float v) {
        return sharedPreferences.getFloat(s, v);
    }

    public boolean getBoolean(String s, boolean b) {
        return sharedPreferences.getBoolean(s, b);
    }

    public boolean contains(String s) {
        return sharedPreferences.contains(s);
    }

    public SharedPreferences.Editor edit() {
        return sharedPreferences.edit();
    }

    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    final SharedPreferences sharedPreferences;

    public SharedPreferencesWithObject(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public <T extends Serializable> T getObject(String objKey, Class<T> clazz){
        return getObject(objKey, clazz,null);
    }
    public <T extends Serializable> T getObject(String objKey, Class<T> clazz, T defaultObj){
        byte[] bytes = sharedPreferences.getString(objKey, "").getBytes();
        if (bytes.length == 0) {
            return defaultObj;
        }
        try {
            ByteArrayInputStream byteArray = new ByteArrayInputStream(bytes);
            Base64InputStream base64InputStream = new Base64InputStream(byteArray, Base64.DEFAULT);
            ObjectInputStream in;
            in = new ObjectInputStream(base64InputStream);
            return (T) in.readObject();
        }catch (IOException ex){
            Log.e(TAG, ex.getMessage(), ex);
        } catch (ClassNotFoundException ex){
            Log.e(TAG, ex.getMessage(), ex);
        }
        return defaultObj;
    }

    public <T> void putAndCommitObject(String key, T obj){
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ObjectOutputStream objectOutput;
        try {
            objectOutput = new ObjectOutputStream(arrayOutputStream);
            objectOutput.writeObject(obj);
            byte[] data = arrayOutputStream.toByteArray();
            objectOutput.close();
            arrayOutputStream.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Base64OutputStream b64 = new Base64OutputStream(out, Base64.DEFAULT);
            b64.write(data);
            b64.close();
            out.close();

            ed.putString(key, new String(out.toByteArray()));

            ed.commit();
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage(), ex);
        }
    }
}
