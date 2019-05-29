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

package com.kynetics.uf.android.api

/**
 * @author Daniele Sergio
 */
object UFServiceCommunicationConstants{


    const val MSG_CONFIGURE_SERVICE = 1
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    const val MSG_REGISTER_CLIENT = 2

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    const val MSG_UNREGISTER_CLIENT = 3

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    const val MSG_SERVICE_STATUS = 4

    const val MSG_AUTHORIZATION_REQUEST = 5

    const val MSG_AUTHORIZATION_RESPONSE = 6

    const val MSG_RESUME_SUSPEND_UPGRADE = 7

    const val MSG_SYNC_REQUEST = 8

    const val MSG_SERVICE_CONFIGURATION_STATUS = 9

    const val SERVICE_PACKAGE_NAME = "com.kynetics.uf.service"

    const val SERVICE_ACTION = "com.kynetics.action.BIND_UF_SERVICE"

    const val SERVICE_DATA_KEY = "DATA_KEY"

    const val ACTION_SETTINGS = "com.kynetics.action.SETTINGS"

    const val ACTION_DOWNLOAD_AUTHORIZATION = "com.kynetics.action.DOWNLOAD_AUTHORIZATION"

    const val ACTION_RESTART_AUTHORIZATION = "com.kynetics.action.RESTART_AUTHORIZATION"

}
