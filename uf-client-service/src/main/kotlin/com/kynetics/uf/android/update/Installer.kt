package com.kynetics.uf.android.update

import android.content.Context
import com.kynetics.updatefactory.ddiclient.core.api.Updater

interface Installer<out T> {
    fun install(
        artifact: Updater.SwModuleWithPath.Artifact,
        currentUpdateState: CurrentUpdateState,
        messenger: Updater.Messenger,
        context: Context
    ): T
}
