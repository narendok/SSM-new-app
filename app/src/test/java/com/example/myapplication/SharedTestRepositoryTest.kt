package com.example.myapplication

import com.example.myapplication.data.repository.SharedTestRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SharedTestRepositoryTest {
    @Test fun readsUseCurrentSessionAndNoLocalFallback() = runBlocking {
        val calls = mutableListOf<String>()
        val repository = SharedTestRepository { method, payload, token ->
            calls.add("$method|$payload|$token")
            Result.success("{\"records\":[],\"tasks\":[]}")
        }
        assertEquals(0, repository.load("account-a")["records"]!!.jsonArray.size)
        repository.load("account-b")
        assertEquals(listOf("get_shared_test_workspace|{}|account-a", "get_shared_test_workspace|{}|account-b"), calls)
        val denied = SharedTestRepository { _, _, _ -> Result.failure(SecurityException("Denied")) }
        try { denied.load("expired"); fail("Must fail closed") } catch (e: SecurityException) { assertEquals("Denied", e.message) }
    }
    @Test fun mutationPreservesRetryIdentityAndStaleVersionContract() = runBlocking {
        val bodies = mutableListOf<JsonObject>()
        val repository = SharedTestRepository { method, body, token ->
            assertEquals("mutate_shared_test_workspace", method)
            assertEquals("employee-session", token)
            bodies.add(Json.parseToJsonElement(body).jsonObject)
            Result.success("{}")
        }
        val payload = buildJsonObject { put("id", "task-id"); put("status", "Complete"); put("expected_updated_at", "server-version") }
        repeat(2) { repository.mutate("employee-session", "update_task", payload, "same-request-id") }
        assertEquals(bodies[0], bodies[1])
        assertEquals(payload, bodies[0]["p_payload"])
        assertEquals("same-request-id", bodies[0]["p_request_id"]!!.jsonPrimitive.content)
        assertFalse(bodies[0].containsKey("organization_id"))
        assertFalse(bodies[0].containsKey("role"))
    }
}
