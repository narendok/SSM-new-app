package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.SupabaseClient
import kotlinx.serialization.json.*

/** Same authenticated RPC contract as web. No browser/phone sample fallback. */
class SharedBusinessRepository(private val invoke: suspend (String,String,String)->Result<String>) {
    constructor(client:SupabaseClient):this({method,payload,token->client.invokeRpc(method,payload,token)})
    private val json=Json { ignoreUnknownKeys=true }
    suspend fun load(token:String):JsonObject=json.parseToJsonElement(invoke("get_business_workspace","{}",token).getOrThrow()).jsonObject
    suspend fun loadExpenseCatalogue(token:String,departmentId:String?):JsonObject = json.parseToJsonElement(
        invoke("get_expense_catalogue",buildJsonObject { if(departmentId.isNullOrBlank()) put("p_department",JsonNull) else put("p_department",departmentId) }.toString(),token).getOrThrow()
    ).jsonObject
    suspend fun loadTravelPolicy(token:String,departmentId:String?):JsonObject = json.parseToJsonElement(
        invoke("get_travel_policy",buildJsonObject { if(departmentId.isNullOrBlank()) put("p_department",JsonNull) else put("p_department",departmentId) }.toString(),token).getOrThrow()
    ).jsonObject
    suspend fun saveTravelPolicy(token:String,departmentId:String?,value:JsonObject,version:Int,publish:Boolean):JsonObject = json.parseToJsonElement(
        invoke("save_travel_policy",buildJsonObject {
            if(departmentId.isNullOrBlank()) put("p_department",JsonNull) else put("p_department",departmentId)
            put("p_value",value);put("p_version",version);put("p_publish",publish)
        }.toString(),token).getOrThrow()
    ).jsonObject
    suspend fun requestExpenseLocation(token:String,name:String,city:String,reason:String,requestId:String):JsonObject = json.parseToJsonElement(
        invoke("request_expense_location",buildJsonObject {
            put("p_value",buildJsonObject { put("name",name);put("city",city);put("reason",reason) })
            put("p_request_id",requestId)
        }.toString(),token).getOrThrow()
    ).jsonObject
    suspend fun mutate(token:String,action:String,payload:JsonObject,requestId:String) {
        val response = invoke("mutate_business_workspace",buildJsonObject { put("p_action",action);put("p_payload",payload);put("p_request_id",requestId) }.toString(),token).getOrThrow()
        require(json.parseToJsonElement(response).jsonObject["saved"]?.jsonPrimitive?.booleanOrNull == true) { "Shared save was not confirmed" }
    }
}
