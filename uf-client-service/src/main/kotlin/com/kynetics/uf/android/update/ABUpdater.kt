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
import android.os.*
import android.os.UpdateEngine.ErrorCodeConstants.*
import android.util.Log
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import java.io.File
import android.support.annotation.RequiresApi
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import kotlin.streams.toList
import android.support.v4.content.ContextCompat.getSystemService
import android.os.PowerManager



@RequiresApi(Build.VERSION_CODES.O)
class ABUpdater(context: Context) : AndroidUpdater(context) {

    private val updateEngine: UpdateEngine = UpdateEngine()

    companion object {
        val TAG: String = ABUpdater::class.java.simpleName
        private const val PROPERTY_FILE = "payload_properties.txt"
        private const val PAYLOAD_FILE = "payload.bin"
        private val UPDATE_STATUS = mapOf(
                UpdateEngine.UpdateStatusConstants.IDLE to "Idle",
                UpdateEngine.UpdateStatusConstants.CHECKING_FOR_UPDATE to "Checking for update",
                UpdateEngine.UpdateStatusConstants.UPDATE_AVAILABLE to "Update available",
                UpdateEngine.UpdateStatusConstants.DOWNLOADING to "Copying file",
                UpdateEngine.UpdateStatusConstants.VERIFYING to "Verifying",
                UpdateEngine.UpdateStatusConstants.FINALIZING to "Finalizing",
                UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT to "Rebooting",
                UpdateEngine.UpdateStatusConstants.REPORTING_ERROR_EVENT to "Reporting error event",
                UpdateEngine.UpdateStatusConstants.ATTEMPTING_ROLLBACK to "Attempting rollback",
                UpdateEngine.UpdateStatusConstants.DISABLED to "Disable")
    }

    override fun requiredSoftwareModulesAndPriority(swModules: Set<Updater.SwModule>): Updater.SwModsApplication {
        return Updater.SwModsApplication(0,
                swModules
                        .filter { it.type == "os" /*&& it.metadata?.contains(Updater.SwModule.Metadata("UpdateType", "AB")) ?: false */}
                        .map {
                            Updater.SwModsApplication.SwModule(
                                    it.type,
                                    it.name,
                                    it.version,
                                    it.artifacts.map { a -> a.hashes }.toSet())
                        }.toSet())
    }

    override fun applyUpdate(modules: Set<Updater.SwModuleWithPath>, messenger: Updater.Messenger): Boolean {
        return modules.dropWhile {
            Log.d(TAG, "apply module ${it.name} ${it.version} of type ${it.type}")
            it.artifacts.dropWhile { a ->
                Log.d(TAG, "install artifact ${a.filename} from file ${a.path}")
                installOta(a, currentUpdateState, messenger).success
            }.isEmpty()
        }.isEmpty()
    }

    class MyUpdateEngineCallback(
            private val context: Context,
            private val messenger: Updater.Messenger,
            private val updateStatus: CompletableFuture<Int>) : UpdateEngineCallback() {
        var previosState = Int.MAX_VALUE

        override fun onStatusUpdate(i: Int, v: Float) {  //i==status  v==percent
            Log.d(TAG, "status:$i")
            Log.d(TAG, "percent:$v")
            if(previosState != i){
                previosState = i
                messenger.sendMessageToServer(UPDATE_STATUS.getValue(i))
            }
            //todo ask authorization before reboot (if not forced)
            if(i == UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT){
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
                pm!!.reboot(null)
            }
        }

        override fun onPayloadApplicationComplete(errorNum: Int) {
            Log.d(TAG, "onPayloadApplicationComplete: $errorNum")
            updateStatus.complete(errorNum)
        }
    }

    private fun installOta(artifact: Updater.SwModuleWithPath.Artifact,
                           currentUpdateState: CurrentUpdateState,
                           messenger: Updater.Messenger): CurrentUpdateState.InstallationResult {

        if(currentUpdateState.artifactInstallationState(artifact) == CurrentUpdateState.ArtifacInstallationState.PENDING){
            val result = currentUpdateState.lastABIntallationResult(artifact)
            val message = "Installation result of Ota named ${artifact.filename} is ${if(result.success) "success" else "failure"}"
            messenger.sendMessageToServer(message + result.errors)
            Log.i(TAG, message)
            return result
        }

        currentUpdateState.saveSlotName()
        val updateStatus = CompletableFuture<Int>()
        val updateDir = File(artifact.path).parentFile
        val zipFile = ZipFile(artifact.path)

        zipFile.getInputStream(zipFile.getEntry(PAYLOAD_FILE))
                .use{ input ->
                    File(updateDir, PAYLOAD_FILE).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

        val prop = zipFile.getInputStream(zipFile.getEntry(PROPERTY_FILE))
                .bufferedReader().lines().toList().toTypedArray()

        Log.d(TAG, prop.joinToString())

        updateEngine.bind(MyUpdateEngineCallback(context, messenger, updateStatus))
        currentUpdateState.addPendingInstallation(artifact)//todo handle error on file creation
        messenger.sendMessageToServer("Applying A/B ota update (${artifact.filename})...")
        val payloadPath = "file://${File(updateDir, PAYLOAD_FILE).absolutePath}"
        Log.d(TAG, payloadPath)
        updateEngine.applyPayload(payloadPath, 0, 0, prop)
        val result: Int = updateStatus.get(30, TimeUnit.MINUTES)
        updateEngine.unbind()


        return when(result){

             SUCCESS, UPDATED_BUT_NOT_ACTIVE -> {
                Log.d(TAG, "result: $result")
                CurrentUpdateState.InstallationResult()

            }

            else -> {
                Log.d(TAG, "result: $result")
                CurrentUpdateState.InstallationResult(listOf("error code: $result"))
            }

        }

    }
}