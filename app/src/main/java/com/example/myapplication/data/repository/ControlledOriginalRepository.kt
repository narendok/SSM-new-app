package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.SupabaseClient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Uses the web authorization and byte-verification contracts before signing a short-lived link. */
class ControlledOriginalRepository(
    private val access: suspend (String, String) -> JsonObject,
    private val verify: suspend (String, String) -> Result<Unit>,
    private val sign: suspend (String, String) -> Result<String>
) {
    constructor(client: SupabaseClient) : this(
        access = { token, id -> SharedTestRepository(client).controlledFileAccess(token, id) },
        verify = { token, id -> client.verifyControlledOriginal(id, token) },
        sign = { token, path -> client.createSignedUrl("controlled-documents", path, token, 60) }
    )

    suspend fun downloadUrl(token: String, fileId: String): String {
        require(token.isNotBlank()) { "Authentication required" }
        val file = access(token, fileId)
        val path = file["storage_path"]?.jsonPrimitive?.content
            ?.takeIf { it.isNotBlank() } ?: throw SecurityException("Original unavailable")
        verify(token, fileId).getOrThrow()
        return sign(token, path).getOrThrow()
    }
}
