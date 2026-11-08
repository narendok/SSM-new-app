package com.example.myapplication

import com.example.myapplication.data.model.CustomerChangeSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomerChangeContractTest {
    @Test
    fun sourceContractProjectionDecodesWithoutAddingAnAndroidRpc() {
        val json = Json { ignoreUnknownKeys = true }
        val result = json.decodeFromString<CustomerChangeSnapshot>(
            """
            {
              "requests": [{
                "id": "10000000-0000-4000-8000-000000000001",
                "organization_id": "10000000-0000-4000-8000-000000000002",
                "project_record_id": "10000000-0000-4000-8000-000000000003",
                "input_sheet_id": "10000000-0000-4000-8000-000000000004",
                "requested_by": "10000000-0000-4000-8000-000000000005",
                "title": "Synthetic requirement change",
                "customer_reference": "TEST-CHANGE-01",
                "description": "A test-only requirement change.",
                "rationale": "Synthetic cross-platform decoder fixture.",
                "status": "Impact review",
                "baseline_revision": 2,
                "baseline_version": 9,
                "before_values": {"system_voltage": {"input": "12 V"}},
                "proposed_values": {"system_voltage": {"input": "24 V"}},
                "version": 3,
                "decision_notes": "",
                "approved_revision": null,
                "created_at": "2026-11-01T10:00:00Z",
                "updated_at": "2026-11-01T10:05:00Z",
                "impacts": [{
                  "id": "10000000-0000-4000-8000-000000000006",
                  "department_id": "10000000-0000-4000-8000-000000000007",
                  "department_code": "EHW",
                  "impact_scope": {
                    "tracks": ["hardware"],
                    "stages": ["Stage 2"],
                    "documents": ["TEST-DOC-01"],
                    "bom": ["TEST-BOM-01"],
                    "reusableAssets": ["TEST-ASSET-01"]
                  },
                  "status": "Pending",
                  "assessment": "",
                  "outcome": "",
                  "assessed_by": null,
                  "assessed_at": null,
                  "version": 1,
                  "updated_at": "2026-11-01T10:00:00Z"
                }],
                "events": [{
                  "id": "10000000-0000-4000-8000-000000000008",
                  "actor_id": "10000000-0000-4000-8000-000000000005",
                  "actor_name": "Synthetic requester",
                  "action": "submitted",
                  "request_version": 2,
                  "details": {},
                  "created_at": "2026-11-01T10:02:00Z"
                }]
              }]
            }
            """.trimIndent()
        )

        val request = result.requests.single()
        assertEquals("Impact review", request.status)
        assertEquals(2, request.baselineRevision)
        assertNull(request.approvedRevision)
        assertEquals(
            "12 V",
            request.beforeValues["system_voltage"]!!.jsonObject["input"]!!.jsonPrimitive.content
        )
        assertEquals(listOf("TEST-ASSET-01"), request.impacts.single().impactScope.reusableAssets)
        assertEquals("EHW", request.impacts.single().departmentCode)
        assertEquals("submitted", request.events.single().action)
    }
}
