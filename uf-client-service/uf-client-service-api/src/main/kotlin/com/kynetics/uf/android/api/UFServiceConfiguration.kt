package com.kynetics.uf.android.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
data class UFServiceConfiguration(
    val tenant: String,
    val controllerId: String,
    val retryDelay: Long,
    val url: String,
    val targetToken: String,
    val gatewayToken: String,
    val isApiMode: Boolean,
    val isEnable: Boolean,
    val isUpdateFactoryServe: Boolean,
    val targetAttributes: Map<String, String>
) : java.io.Serializable {

    fun toJson(): String {
        return json.stringify(serializer(), this)
    }

    class Builder internal constructor() {

        private var tenant: String? = ""
        private var controllerId: String? = ""
        private var retryDelay: Long = 900000
        private var url: String? = ""
        private var apiMode = true
        private var enable = true
        private var isUpdateFactoryServer = true
        private var targetToken: String? = ""
        private var gatewayToken: String? = ""
        private var targetAttributes: Map<String, String> = mutableMapOf()
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

        @Deprecated("As of release 0.3.4, replaced by {@link #withEnable(boolean)}")
        fun witEnable(enable: Boolean): Builder {
            return withEnable(enable)
        }

        @Deprecated("As of release 0.3.4, replaced by {@link #withTargetAttributes(Map<String,String>)}")
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
            if (retryDelay < 0) {
                throw IllegalStateException("retryDelay must be grater than 0")
            }
            return UFServiceConfiguration(tenant ?: "", controllerId ?: "", retryDelay, url ?: "",
                    targetToken ?: "",
                    gatewayToken ?: "",
                    apiMode, enable, isUpdateFactoryServer,
                    targetAttributes)
        }

        fun configurationIsValid(): Boolean {
            return (notEmptyString(tenant) &&
                    notEmptyString(controllerId) &&
                    notEmptyString(url) &&
                    retryDelay > 0)
        }

        private fun notEmptyString(stringToTest: String?): Boolean {
            return stringToTest != null && !stringToTest.isEmpty()
        }
    }

    companion object {
        private val json = Json(JsonConfiguration.Stable.copy(strictMode = false))
        private const val serialVersionUID = -6025361892414738765L

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }

        @JvmStatic
        fun fromJson(data: String): UFServiceConfiguration {
            return json.parse(serializer(), data)
        }
    }

    @Deprecated("As of release 0.3.4, replaced by {@link #getTargetAttributes()}")
    fun getArgs(): Map<String, String> {
        return this.targetAttributes
    }
}
