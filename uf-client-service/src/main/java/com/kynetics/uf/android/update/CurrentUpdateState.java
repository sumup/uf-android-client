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

import com.kynetics.uf.android.BuildConfig;

import static android.content.Context.MODE_PRIVATE;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.SERVICE_PACKAGE_NAME;

public class CurrentUpdateState {

    private static final String SHARED_PREFERENCES_FILE_NAME = "CURRENT_UPDATE_STATE";
    private static final String OTA_IS_FOUND_KEY = "OTA_IS_FOUND";
    private static final String APK_IS_FOUND_KEY = "APK_IS_FOUND";

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

    public void resetApkOsFlag(){
        sharedPreferences.edit()
                .remove(OTA_IS_FOUND_KEY)
                .remove(APK_IS_FOUND_KEY)
                .apply();
    }
}
