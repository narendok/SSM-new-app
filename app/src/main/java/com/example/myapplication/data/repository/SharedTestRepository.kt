package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.SupabaseClient
import com.example.myapplication.data.model.ProjectCoordinationProjection
import kotlinx.serialization.json.*

/** Online-only adapter for the deployed web TEST contract. No local demo fallback. */
class SharedTestRepository(private val invoke: suspend (String, String, String) -> Result<String>) {
    constructor(client: SupabaseClient) : this({ method, payload, token -> client.invokeRpc(method, payload, token) })
    private val json = Json { ignoreUnknownKeys = true }
    suspend fun load(token: String): JsonObject = json.parseToJsonElement(
        invoke("get_shared_test_workspace", "{}", token).getOrThrow()).jsonObject
    suspend fun loadProjectCoordination(token: String): ProjectCoordinationProjection =
        json.decodeFromString(invoke("get_project_coordination", "{}", token).getOrThrow())
    /** Reads the organization-scoped TEST lifecycle and checksum-bound reusable revisions. */
    suspend fun loadLifecycleWorkspace(token: String): JsonObject {
        require(token.isNotBlank()) { "Authentication required" }
        return json.parseToJsonElement(invoke("get_lifecycle_workspace", "{}", token).getOrThrow()).jsonObject
    }
    /** Links one exact server-owned revision to a project; it never changes physical stock. */
    suspend fun linkReusableAsset(token: String, payload: JsonObject, requestId: String): JsonObject {
        require(token.isNotBlank()) { "Authentication required" }
        require(runCatching { java.util.UUID.fromString(requestId).toString() == requestId.lowercase() }.getOrDefault(false)) {
            "A stable request ID is required"
        }
        require(!payload["projectId"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) { "Select a project" }
        require(!payload["assetRevisionId"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) { "Select an exact asset revision" }
        require(!payload["assessment"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) { "Describe intended use and compatibility" }
        return json.parseToJsonElement(invoke("mutate_lifecycle_workspace", buildJsonObject {
            put("p_action", "link_asset")
            put("p_payload", payload)
            put("p_request_id", requestId)
        }.toString(), token).getOrThrow()).jsonObject.also { receipt ->
            require(!receipt["id"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) { "Missing asset-use receipt" }
            require(receipt["action"]?.jsonPrimitive?.contentOrNull == "link_asset") { "Unexpected asset-use receipt action" }
        }
    }
    /** The caller retains requestId across retries; server derives tenant/actor and returns a Draft receipt. */
    suspend fun registerDocument(token: String, payload: JsonObject, requestId: String): JsonObject {
        require(token.isNotBlank()) { "Authentication required" }
        require(runCatching { java.util.UUID.fromString(requestId).toString() == requestId.lowercase() }.getOrDefault(false)) {
            "A stable request ID is required"
        }
        return json.parseToJsonElement(invoke("register_controlled_document", buildJsonObject {
            put("p_payload", payload)
            put("p_request_id", requestId)
        }.toString(), token).getOrThrow()).jsonObject.also { receipt ->
            require(!receipt["id"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) { "Missing saved document receipt" }
        }
    }
    /** Register a checksum-bound original through the department-scoped TEST RPC. */
    suspend fun registerOriginal(token: String, payload: JsonObject, requestId: String): JsonObject {
        require(token.isNotBlank()) { "Authentication required" }
        require(runCatching { java.util.UUID.fromString(requestId).toString() == requestId.lowercase() }.getOrDefault(false)) {
            "A stable request ID is required"
        }
        return json.parseToJsonElement(invoke("register_test_controlled_original", buildJsonObject {
            put("p_payload", payload)
            put("p_request_id", requestId)
        }.toString(), token).getOrThrow()).jsonObject.also { receipt ->
            require(!receipt["id"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) { "Missing original registration receipt" }
        }
    }
    /** Resolve an original only through the same server authorization used by web. */
    suspend fun controlledFileAccess(token: String, fileId: String): JsonObject {
        require(token.isNotBlank()) { "Authentication required" }
        require(runCatching { java.util.UUID.fromString(fileId).toString() == fileId.lowercase() }.getOrDefault(false)) {
            "A canonical file ID is required"
        }
        val response = json.parseToJsonElement(invoke("get_controlled_file_access", buildJsonObject {
            put("p_file_id", fileId)
        }.toString(), token).getOrThrow()).jsonArray
        return response.singleOrNull()?.jsonObject
            ?: throw SecurityException("Original unavailable for this account")
    }
    suspend fun mutate(token: String, action: String, payload: JsonObject, requestId: String) {
        invoke("mutate_shared_test_workspace", buildJsonObject {
            put("p_action", action); put("p_payload", payload); put("p_request_id", requestId)
        }.toString(), token).getOrThrow()
    }
}
