package com.example.myapplication

import com.example.myapplication.ui.screens.visibleDocumentControlTabIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentControlTabMappingTest {
    @Test
    fun salesCrmLogicalTabMapsAroundHiddenClientPortal() {
        assertEquals(3, visibleDocumentControlTabIndex(4, canViewClientPortal = false, canViewSalesCrm = true))
    }

    @Test
    fun adminCrmTabRetainsItsPositionWhenAllTabsAreVisible() {
        assertEquals(4, visibleDocumentControlTabIndex(4, canViewClientPortal = true, canViewSalesCrm = true))
    }

    @Test
    fun unavailableLogicalTabFallsBackToFirstVisibleTab() {
        assertEquals(0, visibleDocumentControlTabIndex(4, canViewClientPortal = false, canViewSalesCrm = false))
    }

    @Test
    fun rememberedOrStaleTabIndexCanBeClampedToTheRenderedRange() {
        assertEquals(3, 4.coerceIn(0, 3))
        assertEquals(0, (-1).coerceIn(0, 3))
    }
}
