package com.example.myapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.myapplication.data.model.AutomotiveProjects
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.ProjectMetadata
import com.example.myapplication.data.model.SharedAccess
import com.example.myapplication.data.repository.ScmsRepository
import com.example.myapplication.data.repository.ScmsRepositoryImpl
import com.example.myapplication.ui.components.ProjectFilterDropdown
import com.example.myapplication.ui.screens.CommandCenterScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class S10ProjectSelectionUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun CommandCenterWithDropdown(
        repository: ScmsRepository,
        catalog: List<ProjectMetadata>,
        isAuthenticated: Boolean = false
    ) {
        val selectedProject by repository.selectedProject.collectAsState()
        MyApplicationTheme {
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                Column(Modifier.fillMaxSize()) {
                    ProjectFilterDropdown(
                        selectedProjectId = selectedProject,
                        onProjectSelected = { repository.selectProject(it) },
                        projects = catalog
                    )
                    Box(Modifier.weight(1f)) {
                        CommandCenterScreen(
                            scmsRepository = repository,
                            onOpenRoleSwitcher = {},
                            isAuthenticated = isAuthenticated,
                            projectCatalog = catalog
                        )
                    }
                }
            }
        }
    }

    @Test
    fun selectingDemoProj010RetainsProjectAndDisplaysTestWorkspaceBannerAndResetAction() {
        val repository = ScmsRepositoryImpl()

        composeRule.setContent {
            CommandCenterWithDropdown(
                repository = repository,
                catalog = AutomotiveProjects.all,
                isAuthenticated = false
            )
        }

        // 1. Open project selector dropdown in local demo mode
        composeRule.onNodeWithContentDescription("Project Selector").performClick()

        // 2. Click the S10 fictional tracker project option
        composeRule.onNodeWithText("Northstar Field Systems — Offline-Capable 4G/GNSS Telematics Tracker PoC")
            .assertIsDisplayed()
            .performClick()

        // 3. Verify dropdown trigger retains the exact project reference
        assertTrue(
            "Dropdown retains DEMO-PROJ-010 selection",
            composeRule.onAllNodesWithText("DEMO-PROJ-010").fetchSemanticsNodes().isNotEmpty()
        )

        // 4. Verify ProjectOverviewBanner displays realistic project metadata & test workspace banner
        composeRule.onNodeWithText("Northstar Field Systems — Offline-Capable 4G/GNSS Telematics Tracker PoC").assertIsDisplayed()
        composeRule.onNodeWithText("Pending Feasibility").assertIsDisplayed()
        composeRule.onNodeWithText("Deliverable: Field Telematics Gateway (4G LTE Cat-1 / NavIC GNSS / LoRa Research · Unfrozen)").assertIsDisplayed()
        composeRule.onNodeWithText("Client: Northstar Field Systems (fictional client) · Total: ₹4.85 Lakh (PoC budget estimate · unapproved)").assertIsDisplayed()

        // 5. Verify prominent TEST WORKSPACE banner and Reset Workspace button are displayed
        composeRule.onNodeWithText("TEST WORKSPACE · SIMULATION ENVIRONMENT").assertIsDisplayed()
        composeRule.onNodeWithText("Reset Workspace").assertIsDisplayed()

        // 6. Verify Architecture evaluation card is visible
        composeRule.onNodeWithText("ARCHITECTURE CANDIDATE EVALUATION (UNRESOLVED):", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun nonAdminDemoRoleSeesTestBannerButNotResetAction() {
        val repository = ScmsRepositoryImpl()
        repository.switchRole(DepartmentRole.EHW)

        composeRule.setContent {
            CommandCenterWithDropdown(
                repository = repository,
                catalog = AutomotiveProjects.all,
                isAuthenticated = false
            )
        }

        composeRule.onNodeWithContentDescription("Project Selector").performClick()
        composeRule.onNodeWithText("Northstar Field Systems — Offline-Capable 4G/GNSS Telematics Tracker PoC")
            .performClick()

        composeRule.onNodeWithText("TEST WORKSPACE · SIMULATION ENVIRONMENT").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Reset Workspace").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun authenticatedModeStrictlyExcludesDemoProj010FromProjectCatalog() {
        val repository = ScmsRepositoryImpl()
        val authSnapshot = Json.parseToJsonElement("""{
            "records": [
                {"id": "auth-proj-1", "record_type": "Project", "reference": "PRJ-AUTH-001", "title": "Authorized Telematics"}
            ]
        }""").jsonObject
        val authenticatedCatalog = SharedAccess.projects(authSnapshot)

        composeRule.setContent {
            CommandCenterWithDropdown(
                repository = repository,
                catalog = authenticatedCatalog,
                isAuthenticated = true
            )
        }

        // 1. Open project selector in authenticated mode
        composeRule.onNodeWithContentDescription("Project Selector").performClick()

        // 2. Verify authorized project is present
        composeRule.onNodeWithText("PRJ-AUTH-001").assertIsDisplayed()

        // 3. Verify DEMO-PROJ-010 and fictional demo text are strictly excluded from authenticated selector
        assertTrue(composeRule.onAllNodesWithText("DEMO-PROJ-010").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Northstar Field Systems").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("TEST WORKSPACE · SIMULATION ENVIRONMENT").fetchSemanticsNodes().isEmpty())
    }
}
