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
import kotlinx.serialization.stringify
import java.lang.IllegalArgumentException

@Serializable
sealed class UFMessage {
    override fun toString(): String {
        return this.javaClass.simpleName
    }

    abstract val description: String

    @Serializable
    sealed class State(override val description: String):UFMessage(){
        object Downloading: State("Client is downloading artifacts from server")
        object Updating: State("The update process is started. Any request to cancel an update will be rejected")
        object CancellingUpdate: State("Last update request is being cancelled")
        object WaitingDownloadAuthorization: State("Waiting authorization to start download")
        object WaitingUpdateAuthorization: State("Waiting authorization to start update")
        @Serializable
        data class Error(val details:List<String> = emptyList()) : State("An error is occurred"){
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson():String{
                return Json(JsonConfiguration.Stable).stringify(serializer(), this)
            }
        }
        object Waiting: State("There isn't any request from server")

        @UseExperimental(ImplicitReflectionSerializer::class)
        override fun toJson():String{
            return Json(JsonConfiguration.Stable).stringify(serializer(), this)
        }
    }

    @Serializable
    sealed class Event(override val description: String):UFMessage(){
        object Polling: Event("Client is contacting server to retrieve new action to execute")
        @Serializable
        data class StartDownloadFile(val fileName: String): Event("A file downloading is started"){
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson():String{
                return Json(JsonConfiguration.Stable).stringify(serializer(), this)
            }
        }
        @Serializable
        data class FileDownloaded(val fileDownloaded:String): Event("A file is downloaded"){
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson():String{
                return Json(JsonConfiguration.Stable).stringify(serializer(), this)
            }
        }
        @Serializable
        data class DownloadProgress(val fileName: String, val percentage:Double = 0.0): Event("Percent of file downloaded"){
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson():String{
                return Json(JsonConfiguration.Stable).stringify(serializer(), this)
            }
        }

        object AllFilesDownloaded: Event("All file needed are downloaded")
        @Serializable
        data class UpdateFinished(val successApply: Boolean, val details:List<String> = emptyList()): Event("The update is finished"){
            @UseExperimental(ImplicitReflectionSerializer::class)
            override fun toJson():String{
                return Json(JsonConfiguration.Stable).stringify(serializer(), this)
            }
        }

        @UseExperimental(ImplicitReflectionSerializer::class)
        override fun toJson():String{
            return Json(JsonConfiguration.Stable).stringify(serializer(), this)
        }
    }

    abstract fun toJson():String

    companion object{
        @UseExperimental(ImplicitReflectionSerializer::class)
        fun fromJson(jsonContent:String):UFMessage{
            val json = Json(JsonConfiguration.Stable.copy(strictMode = false))
            val jsonElement = json.parseJson(jsonContent)
            return when(jsonElement.jsonObject["description"]?.primitive?.content){
                State.Downloading.description -> State.Downloading
                State.Error().description -> json.fromJson<State.Error>(jsonElement)
                State.Updating.description -> State.Updating
                State.CancellingUpdate.description -> State.CancellingUpdate
                State.WaitingDownloadAuthorization.description -> State.WaitingDownloadAuthorization
                State.WaitingUpdateAuthorization.description -> State.WaitingUpdateAuthorization
                State.Waiting.description -> State.Waiting

                Event.StartDownloadFile("").description -> json.fromJson<Event.StartDownloadFile>(jsonElement)
                Event.DownloadProgress("").description -> json.fromJson<Event.DownloadProgress>(jsonElement)
                Event.FileDownloaded("").description -> json.fromJson<Event.FileDownloaded>(jsonElement)
                Event.UpdateFinished(false).description -> json.fromJson<Event.UpdateFinished>(jsonElement)
                Event.Polling.description -> Event.Polling
                Event.AllFilesDownloaded.description -> Event.AllFilesDownloaded

                else -> throw IllegalArgumentException("$jsonContent is not obtained by toJson method of ${UFMessage::class.java.simpleName}")

            }
        }
    }
}
