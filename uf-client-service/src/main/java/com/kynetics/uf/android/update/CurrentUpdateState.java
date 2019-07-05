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

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

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
    private static final String APK_PACKAGE_START_KEY = "APK_PACKAGE";
    private static final String APK_PACKAGE_TEMPLATE_KEY = "APK_PACKAGE_%s_KEY";

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


    public Boolean isPackageInstallationTerminated(String packageName, Long versionCode){
        final String key = String.format(APK_PACKAGE_TEMPLATE_KEY, getPackageKey(packageName));
        final Long version = getVersion(versionCode);
        return sharedPreferences.getLong(key, version + 1) <= version;
    }

    @NotNull
    private String getPackageKey(String packageName) {
        return packageName == null ? "NULL" : packageName.replaceAll(".", "_");
    }

    @NotNull
    private Long getVersion(Long versionCode) {
        return versionCode == null ? 0 : versionCode;
    }

    public void packageInstallationTerminated(String packageName, Long versionCode){
        final String key = String.format(APK_PACKAGE_TEMPLATE_KEY, getPackageKey(packageName));
        sharedPreferences.edit()
                .putLong(key, getVersion(versionCode))
                .apply();
    }

    public boolean existPackgeKey(){
        for(String key: sharedPreferences.getAll().keySet()){
            if(key.startsWith(APK_PACKAGE_START_KEY)){
                return true;
            }
        }
        return false;
    }

    public void clearState(){
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        final Set<String> keys = sharedPreferences.getAll().keySet();
        for(String key: keys){
            if(key.startsWith(APK_PACKAGE_START_KEY)){
                editor.remove(key);
            }
        }
        editor
                .remove(OTA_IS_FOUND_KEY)
                .remove(APK_IS_FOUND_KEY)
                .remove(UF_SERVICE_IS_UPDATED_KEY)
                .remove(APK_ALREADY_INSTALLED_KEY)
                .remove(APK_DISTRIBUTION_REPORT_SUCCESS_KEY)
                .remove(APK_DISTRIBUTION_REPORT_ERROR_KEY)
                .apply();
    }

}
