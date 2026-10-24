package com.example.myapplication
import com.example.myapplication.data.repository.SharedBusinessRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SharedBusinessRepositoryTest {
 @Test fun readFailureNeverSubstitutesLocalExamples() = runBlocking {
  val repository=SharedBusinessRepository { _,_,_->Result.failure(IllegalStateException("Denied")) }
  try { repository.load("token");fail("Expected authorization failure") } catch(e:IllegalStateException){assertEquals("Denied",e.message)}
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
