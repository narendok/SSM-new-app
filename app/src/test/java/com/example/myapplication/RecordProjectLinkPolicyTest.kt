package com.example.myapplication

import com.example.myapplication.ui.screens.recordProjectLinkMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordProjectLinkPolicyTest {
    @Test
    fun preProjectSalesAndPeopleTypesDoNotShowProjectPicker() {
        assertEquals("none", recordProjectLinkMode("SAL", "Enquiry", null, emptyList()))
        assertEquals("optional", recordProjectLinkMode("SAL", "Opportunity", "optional", emptyList()))
        assertEquals("optional", recordProjectLinkMode("SAL", "Order", "optional", emptyList()))
        assertEquals("none", recordProjectLinkMode("HR", "Training", "required", emptyList()))
        assertEquals("none", recordProjectLinkMode("HROPS", "Person", null, emptyList()))
    }

    @Test
    fun onlyProjectConfiguredDepartmentTypesShowOptionalPicker() {
        val catalogue = Json.parseToJsonElement(
            """{"projectLinkByType":{"PRS":"optional","BOM":"optional"}}"""
        ).jsonObject
        val links = catalogue["projectLinkByType"]!!.jsonObject
        assertEquals("optional", recordProjectLinkMode("EHW", "PRS", links["PRS"]!!.toString().trim('"'), emptyList()))
        assertEquals("none", recordProjectLinkMode("EHW", "Hardware tracker", links["unknown"]?.toString(), emptyList()))
    }

    @Test
    fun requiredProjectMasterFieldRemainsRequired() {
        val field = Json.parseToJsonElement(
            """{"key":"projectId","master":"projects","required":true}"""
        ).jsonObject
        assertEquals("required", recordProjectLinkMode("MECH", "STEP clearance", null, listOf(field)))
    }
}
