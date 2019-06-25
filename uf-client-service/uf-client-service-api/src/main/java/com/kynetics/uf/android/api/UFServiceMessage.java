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

package com.kynetics.uf.android.api;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Daniele Sergio
 * @deprecated  As of release 1.0.0 replaced by {@link com.kynetics.uf.android.api.v1.UFMessage}
 * )}
 */
@Deprecated
public class UFServiceMessage implements Serializable {


    public enum Suspend{
        NONE, DOWNLOAD, UPDATE
    }

    public UFServiceMessage(String eventName, String oldState, String currentState, Suspend suspend) {
        this.eventName = eventName;
        this.oldState = oldState;
        this.currentState = currentState;
        this.suspend = suspend;
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        this.dateTime = dateFormat.format(new Date());
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getEventName() {
        return eventName;
    }

    public String getOldState() {
        return oldState;
    }

    public String getCurrentState() {
        return currentState;
    }

    public Suspend getSuspend() {
        return suspend;
    }

    private static final long serialVersionUID = -7571115123564137773L;
    private final String eventName;
    private final String oldState;
    private final String currentState;
    private final Suspend suspend;
    private final String dateTime;


}
