package com.example.myapplication

import com.example.myapplication.data.model.AutomotiveProjects
import com.example.myapplication.data.model.LifecycleScenarios
import com.example.myapplication.data.model.SharedAccess
import com.example.myapplication.data.repository.ScmsRepositoryImpl
import com.example.myapplication.ui.components.resolveProjectSelection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class S10LifecycleScenarioTest {

    @Test
    fun test1_s10EnquiryProjectAndStageIdentityConsistency() {
        val s10 = LifecycleScenarios.get("S10")
        assertNotNull("S10 scenario must exist in LifecycleScenarios", s10)
        assertEquals("S10", s10?.id)
        assertEquals("LC2-P10-OFFLINE-TELEMATICS", s10?.fixtureKey)
        assertEquals("DEMO-PROJ-010", s10?.projectCode)
        assertEquals("DEMO-ENQ-010", s10?.enquiryCode)
        assertEquals("Northstar Field Systems (fictional client)", s10?.clientName)
        assertTrue(s10?.isExternalClient == true)
        assertEquals(
            "Northstar Field Systems — Offline-Capable 4G/GNSS Field Telematics Tracker PoC",
            s10?.title
        )

        // Lookup by project code and fixture key must resolve to the identical scenario
        val byProjCode = LifecycleScenarios.get("DEMO-PROJ-010")
        assertEquals(s10, byProjCode)
        val byKey = LifecycleScenarios.get("LC2-P10-OFFLINE-TELEMATICS")
        assertEquals(s10, byKey)

        // Selectable AutomotiveProjects metadata entry for DEMO-PROJ-010
        val projectMeta = AutomotiveProjects.findById("DEMO-PROJ-010")
        assertEquals("DEMO-PROJ-010", projectMeta.id)
        assertEquals("DEMO-PROJ-010", projectMeta.reference)
        assertEquals("Northstar Field Systems (fictional client)", projectMeta.client)
        assertEquals("Stage 2: Feasibility & Requirements Review", projectMeta.stage)
        assertEquals("Pending Feasibility", projectMeta.status)
        assertEquals(
            "Field Telematics Gateway (4G LTE Cat-1 / NavIC GNSS / LoRa Research · Unfrozen)",
            projectMeta.deliverable
        )
        assertEquals("10 units (PoC prototypes · preliminary)", projectMeta.volume)
        assertEquals("₹4.85 Lakh (PoC budget estimate · unapproved)", projectMeta.totalValue)
    }

    @Test
    fun test2_departmentRelevantDependenciesIsolatedWithoutLeakingUnrelatedDepartments() {
        val s10 = LifecycleScenarios.get("S10")!!
        val deps = s10.dependencies
        assertEquals("S10 must define exactly 4 department-scoped dependencies", 4, deps.size)

        // 1. Electronics / EHW scoping
        val ehwDeps = deps.filter { it.providerDept.contains("EHW") }
        assertEquals(1, ehwDeps.size)
        val ehw = ehwDeps.first()
        assertEquals("dep-s10-ehw-1", ehw.id)
        assertEquals("DEMO-PROD-010-EHW-FEAS", ehw.deliverableCode)
        assertEquals("Electronics Manufacturing / EHW", ehw.providerDept)
        assertEquals("Avery Vale", ehw.providerLead)
        assertTrue(ehw.criticalPath)

        // 2. SW / Systems Engineering scoping
        val swDeps = deps.filter { it.providerDept.contains("SW") }
        assertEquals(1, swDeps.size)
        val sw = swDeps.first()
        assertEquals("dep-s10-lora-1", sw.id)
        assertEquals("DEMO-PROD-010-SW-LO-RESEARCH", sw.deliverableCode)
        assertEquals("SW / Systems Engineering", sw.providerDept)
        assertEquals("Siddharth Roy", sw.providerLead)
        assertTrue(sw.criticalPath)

        // 3. Purchasing scoping
        val purDeps = deps.filter { it.providerDept.contains("Purchasing") }
        assertEquals(1, purDeps.size)
        val pur = purDeps.first()
        assertEquals("dep-s10-pur-1", pur.id)
        assertEquals("DEMO-PROD-010-PUR-FEAS", pur.deliverableCode)
        assertEquals("Purchasing", pur.providerDept)
        assertEquals("Morgan Reed", pur.providerLead)
        assertFalse(pur.criticalPath)

        // 4. Quality & Testing scoping
        val qaDeps = deps.filter { it.providerDept.contains("Quality") }
        assertEquals(1, qaDeps.size)
        val qa = qaDeps.first()
        assertEquals("dep-s10-qa-1", qa.id)
        assertEquals("DEMO-PROD-010-QA-PLAN", qa.deliverableCode)
        assertEquals("Quality & Testing", qa.providerDept)
        assertEquals("Jordan Blake", qa.providerLead)
        assertTrue(qa.criticalPath)

        // 5. Unrepresented departments must return 0 deliverables from S10 fixture
        val mechDeps = deps.filter { it.providerDept.contains("MECH") }
        assertTrue("MECH has no deliverables in S10 pre-freeze", mechDeps.isEmpty())

        val finDeps = deps.filter { it.providerDept.contains("Finance") || it.providerDept.contains("FIN") }
        assertTrue("FIN has no deliverables in S10 pre-freeze", finDeps.isEmpty())

        val hrDeps = deps.filter { it.providerDept.contains("HR") }
        assertTrue("HR has no deliverables in S10 pre-freeze", hrDeps.isEmpty())
    }

    @Test
    fun test3_pendingFeasibilityBlocksDownstreamProductionAndMustNotImplyApproval() {
        val s10 = LifecycleScenarios.get("S10")!!

        // Must NOT have production reconciliation math (unlike bulk production S06)
        assertNull("S10 must not have production reconciliation lot math", s10.productionReconciliation)

        // Stage remark must explicitly record test workspace assumptions and unresolved architecture
        assertTrue(s10.stageRemark.startsWith("TEST WORKSPACE ASSUMPTION"))
        assertTrue(s10.stageRemark.contains("unapproved test-fixture assumptions"))
        assertTrue(s10.stageRemark.contains("no customer sign-off or frozen baseline is claimed"))

        // Current position must record that preliminary feasibility evaluation is underway
        assertTrue(s10.currentPosition.contains("prerequisite requirement-freeze gate pending"))
        assertTrue(s10.currentPosition.contains("Pilot and bulk production are outside current authorization"))

        // Next action requires completing review gates before freeze
        assertTrue(s10.currentNextAction.contains("establish an authorized versioned requirement freeze"))

        // AutomotiveProjects metadata must mark pending feasibility and unapproved PoC
        val meta = AutomotiveProjects.findById("DEMO-PROJ-010")
        assertEquals("Pending Feasibility", meta.status)
        assertEquals("10 units (PoC prototypes · preliminary)", meta.volume)
        assertEquals("₹4.85 Lakh (PoC budget estimate · unapproved)", meta.totalValue)
        assertTrue(meta.deliverable.contains("Unfrozen"))
    }

    @Test
    fun test4_projectSelectionIsStrictlyAViewFilter() {
        // Selection resolution
        assertEquals(
            "DEMO-PROJ-010",
            resolveProjectSelection("DEMO-PROJ-010", AutomotiveProjects.all)
        )
        assertEquals(
            "ALL",
            resolveProjectSelection("ALL", AutomotiveProjects.all)
        )
        assertEquals(
            "ALL",
            resolveProjectSelection("NON_EXISTENT_PROJ", AutomotiveProjects.all)
        )

        // In-memory view filter semantics: does not alter source collections
        val allProjects = AutomotiveProjects.all
        val filteredForS10 = allProjects.filter { it.id == "DEMO-PROJ-010" }
        assertEquals(1, filteredForS10.size)
        assertEquals("DEMO-PROJ-010", filteredForS10.first().id)

        // The master catalog remains untouched
        assertTrue(allProjects.size >= 5)
        assertTrue(allProjects.any { it.id == "ALL" })
    }

    @Test
    fun test5_localDemoDataNeverAppearsAsSharedOrAuthenticatedState() {
        // Sample authenticated snapshot from shared workspace
        val authenticatedSnapshot = Json.parseToJsonElement("""{
            "membership": {
                "organization_id": "org-hex-001",
                "role": "engineer",
                "department_id": "dept-ehw"
            },
            "departments": [
                {"id": "dept-ehw", "code": "EHW", "name": "Electronics / Hardware"}
            ],
            "records": [
                {
                    "id": "proj-auth-001",
                    "record_type": "Project",
                    "reference": "PRJ-AUTH-2026-01",
                    "title": "Authenticated Fleet Gateway",
                    "stage": "DVT",
                    "status": "Active"
                }
            ],
            "tasks": [
                {
                    "id": "tsk-auth-01",
                    "title": "Bring-up DVT board",
                    "department_id": "dept-ehw",
                    "department_code": "EHW",
                    "status": "In progress",
                    "priority": "High",
                    "record_id": "proj-auth-001"
                }
            ],
            "documents": [
                {
                    "id": "doc-auth-01",
                    "title": "DVT Test Plan",
                    "department_id": "dept-ehw",
                    "record_id": "proj-auth-001"
                }
            ]
        }""").jsonObject

        // 1. SharedAccess derives projects exclusively from snapshot records (prepending "ALL")
        val authenticatedProjects = SharedAccess.projects(authenticatedSnapshot)
        assertEquals(2, authenticatedProjects.size)
        assertEquals("ALL", authenticatedProjects[0].id)
        assertEquals("proj-auth-001", authenticatedProjects[1].id)
        assertEquals("PRJ-AUTH-2026-01", authenticatedProjects[1].reference)

        // 2. Local demo projects (including DEMO-PROJ-010) must NEVER be in authenticated projects
        assertFalse(
            "Authenticated projects must not contain DEMO-PROJ-010",
            authenticatedProjects.any { it.id == "DEMO-PROJ-010" }
        )
        assertFalse(
            "Authenticated projects must not contain demo HLPL projects",
            authenticatedProjects.any { it.id == "HLPL-AI4T-01" }
        )

        // 3. Selection of DEMO-PROJ-010 against authenticated catalog safely falls back to ALL
        val resolvedInAuth = resolveProjectSelection("DEMO-PROJ-010", authenticatedProjects)
        assertEquals("ALL", resolvedInAuth)

        // 4. Tasks in authenticated mode come from snapshot, not demo tasks
        val authTasks = SharedAccess.rows(authenticatedSnapshot, "tasks")
        assertEquals(1, authTasks.size)
        assertEquals("tsk-auth-01", SharedAccess.text(authTasks.first(), "id"))
        assertFalse(
            "Authenticated tasks must not contain demo S10 tasks",
            authTasks.any { SharedAccess.text(it, "title").contains("DEMO") }
        )
    }

    @Test
    fun test6_linkedSampleRecordsAcrossFullLifecycle() {
        val repo = ScmsRepositoryImpl()

        // 1. Tasks: 8 connected tasks for DEMO-PROJ-010
        val s10Tasks = repo.tasks.value.filter { it.projectId == "DEMO-PROJ-010" }
        assertEquals("DEMO-PROJ-010 must have exactly 8 connected tasks", 8, s10Tasks.size)

        // Status distribution across lanes
        val completedTasks = s10Tasks.filter { it.lane == "Completed" }
        assertEquals(2, completedTasks.size)
        assertTrue(completedTasks.any { it.id == "TSK-10-01" && it.title.contains("Northstar Field Systems") })
        assertTrue(completedTasks.any { it.id == "TSK-10-02" && it.title.contains("power budget") })

        val todayTasks = s10Tasks.filter { it.lane == "Today" }
        assertEquals(2, todayTasks.size)
        assertTrue(todayTasks.any { it.id == "TSK-10-03" && it.title.contains("NavIC") })
        assertTrue(todayTasks.any { it.id == "TSK-10-04" && it.title.contains("LoRa") })

        val reviewTasks = s10Tasks.filter { it.lane == "Review" }
        assertEquals(2, reviewTasks.size)
        assertTrue(reviewTasks.any { it.id == "TSK-10-05" && it.title.contains("cellular backhaul") })
        assertTrue(reviewTasks.any { it.id == "TSK-10-06" && it.title.contains("lead-time sheet") })

        val tomorrowBlockedTasks = s10Tasks.filter { it.lane == "Tomorrow" }
        assertEquals(2, tomorrowBlockedTasks.size)
        assertTrue(tomorrowBlockedTasks.any { it.id == "TSK-10-07" && it.title.contains("BLOCKED") })
        assertTrue(tomorrowBlockedTasks.any { it.id == "TSK-10-08" && it.title.contains("BLOCKED") })

        // 2. Connected Handoffs
        val s10Handoffs = repo.handoffs.value.filter { it.projectId == "DEMO-PROJ-010" }
        assertEquals("DEMO-PROJ-010 must have 4 cross-department handoffs", 4, s10Handoffs.size)
        val hndSal = s10Handoffs.first { it.id == "HND-10-01" }
        assertEquals("SAL", hndSal.fromDept)
        assertEquals("EHW", hndSal.toDept)
        assertEquals("Accepted", hndSal.status)

        val hndSw = s10Handoffs.first { it.id == "HND-10-02" }
        assertEquals("EHW", hndSw.fromDept)
        assertEquals("SW", hndSw.toDept)
        assertEquals("Under Review", hndSw.status)

        val hndPur = s10Handoffs.first { it.id == "HND-10-03" }
        assertEquals("PUR", hndPur.fromDept)
        assertEquals("EHW", hndPur.toDept)
        assertEquals("Pending Acceptance", hndPur.status)

        val hndQa = s10Handoffs.first { it.id == "HND-10-04" }
        assertEquals("QA", hndQa.fromDept)
        assertEquals("EHW", hndQa.toDept)
        assertEquals("Pending Acceptance", hndQa.status)

        // 3. Customer ticket tkt-10-01 (Simulated Test Request)
        val s10Ticket = repo.serviceTickets.value.firstOrNull { it.projectId == "DEMO-PROJ-010" }
        assertNotNull("Must have customer ticket for DEMO-PROJ-010", s10Ticket)
        assertEquals("TKT-2026-1010", s10Ticket?.reference)
        assertTrue(s10Ticket?.requesterName?.contains("Dr. Evelyn Vance") == true)
        assertEquals("Northstar Field Systems (fictional client)", s10Ticket?.clientOrganization)
        assertEquals("High", s10Ticket?.priority)
        assertEquals(2, s10Ticket?.replies?.size)
        assertTrue(s10Ticket?.replies?.any { it.isInternalNote && it.authorName == "Avery Vale" } == true)
        assertTrue(s10Ticket?.replies?.any { !it.isInternalNote && it.authorName == "Siddharth Roy" } == true)

        // 4. Feasibility Reviews for DEMO-PROJ-010 (Unsigned Draft Assessments awaiting HOD review)
        repo.selectProject("DEMO-PROJ-010")
        val s10Reviews = repo.feasibilityReviews.value
        assertEquals(4, s10Reviews.size)
        assertTrue(s10Reviews.any { it.deptCode == "EHW" && it.leadName == "Avery Vale" })
        assertTrue(s10Reviews.any { it.deptCode == "SW" && it.leadName == "Siddharth Roy" })
        assertTrue(s10Reviews.any { it.deptCode == "PUR" && it.leadName == "Morgan Reed" })
        assertTrue(s10Reviews.any { it.deptCode == "QA" && it.leadName == "Jordan Blake" })

        // Must be unsigned draft assessments with zero synthetic signatures and no approval implied
        s10Reviews.forEach { review ->
            assertEquals("UNSIGNED_DRAFT_ASSESSMENT", review.verdict)
            assertNull("S10 review must not have signedAt timestamp", review.signedAt)
            assertNull("S10 review must not have signatureHash", review.signatureHash)
            assertTrue("Review must state no approval implied: ${review.clientHighlights}", review.clientHighlights.contains("no approval", ignoreCase = true))
            assertTrue("Internal remarks must state unapproved assumption: ${review.internalRemarks}", review.internalRemarks.contains("unapproved", ignoreCase = true))
        }
    }

    @Test
    fun test7_resetSampleWorkspaceRestoresBaselineSimulationState() {
        val repo = ScmsRepositoryImpl()

        // Mutate local state: move task lane, toggle task, update ticket status
        repo.moveTaskLane("TSK-10-03", "Completed")
        repo.toggleTask("TSK-10-04")
        repo.updateServiceTicketStatus("tkt-10-01", "Closed")
        repo.selectProject("HLPL-AI4T-01")

        // Verify state changed
        val movedTask = repo.tasks.value.first { it.id == "TSK-10-03" }
        assertEquals("Completed", movedTask.lane)
        val toggledTask = repo.tasks.value.first { it.id == "TSK-10-04" }
        assertTrue(toggledTask.isCompleted)
        val ticket = repo.serviceTickets.value.first { it.id == "tkt-10-01" }
        assertEquals("Closed", ticket.status)
        assertEquals("HLPL-AI4T-01", repo.selectedProject.value)

        // Execute reset
        repo.resetSampleWorkspace()

        // Verify restoration
        val restoredTask3 = repo.tasks.value.first { it.id == "TSK-10-03" }
        assertEquals("Today", restoredTask3.lane)
        assertFalse(restoredTask3.isCompleted)

        val restoredTask4 = repo.tasks.value.first { it.id == "TSK-10-04" }
        assertEquals("Today", restoredTask4.lane)
        assertFalse(restoredTask4.isCompleted)

        val restoredTicket = repo.serviceTickets.value.first { it.id == "tkt-10-01" }
        assertEquals("In Progress", restoredTicket.status)

        assertEquals("DEMO-PROJ-010", repo.selectedProject.value)

        val s10Reviews = repo.feasibilityReviews.value
        assertEquals(4, s10Reviews.size)
        assertTrue(s10Reviews.all { it.verdict == "UNSIGNED_DRAFT_ASSESSMENT" && it.signedAt == null })
    }

    @Test
    fun test8_simulatedTicketExplicitlyLabeledNotFromRealCustomer() {
        val repo = ScmsRepositoryImpl()
        val s10Ticket = repo.serviceTickets.value.first { it.id == "tkt-10-01" }

        // Must be explicitly labeled as simulated test request
        assertTrue(
            "Ticket subject must state SIMULATED TEST REQUEST: ${s10Ticket.subject}",
            s10Ticket.subject.contains("[SIMULATED TEST REQUEST", ignoreCase = true)
        )
        assertTrue(
            "Ticket description must state not from a real customer: ${s10Ticket.description}",
            s10Ticket.description.contains("NOT received from a real customer", ignoreCase = true)
        )

        // Technical calculations must be marked as editable, unapproved assumptions
        val hwReply = s10Ticket.replies.first { it.authorName == "Avery Vale" }
        assertTrue(
            "HW reply must mark calculations as editable unapproved test assumption",
            hwReply.message.contains("EDITABLE UNAPPROVED TEST ASSUMPTION", ignoreCase = true)
        )
        assertTrue(hwReply.message.contains("16MB SPI NOR flash"))
        assertTrue(hwReply.message.contains("42 uA"))

        val swReply = s10Ticket.replies.first { it.authorName == "Siddharth Roy" }
        assertTrue(
            "SW reply must mark response as draft only with unfrozen architecture",
            swReply.message.contains("SIMULATED DRAFT RESPONSE", ignoreCase = true) &&
                    swReply.message.contains("remains unfrozen", ignoreCase = true)
        )
    }

    @Test
    fun test9_portfolioCoversDistinctLifecycleSituationsAndTrackCombinations() {
        val allProjects = AutomotiveProjects.all
        val repo = ScmsRepositoryImpl()

        // 1. Enquiry only
        val enq = allProjects.firstOrNull { it.id == "DEMO-PORT-01-ENQ" }
        assertNotNull("Enquiry-only portfolio project must exist", enq)
        assertEquals("Stage 1: Intake & Opportunity", enq?.stage)
        assertEquals("Enquiry Only", enq?.status)
        val enqTasks = repo.tasks.value.filter { it.projectId == "DEMO-PORT-01-ENQ" }
        assertTrue("Enquiry must have completed intake task", enqTasks.any { it.lane == "Completed" })
        assertTrue("Enquiry must have active today task", enqTasks.any { it.lane == "Today" })
        assertTrue("Enquiry must have review task", enqTasks.any { it.lane == "Review" })

        // 2. Active feasibility / requirements (unfrozen)
        val feas = allProjects.firstOrNull { it.id == "DEMO-PROJ-010" }
        assertNotNull("Active feasibility project must exist", feas)
        assertEquals("Stage 2: Feasibility & Requirements Review", feas?.stage)
        assertEquals("Pending Feasibility", feas?.status)

        // 3. Requirements frozen, HW development
        val frz = allProjects.firstOrNull { it.id == "DEMO-PORT-03-FRZ" }
        assertNotNull("Requirements frozen project must exist", frz)
        assertEquals("Stage 3: Simulated Requirements Freeze & HW Kickoff", frz?.stage)
        assertEquals("Freeze Workflow Complete · Local TEST", frz?.status)
        val frzTasks = repo.tasks.value.filter { it.projectId == "DEMO-PORT-03-FRZ" }
        assertTrue("Frozen project must have completed freeze task", frzTasks.any { it.lane == "Completed" })
        assertTrue("Frozen project must have active layout task", frzTasks.any { it.lane == "Today" })
        assertTrue("Frozen project must have blocked task", frzTasks.any { it.lane == "Tomorrow" && it.title.contains("BLOCKED") })

        // 4. HW + FW integration
        val hf = allProjects.firstOrNull { it.id == "DEMO-PORT-04-HF" }
        assertNotNull("HW + FW integration project must exist", hf)
        assertEquals("Stage 4: Engineering Design Review (TEST)", hf?.stage)
        assertEquals("HW + FW Review · Local TEST", hf?.status)
        val hfTasks = repo.tasks.value.filter { it.projectId == "DEMO-PORT-04-HF" }
        assertTrue("HF project must have active FW bring-up tasks", hfTasks.any { it.lane == "Today" && it.dept == "FW" })
        assertTrue("HF project must have blocked cloud task", hfTasks.any { it.lane == "Tomorrow" && it.title.contains("BLOCKED") })

        // 5. HW + FW + SW integration
        val sys = allProjects.firstOrNull { it.id == "HLPL-AI4T-01" }
        assertNotNull("Full system integration project must exist", sys)
        assertTrue(sys?.stage?.contains("Stage 4") == true || sys?.stage?.contains("Stage 5") == true)

        // 6. QA / PoC or production/service feedback loop
        val qa = allProjects.firstOrNull { it.id == "DEMO-PORT-06-QA" }
        assertNotNull("QA / service feedback project must exist", qa)
        assertEquals("Stage 6: QA Checklist Review (TEST)", qa?.stage)
        assertEquals("QA / Synthetic Service Feedback · Local TEST", qa?.status)
        val qaTasks = repo.tasks.value.filter { it.projectId == "DEMO-PORT-06-QA" }
        assertTrue("QA project must have completed ISO test task", qaTasks.any { it.lane == "Completed" })
        assertTrue("QA project must have active thermal test task", qaTasks.any { it.lane == "Today" })
        assertTrue("QA project must have review task for field RMA ticket", qaTasks.any { it.lane == "Review" && it.title.contains("RMA") })
    }

    @Test
    fun test10_hardwareMasterDataSupportsFeasibilityAndBudgetAnalysis() {
        val repo = ScmsRepositoryImpl()
        val hw = repo.getHardwareMasterData("DEMO-PROJ-010")

        assertEquals("DEMO-PROJ-010", hw.projectId)
        assertTrue(hw.projectName.contains("Northstar Field Systems"))

        // 1. Requirements & Provenance: Explicit local test draft reference, not customer-issued
        assertTrue(
            "SOR reference must be explicitly labeled as local draft reference: ${hw.requirementsAndProvenance.customerSorReference}",
            hw.requirementsAndProvenance.customerSorReference.startsWith("Local TEST draft reference — not customer-issued")
        )
        assertTrue(hw.requirementsAndProvenance.provenanceBaseline.contains("Platform V2"))
        assertTrue(hw.requirementsAndProvenance.operatingTemp.contains("-40°C to +85°C"))
        assertTrue(hw.requirementsAndProvenance.powerBudgetDeepSleep.contains("42 µA"))

        // 2. Reusable Assets: No unsupported 'Pass', 'Verified', 'Qualified', or 'Certified' claims
        assertEquals(4, hw.reusableAssets.size)
        hw.reusableAssets.forEach { asset ->
            assertFalse("Component cert must not claim Pass/Passed: ${asset.componentCertification}", asset.componentCertification.contains("Pass", ignoreCase = true))
            assertFalse("Component cert must not claim Qualified: ${asset.componentCertification}", asset.componentCertification.contains("Qualified", ignoreCase = true))
            assertFalse("Component cert must not claim Certified: ${asset.componentCertification}", asset.componentCertification.contains("Certified", ignoreCase = true))
            assertTrue(
                "Component cert must state proposed test or result pending: ${asset.componentCertification}",
                asset.componentCertification.contains("Proposed test / result pending") || asset.componentCertification.contains("no test evidence attached")
            )
            assertTrue(
                "System certification must state not verified / no evidence: ${asset.systemCertificationStatus}",
                asset.systemCertificationStatus.contains("Not verified — no evidence attached")
            )
            assertFalse(
                "System configuration must not claim certified to any standard: ${asset.systemCertificationStatus}",
                asset.systemCertificationStatus.contains("Certified to", ignoreCase = true)
            )
        }

        // 3. Tentative BOM Lines: Exact inventory matching or unset link
        assertEquals(6, hw.tentativeBom.size)
        val eg915 = hw.tentativeBom.first { it.mpn == "EG915N-EU" }
        assertEquals("EG915N-EU", eg915.inventoryLinkMpn)
        val stm32 = hw.tentativeBom.first { it.mpn == "STM32G0B1VCT6" }
        assertEquals("STM32G0B1VCT6", stm32.inventoryLinkMpn)

        // Lines without exact catalog match must have inventoryLinkMpn == null (unset)
        assertNull("TAU1302 has no catalog match", hw.tentativeBom.first { it.mpn == "TAU1302" }.inventoryLinkMpn)
        assertNull("SX1262IMLTRT has no catalog match", hw.tentativeBom.first { it.mpn == "SX1262IMLTRT" }.inventoryLinkMpn)
        assertNull("W25Q128JVS has no catalog match", hw.tentativeBom.first { it.mpn == "W25Q128JVS" }.inventoryLinkMpn)
        assertNull("TCAN1042HVD has no catalog match", hw.tentativeBom.first { it.mpn == "TCAN1042HVD" }.inventoryLinkMpn)

        // Non-null inventory links must resolve exactly to registered parts
        val registeredMpns = repo.parts.value.map { it.mpn }.toSet()
        hw.tentativeBom.mapNotNull { it.inventoryLinkMpn }.forEach { linkMpn ->
            assertTrue("Linked MPN $linkMpn must exist in registered catalog", registeredMpns.contains(linkMpn))
        }

        // 4. Reconciled BOM Subtotal Arithmetic: quantity × unit cost for included lines
        val expectedSubtotalUsd = hw.tentativeBom.sumOf { it.quantityPerUnit * it.estimatedUnitCostUsd }
        assertEquals(31.10, expectedSubtotalUsd, 0.001)
        assertEquals(expectedSubtotalUsd, hw.budgetSummary.estimatedBomCostPerUnitUsd, 0.001)
        assertEquals(10, hw.budgetSummary.prototypeLotSize)
        assertTrue("Budget summary must be labeled as partial: ${hw.budgetSummary.isPartial}", hw.budgetSummary.isPartial)
        assertNotNull("Budget summary must enumerate exclusions", hw.budgetSummary.excludedItemsNote)
        assertTrue(
            "Exclusions must mention unlisted passives/enclosure/harness: ${hw.budgetSummary.excludedItemsNote}",
            hw.budgetSummary.excludedItemsNote!!.contains("passives", ignoreCase = true)
        )
        assertTrue(hw.budgetSummary.statusDisclaimer.contains("UNAPPROVED TEST ASSUMPTIONS"))

        // 5. Binary Deliverables: Marked as Absent Metadata
        assertEquals(7, hw.binaryDeliverables.size)
        hw.binaryDeliverables.forEach { bin ->
            assertEquals("Absent Metadata / File Not Uploaded", bin.status)
            assertTrue(
                "Must explain binary bytes are absent / not present / not uploaded: ${bin.details}",
                bin.details.contains("not present", ignoreCase = true) ||
                bin.details.contains("absent", ignoreCase = true) ||
                bin.details.contains("not uploaded", ignoreCase = true)
            )
        }

        // 6. Fallback Portfolio Project Verifications (HLPL-AI4T-01)
        val fallbackHw = repo.getHardwareMasterData("HLPL-AI4T-01")
        assertTrue(fallbackHw.requirementsAndProvenance.customerSorReference.startsWith("Local TEST draft reference — not customer-issued"))
        fallbackHw.reusableAssets.forEach { asset ->
            assertFalse("Fallback asset must not claim Qualified: ${asset.componentCertification}", asset.componentCertification.contains("Qualified", ignoreCase = true))
            assertFalse("Fallback asset must not claim Certified: ${asset.systemCertificationStatus}", asset.systemCertificationStatus.contains("Certified to", ignoreCase = true))
            assertTrue(asset.componentCertification.contains("Proposed test / result pending"))
            assertTrue(asset.systemCertificationStatus.contains("Not verified — no evidence attached"))
        }
        val expectedFallbackSubtotal = fallbackHw.tentativeBom.sumOf { it.quantityPerUnit * it.estimatedUnitCostUsd }
        assertEquals(32.30, expectedFallbackSubtotal, 0.001)
        assertEquals(expectedFallbackSubtotal, fallbackHw.budgetSummary.estimatedBomCostPerUnitUsd, 0.001)
        assertTrue(fallbackHw.budgetSummary.isPartial)
        assertEquals("EG915N-EU", fallbackHw.tentativeBom.first { it.mpn == "EG915N-EU" }.inventoryLinkMpn)
        assertNull(fallbackHw.tentativeBom.first { it.mpn == "STM32H753" }.inventoryLinkMpn)
        assertNull(fallbackHw.tentativeBom.first { it.mpn == "TCAN1042" }.inventoryLinkMpn)
    }

    @Test
    fun test11_authenticatedDocumentsPathQuarantinesLocalHardwareFixtures() {
        val sharedSnapshot = kotlinx.serialization.json.Json.parseToJsonElement("""{
            "departments": [{"id": "dept-hw", "code": "EHW"}],
            "records": [
                {"id": "auth-p1", "record_type": "Project", "reference": "PROJ-AUTH-01", "title": "Authorized Telematics Unit"}
            ],
            "documents": [
                {"id": "doc-auth-01", "title": "Authorized System Spec", "department_id": "dept-hw", "record_id": "auth-p1"}
            ]
        }""").jsonObject

        val authProjects = com.example.myapplication.data.model.SharedAccess.projects(sharedSnapshot)
        assertEquals(2, authProjects.size)
        assertEquals("ALL", authProjects[0].id)
        assertEquals("auth-p1", authProjects[1].id)

        // Assert all local test portfolio fixtures are excluded from authenticated projects catalog
        val localPortfolioIds = listOf("DEMO-PORT-01-ENQ", "DEMO-PROJ-010", "DEMO-PORT-03-FRZ", "DEMO-PORT-04-HF", "HLPL-AI4T-01", "DEMO-PORT-06-QA")
        localPortfolioIds.forEach { localId ->
            assertFalse("Local fixture $localId must not leak into authenticated projects catalog", authProjects.any { it.id == localId })
        }

        // Assert shared documents only contain backend documents, not local hardware master data
        val authDocs = com.example.myapplication.data.model.SharedAccess.sharedDocuments(sharedSnapshot, "ALL")
        assertEquals(1, authDocs.size)
        assertEquals("Authorized System Spec", authDocs.first().title)
        assertFalse(authDocs.any { it.title.contains("Northstar") || it.title.contains("BOM") || it.title.contains("Hardware Master Data") })
    }

    @Test
    fun test12_documentsPageVerificationScope() {
        val repo = ScmsRepositoryImpl()

        // 1. Project selection filters documents and client deliverables
        val allDocs = repo.documents.value
        assertTrue("All documents must contain multiple projects", allDocs.size >= 10)

        val s10Docs = allDocs.filter { it.projectId == "DEMO-PROJ-010" }
        assertEquals("S10 must have exactly 7 controlled docs in portfolio", 7, s10Docs.size)
        assertTrue(s10Docs.all { it.projectId == "DEMO-PROJ-010" })

        val hlplDocs = allDocs.filter { it.projectId == "HLPL-AI4T-01" }
        assertTrue("HLPL-AI4T-01 must have controlled docs", hlplDocs.isNotEmpty())
        assertTrue(hlplDocs.all { it.projectId == "HLPL-AI4T-01" })

        val allDeliverables = repo.clientDeliverables.value
        val s10Deliverables = allDeliverables.filter { it.projectId == "DEMO-PROJ-010" }
        assertEquals(3, s10Deliverables.size)
        assertTrue(s10Deliverables.any { it.code.contains("SOR") })
        assertTrue(s10Deliverables.any { it.code.contains("QUO") || it.code.contains("COMM") })
        assertTrue(s10Deliverables.any { it.code.contains("TAC") })

        val hlplDeliverables = allDeliverables.filter { it.projectId == "HLPL-AI4T-01" || it.code.startsWith("HLPL-AI4T-01-") }
        assertTrue(hlplDeliverables.isNotEmpty())
        assertTrue(hlplDeliverables.all { it.projectId == "HLPL-AI4T-01" })

        // 2. Department selection defaults: Admin defaults to All, non-Admin defaults to department code
        fun defaultDeptForRole(role: com.example.myapplication.data.model.DepartmentRole): String {
            return if (role == com.example.myapplication.data.model.DepartmentRole.ADMIN) "All"
            else when (role.code) {
                "ESW" -> "FW"
                "TST" -> "QA"
                else -> role.code
            }
        }
        assertEquals("All", defaultDeptForRole(com.example.myapplication.data.model.DepartmentRole.ADMIN))
        assertEquals("EHW", defaultDeptForRole(com.example.myapplication.data.model.DepartmentRole.EHW))
        assertEquals("SAL", defaultDeptForRole(com.example.myapplication.data.model.DepartmentRole.SAL))
        assertEquals("PUR", defaultDeptForRole(com.example.myapplication.data.model.DepartmentRole.PUR))
        assertEquals("QA", defaultDeptForRole(com.example.myapplication.data.model.DepartmentRole.QA))
        assertEquals("FW", defaultDeptForRole(com.example.myapplication.data.model.DepartmentRole.FW))

        // Non-admin departments list does not contain "All"
        fun availableDeptsForRole(role: com.example.myapplication.data.model.DepartmentRole, allDepts: List<String>): List<String> {
            return (if (role == com.example.myapplication.data.model.DepartmentRole.ADMIN) listOf("All") else emptyList()) + allDepts.sorted()
        }
        val dummyDepts = listOf("EHW", "SAL", "PUR", "QA")
        assertFalse(availableDeptsForRole(com.example.myapplication.data.model.DepartmentRole.EHW, dummyDepts).contains("All"))
        assertTrue(availableDeptsForRole(com.example.myapplication.data.model.DepartmentRole.ADMIN, dummyDepts).contains("All"))

        // 3. Stage exit deliverables: Edit for AUTO_DRAFT vs Upload for UPLOAD_BINARY
        val autoDraftKinds = s10Docs.filter { it.documentKind == "AUTO_DRAFT" }
        val uploadBinaryKinds = s10Docs.filter { it.documentKind == "UPLOAD_BINARY" }
        assertEquals(3, autoDraftKinds.size) // SAL-ENQ, EHW-FEA, PUR-SOURCING
        assertEquals(4, uploadBinaryKinds.size) // EHW-SCH, EHW-GER, QA-TEST, QA-CERT

        assertTrue(uploadBinaryKinds.any { it.docId.contains("SCH") })
        assertTrue(uploadBinaryKinds.any { it.docId.contains("GER") })
        assertTrue(uploadBinaryKinds.any { it.docId.contains("TEST") })
        assertTrue(uploadBinaryKinds.any { it.docId.contains("CERT") })

        // 4. Draft editing saves as local test draft and never marks approved or released
        val originalDraft = autoDraftKinds.first()
        val editedDraft = originalDraft.copy(
            title = "Updated Hardware Feasibility Draft",
            draftContent = "Updated test assumptions",
            status = "Draft · local test"
        )
        repo.addControlledDoc(editedDraft)
        val storedDoc = repo.documents.value.first { it.docId == originalDraft.docId }
        assertEquals("Updated Hardware Feasibility Draft", storedDoc.title)
        assertEquals("Draft · local test", storedDoc.status)
        assertFalse("Draft must not be approved", storedDoc.status.contains("Approved", ignoreCase = true))
        assertFalse("Draft must not be released", storedDoc.status.contains("Released", ignoreCase = true))

        // 5. File picker / attachment is local-only with no shared upload
        val binaryDoc = uploadBinaryKinds.first()
        val attachedDoc = binaryDoc.copy(
            status = "Local file attached (not uploaded)",
            storagePath = "local://candidate_schematic_rev01.pdf",
            draftContent = "[Local Attachment: candidate_schematic_rev01.pdf · Local device only · No backend upload contract available]"
        )
        repo.addControlledDoc(attachedDoc)
        val storedAttachedDoc = repo.documents.value.first { it.docId == binaryDoc.docId }
        assertEquals("Local file attached (not uploaded)", storedAttachedDoc.status)
        assertEquals("local://candidate_schematic_rev01.pdf", storedAttachedDoc.storagePath)
        assertTrue(storedAttachedDoc.draftContent.contains("No backend upload contract available"))

        // 6. Authenticated mode remains metadata-only
        val sharedSnapshot = kotlinx.serialization.json.Json.parseToJsonElement("""{
            "departments": [{"id": "dept-hw", "code": "EHW"}],
            "records": [{"id": "p-1", "record_type": "Project", "reference": "P-1", "title": "Server Proj"}],
            "documents": [{"id": "d-1", "title": "Server Spec", "department_id": "dept-hw", "record_id": "p-1"}]
        }""").jsonObject
        val sharedDocs = SharedAccess.sharedDocuments(sharedSnapshot, "p-1")
        assertEquals(1, sharedDocs.size)
        assertEquals("Server Spec", sharedDocs.first().title)
        // Verify local fixtures do not leak into shared documents
        assertFalse(sharedDocs.any { it.id == storedDoc.docId || it.id == storedAttachedDoc.docId })
    }
}
