/*
 * Copyright © 2017-2020  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.configuration

import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Log
import com.kynetics.uf.android.BuildConfig
import com.kynetics.uf.android.R
import com.kynetics.uf.android.UpdateFactoryService
import com.kynetics.uf.android.api.Communication
import com.kynetics.uf.android.api.UFServiceConfiguration
import com.kynetics.uf.android.communication.MessengerHandler
import com.kynetics.uf.android.content.SharedPreferencesWithObject
import com.kynetics.uf.android.content.UFSharedPreferences
import com.kynetics.uf.android.update.CurrentUpdateState
import com.kynetics.uf.android.update.SystemUpdateType
import com.kynetics.uf.android.update.application.ApkUpdater
import com.kynetics.uf.android.update.system.OtaUpdater
import com.kynetics.updatefactory.ddiclient.core.UpdateFactoryClientDefaultImpl
import com.kynetics.updatefactory.ddiclient.core.api.*
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.Locale

data class ConfigurationHandler(
    private var ufService: UpdateFactoryClient?,
    private val context: UpdateFactoryService,
    private val sharedPreferences: UFSharedPreferences
) {

    fun getConfigurationFromFile(): UFServiceConfiguration? = configurationFile.newFileConfiguration

    fun getServiceConfigurationFromIntent(intent: Intent): UFServiceConfiguration? {
        Log.i(TAG, "Loaded new configuration from intent")
        val serializable = intent.getSerializableExtra(Communication.V1.SERVICE_DATA_KEY)
        val string = intent.getStringExtra(Communication.V1.SERVICE_DATA_KEY)
        return try {
            when {

                serializable is String -> UFServiceConfiguration.fromJson(serializable)

                serializable is UFServiceConfiguration -> serializable

                string != null -> UFServiceConfiguration.fromJson(string)

                else -> null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Deserialization error", e)
            null
        }
    }

    fun saveServiceConfigurationToSharedPreferences(
        configuration: UFServiceConfiguration?
    ) {
        if (configuration == null) {
            return
        }
        sharedPreferences.edit().apply {
            putString(sharedPreferencesControllerIdKey, configuration.controllerId)
            putString(sharedPreferencesTenantKey, configuration.tenant)
            putString(sharedPreferencesServerUrlKey, configuration.url)
            putString(sharedPreferencesGatewayToken, configuration.gatewayToken)
            putString(sharedPreferencesTargetToken, configuration.targetToken)
            putLong(sharedPreferencesRetryDelayKey, configuration.retryDelay)
            putBoolean(sharedPreferencesApiModeKey, configuration.isApiMode())
            putBoolean(sharedPreferencesServiceEnableKey, configuration.isEnable())
            putBoolean(sharedPreferencesIsUpdateFactoryServerType, configuration.isUpdateFactoryServe)
            apply()
        }

        sharedPreferences.putAndCommitObject(sharedPreferencesTargetAttributes, configuration.targetAttributes)
    }

    fun getCurrentConfiguration(): UFServiceConfiguration {
        val serviceIsEnable = ufService != null && sharedPreferences.getBoolean(sharedPreferencesServiceEnableKey, false)
        val url = sharedPreferences.getString(sharedPreferencesServerUrlKey, "")
        val controllerId = sharedPreferences.getString(sharedPreferencesControllerIdKey, "")
        val gatewayToken = sharedPreferences.getString(sharedPreferencesGatewayToken, "")
        val targetToken = sharedPreferences.getString(sharedPreferencesTargetToken, "")
        val tenant = sharedPreferences.getString(sharedPreferencesTenantKey, "")
        val delay = sharedPreferences.getLong(sharedPreferencesRetryDelayKey, 900000)
        val apiMode = sharedPreferences.getBoolean(sharedPreferencesApiModeKey, true)
        val targetAttributes: MutableMap<String, String>? = getTargetAttributes()
        val serverType = getServerType()
        return UFServiceConfiguration.builder()
                .withTargetAttributes(targetAttributes)
                .withEnable(serviceIsEnable)
                .withApiMode(apiMode)
                .withControllerId(controllerId)
                .withGatewayToken(gatewayToken)
                .withRetryDelay(delay)
                .withTargetToken(targetToken)
                .withTenant(tenant)
                .withIsUpdateFactoryServer(serverType == UpdateFactoryClientData.ServerType.UPDATE_FACTORY)
                .withUrl(url)
                .build()
    }

    fun apiModeIsEnabled() = sharedPreferences.getBoolean(sharedPreferencesApiModeKey, false)

    fun buildServiceFromPreferences(
        deploymentPermitProvider: DeploymentPermitProvider,
        listeners: List<MessageListener>,
        currentService: UpdateFactoryClient?
    ): UpdateFactoryClient? {
        currentService?.stop()
        val serviceConfiguration = getCurrentConfiguration()
        if (sharedPreferences.getBoolean(sharedPreferencesServiceEnableKey, false)) {
            try {
                val newService = serviceConfiguration.toService(deploymentPermitProvider, listeners)
                newService.startAsync()
                ufService = newService
                return newService
            } catch (e: RuntimeException) {
                ufService = null
                // sharedPreferences.edit().putBoolean(sharedPreferencesServiceEnableKey, false).apply()
                MessengerHandler.onConfigurationError(listOf(e.message ?: "Error"))
                MessengerHandler.sendMessage(Communication.V1.Out.ServiceNotification.ID)
                Log.e(TAG, e.message, e)
            }
        }
        return ufService
    }

    fun needReboot(newConf: UFServiceConfiguration): Boolean {
        val currentConf = getCurrentConfiguration()
        return currentConf.copy(targetAttributes = emptyMap()) != newConf.copy(targetAttributes = emptyMap())
    }

    private fun buildConfigDataProvider(): ConfigDataProvider {
        return object : ConfigDataProvider {
            override fun configData(): Map<String, String> {
                return decorateTargetAttribute()
            }

            override fun isUpdated(): Boolean {
                val md5 = decorateTargetAttribute().toMD5()
                return md5 == sharedPreferences.getString(LAST_TARGET_ATTRIBUTES_MD5_SENT_KEY, "")
            }

            override fun onConfigDataUpdate() {
                val md5 = decorateTargetAttribute().toMD5()
                sharedPreferences.edit().putString(LAST_TARGET_ATTRIBUTES_MD5_SENT_KEY, md5)
                        .apply()
            }
        }
    }

    private fun getServerType(): UpdateFactoryClientData.ServerType {
        return if (sharedPreferences.getBoolean(sharedPreferencesIsUpdateFactoryServerType, true)) {
            UpdateFactoryClientData.ServerType.UPDATE_FACTORY
        } else {
            UpdateFactoryClientData.ServerType.HAWKBIT
        }
    }

    private fun getTargetAttributes(): MutableMap<String, String> {
        val targetAttributes: MutableMap<String, String>? = sharedPreferences
                .getObject<HashMap<String, String>>(
                        sharedPreferencesTargetAttributes,
                        HashMap<String, String>().javaClass
                )
        return targetAttributes ?: HashMap()
    }

    private fun decorateTargetAttribute(): Map<String, String> {
        val targetAttributes = getTargetAttributes()
        targetAttributes[CLIENT_TYPE_TARGET_TOKEN_KEY] = "Android"
        targetAttributes[CLIENT_VERSION_TARGET_ATTRIBUTE_KEY] = BuildConfig.VERSION_NAME // TODO: 4/17/18 refactor
        targetAttributes[CLIENT_VERSION_CODE_ATTRIBUTE_KEY] = BuildConfig.VERSION_CODE.toString()
        val buildDate = Date(Build.TIME)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.UK)
        targetAttributes[ANDROID_BUILD_DATE_TARGET_ATTRIBUTE_KEY] = dateFormat.format(buildDate)
        targetAttributes[ANDROID_BUILD_TYPE_TARGET_ATTRIBUTE_KEY] = Build.TYPE
        targetAttributes[ANDROID_FINGERPRINT_TARGET_ATTRIBUTE_KEY] = Build.FINGERPRINT
        targetAttributes[ANDROID_KEYS_TARGET_ATTRIBUTE_KEY] = Build.TAGS
        targetAttributes[ANDROID_VERSION_TARGET_ATTRIBUTE_KEY] = Build.VERSION.RELEASE
        targetAttributes[DEVICE_NAME_TARGET_ATTRIBUTE_KEY] = Build.DEVICE
        targetAttributes[SYSTEM_UPDATE_TYPE] = systemUpdateType!!.name
        return targetAttributes
    }

    private fun Map<String, String>.toMD5(): String {
        val content = entries.sortedBy { it.key }.joinToString("-") { "${it.key}_${it.value}" }
        val bytes = MessageDigest.getInstance("MD5").digest(content.toByteArray())
        return bytes.toMD5()
    }

    private fun ByteArray.toMD5(): String {
        return this.joinToString("") { "%02x".format(it) }
    }

    private fun UFServiceConfiguration.toService(
        deploymentPermitProvider: DeploymentPermitProvider,
        listeners: List<MessageListener>
    ): UpdateFactoryClient {
        val newUfService = UpdateFactoryClientDefaultImpl()
        newUfService.init(
                toClientData(),
                object : DirectoryForArtifactsProvider {
                    override fun directoryForArtifacts(): File = currentUpdateState.rootDir()
                },
                buildConfigDataProvider(),
                deploymentPermitProvider,
                listeners,
            OtaUpdater(context),
            ApkUpdater(context)
        )
        return newUfService
    }

    private fun UFServiceConfiguration.toClientData(): UpdateFactoryClientData {
        return UpdateFactoryClientData(
                tenant,
                controllerId,
                url,
                if (isUpdateFactoryServe) {
                    UpdateFactoryClientData.ServerType.UPDATE_FACTORY
                } else {
                    UpdateFactoryClientData.ServerType.HAWKBIT
                },
                gatewayToken,
                targetToken,
                object:TargetTokenFoundListener{
                    override fun onFound(targetToken: String): () -> Unit {
                        Log.d(TAG, "New target token received")
                        sharedPreferences.edit()
                                .putString(sharedPreferencesTargetToken, targetToken)
                                .apply()
                        return {}
                    }
                }
        )
    }

    private var systemUpdateType: SystemUpdateType? = SystemUpdateType.getSystemUpdateType()
    private val configurationFile = ConfigurationFileLoader(sharedPreferences, UF_CONF_FILE, context)

    private val sharedPreferencesServerUrlKey = context.getString(R.string.shared_preferences_server_url_key)
    private val sharedPreferencesApiModeKey = context.getString(R.string.shared_preferences_api_mode_key)
    private val sharedPreferencesTenantKey = context.getString(R.string.shared_preferences_tenant_key)
    private val sharedPreferencesControllerIdKey = context.getString(R.string.shared_preferences_controller_id_key)
    private val sharedPreferencesRetryDelayKey = context.getString(R.string.shared_preferences_retry_delay_key)
    private val sharedPreferencesServiceEnableKey = context.getString(R.string.shared_preferences_is_enable_key)
    private val sharedPreferencesGatewayToken = context.getString(R.string.shared_preferences_gateway_token_key)
    private val sharedPreferencesTargetToken = context.getString(R.string.shared_preferences_target_token_key)
    private val sharedPreferencesTargetAttributes = context.getString(R.string.shared_preferences_args_key)
    private val sharedPreferencesIsUpdateFactoryServerType = context.getString(R.string.shared_preferences_is_update_factory_server_type_key)
    private val currentUpdateState: CurrentUpdateState = CurrentUpdateState(context)

    companion object {
        private const val LAST_TARGET_ATTRIBUTES_MD5_SENT_KEY = "LAST_TARGET_ATTRIBUTES_MD5_SET_KEY"
        private const val CLIENT_VERSION_TARGET_ATTRIBUTE_KEY = "client_version"
        private const val CLIENT_VERSION_CODE_ATTRIBUTE_KEY = "client_version_code"
        private const val ANDROID_BUILD_DATE_TARGET_ATTRIBUTE_KEY = "android_build_date"
        private const val ANDROID_BUILD_TYPE_TARGET_ATTRIBUTE_KEY = "android_build_type"
        private const val ANDROID_FINGERPRINT_TARGET_ATTRIBUTE_KEY = "android_fingerprint"
        private const val ANDROID_KEYS_TARGET_ATTRIBUTE_KEY = "android_keys"
        private const val ANDROID_VERSION_TARGET_ATTRIBUTE_KEY = "android_version"
        private const val DEVICE_NAME_TARGET_ATTRIBUTE_KEY = "device_name"
        private const val SYSTEM_UPDATE_TYPE = "system_update_type"
        private const val CLIENT_TYPE_TARGET_TOKEN_KEY = "client"
        private val EXTERNAL_STORAGE_DIR = Environment.getExternalStorageDirectory().path
        private val UF_CONF_FILE = "$EXTERNAL_STORAGE_DIR/UpdateFactoryConfiguration/ufConf.conf"
        private val TAG: String = ConfigurationHandler::class.java.simpleName
    }
}
