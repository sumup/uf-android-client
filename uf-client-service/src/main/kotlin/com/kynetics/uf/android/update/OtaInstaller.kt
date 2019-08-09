package com.kynetics.uf.android.update

import android.content.Context
import com.kynetics.updatefactory.ddiclient.core.api.Updater

interface OtaInstaller {
    fun install(artifact: Updater.SwModuleWithPath.Artifact,
        currentUpdateState: CurrentUpdateState,
        messenger: Updater.Messenger,
        context: Context
    ): CurrentUpdateState.InstallationResult

    fun isFeedbackReliable(context: Context):Boolean = true
}