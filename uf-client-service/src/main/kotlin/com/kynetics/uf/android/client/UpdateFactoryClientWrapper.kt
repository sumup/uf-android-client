package com.kynetics.uf.android.client

import com.kynetics.updatefactory.ddiclient.core.api.*

class UpdateFactoryClientWrapper(var delegate: UpdateFactoryClient? = null): UpdateFactoryClient {
    override fun forcePing() {
        delegate?.forcePing()
    }

    override fun init(updateFactoryClientData: UpdateFactoryClientData, directoryForArtifactsProvider: DirectoryForArtifactsProvider, configDataProvider: ConfigDataProvider, deploymentPermitProvider: DeploymentPermitProvider, messageListeners: List<MessageListener>, vararg updaters: Updater) {
        delegate?.init(updateFactoryClientData, directoryForArtifactsProvider, configDataProvider, deploymentPermitProvider, messageListeners, *updaters)
    }

    override fun startAsync() {
        delegate?.startAsync()
    }

    override fun stop() {
        delegate?.stop()
    }
}