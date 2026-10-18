package com.example.myapplication.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenericViewRecord(
    val id: String? = null,
    val attributes: Map<String, String> = emptyMap()
)

enum class SafeViewName(val viewIdentifier: String) {
    OPERATIONAL_RECEIVING_PURCHASE_ORDERS("operational_receiving_purchase_orders"),
    OPERATIONAL_RECEIVING_PURCHASE_ORDER_ITEMS("operational_receiving_purchase_order_items"),
    OPERATIONAL_PURCHASE_REQUESTS("operational_purchase_requests"),
    OPERATIONAL_PURCHASE_REQUEST_ITEMS("operational_purchase_request_items"),
    OPERATIONAL_RFQS("operational_rfqs"),
    OPERATIONAL_RFQ_VENDORS("operational_rfq_vendors"),
    OPERATIONAL_PO_SHIPMENTS("operational_po_shipments"),
    OPERATIONAL_VENDORS("operational_vendors"),
    LIFECYCLE_READ_DATA("lifecycle_read_data"),
    DOCUMENT_CONTROL_REGISTERS("document_control_registers"),
    CONTROLLED_DOCUMENT_AUDIT_TIMELINE("controlled_document_audit_timeline")
}
