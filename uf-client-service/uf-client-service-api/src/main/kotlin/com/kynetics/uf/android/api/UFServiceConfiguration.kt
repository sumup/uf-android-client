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
import java.util.Objects

/**
 * @author Daniele Sergio
 */
data class UFServiceConfiguration (val tenant: String,
                                   val controllerId: String,
                                   val retryDelay: Long = 900000,
                                   val url: String,
                                   val targetToken: String,
                                   val gatewayToken: String,
                                   val apiMode: Boolean = true,
                                   val enable: Boolean = true,
                                   val isUpdateFactoryServe: Boolean = true,
                                   val targetAttributes: Map<String, String> = mapOf()) : Serializable {

    val isApiMode: Boolean?
        get() = apiMode

    val isEnable: Boolean?
        get() = enable


    val args: Map<String, String>
        @Deprecated("As of release 0.3.4, replaced by {@link #getTargetAttributes()}")
        get() = targetAttributes


    class Builder internal constructor() {
        private var tenant: String? = null
        private var controllerId: String? = null
        private var retryDelay: Long = 900000
        private var url: String? = null
        private var apiMode = true
        private var enable = true
        private var isUpdateFactoryServer = true
        private var targetToken: String? = null
        private var gatewayToken: String? = null
        private var targetAttributes: Map<String, String>? = null
        fun withTenant(tenant: String?): Builder {
            this.tenant = tenant
            return this
        }

        fun withControllerId(controllerId: String?): Builder {
            this.controllerId = controllerId
            return this
        }

        fun withGetawayToken(gatewayToken: String?): Builder {
            this.gatewayToken = gatewayToken
            return this
        }

        fun withTargetToken(targetToken: String?): Builder {
            this.targetToken = targetToken
            return this
        }

        fun withRetryDelay(retryDelay: Long): Builder {
            this.retryDelay = retryDelay
            return this
        }

        fun withUrl(url: String?): Builder {
            this.url = url
            return this
        }

        fun withApiMode(apiMode: Boolean): Builder {
            this.apiMode = apiMode
            return this
        }


        @Deprecated("As of release 0.3.4, replaced by {@link #withEnable(boolean)}", ReplaceWith("withEnable(enable)"))
        fun witEnable(enable: Boolean): Builder {
            return withEnable(enable)
        }


        @Deprecated("As of release 0.3.4, replaced by {@link #withTargetAttributes(Map<String,String>)}", ReplaceWith("withTargetAttributes(args)"))
        fun witArgs(args: Map<String, String>): Builder {
            return withTargetAttributes(args)
        }

        fun withEnable(enable: Boolean): Builder {
            this.enable = enable
            return this
        }

        fun withTargetAttributes(targetAttribute: Map<String, String>?): Builder {
            if (targetAttribute != null && targetAttribute.size > 0) {
                this.targetAttributes = targetAttribute
            }
            return this
        }

        fun withIsUpdateFactoryServer(isUpdateFactoryServer: Boolean): Builder {
            this.isUpdateFactoryServer = isUpdateFactoryServer
            return this
        }

        fun build(): UFServiceConfiguration {
            Objects.requireNonNull<String>(tenant)
            Objects.requireNonNull<String>(controllerId)
            Objects.requireNonNull<String>(url)
            if (retryDelay < 0) {
                throw IllegalStateException("retryDelay must be grater than 0")
            }
            return UFServiceConfiguration(tenant!!, controllerId!!, retryDelay, url!!,
                    targetToken ?: "",
                    gatewayToken ?: "",
                    apiMode, enable, isUpdateFactoryServer,
                    targetAttributes ?: mapOf())
        }

        fun configurationIsValid(): Boolean {
            return (notEmptyString(tenant)
                    && notEmptyString(controllerId)
                    && notEmptyString(url)
                    && retryDelay > 0)
        }

        private fun notEmptyString(stringToTest: String?): Boolean {
            return stringToTest != null && !stringToTest.isEmpty()
        }
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as UFServiceConfiguration?
        return retryDelay == that!!.retryDelay &&
                apiMode == that.apiMode &&
                enable == that.enable &&
                isUpdateFactoryServe == that.isUpdateFactoryServe &&
                tenant == that.tenant &&
                controllerId == that.controllerId &&
                url == that.url &&
                targetToken == that.targetToken &&
                gatewayToken == that.gatewayToken &&
                targetAttributes == that.targetAttributes
    }

    override fun hashCode(): Int {
        return Objects.hash(tenant, controllerId, retryDelay, url, targetToken, gatewayToken, apiMode, enable, targetAttributes, isUpdateFactoryServe)
    }

    companion object {

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }

        private const val serialVersionUID = -6025361892414738765L
    }
}
