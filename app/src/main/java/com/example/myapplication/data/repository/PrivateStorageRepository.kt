package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.SupabaseClient
import com.example.myapplication.data.security.SecureStorage

interface PrivateStorageRepository {
    suspend fun getShortLivedSignedUrl(
        bucketName: String,
        filePath: String,
        expiresInSeconds: Int = 60
    ): Result<String>
}

class PrivateStorageRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val secureStorage: SecureStorage
) : PrivateStorageRepository {

    companion object {
        val PRIVATE_BUCKETS = setOf(
            "controlled-documents",
            "project-drive",
            "pcb-photos",
            "datasheet-cache",
            "external-staging"
        )
        const val MAX_EXPIRY_SECONDS = 60
    }

    override suspend fun getShortLivedSignedUrl(
        bucketName: String,
        filePath: String,
        expiresInSeconds: Int
    ): Result<String> {
        val accessToken = secureStorage.getAccessToken()
            ?.takeIf { it.isNotBlank() }
            ?: return Result.failure(SecurityException("Sign in is required before requesting a private document link."))
        if (bucketName !in PRIVATE_BUCKETS) {
            return Result.failure(SecurityException("Only approved private document buckets can be accessed."))
        }
        if (filePath.isBlank() || filePath.split('/').any { it == ".." }) {
            return Result.failure(IllegalArgumentException("A valid private document path is required."))
        }
        val safeExpiry = expiresInSeconds.coerceIn(1, MAX_EXPIRY_SECONDS)
        return supabaseClient.createSignedUrl(
            bucketName = bucketName,
            filePath = filePath,
            accessToken = accessToken,
            expiresInSeconds = safeExpiry
        )
    }
}
