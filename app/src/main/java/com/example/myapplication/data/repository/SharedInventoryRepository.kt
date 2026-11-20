package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.SupabaseClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/** Authenticated shared TEST inventory contract mirrored from Web. */
class SharedInventoryRepository(private val client: SupabaseClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(accessToken: String): JsonObject = json.parseToJsonElement(
        client.invokeRpc("get_shared_inventory_part_catalog", "{}", accessToken).getOrThrow()
    ).jsonObject

    suspend fun recordEvent(action: String, payload: JsonObject, accessToken: String): JsonObject = json.parseToJsonElement(
        client.invokeRpc(
            "record_shared_inventory_event",
            kotlinx.serialization.json.buildJsonObject {
                put("p_action", JsonPrimitive(action))
                put("p_payload", payload)
            }.toString(),
            accessToken
        ).getOrThrow()
    ).jsonObject

    suspend fun issueBomKit(payload: JsonObject, accessToken: String): JsonObject = json.parseToJsonElement(
        client.invokeRpc(
            "issue_shared_inventory_bom_kit",
            kotlinx.serialization.json.buildJsonObject { put("p_payload", payload) }.toString(),
            accessToken
        ).getOrThrow()
    ).jsonObject
}
