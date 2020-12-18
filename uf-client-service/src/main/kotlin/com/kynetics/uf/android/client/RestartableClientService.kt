package com.kynetics.uf.android.client

import android.util.Log
import com.kynetics.uf.android.configuration.ConfigurationHandler
import com.kynetics.updatefactory.ddiclient.core.api.*
import kotlinx.coroutines.*

class RestartableClientService constructor(
        private val client: UpdateFactoryClientWrapper,
        private val  deploymentPermitProvider: DeploymentPermitProvider,
        listeners: List<MessageListener>): UpdateFactoryClient by client{
    private var currentState:MessageListener.Message.State? = null
    private val _listeners:List<MessageListener>
    private val context = newSingleThreadContext("UF restartable client service context")
    companion object{
        val TAG: String = RestartableClientService::class.java.simpleName
        fun newInstance(
                deploymentPermitProvider: DeploymentPermitProvider, listeners: List<MessageListener>): RestartableClientService {
            return RestartableClientService(
                    UpdateFactoryClientWrapper(null),
                    deploymentPermitProvider,
                    listeners)
        }
    }

    fun restartService(conf:ConfigurationHandler)= GlobalScope.launch(context){
        Log.i(TAG,"Try to restart the service")
        while (!serviceRestartable()) {
            Log.i(TAG, "Service not restartable yet.")
            delay(10000)
        }
        Log.d(TAG, "Restarting service")
        client.stop()
        client.delegate = conf.buildServiceFromPreferences(deploymentPermitProvider, _listeners)
        client.startAsync()
        Log.d(TAG, "Service restarted")

    }

    init{
        _listeners = listOf(
                object: MessageListener{
                    override fun onMessage(message: MessageListener.Message) {
                        if(message is MessageListener.Message.State){
                            currentState = message
                        }
                    }

                },
                *listeners.toTypedArray()
        )

    }


    private fun serviceRestartable():Boolean{
        return currentState !=  MessageListener.Message.State.Updating
    }
}

