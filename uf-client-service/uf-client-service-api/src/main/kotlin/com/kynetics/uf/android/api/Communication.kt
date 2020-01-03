/*
 * Copyright © 2017-2020  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.api

import android.os.Bundle
import android.os.Message
import android.os.Messenger
import com.kynetics.uf.android.api.v1.UFServiceMessageV1

fun Message.toOutV1Message(): Communication.V1.Out {
    return when (this.what) {
        Communication.V1.Out.ServiceStatus.ID ->
            Communication.V1.Out.ServiceStatus(UFServiceMessageV1.fromJson(data.getString(
                Communication.V1.SERVICE_DATA_KEY)!!))

        Communication.V1.Out.CurrentServiceConfiguration.ID ->
            Communication.V1.Out.CurrentServiceConfiguration(data.getSerializable(Communication.V1.SERVICE_DATA_KEY
        ) as UFServiceConfiguration)

        Communication.V1.Out.AuthorizationRequest.ID ->
            Communication.V1.Out.AuthorizationRequest(data.getString(
            Communication.V1.SERVICE_DATA_KEY
        )!!)

        else -> throw IllegalArgumentException("This message isn't sent by UF client (with api v1)")
    }
}

sealed class Communication(val id: Int) {

    companion object {
        const val SERVICE_API_VERSION_KEY = "API_VERSION_KEY"
    }

    sealed class V1(id: Int) : Communication(id) {

        companion object {
            const val SERVICE_DATA_KEY = "DATA_KEY"
        }
/**/

        // send to service
        sealed class In(id: Int) : V1(id) {

            open fun toMessage(): Message {
                val msg = Message.obtain(null, id)
                val bundleWithApiVersion = bundle()
                bundleWithApiVersion.putInt(SERVICE_API_VERSION_KEY, ApiCommunicationVersion.V1.versionCode)
                msg.data = bundleWithApiVersion
                return msg
            }

            abstract class WithReplyTo(val replyTo: Messenger, id: Int) : In(id) {
                override fun toMessage(): Message {
                    val msg = super.toMessage()
                    msg.replyTo = replyTo
                    return msg
                }
            }

            open fun bundle(): Bundle = Bundle()

            class ConfigureService(val conf: UFServiceConfiguration) : In(ID) {
                companion object {
                    const val ID = 1
                }

                override fun bundle(): Bundle {
                    return super.bundle().apply {
                        putSerializable(SERVICE_DATA_KEY, conf)
                    }
                }
            }
            class RegisterClient(replyTo: Messenger) : WithReplyTo(replyTo, ID) {
                companion object {
                    const val ID = 2
                }
            }
            class UnregisterClient(replyTo: Messenger) : WithReplyTo(replyTo, ID) {
                companion object {
                    const val ID = 3
                }
            }
            class AuthorizationResponse(val granted: Boolean) : In(ID) {
                companion object {
                    const val ID = 6
                }
                override fun bundle(): Bundle {
                    return super.bundle().apply {
                        putBoolean(SERVICE_DATA_KEY, granted)
                    }
                }
            }
            class Sync(replyTo: Messenger) : WithReplyTo(replyTo, ID) {
                companion object {
                    const val ID = 8
                }
            }
            object ForcePing : In(7)
//            object ResumeSuspend:In(7)
//            object ForcePing:In(10)
        }
        // receive from service
        sealed class Out(id: Int) : V1(id) {
            class ServiceNotification(val content: UFServiceMessageV1) : Out(ID) {
                companion object {
                    const val ID = 4
                }
            }
            class AuthorizationRequest(val authName: String) : Out(ID) {
                companion object {
                    const val ID = 5
                }
            }
            class CurrentServiceConfiguration(val conf: UFServiceConfiguration) : Out(ID) {
                companion object {
                    const val ID = 9
                }
            }
        }
    }
}
