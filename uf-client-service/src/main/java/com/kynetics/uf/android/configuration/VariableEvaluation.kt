package com.kynetics.uf.android.configuration

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.io.File
import java.net.URI

/**
 * @author Daniele Sergio
 */

enum class VariableEvaluation {

    FILE {
        override fun variableEvaluation(uri: URI, context: Context): String? {
            return File(uri.path).bufferedReader().readLine().trim()
        }
    },

    PROPERTY {
        override fun variableEvaluation(uri: URI, context: Context): String? {
            return PROPERTIES[uri.schemeSpecificPart]?.invoke(context)
        }
    };

    protected abstract fun variableEvaluation(uri: URI, context: Context): String?


    companion object {
        private const val PROPERTY_ANDROID_ID_KEY = "ANDROID_ID"

        fun parseStringWithVariable(path: String, context: Context): String {
            val regex = "\\$\\{[^$\\{\\}]*\\}".toRegex()
            return regex.replace(path){
                it -> VariableEvaluation.variableEvaluation(
                    it.value.substringAfter("{")
                            .substringBefore("}"), context)
            }
        }

        private fun variableEvaluation(variable: String, context: Context): String {
            return try{
                val uri = URI.create(variable)
                VariableEvaluation.valueOf(uri.scheme.toUpperCase())
                        .variableEvaluation(uri, context)
                        ?: (DEFAULT(context))

            } catch (e: IllegalArgumentException){
                DEFAULT(context)
            }
        }

        @SuppressLint("HardwareIds")
        private val DEFAULT: (Context) -> String = { c -> Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID) }

        protected val PROPERTIES : Map<String, (Context) -> String > =
                mapOf(PROPERTY_ANDROID_ID_KEY to DEFAULT)
    }

}