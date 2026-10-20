package com.example.myapplication.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.myapplication.data.model.AuthSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class SecureStorage(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "scms_secure_prefs_keystore_only",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        throw SecurityException("Secure Keystore storage unavailable: Android Keystore is required for auth/session secrets and idempotency tokens.", e)
    }

    fun saveSession(session: AuthSession) {
        val jsonStr = json.encodeToString(session)
        prefs.edit {
            putString(KEY_SESSION, jsonStr)
            putString(KEY_ACCESS_TOKEN, session.accessToken)
        }
    }

    fun getSession(): AuthSession? {
        val jsonStr = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            json.decodeFromString<AuthSession>(jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun clearSession() {
        prefs.edit {
            remove(KEY_SESSION)
            remove(KEY_ACCESS_TOKEN)
        }
    }

    fun getOrCreateClientMutationUuid(): String {
        var uuid = prefs.getString(KEY_CLIENT_MUTATION_UUID, null)
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            prefs.edit { putString(KEY_CLIENT_MUTATION_UUID, uuid) }
        }
        return uuid
    }

    /** Identity-scoped retry IDs only, not an offline mutation queue. */
    fun sharedRequestId(userId: String, signature: String): String {
        val key = sharedRequestKey(userId, signature)
        return prefs.getString(key, null) ?: UUID.randomUUID().toString().also { id ->
            check(prefs.edit().putString(key, id).commit()) { "Unable to persist retry identity" }
        }
    }

    fun completeSharedRequest(userId: String, signature: String) {
        check(prefs.edit().remove(sharedRequestKey(userId, signature)).commit()) { "Unable to clear retry identity" }
    }

    private fun sharedRequestKey(userId: String, signature: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest((userId + "|" + signature).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "shared_request_$digest"
    }

    companion object {
        private const val KEY_SESSION = "auth_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_CLIENT_MUTATION_UUID = "client_mutation_uuid"
    }
}
