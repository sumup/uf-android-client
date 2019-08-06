package com.kynetics.uf.android.communication

import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import com.kynetics.uf.android.api.ApiCommunicationVersion
import com.kynetics.uf.android.api.UFServiceCommunicationConstants
import com.kynetics.updatefactory.ddiclient.core.api.MessageListener
import java.io.Serializable

object MessangerHandler {

    private val TAG = MessangerHandler::class.java.simpleName

    private val lastSharedMessagesByVersion = mutableMapOf(
            ApiCommunicationVersion.V0_1 to V0(),
            ApiCommunicationVersion.V1 to V1()
    )

    private val mClients = mutableMapOf<Messenger, ApiCommunicationVersion>()

    fun getlastSharedMessage(version: ApiCommunicationVersion) = lastSharedMessagesByVersion.getValue(version)

    fun hasMessage(version: ApiCommunicationVersion): Boolean {
        return lastSharedMessagesByVersion.getValue(version).hasMessage()
    }

    fun onAction(action: MessageHandler.Action) {
        lastSharedMessagesByVersion.forEach {
            lastSharedMessagesByVersion[it.key] = it.value.onAction(action)
        }
    }

    fun onMessageReceived(msg: MessageListener.Message) {
        lastSharedMessagesByVersion.forEach {
            lastSharedMessagesByVersion[it.key] = it.value.onMessage(msg)
        }
    }

    internal fun sendMessage(messageContent: Serializable?, code: Int, messenger: Messenger?) {
        if (messenger == null) {
            Log.i(TAG, "Response isn't' sent because there isn't a receiver (replyTo is null)")
            return
        }
        val message = getMessage(messageContent, code)
        try {
            messenger.send(message)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    internal fun sendMessage(messageCode: Int, message: Serializable? = null) {
        mClients.keys.filter { hasMessage(mClients.getValue(it)) }
                .forEach { messenger ->
                    try {
                        val ApiCommunicationVersion = mClients.getValue(messenger)
                        messenger.send(
                                getMessage(
                                        message
                                        ?: lastSharedMessagesByVersion.getValue(ApiCommunicationVersion).currentMessage, messageCode)
                        )
                    } catch (e: RemoteException) {
                        mClients.remove(messenger)
                    }
                }
    }

    internal fun subscribeClient(messenger: Messenger?, ApiCommunicationVersion: ApiCommunicationVersion) {
        if (messenger != null) {
            mClients[messenger] = ApiCommunicationVersion
            Log.i(TAG, "client subscription")
        } else {
            Log.i(TAG, "client subscription ignored. Field replyTo mustn't be null")
        }
    }

    internal fun unsubscribeClient(messenger: Messenger?) {
        if (messenger != null) {
            mClients.remove(messenger)
            Log.i(TAG, "client unsubscription")
        } else {
            Log.i(TAG, "client unsubscription ignored. Field replyTo mustn't be null")
        }
    }
    private fun getMessage(messageContent: Serializable?, messageCode: Int): Message {
        val message = Message.obtain(null, messageCode)
        val data = Bundle()
        data.putSerializable(UFServiceCommunicationConstants.SERVICE_DATA_KEY, messageContent)
        message.data = data
        return message
    }
}
