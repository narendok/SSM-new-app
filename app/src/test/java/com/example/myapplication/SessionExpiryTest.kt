package com.example.myapplication
import com.example.myapplication.data.model.AuthSession
import com.example.myapplication.data.model.needsRefresh
import org.junit.Assert.*
import org.junit.Test
class SessionExpiryTest {
    @Test fun refreshesExpiredUnknownAndNearExpirySessions() {
        assertTrue(AuthSession("token", expiresAt = 0).needsRefresh(1000))
        assertTrue(AuthSession("token", expiresAt = 999).needsRefresh(1000))
        assertTrue(AuthSession("token", expiresAt = 1060).needsRefresh(1000))
        assertFalse(AuthSession("token", expiresAt = 1061).needsRefresh(1000))
    }
}
