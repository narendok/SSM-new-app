package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.SupabaseClient
import kotlinx.serialization.json.*

/** Online-only adapter for the deployed web TEST contract. No local demo fallback. */
class SharedTestRepository(private val invoke: suspend (String, String, String) -> Result<String>) {
    constructor(client: SupabaseClient) : this({ method, payload, token -> client.invokeRpc(method, payload, token) })
    private val json = Json { ignoreUnknownKeys = true }
    suspend fun load(token: String): JsonObject = json.parseToJsonElement(
        invoke("get_shared_test_workspace", "{}", token).getOrThrow()).jsonObject
    suspend fun mutate(token: String, action: String, payload: JsonObject, requestId: String) {
        invoke("mutate_shared_test_workspace", buildJsonObject {
            put("p_action", action); put("p_payload", payload); put("p_request_id", requestId)
        }.toString(), token).getOrThrow()
    }
}
