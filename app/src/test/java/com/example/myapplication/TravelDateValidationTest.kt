package com.example.myapplication

import com.example.myapplication.ui.screens.travelDateValidationMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TravelDateValidationTest {
    @Test fun requiresBothTravelDates() {
        assertEquals("Choose a travel start date.", travelDateValidationMessage(null, "2026-11-21"))
        assertEquals("Choose a travel end date.", travelDateValidationMessage("2026-11-20", ""))
    }

    @Test fun rejectsMalformedDatesAndEndBeforeStart() {
        assertEquals("Choose a valid travel start date.", travelDateValidationMessage("20/11/2026", "2026-11-21"))
        assertEquals(
            "Travel end date must be on or after the start date.",
            travelDateValidationMessage("2026-11-21", "2026-11-20")
        )
    }

    @Test fun acceptsSameDayAndIncreasingDateRanges() {
        assertNull(travelDateValidationMessage("2026-11-20", "2026-11-20"))
        assertNull(travelDateValidationMessage("2026-11-20", "2026-11-21"))
    }
}
