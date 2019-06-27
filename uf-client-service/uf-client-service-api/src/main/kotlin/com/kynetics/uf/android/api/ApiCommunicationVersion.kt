package com.kynetics.uf.android.api

enum class ApiCommunicationVersion(val versionCode:Int, val versionName:String) {
    V0_1(0, "0.1"),
    V1(1, "1.0");

    companion object {
        fun fromVersionCode(versionCode: Int): ApiCommunicationVersion {
            return when (versionCode) {
                1 -> V1
                else -> V0_1
            }
        }
    }
}