package com.example.myapplication
import com.example.myapplication.data.repository.SharedBusinessRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SharedBusinessRepositoryTest {
 @Test fun expenseCategoriesComeFromTheSharedAuthenticatedCatalogue() = runBlocking {
  var method="";var args=buildJsonObject{}
  val repository=SharedBusinessRepository { name,body,_ -> method=name;args=Json.parseToJsonElement(body).jsonObject;Result.success("""{"categories":["Travel & Transport","Software & Subscriptions"],"services":[{"id":"github","name":"GitHub subscription","category":"Software & Subscriptions","provider":"GitHub","cycle":"Monthly","owner":"Software lead"}],"suppliers":[{"id":"supplier-uuid","reference":"SUP-01","title":"Canonical Supplier Ltd","status":"Approved"}]}""") }
  val result=repository.loadExpenseCatalogue("test-token","department-uuid")
  assertEquals("get_expense_catalogue",method);assertEquals("department-uuid",args["p_department"]?.jsonPrimitive?.content)
  assertEquals(listOf("Travel & Transport","Software & Subscriptions"),result["categories"]?.jsonArray?.map{it.jsonPrimitive.content})
  assertEquals("GitHub subscription",result["services"]?.jsonArray?.first()?.jsonObject?.get("name")?.jsonPrimitive?.content)
  assertEquals("supplier-uuid",result["suppliers"]?.jsonArray?.first()?.jsonObject?.get("id")?.jsonPrimitive?.content)
 }
 @Test fun readFailureNeverSubstitutesLocalExamples() = runBlocking {
  val repository=SharedBusinessRepository { _,_,_->Result.failure(IllegalStateException("Denied")) }
  try { repository.load("token");fail("Expected authorization failure") } catch(e:IllegalStateException){assertEquals("Denied",e.message)}
 }
 @Test fun travelPolicySaveUsesTheSharedVersionedAdminOrOpsRpc() = runBlocking {
  var method="";var args=buildJsonObject{}
  val repository=SharedBusinessRepository { name,body,token->
   method=name;assertEquals("finance-token",token);args=Json.parseToJsonElement(body).jsonObject
   Result.success("""{"company":{"currency":"INR","status":"Draft","advancePercent":80,"oneOpenAdvance":true,"cityTiers":[],"bands":[]},"companyVersion":3,"departmentVersion":0,"scopeDepartment":null,"isAdmin":true,"canView":true,"ownDepartment":null,"canManageCompany":true,"canManageDepartment":false}""")
  }
  val value=buildJsonObject { put("currency","INR");put("status","Draft");put("advancePercent",80);put("oneOpenAdvance",true) }
  val result=repository.saveTravelPolicy("finance-token",null,value,2,false)
  assertEquals("save_travel_policy",method)
  assertTrue(args["p_department"] is JsonNull)
  assertEquals(2,args["p_version"]?.jsonPrimitive?.int)
  assertEquals(false,args["p_publish"]?.jsonPrimitive?.boolean)
  assertEquals(value,args["p_value"]?.jsonObject)
  assertEquals(3,result["companyVersion"]?.jsonPrimitive?.int)
 }
 @Test fun mutationUsesTheSameRpcIdentityAndRequestIdAsWeb() = runBlocking {
  val calls=mutableListOf<JsonObject>()
  val repository=SharedBusinessRepository { name,body,token->
   assertEquals("mutate_business_workspace",name);assertEquals("real-token",token)
   calls.add(Json.parseToJsonElement(body).jsonObject);Result.success("{\"id\":\"example\",\"saved\":true}")
  }
  val payload=buildJsonObject {put("id","request");put("version",2);putJsonObject("data"){put("allocationId","allocation")}}
  repeat(2){repository.mutate("real-token","approve_expense",payload,"stable-retry-id")}
  assertEquals(calls[0],calls[1]);assertEquals("stable-retry-id",calls[0]["p_request_id"]?.jsonPrimitive?.content)
  assertFalse(calls[0].containsKey("approverRole"))
 }
 @Test fun malformedSuccessDoesNotReportSaved() = runBlocking {
  val repository=SharedBusinessRepository { _,_,_->Result.success("{\"saved\":false}") }
  try {repository.mutate("token","request_expense",buildJsonObject{},"retry");fail("Unconfirmed write")}
  catch(e:IllegalArgumentException){assertEquals("Shared save was not confirmed",e.message)}
 }
}
