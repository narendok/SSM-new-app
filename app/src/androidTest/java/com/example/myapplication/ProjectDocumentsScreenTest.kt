package com.example.myapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import com.example.myapplication.data.model.ProjectMetadata
import com.example.myapplication.data.model.ControlledDoc
import com.example.myapplication.data.repository.ScmsRepositoryImpl
import com.example.myapplication.ui.screens.FullDocumentSheetView
import com.example.myapplication.ui.screens.SharedDocumentSheetView
import com.example.myapplication.ui.screens.ProjectDocumentsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProjectDocumentsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test fun sharedOriginalRevisionsShowExactNameAndRevision() {
        val doc = com.example.myapplication.data.model.SharedAccess.sharedDocuments(testSnapshot, "project-1").first()
        var requested = ""
        composeRule.setContent {
            MyApplicationTheme {
                SharedDocumentSheetView(doc, {}, loadOriginalRevisions = { id ->
                    requested = id
                    Json.parseToJsonElement("""[{"id":"file-1","revision":"03","original_name":"schematic.pdf"}]""") as kotlinx.serialization.json.JsonArray
                }, originalDownload = { "https://example.invalid/unused" })
            }
        }
        composeRule.onNodeWithText("schematic.pdf · Rev 03").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Open original").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(doc.id, requested) }
    }

    @Test fun sharedOriginalLoadFailureIsVisibleWithoutClaimingAFileExists() {
        val doc = com.example.myapplication.data.model.SharedAccess.sharedDocuments(testSnapshot, "project-1").first()
        composeRule.setContent {
            MyApplicationTheme {
                SharedDocumentSheetView(doc, {}, loadOriginalRevisions = { throw SecurityException("Denied") })
            }
        }
        composeRule.onNodeWithText("Original revisions could not be loaded. Reopen this document to retry.")
            .performScrollTo().assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Open original").fetchSemanticsNodes().isEmpty())
    }

    private val testSnapshot = Json.parseToJsonElement("""{
      "membership":{"role":"department_lead","department_code":"EHW","membership_id":"member-hw"},
      "departments":[{"id":"dept-hw","code":"EHW"}],
      "records":[
        {"id":"project-1","record_type":"Project","reference":"P-001","title":"Tracker PoC","stage":"EVT","status":"Active"},
        {"id":"project-2","record_type":"Project","reference":"P-002","title":"Body Controller","stage":"DVT","status":"Active"},
        {"id":"record-1","record_type":"PRS","reference":"PRS-01","fields":{"project_id":"project-1"}}
      ],
      "documents":[
        {"id":"doc-p1","title":"Tracker PoC Charter","document_type":"Specification","department_id":"dept-hw","record_id":"project-1","created_at":"2026-10-31T10:00:00Z"},
        {"id":"doc-p1-prs","title":"Hardware PRS","document_type":"Requirement","department_id":"dept-hw","record_id":"record-1","created_at":"2026-10-31T11:00:00Z"},
        {"id":"doc-unlinked","title":"Unlinked EHW Note","document_type":"Note","department_id":"dept-hw","record_id":"","created_at":"2026-10-31T12:00:00Z"}
      ]
    }""").jsonObject

    private val catalog = listOf(
        ProjectMetadata("project-1", "P-001", "Tracker PoC", "", "", "", "", "Active", ""),
        ProjectMetadata("project-2", "P-002", "Body Controller", "", "", "", "", "Active", "")
    )

    @Test
    fun generatedDocumentPreviewClearlyShowsFictionalDraftWithoutCustomerAcceptanceClaims() {
        val document = ControlledDoc(
            docId = "TEST-PRJ-PRS-01",
            title = "Product Requirement Specification · Local Draft",
            dept = "EHW",
            stage = "Stage 2",
            owner = "TEST owner",
            revision = "Rev 0.1",
            status = "Draft · local TEST",
            projectId = "DEMO-PROJ-010",
            documentKind = "AUTO_DRAFT",
            draftContent = "Fictional assumptions for local workflow testing only."
        )

        composeRule.setContent {
            MyApplicationTheme {
                FullDocumentSheetView(
                    doc = document,
                    scmsRepository = ScmsRepositoryImpl(),
                    onClose = {},
                    onSignedUrlGenerated = {}
                )
            }
        }

        composeRule.onNodeWithText("FICTIONAL TEST DRAFT · Illustrative assumptions only.", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3.0 Stage Exit Checklist · Not Verified").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("○ Customer review — no real customer decision is represented").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("FICTIONAL TEST WORKSPACE · NOT A CONTROLLED COPY").performScrollTo().assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Tata Motors", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("SOR Accepted", substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun authenticatedDocumentsDisplaysDepartmentMetadataAndClientSideViewFilter() {
        val repository = ScmsRepositoryImpl()

        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    ProjectDocumentsScreen(
                        scmsRepository = repository,
                        isAuthenticated = true,
                        projectCatalog = catalog,
                        selectedProjectId = "ALL",
                        sharedSnapshot = testSnapshot
                    )
                }
            }
        }

        // 1. Department-scoped metadata and client-side view filter labels
        composeRule.onNodeWithText("Department Documents · All Projects View").assertIsDisplayed()
        composeRule.onNodeWithText("All document metadata returned within your authorized department scope").assertIsDisplayed()
        composeRule.onNodeWithText("PROJECT VIEW FILTER (CLIENT-SIDE):").assertIsDisplayed()
        composeRule.onNodeWithText("All projects (view filter)").assertIsDisplayed()
        composeRule.onNodeWithText("View filter only").assertIsDisplayed()
        composeRule.onNodeWithText("Department-Authorized Metadata · Client-Side Project View Filter (get_shared_test_workspace)", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        // 2. Documents within department scope are shown in "ALL" view
        composeRule.onNodeWithText("Tracker PoC Charter").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Hardware PRS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Unlinked EHW Note").performScrollTo().assertIsDisplayed()

        // 4. Authenticated mode hides local demo documents, draft action, mock signed-url tokens, and hardware master data
        assertTrue(composeRule.onAllNodesWithText("Hardware Master Data").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Draft").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("300s Secure Signed Storage Token Generated ✓").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Tata Ace EV 4G Telematics (HLPL-AI4T-01)").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Client: Tata Motors Ltd", substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun selectedSharedProjectExposesSingleDialogWithOriginalUpload() {
        val repository = ScmsRepositoryImpl()
        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    ProjectDocumentsScreen(
                        scmsRepository = repository,
                        isAuthenticated = true,
                        projectCatalog = catalog,
                        selectedProjectId = "project-1",
                        sharedSnapshot = testSnapshot,
                        createSharedDocument = { _, _, _, _, _, _, _ -> "file-id" }
                    )
                }
            }
        }
        composeRule.onNodeWithText("Add document").performClick()
        composeRule.onNodeWithText("Add project document").assertIsDisplayed()
        composeRule.onNodeWithText("Choose original file").assertIsDisplayed()
        composeRule.onNodeWithText("Save draft & upload").assertIsDisplayed()
        composeRule.onNodeWithText("P-001 · Tracker PoC").assertIsDisplayed()
    }

    @Test
    fun projectSelectionFiltersAlreadyReturnedListClientSide() {
        val repository = ScmsRepositoryImpl()

        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    ProjectDocumentsScreen(
                        scmsRepository = repository,
                        isAuthenticated = true,
                        projectCatalog = catalog,
                        selectedProjectId = "project-1",
                        sharedSnapshot = testSnapshot
                    )
                }
            }
        }

        // Header shows specific project view filter
        composeRule.onNodeWithText("Tracker PoC (P-001)").assertIsDisplayed()
        composeRule.onNodeWithText("Project view filter (client-side) · Stage: Active · Status: Active").assertIsDisplayed()

        // Linked documents appear
        composeRule.onNodeWithText("Tracker PoC Charter").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Hardware PRS").performScrollTo().assertIsDisplayed()

        // Documents not explicitly linked to project-1 are filtered out client-side
        assertTrue(composeRule.onAllNodesWithText("Unlinked EHW Note").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun unavailableSnapshotAndEmptyProjectFilterShowClearStates() {
        val repository = ScmsRepositoryImpl()

        // Test unavailable snapshot
        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    ProjectDocumentsScreen(
                        scmsRepository = repository,
                        isAuthenticated = true,
                        projectCatalog = catalog,
                        selectedProjectId = "ALL",
                        sharedSnapshot = null
                    )
                }
            }
        }

        composeRule.onNodeWithText("Shared document metadata unavailable").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("The server workspace snapshot could not be loaded or you do not have an active TEST membership. Local demo records are hidden while authenticated.").assertIsDisplayed()

        // No demo documents or actions exposed
        assertTrue(composeRule.onAllNodesWithText("Draft").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Tata Ace EV", substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun projectWithNoLinkedDocumentsShowsEmptyFilterState() {
        val repository = ScmsRepositoryImpl()

        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    ProjectDocumentsScreen(
                        scmsRepository = repository,
                        isAuthenticated = true,
                        projectCatalog = catalog,
                        selectedProjectId = "project-2",
                        sharedSnapshot = testSnapshot
                    )
                }
            }
        }

        composeRule.onNodeWithText("Body Controller (P-002)").assertIsDisplayed()
        composeRule.onNodeWithText("No department documents linked to this project").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "No documents returned within your department scope have an explicit link to \"Body Controller\". (Project selection is a client-side view filter; backend access is scoped by department.)"
        ).assertIsDisplayed()

        // No documents rendered
        assertTrue(composeRule.onAllNodesWithText("Tracker PoC Charter").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Hardware PRS").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Unlinked EHW Note").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun clickingAuthenticatedDocumentOpensMetadataOnlySheet() {
        val repository = ScmsRepositoryImpl()

        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    ProjectDocumentsScreen(
                        scmsRepository = repository,
                        isAuthenticated = true,
                        projectCatalog = catalog,
                        selectedProjectId = "ALL",
                        sharedSnapshot = testSnapshot
                    )
                }
            }
        }

        composeRule.onNodeWithText("Tracker PoC Charter").performScrollTo().performClick()

        // Metadata inspection sheet
        composeRule.onNodeWithText("Controlled Document").assertIsDisplayed()
        composeRule.onNodeWithText("Project Link").assertIsDisplayed()
        composeRule.onNodeWithText("Server Document ID").assertIsDisplayed()
        composeRule.onNodeWithText("Original access is unavailable in this view.").performScrollTo().assertIsDisplayed()

        // Close sheet
        composeRule.onNodeWithContentDescription("Close").performClick()
    }

    @Test
    fun demoModeHardwareMasterDataDisplaysUnverifiedStatusAndReconciledBom() {
        val repository = ScmsRepositoryImpl()
        repository.selectProject("DEMO-PROJ-010")

        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    ProjectDocumentsScreen(
                        scmsRepository = repository,
                        isAuthenticated = false
                    )
                }
            }
        }

        // Navigate to Hardware Master Data sub-tab
        composeRule.onNodeWithText("Hardware Master Data").performClick()

        // 1. Unverified Evidence & Certification Notice
        composeRule.onNodeWithText("CONFIGURATION CERTIFICATION: PENDING / NOT CERTIFIED").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Local TEST draft reference — not customer-issued", substring = true).assertIsDisplayed()

        // 2. Exact Inventory Matches and No-Match placeholders
        composeRule.onNodeWithText("Inventory Link: EG915N-EU").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Inventory Link: STM32G0B1VCT6").performScrollTo().assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("No catalogue match").fetchSemanticsNodes().isNotEmpty())

        // 3. Reconciled BOM Subtotal Arithmetic and Partial Labeling
        composeRule.onNodeWithText("Included Lines Subtotal").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("$31.10 (Partial)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Exclusions:", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun localDemoPortfolioProjectSelectionDisplaysStageExitDeliverablesAndActions() {
        val repository = ScmsRepositoryImpl()
        repository.selectProject("DEMO-PROJ-010")

        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    ProjectDocumentsScreen(
                        scmsRepository = repository,
                        isAuthenticated = false
                    )
                }
            }
        }

        // 1. Local-only unshared device banner is displayed
        composeRule.onNodeWithText(
            "Local TEST Workspace · fictional sample data only",
            substring = true
        ).assertIsDisplayed()

        // 2. Stage Exit Docs tab is default (tab 0) and shows stage exit deliverables
        assertTrue(composeRule.onAllNodesWithText("Stage exit deliverables", substring = true).fetchSemanticsNodes().isNotEmpty())

        // 3. AUTO_DRAFT records show Edit button
        composeRule.onAllNodesWithText("Edit").fetchSemanticsNodes().let { nodes ->
            assertTrue("Must show Edit buttons for AUTO_DRAFT deliverables", nodes.isNotEmpty())
        }

        // 4. Filter to EHW and scroll to Stage 4 (index 3) to bring Stage 4 binary deliverables (schematic/Gerber) into view
        composeRule.onNodeWithText("EHW").performClick()
        composeRule.onAllNodes(hasScrollToIndexAction())[1].performScrollToIndex(3)
        assertTrue(
            "Must show Upload button for EHW binary deliverables (schematic/Gerber)",
            composeRule.onAllNodesWithText("Upload").fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun allProjectsViewExplainsThatChecklistNeedsSpecificProject() {
        val repository = ScmsRepositoryImpl()
        repository.selectProject("DEMO-PROJ-010")

        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    ProjectDocumentsScreen(
                        scmsRepository = repository,
                        isAuthenticated = false,
                        projectCatalog = com.example.myapplication.data.model.AutomotiveProjects.all
                    )
                }
            }
        }

        composeRule.runOnIdle { repository.selectProject("ALL") }
        composeRule.waitForIdle()
        assertEquals("ALL", repository.selectedProject.value)
        composeRule.onNodeWithText("All projects (view filter)").assertIsDisplayed()
        composeRule.onNodeWithText("Select a project to view its stage-exit checklist.").assertIsDisplayed()
    }

}
