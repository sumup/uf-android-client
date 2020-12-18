/*
 * Copyright © 2017-2020  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

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