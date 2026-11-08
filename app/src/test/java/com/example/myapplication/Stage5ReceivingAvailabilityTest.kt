package com.example.myapplication

import com.example.myapplication.data.model.Stage5ReceivingAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class Stage5ReceivingAvailabilityTest {
    @Test
    fun receivingRemainsUnavailableWhileConfirmedTestContractsAreMissing() {
        assertEquals(
            "BLOCKED_VIEWS_ABSENT_IN_CONFIRMED_TEST_BACKEND",
            Stage5ReceivingAvailability.expectedPurchaseOrderReadsStatus
        )
        assertEquals(
            "BLOCKED_RPC_ABSENT_IN_CONFIRMED_TEST_BACKEND",
            Stage5ReceivingAvailability.finalGrnStatus
        )
        assertFalse(Stage5ReceivingAvailability.isAvailable)
    }
}
