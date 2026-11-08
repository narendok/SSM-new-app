package com.example.myapplication

import com.example.myapplication.data.model.AutomotiveProjects
import com.example.myapplication.data.model.CanonicalLifecycleEngine
import com.example.myapplication.data.model.CanonicalProjectTrack
import com.example.myapplication.data.model.ControlledDoc
import com.example.myapplication.data.model.LifecycleScenarios
import com.example.myapplication.data.repository.ScmsRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalLifecycleTest {

    @Test
    fun testSevenCanonicalTracksAndNormalization() {
        assertEquals(7, CanonicalProjectTrack.entries.size)
        assertEquals(CanonicalProjectTrack.READY_MADE, CanonicalProjectTrack.normalize("ready_made"))
        assertEquals(CanonicalProjectTrack.READY_MADE, CanonicalProjectTrack.normalize("readymade"))
        assertEquals(CanonicalProjectTrack.CUSTOM_HARDWARE, CanonicalProjectTrack.normalize("custom_hardware"))
        assertEquals(CanonicalProjectTrack.CUSTOM_HARDWARE, CanonicalProjectTrack.normalize("hw_only"))
        assertEquals(CanonicalProjectTrack.FIRMWARE_ONLY, CanonicalProjectTrack.normalize("firmware_only"))
        assertEquals(CanonicalProjectTrack.SOFTWARE_ONLY, CanonicalProjectTrack.normalize("software_only"))
        assertEquals(CanonicalProjectTrack.HARDWARE_FIRMWARE, CanonicalProjectTrack.normalize("hardware_firmware"))
        assertEquals(CanonicalProjectTrack.HARDWARE_FIRMWARE, CanonicalProjectTrack.normalize("hw+fw"))
        assertEquals(CanonicalProjectTrack.FIRMWARE_SOFTWARE, CanonicalProjectTrack.normalize("firmware_software"))
        assertEquals(CanonicalProjectTrack.FULL_SYSTEM, CanonicalProjectTrack.normalize("full_system"))
        assertEquals(CanonicalProjectTrack.FULL_SYSTEM, CanonicalProjectTrack.normalize(null))
    }

    @Test
    fun testFreshProjectInvariantOnlyCM01Ready() {
        val completed = emptySet<String>()
        // On a fresh project with 0 completed stages, only CM_01 is ready
        val cm01State = CanonicalLifecycleEngine.computeStageState("CM_01", CanonicalProjectTrack.FULL_SYSTEM, completed)
        assertEquals("ready", cm01State)

        // All subsequent stages must be planned, not ready
        val cm02State = CanonicalLifecycleEngine.computeStageState("CM_02", CanonicalProjectTrack.FULL_SYSTEM, completed)
        assertEquals("planned", cm02State)

        val int01State = CanonicalLifecycleEngine.computeStageState("INT_01", CanonicalProjectTrack.FULL_SYSTEM, completed)
        assertEquals("planned", int01State)

        val cl01State = CanonicalLifecycleEngine.computeStageState("CL_01", CanonicalProjectTrack.FULL_SYSTEM, completed)
        assertEquals("planned", cl01State)

        // Bypassing prerequisite via in_progress input must be strictly rejected
        val bypassedState = CanonicalLifecycleEngine.computeStageState(
            stageCode = "CL_01",
            track = CanonicalProjectTrack.FULL_SYSTEM,
            completedStages = completed,
            inProgressStages = setOf("CL_01")
        )
        assertEquals("planned", bypassedState)
    }

    @Test
    fun testPreProjectFeasibilityRequiresOnlyCM01() {
        // HW_01, FW_01, SW_01 require only CM_01 (enquiry intake), NOT CM_03/CM_04 kickoff
        val hw01Prereqs = CanonicalLifecycleEngine.resolvePrerequisites("HW_01", CanonicalProjectTrack.CUSTOM_HARDWARE)
        assertEquals(listOf("CM_01"), hw01Prereqs)

        val fw01Prereqs = CanonicalLifecycleEngine.resolvePrerequisites("FW_01", CanonicalProjectTrack.FIRMWARE_ONLY)
        assertEquals(listOf("CM_01"), fw01Prereqs)

        val sw01Prereqs = CanonicalLifecycleEngine.resolvePrerequisites("SW_01", CanonicalProjectTrack.SOFTWARE_ONLY)
        assertEquals(listOf("CM_01"), sw01Prereqs)

        // Once CM_01 is completed, feasibility stages become ready before project kickoff
        val completed = setOf("CM_01")
        assertEquals("ready", CanonicalLifecycleEngine.computeStageState("HW_01", CanonicalProjectTrack.CUSTOM_HARDWARE, completed))
        assertEquals("ready", CanonicalLifecycleEngine.computeStageState("FW_01", CanonicalProjectTrack.FIRMWARE_ONLY, completed))
        assertEquals("ready", CanonicalLifecycleEngine.computeStageState("SW_01", CanonicalProjectTrack.SOFTWARE_ONLY, completed))
    }

    @Test
    fun testNonCircularReleaseReadiness() {
        // CL_01 release readiness depends on domain verification evidence, never deployment/closure
        val rmRelease = CanonicalLifecycleEngine.resolvePrerequisites("CL_01", CanonicalProjectTrack.READY_MADE)
        assertEquals(listOf("RM_02"), rmRelease)

        val swRelease = CanonicalLifecycleEngine.resolvePrerequisites("CL_01", CanonicalProjectTrack.SOFTWARE_ONLY)
        assertEquals(listOf("SW_09"), swRelease)

        val fwRelease = CanonicalLifecycleEngine.resolvePrerequisites("CL_01", CanonicalProjectTrack.FIRMWARE_ONLY)
        assertEquals(listOf("FW_11"), fwRelease)

        val hwRelease = CanonicalLifecycleEngine.resolvePrerequisites("CL_01", CanonicalProjectTrack.CUSTOM_HARDWARE)
        assertEquals(listOf("HW_32"), hwRelease)

        val coupledRelease = CanonicalLifecycleEngine.resolvePrerequisites("CL_01", CanonicalProjectTrack.FULL_SYSTEM)
        assertEquals(listOf("INT_02"), coupledRelease)
    }

    @Test
    fun testCanonicalSampleProjectsCatalogue() {
        val samples = CanonicalLifecycleEngine.SAMPLES
        assertEquals(9, samples.size)

        // Exactly 8 projects + 1 declined enquiry
        val projects = samples.filter { !it.isDeclinedEnquiry }
        assertEquals(8, projects.size)

        val p01 = samples.find { it.sampleId == "LC2-P01" }!!
        assertEquals("PRJ-INT-001", p01.projectCode)
        assertEquals(CanonicalProjectTrack.CUSTOM_HARDWARE, p01.track)
        assertEquals("Internal Harness & EOL Test Jig", p01.projectName)

        val p08 = samples.find { it.sampleId == "LC2-P08" }!!
        assertEquals("PRJ-RM-001", p08.projectCode)
        assertEquals(CanonicalProjectTrack.READY_MADE, p08.track)
        assertEquals("Commercial Catalog OBD Tracker Rollout", p08.projectName)

        val declinedEnquiry = samples.find { it.isDeclinedEnquiry }!!
        assertEquals("LC2-E02", declinedEnquiry.sampleId)
        assertNull(declinedEnquiry.projectCode)
        assertEquals("Nordic Fleet Logistics AB", declinedEnquiry.clientName)
    }

    @Test
    fun testScenarioS02DeclinedEnquiryAndS06ProductionReconciliation() {
        val s02 = LifecycleScenarios.get("S02")!!
        assertNull(s02.projectCode)
        assertEquals("ENQ-2026-EXT-01", s02.enquiryCode)

        val s06 = LifecycleScenarios.get("S06")!!
        val recon = s06.productionReconciliation!!
        // Simulated internal QA pass placeholders + 5 held + 45 pending = 200 sample units
        assertEquals(200, recon.lotQuantity)
        assertEquals(200, recon.simulatedQaPassUnits + recon.heldUnits + recon.pendingInspectionUnits)
        // 500 total - 200 lot = 300 remaining planned
        assertEquals(500, recon.totalPlannedUnits)
        assertEquals(300, recon.remainingPlannedUnits)
        assertEquals(recon.totalPlannedUnits - recon.lotQuantity, recon.remainingPlannedUnits)
    }

    @Test
    fun testCanonicalPortfolioWebToAndroidIdMapping() {
        val allProjects = AutomotiveProjects.all

        // 1. Enquiry only: Web LC2-E02 <-> Android DEMO-PORT-01-ENQ
        val enq = allProjects.firstOrNull { it.id == "DEMO-PORT-01-ENQ" }
        assertNotNull("DEMO-PORT-01-ENQ must exist in project catalog", enq)
        assertEquals("Stage 1: Intake & Opportunity", enq?.stage)
        assertEquals("Enquiry Only", enq?.status)
        assertTrue(enq?.title?.contains("Smart Irrigation Sensor Node") == true)

        // 2. S10 Feasibility: Web DEMO-PROJ-010 <-> Android DEMO-PROJ-010
        val s10 = allProjects.firstOrNull { it.id == "DEMO-PROJ-010" }
        assertNotNull("DEMO-PROJ-010 must exist in project catalog", s10)
        assertEquals("Stage 2: Feasibility & Requirements Review", s10?.stage)
        assertEquals("Pending Feasibility", s10?.status)
        assertTrue(s10?.title?.contains("Northstar Field Systems") == true)

        // 3. HW-only track: Web LC2-P01 <-> Android DEMO-PORT-03-FRZ
        val frz = allProjects.firstOrNull { it.id == "DEMO-PORT-03-FRZ" }
        assertNotNull("DEMO-PORT-03-FRZ must exist in project catalog", frz)
        assertEquals("Stage 3: Simulated Requirements Freeze & HW Kickoff", frz?.stage)
        assertEquals("Freeze Workflow Complete · Local TEST", frz?.status)
        assertTrue(frz?.title?.contains("Cedar Ridge") == true)

        // 4. HW+FW track: Web LC2-P03 <-> Android DEMO-PORT-04-HF
        val hf = allProjects.firstOrNull { it.id == "DEMO-PORT-04-HF" }
        assertNotNull("DEMO-PORT-04-HF must exist in project catalog", hf)
        assertEquals("Stage 4: Engineering Design Review (TEST)", hf?.stage)
        assertEquals("HW + FW Review · Local TEST", hf?.status)
        assertTrue(hf?.title?.contains("Juniper E-Mobility") == true)

        // 5. HW+FW+SW multi-track: fictional TEST portfolio project HLPL-AI4T-01
        val multi = allProjects.firstOrNull { it.id == "HLPL-AI4T-01" }
        assertNotNull("HLPL-AI4T-01 must exist in project catalog", multi)
        assertEquals("Stage 4: Design & Development Review (TEST)", multi?.stage)
        assertEquals("In Progress · Local TEST", multi?.status)
        assertTrue(multi?.title?.contains("Northstar Field Gateway") == true)

        // 6. QA / service-feedback: Web LC2-P05 <-> Android DEMO-PORT-06-QA
        val qa = allProjects.firstOrNull { it.id == "DEMO-PORT-06-QA" }
        assertNotNull("DEMO-PORT-06-QA must exist in project catalog", qa)
        assertEquals("Stage 6: QA Checklist Review (TEST)", qa?.stage)
        assertEquals("QA / Synthetic Service Feedback · Local TEST", qa?.status)
        assertTrue(qa?.title?.contains("Frostline Cargo") == true)
    }

    @Test
    fun testCanonicalPortfolioTasksAndHandoffsIntegrity() {
        val repo = ScmsRepositoryImpl()
        val allTasks = repo.tasks.value
        val allHandoffs = repo.handoffs.value

        // Every portfolio project has tasks and handoffs
        val portfolioIds = listOf(
            "DEMO-PORT-01-ENQ",
            "DEMO-PROJ-010",
            "DEMO-PORT-03-FRZ",
            "DEMO-PORT-04-HF",
            "HLPL-AI4T-01",
            "DEMO-PORT-06-QA"
        )

        portfolioIds.forEach { projId ->
            val tasks = allTasks.filter { it.projectId == projId }
            assertTrue("Project $projId must have tasks", tasks.isNotEmpty())

            val handoffs = allHandoffs.filter { it.projectId == projId }
            assertTrue("Project $projId must have handoffs", handoffs.isNotEmpty())
        }

        // Verify realistic blocked tasks state their exact blockers
        val blockedTasks = allTasks.filter { it.title.contains("BLOCKED:", ignoreCase = true) }
        assertTrue("Portfolio must contain realistic blocked tasks", blockedTasks.size >= 4)
        assertTrue(blockedTasks.any { it.title.contains("awaiting Stage 3 freeze") && it.projectId == "DEMO-PROJ-010" })
        assertTrue(blockedTasks.any { it.title.contains("awaiting Gerber sign-off") && it.projectId == "DEMO-PORT-03-FRZ" })
        assertTrue(blockedTasks.any { it.title.contains("awaiting carrier APN activation") && it.projectId == "DEMO-PORT-04-HF" })
        assertTrue(blockedTasks.any { it.title.contains("awaiting thermal cycling completion") && it.projectId == "DEMO-PORT-06-QA" })
    }

    @Test
    fun testCanonicalPortfolioControlledDocumentsDistinguishAutoDraftAndUploadBinary() {
        val repo = ScmsRepositoryImpl()
        val allDocs = repo.documents.value

        val portfolioIds = listOf(
            "DEMO-PORT-01-ENQ",
            "DEMO-PROJ-010",
            "DEMO-PORT-03-FRZ",
            "DEMO-PORT-04-HF",
            "HLPL-AI4T-01",
            "DEMO-PORT-06-QA"
        )

        portfolioIds.forEach { projId ->
            val docs = allDocs.filter { it.projectId == projId }
            assertTrue("Project $projId must have controlled docs", docs.isNotEmpty())

            val autoDrafts = docs.filter { it.documentKind == "AUTO_DRAFT" }
            assertTrue("Project $projId must have AUTO_DRAFT docs for structured text records", autoDrafts.isNotEmpty())

            // Projects beyond Stage 1 have binary deliverables (schematic, gerbers, firmware, test evidence)
            if (projId != "DEMO-PORT-01-ENQ") {
                val uploadBinaries = docs.filter { it.documentKind == "UPLOAD_BINARY" }
                assertTrue("Project $projId must have UPLOAD_BINARY docs for externally authored artifacts", uploadBinaries.isNotEmpty())
            }
        }

        // Draft saving must never mark document approved or released
        val testDoc = allDocs.first { it.documentKind == "AUTO_DRAFT" && it.projectId == "DEMO-PORT-03-FRZ" }
        val draftUpdate = testDoc.copy(
            draftContent = "Updated requirements text in local test",
            status = "Draft · local test"
        )
        repo.addControlledDoc(draftUpdate)

        val updatedDoc = repo.documents.value.first { it.docId == testDoc.docId }
        assertEquals("Draft · local test", updatedDoc.status)
        assertFalse(updatedDoc.status.contains("Approved", ignoreCase = true))
        assertFalse(updatedDoc.status.contains("Released", ignoreCase = true))
        assertFalse(updatedDoc.status.contains("Frozen", ignoreCase = true))
    }

    @Test
    fun testWorkspaceResetRestoresCanonicalPortfolio() {
        val repo = ScmsRepositoryImpl()

        // Mutate state
        repo.moveTaskLane("TSK-FRZ-03", "Completed")
        repo.selectProject("HLPL-AI4T-01")

        // Reset
        repo.resetSampleWorkspace()

        // Verified reset
        assertEquals("DEMO-PROJ-010", repo.selectedProject.value)
        val resetTask = repo.tasks.value.first { it.id == "TSK-FRZ-03" }
        assertEquals("Today", resetTask.lane)

        // All 6 portfolio projects are present in deliverables and docs
        val deliverables = repo.clientDeliverables.value
        val portfolioIds = listOf("DEMO-PORT-01-ENQ", "DEMO-PROJ-010", "DEMO-PORT-03-FRZ", "DEMO-PORT-04-HF", "HLPL-AI4T-01", "DEMO-PORT-06-QA")
        portfolioIds.forEach { projId ->
            assertTrue("Deliverables must contain $projId", deliverables.any { it.projectId == projId })
            assertTrue("Documents must contain $projId", repo.documents.value.any { it.projectId == projId })
        }
    }
}
