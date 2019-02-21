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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;

import com.kynetics.uf.android.BuildConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.SERVICE_PACKAGE_NAME;

public class CurrentUpdateState {

    private static final String SHARED_PREFERENCES_FILE_NAME = "CURRENT_UPDATE_STATE";
    private static final String OTA_IS_FOUND_KEY = "OTA_IS_FOUND";
    private static final String APK_IS_FOUND_KEY = "APK_IS_FOUND";
    private static final String UF_SERVICE_IS_UPDATED_KEY = "UF_SERVICE_IS_UPDATED";
    private static final String APK_ALREADY_INSTALLED_KEY = "APK_ALREADY_INSTALLED";
    private static final String APK_DISTRIBUTION_REPORT_SUCCESS_KEY = "APK_DISTRIBUTION_REPORT_SUCCESS";
    private static final String APK_DISTRIBUTION_REPORT_ERROR_KEY = "APK_DISTRIBUTION_REPORT_ERROR";

    private final SharedPreferences sharedPreferences;


    public CurrentUpdateState(Context context) {
        this.sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE);
    }

    public boolean getOtaIsFound(){
        return sharedPreferences.getBoolean(OTA_IS_FOUND_KEY, false);
    }

    public void setOtaIsFoundToTrue(){
        sharedPreferences.edit().putBoolean(OTA_IS_FOUND_KEY, true).apply();
    }

    public boolean getApkIsFound(){
        return sharedPreferences.getBoolean(APK_IS_FOUND_KEY, false);
    }

    public void setApkIsFoundTrue(){
        sharedPreferences.edit().putBoolean(APK_IS_FOUND_KEY, true).apply();
    }

    public boolean isUfServiceUpdated(){
        return sharedPreferences.getBoolean(UF_SERVICE_IS_UPDATED_KEY, false);
    }

    public void setUpdateUpdated(boolean isUpdated){
        sharedPreferences.edit().putBoolean(UF_SERVICE_IS_UPDATED_KEY,isUpdated).apply();
    }

    public void incrementApkAlreadyInstalled(){
        sharedPreferences.edit().putInt(APK_ALREADY_INSTALLED_KEY, getApkAlreadyInstalled() + 1).apply();
    }

    public int getApkAlreadyInstalled(){
        return sharedPreferences.getInt(APK_ALREADY_INSTALLED_KEY, 0);
    }

    public Set<String> getDistributionReportError(){
        return sharedPreferences.getStringSet(APK_DISTRIBUTION_REPORT_ERROR_KEY, new HashSet<>());
    }

    public void setDistributionReportError(Set<String> messages){
        sharedPreferences.edit().putStringSet(APK_DISTRIBUTION_REPORT_ERROR_KEY, messages).apply();
    }

    public Set<String> getDistributionReportSuccess(){
        return sharedPreferences.getStringSet(APK_DISTRIBUTION_REPORT_SUCCESS_KEY, new HashSet<>());
    }

    public void setDistributionReportSuccess(Set<String> messages){
        sharedPreferences.edit().putStringSet(APK_DISTRIBUTION_REPORT_SUCCESS_KEY, messages).apply();
    }




    public void clearState(){
        sharedPreferences.edit()
                .remove(OTA_IS_FOUND_KEY)
                .remove(APK_IS_FOUND_KEY)
                .remove(UF_SERVICE_IS_UPDATED_KEY)
                .remove(APK_ALREADY_INSTALLED_KEY)
                .remove(APK_DISTRIBUTION_REPORT_SUCCESS_KEY)
                .remove(APK_DISTRIBUTION_REPORT_ERROR_KEY)
                .apply();
    }

}
