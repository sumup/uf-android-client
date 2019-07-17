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

package com.kynetics.uf.android.api.v1

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.lang.IllegalArgumentException

@Serializable
sealed class UFServiceMessageV1 {

    enum class MessageName{
        DOWNLOADING,
        ERROR,
        UPDATING,
        CANCELLING_UPDATE,
        WAITING_DOWNLOAD_AUTHORIZATION,
        WAITING_UPDATE_AUTHORIZATION,
        WAITING,
        START_DOWNLOAD_FILE,
        DOWNLOAD_PROGRESS,
        FILE_DOWNLOADED,
        UPDATE_FINISHED,
        POLLING,
        ALL_FILES_DOWNLOADED
    }

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    abstract val description: String
    abstract val name: MessageName
    @Serializable
    sealed class State(override val name: MessageName, override val description: String):UFServiceMessageV1(){
        @Serializable
        data class Downloading(val artifacts:List<Artifact>): State(MessageName.DOWNLOADING,"Client is downloading artifacts from server"){
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson():String{
                return Json(JsonConfiguration.Stable).stringify(serializer(), this)
            }

            @Serializable
            data class Artifact(val name:String, val size:Long, val md5:String)
        }
        object Updating: State(MessageName.UPDATING,"The update process is started. Any request to cancel an update will be rejected")
        object CancellingUpdate: State(MessageName.CANCELLING_UPDATE, "Last update request is being cancelled")
        object WaitingDownloadAuthorization: State(MessageName.WAITING_DOWNLOAD_AUTHORIZATION, "Waiting authorization to start download")
        object WaitingUpdateAuthorization: State(MessageName.WAITING_UPDATE_AUTHORIZATION,"Waiting authorization to start update")
        object Waiting: State(MessageName.WAITING, "There isn't any request from server")

        @UseExperimental(ImplicitReflectionSerializer::class)
        override fun toJson():String{
            return Json(JsonConfiguration.Stable).stringify(serializer(), this)
        }
    }

    @Serializable
    sealed class Event(override val name: MessageName, override val description: String):UFServiceMessageV1(){
        object Polling: Event(MessageName.POLLING, "Client is contacting server to retrieve new action to execute")
        @Serializable
        data class StartDownloadFile(val fileName: String): Event(MessageName.START_DOWNLOAD_FILE, "A file downloading is started"){
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson():String{
                return Json(JsonConfiguration.Stable).stringify(serializer(), this)
            }
        }
        @Serializable
        data class FileDownloaded(val fileDownloaded:String): Event(MessageName.FILE_DOWNLOADED, "A file is downloaded"){
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson():String{
                return Json(JsonConfiguration.Stable).stringify(serializer(), this)
            }
        }
        @Serializable
        data class DownloadProgress(val fileName: String, val percentage:Double = 0.0): Event(MessageName.DOWNLOAD_PROGRESS, "Percent of file downloaded"){
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson():String{
                return Json(JsonConfiguration.Stable).stringify(serializer(), this)
            }
        }

        object AllFilesDownloaded: Event(MessageName.ALL_FILES_DOWNLOADED, "All file needed are downloaded")
        @Serializable
        data class UpdateFinished(val successApply: Boolean, val details:List<String> = emptyList()): Event(MessageName.UPDATE_FINISHED, "The update is finished"){
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson():String{
                return Json(JsonConfiguration.Stable).stringify(serializer(), this)
            }
        }

        @Serializable
        data class Error(val details:List<String> = emptyList()) : Event(MessageName.ERROR, "An error is occurred"){
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson():String{
                return Json(JsonConfiguration.Stable).stringify(serializer(), this)
            }
        }

        @UseExperimental(ImplicitReflectionSerializer::class)
        override fun toJson():String{
            println(Json(JsonConfiguration.Stable).stringify(serializer(), this))
            return Json(JsonConfiguration.Stable).stringify(serializer(), this)
        }
    }

    abstract fun toJson():String

    companion object{
        @UseExperimental(ImplicitReflectionSerializer::class)
        fun fromJson(jsonContent:String):UFServiceMessageV1 {
            val json = Json(JsonConfiguration.Stable.copy(strictMode = false))
            val jsonElement = json.parseJson(jsonContent)
            return when (jsonElement.jsonObject["name"]?.primitive?.content) {
                MessageName.DOWNLOADING.name -> json.fromJson<State.Downloading>(jsonElement)
                MessageName.UPDATING.name -> State.Updating
                MessageName.CANCELLING_UPDATE.name -> State.CancellingUpdate
                MessageName.WAITING_DOWNLOAD_AUTHORIZATION.name -> State.WaitingDownloadAuthorization
                MessageName.WAITING_UPDATE_AUTHORIZATION.name -> State.WaitingUpdateAuthorization
                MessageName.WAITING.name -> State.Waiting

                MessageName.ERROR.name -> json.fromJson<Event.Error>(jsonElement)
                MessageName.START_DOWNLOAD_FILE.name -> json.fromJson<Event.StartDownloadFile>(jsonElement)
                MessageName.DOWNLOAD_PROGRESS.name -> json.fromJson<Event.DownloadProgress>(jsonElement)
                MessageName.FILE_DOWNLOADED.name -> json.fromJson<Event.FileDownloaded>(jsonElement)
                MessageName.UPDATE_FINISHED.name -> json.fromJson<Event.UpdateFinished>(jsonElement)
                MessageName.POLLING.name -> Event.Polling
                MessageName.ALL_FILES_DOWNLOADED.name -> Event.AllFilesDownloaded

                else -> throw IllegalArgumentException("$jsonContent is not obtained by toJson method of ${UFServiceMessageV1::class.java.simpleName}")

            }
        }
    }
}
