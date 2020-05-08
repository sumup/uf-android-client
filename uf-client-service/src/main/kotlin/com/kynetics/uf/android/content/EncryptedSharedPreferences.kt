package com.kynetics.uf.android.content

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object EncryptedSharedPreferences {

    private const val SHARED_PREFERENCE_FILE_NAME = "UF_SECURE_SHARED_FILE"

    fun get(context: Context): SharedPreferences {
        return EncryptedSharedPreferences
                .create(
                        SHARED_PREFERENCE_FILE_NAME,
                        masterKeyAlias(),
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
    }


    private fun masterKeyAlias(): String {
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        return MasterKeys.getOrCreate(keyGenParameterSpec)
    }

}