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

class SupabaseClient {

    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY

    private val httpClient = OkHttpClient.Builder()
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

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Sign-in failed: HTTP ${response.code} - $responseBody")
                )
            }

            val session = json.decodeFromString<AuthSession>(responseBody)
            Result.success(session)
        } catch (e: Exception) {
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
