package com.example.myapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.repository.ScmsRepositoryImpl
import com.example.myapplication.ui.screens.DepartmentRecordsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class Stage5ReceivingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun projectDropdownSelectsCatalogProject() {
        val repository = ScmsRepositoryImpl()

        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    DepartmentRecordsScreen(scmsRepository = repository)
                }
            }
        }

        composeRule.onNodeWithContentDescription("Project Selector").performClick()
        composeRule.onNodeWithText("Cedar Ridge GPS Gateway (TEST)")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("HLPL-SLCT-01").assertIsDisplayed()
    }

    @Test
    fun receivingTabShowsUnavailableStateWithoutLocalSampleRecords() {
        val repository = ScmsRepositoryImpl()
        repository.switchRole(DepartmentRole.EMF)

        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    DepartmentRecordsScreen(scmsRepository = repository)
                }
            }
        }

        composeRule.onNodeWithText("Records").assertIsDisplayed()
        composeRule.onNodeWithText("Receiving").performClick()
        composeRule.onNodeWithText("Receiving unavailable").assertIsDisplayed()
        composeRule.onNodeWithText(
            "operational_receiving_purchase_orders",
            substring = true
        ).assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("STM32G0B1VCT6", substring = true)
                .fetchSemanticsNodes()
                .isEmpty()
        )

        composeRule.onNodeWithText("New").performClick()
        composeRule.onNodeWithText(
            "Local demo only — not shared operational data."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Save Record").assertIsNotEnabled()
    }
}
