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
import android.content.pm.PackageManager
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import java.io.File
import java.util.concurrent.CountDownLatch
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo



class ApkUpdater(context: Context) : AndroidUpdater(context) {

    companion object {
        val TAG:String = ApkUpdater::class.java.simpleName
    }

    override fun requiredSoftwareModulesAndPriority(swModules: Set<Updater.SwModule>): Updater.SwModsApplication {

        return Updater.SwModsApplication( 1,
                swModules
                        .filter { it.type == "bApp" }
                        .map { Updater.SwModsApplication.SwModule(
                                it.type,
                                it.name,
                                it.version,
                                it.artifacts
                                        .filter { a->a.filename.endsWith(".apk", true) }
                                        .map { a -> a.hashes }
                                        .toSet())
                        }.toSet())
    }


    override fun applyUpdate(modules: Set<Updater.SwModuleWithPath>, messenger: Updater.Messenger): Updater.UpdateResult {
        if(Build.VERSION.SDK_INT <  Build.VERSION_CODES.LOLLIPOP) {
            val errorMessage = "Installation of apk is not supported from device with android system " +
                    "com.kynetics.uf.android.api lower than ${Build.VERSION_CODES.LOLLIPOP} (current is ${Build.VERSION.SDK_INT})"
            messenger.sendMessageToServer(errorMessage)
            Log.w(TAG,errorMessage)
            return Updater.UpdateResult(
                    success = false,
                    details = listOf(errorMessage)
            )
        }

        val currentUpdateState = CurrentUpdateState(context)


        modules.flatMap { it.artifacts }
                .filter { !currentUpdateState.isPackageInstallationTerminated(getPackageFromApk(context,it.path), getVersionFromApk(context, it.path)) }.forEach{a ->
                    Log.d(TAG,"install artifact ${a.filename} from file ${a.path}")
                    try {
                        installApk(a, messenger)
                    } catch (t: Throwable){ //new client replace with IOException | IllegalArgumentException e
                        val error = "${a.filename} installation fails with error ${t.message}"
                        currentUpdateState.addErrorToRepor(error)
                        currentUpdateState.packageInstallationTerminated(getPackageFromApk(context, a.path), getVersionFromApk(context, a.path))
                        Log.d(TAG, "Failed to install ${a.filename}")
                        Log.d(TAG, t.message, t)
                    }
                }

        return Updater.UpdateResult(success = currentUpdateState.distributionReportError.isEmpty(),
                details = (currentUpdateState.distributionReportError + currentUpdateState.distributionReportSuccess).toList())
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun installApk(artifact: Updater.SwModuleWithPath.Artifact, messenger: Updater.Messenger){

        val currentUpdateState = CurrentUpdateState(context)
        val apk = File(artifact.path)

        when{
            !apk.exists() ->{
                val errorMessage = "Apk not found"
                Log.w(TAG,errorMessage)
                currentUpdateState.addErrorToRepor(errorMessage)
            }

            else ->{
                val countDownLatch = CountDownLatch(1)
                val packageName = getPackageFromApk(context, apk.absolutePath)
                val packageVersion = getVersionFromApk(context, apk.absolutePath)
                val installerSession = InstallerSession.newInstance(
                        context,
                        countDownLatch,
                        packageName,
                        packageVersion,
                        artifact,
                        messenger,
                        currentUpdateState)

                installerSession.writeSession(apk, packageName)
                installerSession.commitSession()

                val timeout = UpdateConfirmationTimeoutProvider
                        .FixedTimeProvider.ofSeconds(1800).getTimeout(null)

                if(!countDownLatch.await(timeout.value, timeout.timeUnit)){
                    val message = "Time to update exceeds the timeout"
                    currentUpdateState.addSuccessMessageToRepor(message)
                    Log.w(TAG,message)
                }
            }
        }

    }


    private fun getPackageFromApk(context: Context, apkPath: String): String? {
        val packageInfo = context.packageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES)
        if (packageInfo != null) {
            val appInfo = packageInfo.applicationInfo
            return appInfo.packageName
        }
        return null
    }

    private fun getVersionFromApk(context: Context, apkPath: String): Long? {
        val packageInfo = context.packageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES)
        if (packageInfo != null) {
            val appInfo = packageInfo.applicationInfo
            return java.lang.Long.valueOf(appInfo.versionCode.toLong())
        }
        return null
    }

}