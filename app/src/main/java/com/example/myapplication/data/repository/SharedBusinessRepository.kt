package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.SupabaseClient
import kotlinx.serialization.json.*

/** Same authenticated RPC contract as web. No browser/phone sample fallback. */
class SharedBusinessRepository(private val invoke: suspend (String,String,String)->Result<String>) {
    constructor(client:SupabaseClient):this({method,payload,token->client.invokeRpc(method,payload,token)})
    private val json=Json { ignoreUnknownKeys=true }
    suspend fun load(token:String):JsonObject=json.parseToJsonElement(invoke("get_business_workspace","{}",token).getOrThrow()).jsonObject
    suspend fun mutate(token:String,action:String,payload:JsonObject,requestId:String) {
        val response = invoke("mutate_business_workspace",buildJsonObject { put("p_action",action);put("p_payload",payload);put("p_request_id",requestId) }.toString(),token).getOrThrow()
        require(json.parseToJsonElement(response).jsonObject["saved"]?.jsonPrimitive?.booleanOrNull == true) { "Shared save was not confirmed" }
    }
}
