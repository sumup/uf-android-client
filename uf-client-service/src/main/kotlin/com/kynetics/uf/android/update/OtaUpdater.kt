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
import android.os.RecoverySystem
import android.util.Log
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import java.io.File
import java.io.IOException

class OtaUpdater(context: Context) : AndroidUpdater(context) {

    companion object {
        val TAG: String = OtaUpdater::class.java.simpleName
    }

    override fun requiredSoftwareModulesAndPriority(swModules: Set<Updater.SwModule>): Updater.SwModsApplication {
        return Updater.SwModsApplication(0,
                swModules
                        .filter { it.type == "os" }
                        .map { Updater.SwModsApplication.SwModule(
                                it.type,
                                it.name,
                                it.version,
                                it.artifacts.map { a -> a.hashes }.toSet()) }.toSet())
    }

    private val maxMessageForState = 49

    override fun applyUpdate(modules: Set<Updater.SwModuleWithPath>, messenger: Updater.Messenger): Updater.UpdateResult {
        val currentUpdateState = CurrentUpdateState(context)
        val updateDetails = mutableListOf<String>()
        val success = modules.dropWhile {
            Log.d(TAG, "apply module ${it.name} ${it.version} of type ${it.type}")
            it.artifacts.dropWhile { a ->
                Log.d(TAG, "install artifact ${a.filename} from file ${a.path}")
                    val installationResult = installOta(a, currentUpdateState, messenger)
                    updateDetails.addAll(installationResult.details)
                    if (currentUpdateState.isFeebackReliable()) {
                        updateDetails.add("Final feedback message is reliable")
                        val lastLog = currentUpdateState.parseLastLogFile()
                        sendLastLogAsFeedback(lastLog, messenger, installationResult)
                    } else {
                        updateDetails.add("Can't read ${CurrentUpdateState.LAST_LOG_FILE_NAME}, the final feedback message could be unreliable")
                    }
                    installationResult is CurrentUpdateState.InstallationResult.Success
            }.isEmpty()
        }.isEmpty()
        return Updater.UpdateResult(success = success, details = updateDetails)
    }

    private fun sendLastLogAsFeedback(lastLog: List<String>, messenger: Updater.Messenger, installationResult: CurrentUpdateState.InstallationResult) {
        if (installationResult is CurrentUpdateState.InstallationResult.Success) {
            return
        }
        for (i in 0..lastLog.size / maxMessageForState) {
            val message = lastLog.subList(i * maxMessageForState, Math.min(i * maxMessageForState + maxMessageForState, lastLog.size))
            messenger.sendMessageToServer("${CurrentUpdateState.LAST_LOG_FILE_NAME} - $i", *message.toTypedArray())
        }
    }

    private fun verify(artifact: Updater.SwModuleWithPath.Artifact): Boolean {
        return try {
            val packageFile = File(artifact.path)
            RecoverySystem.verifyPackage(packageFile, null, null)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Corrupted package", e)
            false
        }
    }

    private fun installOta(
        artifact: Updater.SwModuleWithPath.Artifact,
        currentUpdateState: CurrentUpdateState,
        messenger: Updater.Messenger
    ): CurrentUpdateState.InstallationResult {

        val installationState = currentUpdateState.getOtaInstallationState(artifact)
        return when {
            installationState == CurrentUpdateState.InstallationState.PENDING -> {
                val result = currentUpdateState.lastIntallationResult(artifact)
                val message = "Installation result of Ota named ${artifact.filename} is ${if (result is CurrentUpdateState.InstallationResult.Success) "success" else "failure"}"
                messenger.sendMessageToServer(message + result.details)
                Log.i(TAG, message)
                result
            }

            installationState == CurrentUpdateState.InstallationState.SUCCESS -> {
                CurrentUpdateState.InstallationResult.Success()
            }

            installationState == CurrentUpdateState.InstallationState.ERROR -> {
                CurrentUpdateState.InstallationResult.Error(listOf("Installation of ${artifact.filename} is failed"))
            }

            verify(artifact) -> {
                val packageFile = File(artifact.path)
                currentUpdateState.addPendingOTAInstallation(artifact) // todo handle error on file creation
                messenger.sendMessageToServer("Applying ota update (${artifact.filename})...")
                return try {
                    RecoverySystem.installPackage(context, packageFile)
                    CurrentUpdateState.InstallationResult.Error(listOf("Error, installation package doesn't return"))
                } catch (ioe: IOException) {
                    CurrentUpdateState.InstallationResult.Error(listOf("Error, unable to reboot in recovery mode",
                            ioe.message ?: ""))
                }
            }

            else -> {
                val message = "Wrong ota signature"
                messenger.sendMessageToServer(message)
                Log.w(TAG, message)
                CurrentUpdateState.InstallationResult.Error(listOf(message))
            }
        }
    }
}
