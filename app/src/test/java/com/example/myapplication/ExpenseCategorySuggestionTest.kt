package com.example.myapplication

import com.example.myapplication.ui.screens.suggestedFinanceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseCategorySuggestionTest {
    private val categories=listOf("Travel & Transport","Engineering & Professional Services","Electronic Components & Sensors","Testing & External Laboratory Services","Maintenance & Calibration")

    @Test fun travelIsSuggestedButReimbursementDoesNotInventOne() {
        assertEquals("Travel & Transport",suggestedFinanceCategory("TG-03 Design", "travel", categories))
        assertNull(suggestedFinanceCategory("TG-03 Design", "reimbursement", categories))
    }

    @Test fun purchaseSuggestionUsesStageAndOnlyConfiguredCategories() {
        assertEquals("Electronic Components & Sensors",suggestedFinanceCategory("TG-03 Hardware design", "purchase", categories))
        assertNull(suggestedFinanceCategory("TG-03 Hardware design", "purchase", listOf("Travel & Transport")))
    }
}
