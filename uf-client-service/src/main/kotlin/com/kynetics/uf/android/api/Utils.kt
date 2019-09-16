package com.kynetics.uf.android.api

import android.os.Message

fun Message.toInV1Message(): Communication.V1.In? {
    val getIfReplyToNotNull = { obj: Communication.V1.In ->
        if (replyTo == null) null else obj
    }
    return when (this.what) {
        Communication.V1.In.ConfigureService.ID ->
            Communication.V1.In.ConfigureService(
                UFServiceConfiguration.fromJson(data.getString(
                    Communication.V1.SERVICE_DATA_KEY)!!))

        Communication.V1.In.RegisterClient.ID ->
            getIfReplyToNotNull(Communication.V1.In.RegisterClient(replyTo))
        Communication.V1.In.UnregisterClient.ID ->
            getIfReplyToNotNull(Communication.V1.In.UnregisterClient(replyTo)) // messenger
        Communication.V1.In.Sync.ID ->
            getIfReplyToNotNull(Communication.V1.In.Sync(replyTo)) // messenger
        Communication.V1.In.ForcePing.id -> Communication.V1.In.ForcePing
        Communication.V1.In.AuthorizationResponse.ID -> Communication.V1.In.AuthorizationResponse(
            data.getBoolean(Communication.V1.SERVICE_DATA_KEY)
        )

        else -> throw IllegalArgumentException("This message isn't sent by UF client (with api v1)")
    }
}
