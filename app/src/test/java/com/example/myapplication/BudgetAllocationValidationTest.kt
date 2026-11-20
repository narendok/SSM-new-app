package com.example.myapplication

import com.example.myapplication.ui.screens.budgetAllocationValidationMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetAllocationValidationTest {
    private fun validate(
        department: String? = "dept-id",
        category: String? = "Travel & Transport",
        amount: String? = "250000",
        currency: String? = "INR",
        period: String? = "FY2026-27",
        start: String? = "2026-04-01",
        end: String? = "2027-03-31",
        financeLimit: String? = ""
    ) = budgetAllocationValidationMessage(
        department, category, listOf("Travel & Transport", "Office Supplies"), amount,
        currency, period, start, end, financeLimit
    )

    @Test fun acceptsAnAuthorizedAllocationWithAnOptionalFinanceLimit() {
        assertNull(validate())
        assertNull(validate(financeLimit = "0"))
    }

    @Test fun requiresDepartmentAndCanonicalFinanceCategory() {
        assertEquals("Choose a department.", validate(department = ""))
        assertEquals(
            "Choose a Finance category from the authorized catalogue.",
            validate(category = "Local-only category")
        )
    }

    @Test fun validatesAmountCurrencyAndPeriod() {
        assertEquals("Enter an approved allocation greater than zero.", validate(amount = "0"))
        assertEquals("Enter a 3-letter currency code, such as INR.", validate(currency = "inr"))
        assertEquals("Enter the approved budget period reference.", validate(period = ""))
    }

    @Test fun validatesDateRangeAndFinanceLimit() {
        assertEquals("Choose a valid allocation start date.", validate(start = "01/04/2026"))
        assertEquals(
            "Allocation end date must be on or after the start date.",
            validate(start = "2026-04-02", end = "2026-04-01")
        )
        assertEquals("Finance limit must be zero or greater.", validate(financeLimit = "-1"))
    }
}
