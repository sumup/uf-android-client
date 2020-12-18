package com.kynetics.uf.android.update

import android.app.PendingIntent
import android.content.*
import android.util.Log
import java.lang.Exception

object PackageInstallerBRHandler {
    const val ACTION_INSTALL_COMPLETE = "com.kynetics.action.INSTALL_COMPLETED"

    private var currentReceiver:BroadcastReceiver? = null

    private val intentFilter = IntentFilter(ACTION_INSTALL_COMPLETE)
    private val intent = Intent(ACTION_INSTALL_COMPLETE)

    fun registerReceiver(context: Context, receiver: PackageInstallerBroadcastReceiver){
        unregisterReceiver(context)
        currentReceiver = receiver
        context.registerReceiver(currentReceiver, intentFilter)
    }

    fun createIntentSender(context: Context, requestCode:Int): IntentSender {
        val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        return pendingIntent.intentSender
    }

    private fun unregisterReceiver(context: Context){
        if(currentReceiver != null){
            try {
                context.unregisterReceiver(currentReceiver)
            } catch (e:Exception){
                Log.i(TAG, "Can't unregister receiver", e)
            }
        }
    }

    private val TAG = PackageInstallerBRHandler::class.java.simpleName
}