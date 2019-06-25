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

package com.kynetics.uf.android.converter

import com.kynetics.uf.android.api.v1.UFMessage
import com.kynetics.updatefactory.ddiclient.core.api.MessageListener

fun MessageListener.Message.toUFMessage(): UFMessage {
    return when(this){
        is MessageListener.Message.State.Downloading -> UFMessage.State.Downloading
        is MessageListener.Message.State.Error -> UFMessage.State.Error(details)
        is MessageListener.Message.State.Updating -> UFMessage.State.Updating
        is MessageListener.Message.State.CancellingUpdate -> UFMessage.State.CancellingUpdate
        is MessageListener.Message.State.WaitingDownloadAuthorization -> UFMessage.State.WaitingDownloadAuthorization
        is MessageListener.Message.State.WaitingUpdateAuthorization -> UFMessage.State.WaitingUpdateAuthorization
        is MessageListener.Message.State.Waiting -> UFMessage.State.Waiting


        is MessageListener.Message.Event.Polling -> UFMessage.Event.Polling
        is MessageListener.Message.Event.AllFilesDownloaded -> UFMessage.Event.AllFilesDownloaded
        is MessageListener.Message.Event.StartDownloadFile -> UFMessage.Event.StartDownloadFile(fileName)
        is MessageListener.Message.Event.DownloadProgress -> UFMessage.Event.DownloadProgress(fileName, percentage)
        is MessageListener.Message.Event.FileDownloaded -> UFMessage.Event.FileDownloaded(fileDownloaded)
        is MessageListener.Message.Event.UpdateFinished -> UFMessage.Event.UpdateFinished(successApply, details)
    }
}