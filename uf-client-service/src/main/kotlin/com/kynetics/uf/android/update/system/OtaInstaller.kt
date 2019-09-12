package com.kynetics.uf.android.update.system

import android.content.Context
import com.kynetics.uf.android.update.CurrentUpdateState
import com.kynetics.uf.android.update.Installer

interface OtaInstaller : Installer<CurrentUpdateState.InstallationResult> {
    fun isFeedbackReliable(context: Context): Boolean = true
}
