package com.mediadock.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "mediadock_secure_tokens",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("mediadock_tokens_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_GUEST_TOKEN = "key_guest_token"
        private const val KEY_SESSION_ID = "key_session_id"
        private const val KEY_EXPIRES_AT = "key_expires_at"
        private const val KEY_QUOTA = "key_quota"
    }

    fun saveGuestSession(token: String, sessionId: String, expiresAt: Long, quota: Int) {
        prefs.edit()
            .putString(KEY_GUEST_TOKEN, token)
            .putString(KEY_SESSION_ID, sessionId)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .putInt(KEY_QUOTA, quota)
            .apply()
    }

    fun getGuestToken(): String? {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val nowSec = System.currentTimeMillis() / 1000
        if (expiresAt > 0 && expiresAt <= nowSec) {
            clearSession()
            return null
        }
        return prefs.getString(KEY_GUEST_TOKEN, null)
    }

    fun getSessionId(): String? = prefs.getString(KEY_SESSION_ID, null)

    fun isSessionValid(): Boolean {
        return getGuestToken() != null
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
