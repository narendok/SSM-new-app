package com.example.myapplication

import com.example.myapplication.data.remote.SsmSupabaseDnsFallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.UnknownHostException

class SupabaseFallbackDnsTest {
    @Test
    fun stagingHostUsesOnlyTheKnownFallbackAddresses() {
        val result = SsmSupabaseDnsFallback.addresses("jiqbxcqlywvvmhbgirzl.supabase.co")

        assertEquals(
            listOf("104.18.38.10", "172.64.149.246"),
            result.map { it.hostAddress }
        )
        assertEquals(
            listOf("jiqbxcqlywvvmhbgirzl.supabase.co", "jiqbxcqlywvvmhbgirzl.supabase.co"),
            result.map { it.hostName }
        )
    }

    @Test
    fun legacyProductionHostRetainsItsExistingFallback() {
        val result = SsmSupabaseDnsFallback.addresses("rzskhndweihuczbdhoxk.supabase.co")

        assertEquals(2, result.size)
        assertEquals("rzskhndweihuczbdhoxk.supabase.co", result.first().hostName)
    }

    @Test
    fun arbitrarySupabaseHostDoesNotReceiveTheFallback() {
        assertThrows(UnknownHostException::class.java) {
            SsmSupabaseDnsFallback.addresses("unrelated-project.supabase.co")
        }
    }
}
