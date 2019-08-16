package com.kynetics.uf.android.configuration

import android.app.NotificationManager
import android.content.Intent
import com.kynetics.uf.android.UpdateFactoryService
import com.kynetics.uf.android.api.UFServiceCommunicationConstants
import com.kynetics.uf.android.communication.MessengerHandler
import com.kynetics.uf.android.ui.MainActivity
import com.kynetics.updatefactory.ddiclient.core.api.DeploymentPermitProvider
import java.util.concurrent.BlockingQueue

class AndroidDeploymentPermitProvider(
    private val apiMode: Boolean,
    private val mNotificationManager: NotificationManager,
    private val authResponse: BlockingQueue<Boolean>,
    private val service: UpdateFactoryService
) : DeploymentPermitProvider {

    private fun allowed(auth: UpdateFactoryService.Companion.AuthorizationType): Boolean {
        if (apiMode) {
            MessengerHandler.sendMessage(UFServiceCommunicationConstants.MSG_AUTHORIZATION_REQUEST, auth.name)
        } else {
            showAuthorizationDialog(auth)
        }

        return try {
            val isGranted = authResponse.take()
            if (isGranted) {
                mNotificationManager.notify(UpdateFactoryService.NOTIFICATION_ID, service.getNotification(auth.event.toString(), true))
                MessengerHandler.onAction(auth.toActionOnGranted)
            } else {
                MessengerHandler.onAction(auth.toActionOnDenied)
            }
            isGranted
        } catch (e: InterruptedException) {
            MessengerHandler.onAction(auth.toActionOnDenied)
            false
        }
    }

    override fun downloadAllowed(): Boolean {
        return allowed(UpdateFactoryService.Companion.AuthorizationType.DOWNLOAD)
    }
    override fun updateAllowed(): Boolean {
        return allowed(UpdateFactoryService.Companion.AuthorizationType.UPDATE)
    }

    private fun showAuthorizationDialog(authorization: UpdateFactoryService.Companion.AuthorizationType) {
        val intent = Intent(service, MainActivity::class.java)
        intent.putExtra(MainActivity.INTENT_TYPE_EXTRA_VARIABLE, authorization.extra)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        service.startActivity(intent)
    }
}
