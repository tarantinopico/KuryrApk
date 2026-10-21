package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

interface PasswordVault {
    fun savePassword(accountId: Long, password: String)
    fun getPassword(accountId: Long): String?
    fun deletePassword(accountId: Long)
    fun isHardwareBackedOrEncrypted(): Boolean
}

/**
 * Bezpečné úložiště hesel a aplikačních hesel využívající Android Keystore (AES256_GCM)
 * a EncryptedSharedPreferences. Hesla nejsou nikdy uložena v nešifrované Room databázi.
 */
@Singleton
class AndroidKeystoreCredentialVault @Inject constructor(
    context: Context
) : PasswordVault {

    private var encryptedBacked: Boolean = false

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedBacked = true
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Throwable) {
        // Fallback pro lokální JVM / Robolectric testovací prostředí bez hardwarového Keystore
        encryptedBacked = false
        context.applicationContext.getSharedPreferences(PREFS_FILE_NAME + "_fallback", Context.MODE_PRIVATE)
    }

    override fun savePassword(accountId: Long, password: String) {
        sharedPreferences.edit().putString(keyFor(accountId), password).apply()
    }

    override fun getPassword(accountId: Long): String? {
        return sharedPreferences.getString(keyFor(accountId), null)
    }

    override fun deletePassword(accountId: Long) {
        sharedPreferences.edit().remove(keyFor(accountId)).apply()
    }

    override fun isHardwareBackedOrEncrypted(): Boolean = encryptedBacked

    private fun keyFor(accountId: Long): String = "acct_pwd_$accountId"

    companion object {
        private const val PREFS_FILE_NAME = "kuryr_keystore_vault"
    }
}

class InMemoryPasswordVault : PasswordVault {
    private val map = mutableMapOf<Long, String>()

    override fun savePassword(accountId: Long, password: String) {
        map[accountId] = password
    }

    override fun getPassword(accountId: Long): String? = map[accountId]

    override fun deletePassword(accountId: Long) {
        map.remove(accountId)
    }

    override fun isHardwareBackedOrEncrypted(): Boolean = true
}
