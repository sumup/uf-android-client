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

package com.kynetics.uf.android.update

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Environment
import android.os.SystemProperties
import android.util.Log
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import java.io.File
import java.util.*

class CurrentUpdateState(context: Context) {

    private val sharedPreferences: SharedPreferences

    val distributionReportError: Set<String>
        get() = sharedPreferences.getStringSet(APK_DISTRIBUTION_REPORT_ERROR_KEY, HashSet())!!

    val distributionReportSuccess: Set<String>
        get() = sharedPreferences.getStringSet(APK_DISTRIBUTION_REPORT_SUCCESS_KEY, HashSet())!!

    fun addErrorToRepor(vararg errors:String){
        val newDistReportError = distributionReportError.toMutableSet()
        newDistReportError.addAll(errors)
        sharedPreferences.edit().putStringSet(APK_DISTRIBUTION_REPORT_ERROR_KEY, newDistReportError).apply()
    }

    fun addSuccessMessageToRepor(vararg messages:String){
        val newDistReportSuccess = distributionReportSuccess.toMutableSet()
        newDistReportSuccess.addAll(messages)
        sharedPreferences.edit().putStringSet(APK_DISTRIBUTION_REPORT_SUCCESS_KEY, newDistReportSuccess).apply()
    }

    init {
        this.sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE)
    }


    fun setUFUpdated() {
        val file = currentInstallationDir()
                .listFiles()
                ?.firstOrNull { it.name.endsWith(SUCCESS_EXTENSION) || it.name.endsWith(ERROR_EXTENSION) }
        file?.renameTo(File("${file.absolutePath}.$SUCCESS_EXTENSION"))
    }

    fun rootDir():File = File(Environment.getDownloadCacheDirectory(), "update_factory")

    private fun currentInstallationDir():File = File(rootDir(), "current_installation")

    private fun previousInstallationDir():File = File(rootDir(), "last_installation")

    fun lastIntallationResult(artifact: Updater.SwModuleWithPath.Artifact):InstallationResult{
        return try {
            val result = File(LAST_INSTALL_FILE).readLines()[1].trim()
            val response = when (result) {
                "1" -> InstallationResult()
                else -> InstallationResult(listOf("last_install result code: $result"))
            }

            persistArtifactInstallationResult(artifact, response)
            response
        } catch (e:Throwable){
            Log.e(TAG, e.message, e)
            InstallationResult(listOf("Installation fails with exception: ${e.message}"))
        }
    }

    fun startUpdate(){
        sharedPreferences.edit()
                .putBoolean(UPDATE_IS_STARTED_KEY, true)
                .apply()
    }

    fun isUpdateStart():Boolean{
        return sharedPreferences.getBoolean(UPDATE_IS_STARTED_KEY, false)
    }

    fun addPendingInstallation(artifact: Updater.SwModuleWithPath.Artifact){
        val file = getPendingInstallationFile(artifact)
        if(!file.exists()){
            file.parentFile.mkdirs()
            file.createNewFile()
        }

    }

    fun persistArtifactInstallationResult(artifact: Updater.SwModuleWithPath.Artifact,
                                          result: InstallationResult){
        val file = getPendingInstallationFile(artifact)
        if(!file.exists()){
            throw IllegalStateException("File named ${file.name} must exist")
        }

        val destinationFile = File(file.parentFile,"${file.name}.${if(result.success) SUCCESS_EXTENSION else ERROR_EXTENSION}")
        file.renameTo(destinationFile)
        result.errors.forEach { destinationFile.appendText(it) }

    }

    fun artifactInstallationState(artifact: Updater.SwModuleWithPath.Artifact): ArtifacInstallationState{
        val pendingInstallationFile = getPendingInstallationFile(artifact)
        return when{
            pendingInstallationFile.exists() -> ArtifacInstallationState.PENDING
            File("${pendingInstallationFile.absolutePath}.$SUCCESS_EXTENSION").exists() -> ArtifacInstallationState.SUCCESS
            File("${pendingInstallationFile.absolutePath}.$ERROR_EXTENSION").exists() -> ArtifacInstallationState.ERROR
            else -> ArtifacInstallationState.NONE
        }
    }

    enum class ArtifacInstallationState{
        PENDING, NONE, SUCCESS, ERROR
    }

    private fun getPendingInstallationFile(artifact: Updater.SwModuleWithPath.Artifact) =
            File(currentInstallationDir(), artifact.hashes.md5)


    data class InstallationResult(val errors:List<String> = emptyList()){
        val success = errors.isEmpty()
    }

    fun clearState() {
        previousInstallationDir().deleteRecursively()
        currentInstallationDir().renameTo(previousInstallationDir())
        sharedPreferences.edit()
                .remove(UF_SERVICE_IS_UPDATED_KEY)
                .remove(APK_DISTRIBUTION_REPORT_SUCCESS_KEY)
                .remove(APK_DISTRIBUTION_REPORT_ERROR_KEY)
                .remove(LAST_SLOT_NAME_SHAREDPREFERENCES_KEY)
                .remove(UPDATE_IS_STARTED_KEY)
                .apply()
    }

    fun saveSlotName(){
        val partionSlotName = SystemProperties.get(LAST_LOST_NAME_PROPERTY_KEY)
        Log.d(TAG, "Using slot named: $partionSlotName")
        sharedPreferences.edit()
                .putString(LAST_SLOT_NAME_SHAREDPREFERENCES_KEY, partionSlotName)
                .apply()
    }

    //todo refactor
    fun lastABIntallationResult(artifact: Updater.SwModuleWithPath.Artifact):InstallationResult{
        return try {
            val currentSlotName = SystemProperties.get(LAST_LOST_NAME_PROPERTY_KEY)
            val previousSlotName =  sharedPreferences.getString(LAST_SLOT_NAME_SHAREDPREFERENCES_KEY, "")
            Log.d(TAG, "(current slot named, previous slot name) ($currentSlotName,$previousSlotName)")
            val success = previousSlotName !=  currentSlotName
            val response = if(success){InstallationResult()} else { InstallationResult(listOf("System reboot on the same partition"))}
            persistArtifactInstallationResult(artifact, response)
            response
        } catch (e:Throwable){
            Log.e(TAG, e.message, e)
            InstallationResult(listOf("Installation fails with exception: ${e.message}"))
        }
    }

    companion object {
        private const val UPDATE_IS_STARTED_KEY = "UPDATE_IS_STARTED"
        private const val LAST_INSTALL_FILE = "cache/recovery/last_install"
        private const val LAST_LOST_NAME_PROPERTY_KEY = "ro.boot.slot_suffix"
        private const val LAST_SLOT_NAME_SHAREDPREFERENCES_KEY = "slot_suffix"
        private const val TAG = "CurrentUpdateState"
        private val SHARED_PREFERENCES_FILE_NAME = "CURRENT_UPDATE_STATE"
        private val UF_SERVICE_IS_UPDATED_KEY = "UF_SERVICE_IS_UPDATED"
        private val APK_ALREADY_INSTALLED_KEY = "APK_ALREADY_INSTALLED"
        private val APK_DISTRIBUTION_REPORT_SUCCESS_KEY = "APK_DISTRIBUTION_REPORT_SUCCESS"
        private val APK_DISTRIBUTION_REPORT_ERROR_KEY = "APK_DISTRIBUTION_REPORT_ERROR"
        private const val SUCCESS_EXTENSION = "OK"
        private const val ERROR_EXTENSION = "KO"
    }

}
