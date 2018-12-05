/*
 * Copyright © 2017-2018  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.configuration;

import android.content.Context;
import android.content.SharedPreferences;

import com.kynetics.uf.android.api.UFServiceConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniele Sergio
 */

public class ConfigurationFileLoader {
    private static final String SHARED_PREFERENCES_LAST_CONFIGURATION_FILE_KEY = "LAST_CONFIGURATION_FILE_KEY";

    private static final String SPLIT_CHAR = "=";
    private static final String TENANT_CONFIGURATION_KEY = "tenant";
    private static final String URL_CONFIGURATION_KEY = "url";
    private static final String CONTROLLER_ID_CONFIGURATION_KEY = "controllerId";
    private static final String GATEWAY_TOKEN_CONFIGURATION_KEY = "gatewayToken";
    private static final String TARGET_TOKEN_CONFIGURATION_KEY = "targetToken";
    private static final String API_MODE_CONFIGURATION_KEY = "apiMode";
    private static final String ENABLE_CONFIGURATION_KEY = "enable";
    private static final String IS_UPDATE_FACTORY_SERVER_KEY = "updateFactoryServer";

    private final Map<String,String> map = new HashMap<>();

    private final SharedPreferences sh;
    private final String configurationFilePath;
    private final Context context;

    public ConfigurationFileLoader(SharedPreferences sh, String configurationFilePath, Context context) {
        this.sh = sh;
        this.configurationFilePath = configurationFilePath;
        this.context = context;
    }

    public UFServiceConfiguration getNewFileConfiguration(){
        if(!configurationFileFound() || !parseFile()){
            return null;
        }
        UFServiceConfiguration.Builder builder =  UFServiceConfiguration.builder()
                .withEnable(getBooleanConfiguration(ENABLE_CONFIGURATION_KEY))
                .withApiMode(getApiModeConfiguration())
                .withIsUpdateFactoryServer(getBooleanConfiguration(IS_UPDATE_FACTORY_SERVER_KEY))
                .withGetawayToken(map.get(GATEWAY_TOKEN_CONFIGURATION_KEY))
                .withTenant(map.get(TENANT_CONFIGURATION_KEY))
                .withTargetToken(map.get(TARGET_TOKEN_CONFIGURATION_KEY))
                .withRetryDelay(30_000)
                .withControllerId(VariableEvaluation.Companion.parseStringWithVariable(map.get(CONTROLLER_ID_CONFIGURATION_KEY), context))
                .withUrl(map.get(URL_CONFIGURATION_KEY));

        return builder.configurationIsValid() ? builder.build() : null;
    }

    private boolean getApiModeConfiguration(){
        final String value = map.get(API_MODE_CONFIGURATION_KEY);
        return value != null && value.equalsIgnoreCase("TRUE");
    }

    private boolean getBooleanConfiguration(String mapKey){
        final String value = map.get(mapKey);
        return value == null || !value.equalsIgnoreCase("FALSE");
    }

    private boolean parseFile(){
        try (final BufferedReader br = new BufferedReader(new FileReader(configurationFilePath))) {
            String line;
            final MessageDigest md5digest = getDigest();
            while ((line = br.readLine()) != null) {
                md5digest.update(line.getBytes());
                final String[] keyValue = line.split(SPLIT_CHAR);
                if(keyValue.length == 2){
                    map.put(keyValue[0].trim(),keyValue[1].trim());
                }
            }
            final BigInteger bigInt = new BigInteger(1,md5digest.digest());
            final String hashtext = bigInt.toString(16);
            final String md5LastConfigurationFileLoaded = sh.getString(SHARED_PREFERENCES_LAST_CONFIGURATION_FILE_KEY, "");
            sh.edit().putString(SHARED_PREFERENCES_LAST_CONFIGURATION_FILE_KEY, hashtext).apply();
            return !hashtext.equals(md5LastConfigurationFileLoaded);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return true;

    }

    private boolean configurationFileFound(){
        final File file = new File(configurationFilePath);
        if(!file.exists()){
            map.clear();
            return false;
        }
        return true;
    }

    private MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        }catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return null;
    }

}
