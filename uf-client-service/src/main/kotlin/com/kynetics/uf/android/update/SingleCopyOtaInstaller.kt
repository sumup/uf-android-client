package com.kynetics.uf.android.update

import android.content.Context
import android.os.RecoverySystem
import android.util.Log
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import java.io.File
import java.io.IOException

internal object SingleCopyOtaInstaller : OtaInstaller {

    val TAG: String = OtaInstaller::class.java.simpleName

    override fun install(
        artifact: Updater.SwModuleWithPath.Artifact,
        currentUpdateState: CurrentUpdateState,
        messenger: Updater.Messenger,
        context: Context
    ): CurrentUpdateState.InstallationResult {
        val installationState = currentUpdateState.getOtaInstallationState(artifact)

        return when {
            installationState == CurrentUpdateState.InstallationState.PENDING ->
                onPending(currentUpdateState, artifact, messenger)

            installationState == CurrentUpdateState.InstallationState.SUCCESS ->
                CurrentUpdateState.InstallationResult.Success()

            installationState == CurrentUpdateState.InstallationState.ERROR ->
                CurrentUpdateState.InstallationResult.Error(listOf("Installation of ${artifact.filename} is failed"))

            verify(artifact) -> onVerified(artifact, currentUpdateState, messenger, context)

            else -> onWrongSignature(messenger)
        }
    }

    override fun isFeedbackReliable(context: Context): Boolean {
        return CurrentUpdateState(context).isFeebackReliable()
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

    private fun onWrongSignature(messenger: Updater.Messenger): CurrentUpdateState.InstallationResult.Error {
        val message = "Wrong ota signature"
        messenger.sendMessageToServer(message)
        Log.w(TAG, message)
        return CurrentUpdateState.InstallationResult.Error(listOf(message))
    }

    private fun onVerified(
        artifact: Updater.SwModuleWithPath.Artifact,
        currentUpdateState: CurrentUpdateState,
        messenger: Updater.Messenger,
        context: Context
    ): CurrentUpdateState.InstallationResult.Error {
        val packageFile = File(artifact.path)
        currentUpdateState.addPendingOTAInstallation(artifact) // todo handle error on file creation
        messenger.sendMessageToServer("Applying ota update (${artifact.filename})...")
        return try {
            RecoverySystem.installPackage(context, packageFile)
            CurrentUpdateState.InstallationResult.Error(listOf("Error, installation package doesn't return"))
        } catch (ioe: IOException) {
            CurrentUpdateState.InstallationResult.Error(
                listOf(
                    "Error, unable to reboot in recovery mode",
                    ioe.message ?: ""
                )
            )
        }
    }

    private fun onPending(
        currentUpdateState: CurrentUpdateState,
        artifact: Updater.SwModuleWithPath.Artifact,
        messenger: Updater.Messenger
    ): CurrentUpdateState.InstallationResult {
        val result = currentUpdateState.lastIntallationResult(artifact)
        val message =
            "Installation result of Ota named ${artifact.filename} is " +
                if (result is CurrentUpdateState.InstallationResult.Success) "success" else "failure"
        messenger.sendMessageToServer(message + result.details)
        Log.i(TAG, message)
        return result
    }
}
