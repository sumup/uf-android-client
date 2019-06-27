package com.kynetics.uf.android.Communication

import com.kynetics.uf.android.api.ApiCommunicationVersion
import com.kynetics.uf.android.api.UFServiceMessage
import com.kynetics.uf.android.converter.toUFMessage
import com.kynetics.updatefactory.ddiclient.core.api.MessageListener
import java.io.Serializable

interface MessageHandler<out T:Serializable?> {

    enum class Action{
        FORCE_PING,
        AUTH_DOWNLOAD_DENIED,
        AUTH_DOWNLOAD_GRANTED,
        AUTH_UPDATE_DENIED,
        AUTH_UPDATE_GRANTED,
        UPDATE_FINISH
    }

    val apiCommunicationVersion:ApiCommunicationVersion
    val messageToSendOnSync:T
    val currentMessage:T

    fun hasMessage():Boolean = messageToSendOnSync != null

    fun onAction(action:Action): MessageHandler<T>{
        return this
    }

    fun onMessage(msg: MessageListener.Message): MessageHandler<T>{
        return this
    }
}

data class V0(
        override val currentMessage:UFServiceMessage? = null,
        private val suspend: UFServiceMessage.Suspend = UFServiceMessage.Suspend.NONE):MessageHandler<UFServiceMessage?>{

    override val apiCommunicationVersion = ApiCommunicationVersion.V0_1
    override val messageToSendOnSync:UFServiceMessage? = currentMessage

    override fun onAction(action: MessageHandler.Action): MessageHandler<UFServiceMessage?> {
        return when(action){
            MessageHandler.Action.AUTH_DOWNLOAD_DENIED ->  copy(suspend = UFServiceMessage.Suspend.DOWNLOAD)
            MessageHandler.Action.AUTH_UPDATE_DENIED ->   copy(suspend = UFServiceMessage.Suspend.UPDATE)
            else -> copy(suspend = UFServiceMessage.Suspend.NONE)
        }
    }

    override fun onMessage(msg: MessageListener.Message): MessageHandler<UFServiceMessage?> {
        return copy(currentMessage = UFServiceMessage("", "", currentMessage.toString(), suspend))
    }
}

data class V1(
        override val messageToSendOnSync:String? = null,
        override val currentMessage:String? = null):MessageHandler<String?>{

    override val apiCommunicationVersion = ApiCommunicationVersion.V1

    override fun onMessage(msg: MessageListener.Message):MessageHandler<String?> {
        return when(msg){
            is MessageListener.Message.Event ->{
                copy(currentMessage = msg.toUFMessage().toJson())
            }
            is MessageListener.Message.State ->{
                val currentMessage = msg.toUFMessage().toJson()
                copy(messageToSendOnSync = currentMessage, currentMessage = currentMessage )
            }
        }

    }
}

