package com.example.myapplication.data.model

internal object Stage5ReceivingAvailability {
    const val expectedPurchaseOrderReadsStatus =
        "BLOCKED_VIEWS_ABSENT_IN_CONFIRMED_TEST_BACKEND"
    const val finalGrnStatus = "BLOCKED_RPC_ABSENT_IN_CONFIRMED_TEST_BACKEND"

    val isAvailable: Boolean
        get() = expectedPurchaseOrderReadsStatus == "AVAILABLE" &&
            finalGrnStatus == "AVAILABLE"
}
