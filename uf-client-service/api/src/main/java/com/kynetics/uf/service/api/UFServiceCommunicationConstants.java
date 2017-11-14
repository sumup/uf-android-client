/*
 * Copyright © 2017   LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.service.api;

/**
 * @author Daniele Sergio
 */
public class UFServiceCommunicationConstants {

    public static final int MSG_CONFIGURE_SERVICE = 1;
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 2;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 3;

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    public static final int MSG_SEND_STRING = 4;

    public static final int MSG_AUTHORIZATION_REQUEST = 5;

    public static final int MSG_AUTHORIZATION_RESPONSE = 6;

    public static final int MSG_RESUME_SUSPEND_UPGRADE = 7;

    public static final int MSG_SYNCH_REQUEST = 8;

    public static final int MSG_SERVICE_CONFIGURATION_STATUS = 9;

    public static final String SERVICE_PACKAGE_NAME = "com.kynetics.ufandroidclient";

    public static final String SERVICE_ACTION = "com.kynetics.ufandroidcliet.service.intent.action.BIND";

    public static final String SERVICE_DATA_KEY = "DATA_KEY";

    public static final String ACTION_SETTINGS = "com.kynetics.action.SETTINGS";

    static final String ACTION_DOWNLOAD_AUTHORIZATION = "com.kynetics.action.DOWNLOAD_AUTHORIZATION";

    static final String ACTION_RESTART_AUTHORIZATION = "com.kynetics.action.RESTART_AUTHORIZATION";

    private UFServiceCommunicationConstants() {
        throw new AssertionError();
    }
}
