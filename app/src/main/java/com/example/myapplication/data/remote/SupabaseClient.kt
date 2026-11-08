package com.example.myapplication.data.remote

import com.example.myapplication.BuildConfig
import com.example.myapplication.data.model.AuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import okhttp3.Dns

class SupabaseClient {

    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY

    private val fallbackDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                val resolved = Dns.SYSTEM.lookup(hostname)
                if (resolved.isNotEmpty()) resolved else fallback(hostname)
            } catch (e: Exception) {
                fallback(hostname)
            }
        }

        private fun fallback(hostname: String): List<InetAddress> {
            if (hostname == "rzskhndweihuczbdhoxk.supabase.co") {
                return listOf(
                    InetAddress.getByAddress(hostname, byteArrayOf(104.toByte(), 18.toByte(), 38.toByte(), 10.toByte())),
                    InetAddress.getByAddress(hostname, byteArrayOf(172.toByte(), 64.toByte(), 149.toByte(), 246.toByte()))
                )
            }
            throw UnknownHostException("Unable to resolve host: $hostname")
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .dns(fallbackDns)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun <T> queryView(
        viewName: String,
        accessToken: String?,
        deserializer: (String) -> List<T>
    ): Result<List<T>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/rest/v1/$viewName?select=*"
            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)

            if (!accessToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $accessToken")
            }

            val request = requestBuilder.get().build()
            val response = httpClient.newCall(request).execute()

            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    return@withContext Result.failure(
                        SecurityException("Access Denied by Server (RLS Policy). HTTP ${response.code}: $responseBody")
                    )
                } else {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }
            }

            val data = deserializer(responseBody)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exchangePkceCode(
        code: String,
        codeVerifier: String
    ): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/auth/v1/token?grant_type=pkce"
            val payload = mapOf(
                "auth_code" to code,
                "code_verifier" to codeVerifier
            )
            val jsonBody = json.encodeToString(payload)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .post(jsonBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            android.util.Log.d("SupabaseClient", "PKCE response code: ${response.code}")

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Failed to exchange PKCE code: HTTP ${response.code} - $responseBody")
                )
            }

            val session = json.decodeFromString<AuthSession>(responseBody)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithPassword(
        email: String,
        password: String
    ): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/auth/v1/token?grant_type=password"
            val payload = mapOf(
                "email" to email.trim(),
                "password" to password
            )
            val jsonBody = json.encodeToString(payload)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .post(jsonBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            android.util.Log.d("SupabaseClient", "Sign-in response code: ${response.code}")

            if (!response.isSuccessful) {
                val err = IOException("Sign-in failed: HTTP ${response.code}. Check your account details and retry.")
                return@withContext Result.failure(err)
            }

            val session = json.decodeFromString<AuthSession>(responseBody)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshSession(refreshToken: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val body = json.encodeToString(mapOf("refresh_token" to refreshToken))
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$baseUrl/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", anonKey).post(body).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 400 || response.code == 401 || response.code == 403)
                        throw SecurityException("Session expired. Please sign in again.")
                    throw IOException("Session refresh unavailable. Retry when online.")
                }
                Result.success(json.decodeFromString<AuthSession>(response.body?.string() ?: ""))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun createSignedUrl(
        bucketName: String,
        filePath: String,
        accessToken: String?,
        expiresInSeconds: Int = 300
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = filePath.trimStart('/')
            val url = "$baseUrl/storage/v1/object/sign/$bucketName/$cleanPath"
            val payload = mapOf("expiresIn" to expiresInSeconds)
            val jsonBody = json.encodeToString(payload)
                .toRequestBody("application/json".toMediaType())

            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)

            if (!accessToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $accessToken")
            }

            val request = requestBuilder.post(jsonBody).build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Failed to generate signed URL: HTTP ${response.code} - $responseBody")
                )
            }

            val jsonObject = json.decodeFromString<JsonObject>(responseBody)
            val signedUrlPath = jsonObject["signedURL"]?.jsonPrimitive?.content
                ?: jsonObject["signedUrl"]?.jsonPrimitive?.content
                ?: return@withContext Result.failure(IOException("signedURL missing in response"))

            val fullSignedUrl = if (signedUrlPath.startsWith("http")) {
                signedUrlPath
            } else {
                "$baseUrl/storage/v1$signedUrlPath"
            }

            Result.success(fullSignedUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Upload only to an already-authorized immutable TEST file registration. */
    suspend fun uploadControlledOriginal(fileId: String, bytes: ByteArray, contentType: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(BuildConfig.SCMS_BACKEND == "staging") { "Original upload rollout is restricted to staging" }
            require(accessToken.isNotBlank()) { "Authentication required" }
            require(bytes.isNotEmpty() && bytes.size <= 25 * 1024 * 1024) { "File must be between 1 byte and 25 MB" }
            val registered = com.example.myapplication.data.repository.SharedTestRepository(this@SupabaseClient)
                .controlledFileAccess(accessToken, fileId)
            val checksum = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            require(registered["checksum"]?.jsonPrimitive?.content?.equals(checksum, true) == true &&
                registered["size_bytes"]?.jsonPrimitive?.content?.toLongOrNull() == bytes.size.toLong()) {
                "Selected file differs from the registered original"
            }
            val path = registered["storage_path"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: throw SecurityException("Original unavailable")
            val request = Request.Builder().url("$baseUrl/storage/v1/object/controlled-documents/$path")
                .header("apikey", anonKey).header("Authorization", "Bearer $accessToken")
                .header("x-upsert", "false").post(bytes.toRequestBody(contentType.toMediaType())).build()
            val uploadStatus = httpClient.newCall(request).execute().use { it.code }
            // Storage duplicate responses vary by version. Only the server's
            // actual-byte verification can turn an interrupted retry into success.
            val verification = verifyControlledOriginal(fileId, accessToken)
            if (verification.isFailure) {
                if (uploadStatus !in 200..299)
                    throw IOException("Original upload failed (HTTP $uploadStatus). Retry the same registered revision.")
                verification.getOrThrow()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Register an immutable, department-scoped original for an existing TEST document. */
    suspend fun registerControlledOriginal(documentId: String, revision: String, originalName: String,
        contentType: String, bytes: ByteArray, accessToken: String, requestId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            require(BuildConfig.SCMS_BACKEND == "staging") { "Original registration is restricted to staging" }
            require(accessToken.isNotBlank()) { "Authentication required" }
            require(java.util.UUID.fromString(documentId).toString() == documentId.lowercase()) { "Canonical document ID required" }
            require(revision.isNotBlank() && revision.length <= 80) { "Revision is required" }
            require(originalName.isNotBlank() && originalName.length <= 180) { "File name is required" }
            require(bytes.isNotEmpty() && bytes.size <= 25 * 1024 * 1024) { "File must be between 1 byte and 25 MB" }
            require(runCatching { java.util.UUID.fromString(requestId).toString() == requestId.lowercase() }.getOrDefault(false)) { "A stable retry identity is required" }
            val checksum = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            val payload = kotlinx.serialization.json.buildJsonObject {
                put("documentId", kotlinx.serialization.json.JsonPrimitive(documentId))
                put("revision", kotlinx.serialization.json.JsonPrimitive(revision))
                put("originalName", kotlinx.serialization.json.JsonPrimitive(originalName))
                put("contentType", kotlinx.serialization.json.JsonPrimitive(contentType))
                put("sizeBytes", kotlinx.serialization.json.JsonPrimitive(bytes.size))
                put("checksum", kotlinx.serialization.json.JsonPrimitive(checksum))
            }
            val registration = com.example.myapplication.data.repository.SharedTestRepository(this@SupabaseClient)
                .registerOriginal(accessToken, payload, requestId)
            val fileId = registration["id"]?.jsonPrimitive?.content
                ?: throw IOException("Original registration returned no file ID")
            uploadControlledOriginal(fileId, bytes, contentType, accessToken).getOrThrow()
            Result.success(fileId)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun listControlledOriginals(documentId: String, accessToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            require(accessToken.isNotBlank()) { "Authentication required" }
            require(java.util.UUID.fromString(documentId).toString() == documentId.lowercase()) { "Canonical document ID required" }
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/controlled_document_files?document_id=eq.$documentId&select=id,document_id,revision,original_name,content_type,size_bytes,checksum,uploaded_at,superseded_at&order=uploaded_at.desc")
                .header("apikey", anonKey).header("Authorization", "Bearer $accessToken").get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Original revisions unavailable (HTTP ${response.code}).")
                Result.success(response.body?.string() ?: throw IOException("Missing revision response"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun verifyControlledOriginal(fileId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(accessToken.isNotBlank()) { "Authentication required" }
            val payload = json.encodeToString(mapOf("fileId" to fileId))
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$baseUrl/functions/v1/verify-controlled-original")
                .header("apikey", anonKey).header("Authorization", "Bearer $accessToken")
                .post(payload).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Original verification failed (HTTP ${response.code}).")
                val result = json.decodeFromString<JsonObject>(response.body?.string().orEmpty())
                if (result["verified"]?.jsonPrimitive?.content != "true")
                    throw SecurityException("Original has not been verified")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun invokeRpc(
        rpcName: String,
        payloadJson: String,
        accessToken: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/rest/v1/rpc/$rpcName"
            val jsonBody = payloadJson.toRequestBody("application/json".toMediaType())

            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .post(jsonBody)

            if (!accessToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $accessToken")
            }

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            android.util.Log.d("SupabaseClient", "RPC response code: ${response.code}")

            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    return@withContext Result.failure(
                        SecurityException("RPC Access Denied (HTTP ${response.code}): $responseBody")
                    )
                } else {
                    return@withContext Result.failure(
                        IOException("RPC Error (HTTP ${response.code}): $responseBody")
                    )
                }
            }

            Result.success(responseBody)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
