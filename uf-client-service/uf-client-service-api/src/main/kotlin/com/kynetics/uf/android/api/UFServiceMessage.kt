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

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date

/**
 * @author Daniele Sergio
 */
@Deprecated(
    "As of release 1.0.0 replaced by {@link UFServiceMessageV1}"
)
class UFServiceMessage(
    val eventName: String,
    val oldState: String,
    val currentState: String,
    val suspend: Suspend
) : Serializable {
    val dateTime: String

    enum class Suspend {
        NONE, DOWNLOAD, UPDATE
    }

    init {
        val dateFormat = SimpleDateFormat("HH:mm:ss")
        this.dateTime = dateFormat.format(Date())
    }

    companion object {
        private const val serialVersionUID = -7571115123564137773L
    }
}
