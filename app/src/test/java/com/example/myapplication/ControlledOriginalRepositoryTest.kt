package com.example.myapplication

import com.example.myapplication.data.repository.ControlledOriginalRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test

class ControlledOriginalRepositoryTest {
    @Test fun accessAndVerificationPrecedeSigningWithTheSameSession() = runBlocking {
        val calls = mutableListOf<String>()
        val repository = ControlledOriginalRepository(
            access = { token, id -> calls += "access:$token:$id"; buildJsonObject { put("storage_path", "org/dept/doc/original.pdf") } },
            verify = { token, id -> calls += "verify:$token:$id"; Result.success(Unit) },
            sign = { token, path -> calls += "sign:$token:$path"; Result.success("https://test.invalid/short-lived") }
        )
        assertEquals("https://test.invalid/short-lived", repository.downloadUrl("session", "file-id"))
        assertEquals(listOf("access:session:file-id", "verify:session:file-id", "sign:session:org/dept/doc/original.pdf"), calls)
    }

    @Test fun failedVerificationNeverIssuesALink() = runBlocking {
        var signed = false
        val repository = ControlledOriginalRepository(
            access = { _, _ -> buildJsonObject { put("storage_path", "authorized/path") } },
            verify = { _, _ -> Result.failure(SecurityException("Digest mismatch")) },
            sign = { _, _ -> signed = true; Result.success("forbidden") }
        )
        try { repository.downloadUrl("session", "file-id"); fail("Unverified bytes must fail") }
        catch (e: SecurityException) { assertEquals("Digest mismatch", e.message) }
        assertFalse(signed)
    }

    @Test fun missingSessionOrDeniedAccessNeverReachesStorage() = runBlocking {
        var calls = 0
        val repository = ControlledOriginalRepository(
            access = { _, _ -> calls++; throw SecurityException("Denied") },
            verify = { _, _ -> fail("No verification after denial"); Result.success(Unit) },
            sign = { _, _ -> fail("No signing after denial"); Result.success("") }
        )
        try { repository.downloadUrl("", "file-id"); fail("Session required") }
        catch (_: IllegalArgumentException) { }
        assertEquals(0, calls)
        try { repository.downloadUrl("session", "file-id"); fail("Access denied") }
        catch (_: SecurityException) { }
        assertEquals(1, calls)
    }
}
