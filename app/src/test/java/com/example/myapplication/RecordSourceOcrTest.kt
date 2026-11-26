package com.example.myapplication

import com.example.myapplication.ui.screens.isRecordTitleSourceLabel
import com.example.myapplication.ui.screens.parseRecordSourcePairs
import com.example.myapplication.ui.screens.suggestRecordSourceField
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordSourceOcrTest {
    @Test
    fun enquiryTitleMapsToRecordTitleAndDateMapsToCatalogueField() {
        val pairs = parseRecordSourcePairs(
            "Enquiry title: Northstar Cargo EV tracker expansion request\nTarget date: 2026-12-15"
        )

        val title = pairs.single { it.label == "Enquiry title" }
        val date = pairs.single { it.label == "Target date" }
        val fields = listOf(
            Json.parseToJsonElement("""{"key":"targetDate","label":"Target date","kind":"date"}""").jsonObject
        )

        assertTrue(isRecordTitleSourceLabel(title.label))
        assertEquals("targetDate", suggestRecordSourceField(date, fields))
        assertEquals("Northstar Cargo EV tracker expansion request", title.value)
    }
}
