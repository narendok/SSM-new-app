package com.example.myapplication

import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.PartComponent
import com.example.myapplication.data.repository.ScmsRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScmsRepositoryTest {

    private lateinit var repository: ScmsRepositoryImpl

    @Before
    fun setUp() {
        repository = ScmsRepositoryImpl()
    }

    @Test
    fun testRoleSwitching() {
        assertEquals(DepartmentRole.ADMIN, repository.currentRole.value)
        repository.switchRole(DepartmentRole.EHW)
        assertEquals(DepartmentRole.EHW, repository.currentRole.value)
        assertEquals("Avery Vale", repository.currentRole.value.leadName)
    }

    @Test
    fun testMouserCatalogLookup() {
        val part = repository.lookupMouserCatalog("STM32G0B1VCT6")
        assertNotNull(part)
        assertEquals("STMicroelectronics", part?.manufacturer)
        assertEquals("AEC-Q100 Grade 1", part?.aecRating)
        assertEquals(4.85, part?.unitCostUsd ?: 0.0, 0.001)
    }

    @Test
    fun testPartRegistration() {
        val initialCount = repository.parts.value.size
        val newPart = PartComponent(
            mpn = "TEST-RES-01",
            description = "Test Precision Resistor",
            manufacturer = "Vishay",
            packageFootprint = "0603",
            category = "Electronics",
            onHand = 500,
            released = 500,
            iqcHold = 0,
            allocated = 0,
            storageLocation = "Warehouse Zone A",
            binCoordinate = "A-01-01",
            reorderBuffer = 100,
            unitCostUsd = 0.05,
            unitCostInr = 4.15
        )
        val result = repository.registerPart(newPart)
        assertTrue(result.isSuccess)
        assertEquals(initialCount + 1, repository.parts.value.size)

        val latestLog = repository.auditLogs.value.first()
        assertEquals("Receipt", latestLog.action)
        assertEquals("TEST-RES-01", latestLog.partMpn)
    }

    @Test
    fun testMaterialIssuanceAndValidation() {
        val targetMpn = "STM32G0B1VCT6"
        val initialPart = repository.parts.value.find { it.mpn == targetMpn }!!
        val initialReleased = initialPart.released

        // Issue 50 pcs to Avery Vale
        val result = repository.issueMaterial(
            mpn = targetMpn,
            recipient = "Avery Vale",
            dept = "EHW",
            quantity = 50,
            issuer = "Stores Admin"
        )
        assertTrue(result.isSuccess)

        val updatedPart = repository.parts.value.find { it.mpn == targetMpn }!!
        assertEquals(initialReleased - 50, updatedPart.released)
        assertEquals(initialPart.allocated + 50, updatedPart.allocated)

        // Try to issue more than remaining released stock
        val failResult = repository.issueMaterial(
            mpn = targetMpn,
            recipient = "Avery Vale",
            dept = "EHW",
            quantity = updatedPart.released + 1000,
            issuer = "Stores Admin"
        )
        assertTrue(failResult.isFailure)
    }

    @Test
    fun testMaterialReturn() {
        val initialAlloc = repository.allocations.value.first()
        val returnResult = repository.returnMaterial(initialAlloc.id, 10)
        assertTrue(returnResult.isSuccess)

        val updatedAlloc = repository.allocations.value.find { it.id == initialAlloc.id }!!
        assertEquals(10, updatedAlloc.returnedQuantity)
    }

    @Test
    fun testWhatIfSimulationDeficit() {
        val result = repository.runWhatIfSimulation(5000)
        assertEquals(5000, result.buildQuantity)
        assertTrue("Expected shortage lines for 5000 units build", result.shortageLinesCount > 0)
        assertTrue("Expected deficit USD", result.totalDeficitUsd > 0.0)

        // Check STM32: onHand is 18540, required is 5000 -> should NOT be a shortage
        val stm32Line = result.lineStatus.find { it.mpn == "STM32G0B1VCT6" }
        assertNotNull(stm32Line)
        assertFalse(stm32Line!!.isShortage)
    }

    @Test
    fun testEcoPromotionBlocksProvisionalComponents() {
        // v0.3-tentative contains C0603C104K5RACTU marked provisional
        val result = repository.promoteBomRevision(
            targetRevision = "Rev B ECO-2026-005",
            ecoNumber = "ECO-2026-005",
            rationale = "Testing baseline promotion"
        )
        // Must be blocked per IATF §8.3.6
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("ECO Promotion Blocked") == true)
    }

    @Test
    fun testStrictInvariantZeroAutoStockPostingOnIngestion() {
        val initialPart = repository.parts.value.find { it.mpn == "STM32G0B1VCT6" }!!
        val initialReleased = initialPart.released

        // Ingest row ING-01 (2500 pcs billed)
        val result = repository.generateGrnFromIngestion("ING-01")
        assertTrue(result.isSuccess)
        val grnNo = result.getOrNull()!!

        // Find GRN
        val grn = repository.grnRecords.value.find { it.grnNumber == grnNo }!!
        assertEquals(0, grn.quantityAccepted) // ZERO AUTO-STOCK POSTING
        assertEquals(2500, grn.quantityHeld)  // 100% IN QUARANTINE
        assertEquals("IQC-HOLD-STAGE-01", grn.assignedBin)
        assertEquals("Pending IQC", grn.status)

        // Released stock MUST NOT have increased
        val updatedPart = repository.parts.value.find { it.mpn == "STM32G0B1VCT6" }!!
        assertEquals(initialReleased, updatedPart.released)
        assertEquals(initialPart.iqcHold + 2500, updatedPart.iqcHold)
    }

    @Test
    fun testIqcQualityRelease() {
        // First inward a lot
        val inwardResult = repository.generateGrnFromIngestion("ING-01")
        val grnNo = inwardResult.getOrNull()!!

        val initialPart = repository.parts.value.find { it.mpn.contains("STM32") }!!
        val initialReleased = initialPart.released

        // Quality release disposition
        val releaseResult = repository.releaseIqcHold(
            grnNumber = grnNo,
            inspectionRef = "QA-IR-2026-088",
            inspector = "Jordan Blake (QA Lead)"
        )
        assertTrue(releaseResult.isSuccess)

        val updatedGrn = repository.grnRecords.value.find { it.grnNumber == grnNo }!!
        assertEquals("Released", updatedGrn.status)
        assertEquals(2500, updatedGrn.quantityAccepted)
        assertEquals(0, updatedGrn.quantityHeld)

        // Released inventory should now be incremented by 2500
        val updatedPart = repository.parts.value.find { it.mpn.contains("STM32") }!!
        assertEquals(initialReleased + 2500, updatedPart.released)
    }

    @Test
    fun testDvprTestStatusUpdate() {
        repository.updateDvprTestStatus("DVP-03", "Passed", "Jordan Blake (QA Lead)")
        val test = repository.dvprTests.value.find { it.testId == "DVP-03" }!!
        assertEquals("Passed", test.status)
        assertEquals("Jordan Blake (QA Lead)", test.technician)
        assertNotNull(test.signOffTimestamp)
    }

    @Test
    fun testLocalRepositoryCannotFabricatePrivateSignedUrl() {
        val result = repository.requestSignedStorageUrl("controlled-documents", "03_Hardware/EHW-SCH-01.pdf")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot mint private file URLs") == true)
    }

    @Test
    fun test6HodFeasibilityReviewsLoaded() {
        val reviews = repository.feasibilityReviews.value
        assertEquals(6, reviews.size)
        val deptCodes = reviews.map { it.deptCode }.toSet()
        assertTrue(deptCodes.containsAll(listOf("EHW", "ESW", "MECH", "PUR", "QA", "FIN")))

        val ehw = reviews.find { it.deptCode == "EHW" }!!
        assertEquals("Avery Vale", ehw.leadName)
        assertEquals("FEASIBLE", ehw.verdict)
        assertTrue(ehw.signatureHash.startsWith("SHA256:"))
        assertTrue(ehw.keyParameters.containsKey("MCU Clock"))

        val pur = reviews.find { it.deptCode == "PUR" }!!
        assertEquals("FEASIBLE_WITH_CONDITIONS", pur.verdict)
        assertEquals("Marcus Vance", pur.leadName)
    }

    @Test
    fun testHardwarePathSelectionAndAuditLogging() {
        assertEquals("pilot", repository.hardwarePath.value)
        repository.selectHardwarePath("poc", "Proof-of-concept testing required for new telemetry RF front-end")
        assertEquals("poc", repository.hardwarePath.value)

        val latestLog = repository.auditLogs.value.first()
        assertEquals("HardwarePathSelection", latestLog.action)
        assertTrue(latestLog.reason.contains("poc"))
    }

    @Test
    fun testCustomerRegistration() {
        val initialCount = repository.customers.value.size
        val newCust = com.example.myapplication.data.model.CustomerAccount(
            id = "CUST-HERO-05",
            name = "Hero Electric Vehicles",
            division = "Commercial Delivery Fleet",
            contactPerson = "Amit Verma",
            email = "amit.verma@heroelectric.in",
            phone = "+91 11 4160 5000",
            location = "New Delhi",
            activeProjects = emptyList(),
            ndaStatus = "Active & Executed",
            tierRating = "Tier-1 OEM"
        )
        repository.addCustomer(newCust)
        assertEquals(initialCount + 1, repository.customers.value.size)
        assertEquals("CUST-HERO-05", repository.customers.value.first().id)

        val latestLog = repository.auditLogs.value.first()
        assertEquals("CustomerRegistered", latestLog.action)
    }

    @Test
    fun testSalesOpportunityCreation() {
        val initialCount = repository.salesOpportunities.value.size
        val newOpp = com.example.myapplication.data.model.SalesOpportunity(
            id = "OPP-2026-003",
            title = "Hero Electric Smart Fleet 4G Module",
            clientName = "Hero Electric Vehicles",
            volume = 30000,
            projectedValueInr = "₹12.20 Cr",
            status = "Enquiry Intake",
            sorDocId = "HLPL-HEV-RFQ-2026",
            isOpportunity = true
        )
        repository.createOpportunity(newOpp)
        assertEquals(initialCount + 1, repository.salesOpportunities.value.size)
        assertEquals("OPP-2026-003", repository.salesOpportunities.value.first().id)

        val latestLog = repository.auditLogs.value.first()
        assertEquals("OpportunityCreated", latestLog.action)
    }

    @Test
    fun testClientTicketSubmissionCreatesDailyTaskAndLog() {
        val initialTasks = repository.tasks.value.size
        val result = repository.submitClientTicket(
            title = "FOTA Rollback Handshake Inquiry",
            description = "Need clarification on delta update retry threshold under cellular fringe zones."
        )
        assertTrue(result.isSuccess)
        val ticketId = result.getOrNull()!!
        assertTrue(ticketId.startsWith("TICK-2026-"))

        assertEquals(initialTasks + 1, repository.tasks.value.size)
        val task = repository.tasks.value.first()
        assertEquals(ticketId, task.id)
        assertTrue(task.title.contains("FOTA Rollback Handshake Inquiry"))
        assertEquals("High", task.priority)
        assertFalse(task.isCompleted)

        val latestLog = repository.auditLogs.value.first()
        assertEquals("ClientTicketSubmission", latestLog.action)
    }

    @Test
    fun testApprovedVendorsAndPurchaseOrders() {
        val avl = repository.approvedVendors.value
        assertEquals(6, avl.size)
        assertTrue(avl.any { it.vendorCode == "VND-MOU-01" && it.aslStatus == "Verified ASL" })

        val pos = repository.purchaseOrders.value
        assertEquals(3, pos.size)
        val po42 = pos.find { it.poNumber == "HLPL-PO-2026-042" }!!
        assertEquals("Mouser Electronics India", po42.vendorName)
        assertEquals(14250.00, po42.totalAmountUsd, 0.01)
    }

    @Test
    fun testReceivingDraftVoucherCreation() {
        val initialCount = repository.receivingDrafts.value.size
        val draft = com.example.myapplication.data.model.ReceivingDraftVoucher(
            voucherId = "RCV-TEST-001",
            poNumber = "HLPL-PO-2026-042",
            vendorName = "Mouser Electronics India",
            mpn = "STM32G0B1VCT6",
            lotNumber = "LOT-2026-TEST-99",
            batchCode = "B-001",
            dateCode = "2640",
            reelId = "REEL-99901",
            quantityReceived = 500,
            mslRating = "MSL-3 (168 hrs)",
            esdCompliant = true,
            packageCondition = "Intact / Verified",
            inspectorName = "Test Inspector",
            timestamp = "2026-10-18 10:00"
        )
        val result = repository.saveReceivingDraft(draft)
        assertTrue(result.isSuccess)
        assertEquals("RCV-TEST-001", result.getOrNull())
        assertEquals(initialCount + 1, repository.receivingDrafts.value.size)
        assertEquals("ReceivingDraftSaved", repository.auditLogs.value.first().action)
    }

    @Test
    fun testCycleCountRecording() {
        val record = com.example.myapplication.data.model.CycleCountRecord(
            countId = "CC-2026-003",
            mpn = "TCAN1042VDRQ1",
            description = "Automotive CAN Transceiver AEC-Q100",
            binCoordinate = "B-03-11",
            systemQty = 24000,
            countedQty = 24000,
            variance = 0,
            counterName = "Test Counter",
            status = "Matched"
        )
        val result = repository.recordCycleCount(record)
        assertTrue(result.isSuccess)
        val updated = repository.cycleCounts.value.find { it.countId == "CC-2026-003" }!!
        assertEquals("Matched", updated.status)
        assertEquals(24000, updated.countedQty)
        assertEquals("CycleCountRecorded", repository.auditLogs.value.first().action)
    }

    @Test
    fun testPickLineConfirmation() {
        val pickId = "PICK-003"
        val initialItem = repository.pickList.value.find { it.pickId == pickId }!!
        assertFalse(initialItem.isConfirmed)

        val result = repository.confirmPickLine(pickId, 100)
        assertTrue(result.isSuccess)

        val updatedItem = repository.pickList.value.find { it.pickId == pickId }!!
        assertTrue(updatedItem.isConfirmed)
        assertEquals(100, updatedItem.pickedQty)
        assertEquals("PickLineConfirmed", repository.auditLogs.value.first().action)
    }

    @Test
    fun testProductionWorkOrderRoutingAdvance() {
        val woNo = "WO-2026-AI4T-001"
        val initialWo = repository.workOrders.value.find { it.workOrderNo == woNo }!!
        assertEquals(2, initialWo.currentStationNo)
        assertEquals("In Progress", initialWo.stations.find { it.stepNo == 2 }?.status)

        // Advance Step 2 (Reflow Oven)
        val result = repository.advanceWorkOrderStep(woNo, 2, "Casey Rowan")
        assertTrue(result.isSuccess)

        val updatedWo = repository.workOrders.value.find { it.workOrderNo == woNo }!!
        assertEquals(3, updatedWo.currentStationNo)
        assertEquals("Passed", updatedWo.stations.find { it.stepNo == 2 }?.status)
        assertEquals("In Progress", updatedWo.stations.find { it.stepNo == 3 }?.status)
        assertEquals("RoutingStationPassed", repository.auditLogs.value.first().action)
    }

    @Test
    fun testUniversalScannerEntityResolution() {
        // 1. MPN Scan
        val mpnResult = repository.resolveScanQuery("STM32G0B1VCT6")
        assertEquals(com.example.myapplication.data.model.ScannedEntityType.COMPONENT, mpnResult.entityType)
        assertEquals("STM32G0B1VCT6", mpnResult.primaryTitle)
        assertEquals("inventory", mpnResult.actionRoute)

        // 2. Bin Scan
        val binResult = repository.resolveScanQuery("A-12-04")
        assertEquals(com.example.myapplication.data.model.ScannedEntityType.STORAGE_BIN, binResult.entityType)
        assertTrue(binResult.primaryTitle.contains("A-12-04"))

        // 3. PO Scan
        val poResult = repository.resolveScanQuery("HLPL-PO-2026-042")
        assertEquals(com.example.myapplication.data.model.ScannedEntityType.PURCHASE_ORDER, poResult.entityType)
        assertEquals("receiving", poResult.actionRoute)

        // 4. Asset Scan
        val assetResult = repository.resolveScanQuery("EQ-SMT-001")
        assertEquals(com.example.myapplication.data.model.ScannedEntityType.PLANT_ASSET, assetResult.entityType)
        assertTrue(assetResult.primaryTitle.contains("Yamaha"))

        // 5. Doc Scan
        val docResult = repository.resolveScanQuery("EHW-SCH-01")
        assertEquals(com.example.myapplication.data.model.ScannedEntityType.CONTROLLED_DOC, docResult.entityType)
        assertEquals("documents", docResult.actionRoute)

        // 6. Gate Pass Scan (Stage 8)
        val gatePassResult = repository.resolveScanQuery("GP-2026-019")
        assertEquals(com.example.myapplication.data.model.ScannedEntityType.GATE_PASS, gatePassResult.entityType)
        assertTrue(gatePassResult.primaryTitle.contains("GP-2026-019"))

        // 7. Staff Badge Scan (Stage 9)
        val badgeResult = repository.resolveScanQuery("HLPL-EMP-101")
        assertEquals(com.example.myapplication.data.model.ScannedEntityType.EMPLOYEE_BADGE, badgeResult.entityType)
        assertTrue(badgeResult.primaryTitle.contains("Avery Vale"))

        // 8. Calibration Cert Scan (Stage 7)
        val certResult = repository.resolveScanQuery("CAL-2026-089")
        assertEquals(com.example.myapplication.data.model.ScannedEntityType.CALIBRATION_CERT, certResult.entityType)
        assertTrue(certResult.primaryTitle.contains("CAL-2026-089"))
        assertTrue(certResult.secondarySubtitle.contains("Keysight"))

        // 9. Unknown fallback
        val unknownResult = repository.resolveScanQuery("RANDOM_BARCODE_XYZ")
        assertEquals(com.example.myapplication.data.model.ScannedEntityType.UNKNOWN, unknownResult.entityType)
    }

    @Test
    fun testMaintenanceLoggingAndCalibrationSignOff() {
        val initialLogsCount = repository.maintenanceLogs.value.size
        val record = com.example.myapplication.data.model.MaintenanceLogRecord(
            logId = "PM-TEST-001",
            assetTag = "EQ-SMT-001",
            assetName = "Yamaha YSM20R Pick & Place",
            maintenanceType = "Preventive PM",
            title = "Test Monthly PM",
            technicianName = "Elena Vance",
            status = "Scheduled",
            priority = "Normal",
            scheduledDate = "2026-10-20",
            stoppageHours = 1.0,
            remarks = "Test remarks"
        )
        val logResult = repository.logMaintenanceTicket(record)
        assertTrue(logResult.isSuccess)
        assertEquals(initialLogsCount + 1, repository.maintenanceLogs.value.size)

        // Test Calibration Sign-Off
        val targetCertId = "CAL-2026-091"
        val initialCert = repository.calibrationCerts.value.find { it.certId == targetCertId }!!
        assertEquals("Expiring (< 30d)", initialCert.status)

        val signOffResult = repository.signOffCalibration(targetCertId, "Elena Vance")
        assertTrue(signOffResult.isSuccess)

        val updatedCert = repository.calibrationCerts.value.find { it.certId == targetCertId }!!
        assertEquals("Valid / Certified", updatedCert.status)
    }

    @Test
    fun testSecurityGatePassAndVisitorFlow() {
        // 1. Visitor Check-In
        val initialVisitorCount = repository.visitorLogs.value.size
        val newVisitor = com.example.myapplication.data.model.VisitorLogEntry(
            visitorId = "VIS-TEST-001",
            fullName = "John Doe",
            company = "Bosch Mobility",
            hostEmployee = "Avery Vale",
            purpose = "Supplier Quality Audit",
            badgeNumber = "SEC-VIS-999",
            checkInTime = "2026-10-18 10:00",
            status = "Active On-Site"
        )
        val checkInResult = repository.checkInVisitor(newVisitor)
        assertTrue(checkInResult.isSuccess)
        assertEquals(initialVisitorCount + 1, repository.visitorLogs.value.size)

        // 2. Visitor Check-Out
        val checkOutResult = repository.checkOutVisitor("VIS-TEST-001")
        assertTrue(checkOutResult.isSuccess)
        val updatedVisitor = repository.visitorLogs.value.find { it.visitorId == "VIS-TEST-001" }!!
        assertEquals("Checked Out", updatedVisitor.status)
        assertNotNull(updatedVisitor.checkOutTime)

        // 3. Gate Pass Verification
        val targetPass = "GP-2026-021"
        val initialPass = repository.gatePasses.value.find { it.passNumber == targetPass }!!
        assertEquals("Awaiting Verification", initialPass.status)

        val verifyResult = repository.verifyGatePass(targetPass, "Security Officer")
        assertTrue(verifyResult.isSuccess)

        val updatedPass = repository.gatePasses.value.find { it.passNumber == targetPass }!!
        assertEquals("Cleared & Gate Passed", updatedPass.status)
    }

    @Test
    fun testAiProceduralAssistantQueries() {
        // MSL Query
        val mslResponse = repository.queryAiAssistant("What is MSL Level 3 floor life limit?")
        assertEquals("ESD & Moisture Sensitivity (J-STD-033)", mslResponse.detectedTopic)
        assertTrue(mslResponse.referencedSop.contains("SOP-ESD-002"))
        assertTrue(mslResponse.confidencePct >= 95)
        assertTrue(mslResponse.answerText.contains("168 hours"))

        // Calibration Query
        val calResponse = repository.queryAiAssistant("What is the IMTE calibration frequency?")
        assertEquals("IMTE Instrument Calibration Frequency", calResponse.detectedTopic)
        assertTrue(calResponse.referencedSop.contains("SOP-QA-008"))
        assertTrue(calResponse.answerText.contains("NABL"))

        // Visitor Security Query
        val secResponse = repository.queryAiAssistant("What is visitor escort protocol?")
        assertEquals("Workplace & Gatehouse Security Protocol", secResponse.detectedTopic)
        assertTrue(secResponse.referencedSop.contains("SOP-SEC-001"))
        assertTrue(secResponse.answerText.contains("BADGE-V-xxx"))

        // General QMS Query
        val qmsResponse = repository.queryAiAssistant("quarantine non-conforming parts")
        assertEquals("SCMS Automotive Operations Standard", qmsResponse.detectedTopic)
        assertTrue(qmsResponse.referencedSop.contains("IATF 16949"))
    }

    @Test
    fun testApprovalCategoriesAndDecisionFlow() {
        val initialApprovals = repository.approvals.value
        assertTrue("Approvals queue must contain seeded governance records", initialApprovals.isNotEmpty())

        // 1. Check all user-specified enterprise categories are populated
        val expenseReq = initialApprovals.find { it.category == com.example.myapplication.data.model.ApprovalCategory.EXPENSE }
        assertNotNull("Expense request must exist", expenseReq)
        assertEquals("Casey Rowan", expenseReq?.requester)
        assertEquals(42500.0, expenseReq?.amountInr ?: 0.0, 0.01)

        val travelReq = initialApprovals.find { it.category == com.example.myapplication.data.model.ApprovalCategory.TRAVEL }
        assertNotNull("Travel expense request must exist", travelReq)
        assertTrue(travelReq!!.travelPolicyCompliant)
        assertEquals("Devon Lane", travelReq.requester)

        val taskReq = initialApprovals.find { it.category == com.example.myapplication.data.model.ApprovalCategory.TASK }
        assertNotNull("Task approval request must exist", taskReq)

        val devReq = initialApprovals.find { it.category == com.example.myapplication.data.model.ApprovalCategory.PROCUREMENT_DEVIATION }
        assertNotNull("Procurement deviation must exist", devReq)
        assertEquals("Morgan Reed", devReq?.requester)

        val poReq = initialApprovals.find { it.category == com.example.myapplication.data.model.ApprovalCategory.PURCHASE_ORDER }
        assertNotNull("Purchase Order request must exist", poReq)

        val finReq = initialApprovals.find { it.category == com.example.myapplication.data.model.ApprovalCategory.FINANCE_DISBURSEMENT }
        assertNotNull("Finance approval must exist", finReq)

        val assetReq = initialApprovals.find { it.category == com.example.myapplication.data.model.ApprovalCategory.OFFICE_ASSET }
        assertNotNull("Office/Plant asset approval must exist", assetReq)
        assertEquals("Taylor Quinn", assetReq?.requester)

        // 2. Test Approval Decision Flow with Audit Attribution
        val targetId = expenseReq!!.id
        val approveResult = repository.approveGate(targetId, "Verified against SMT Line 2 pilot budget. Approved.")
        assertTrue(approveResult.isSuccess)

        val approvedItem = repository.approvals.value.find { it.id == targetId }!!
        assertEquals("Approved", approvedItem.outcome)
        assertTrue(approvedItem.signedBy!!.contains("Executive Admin / MD"))
        assertEquals("Verified against SMT Line 2 pilot budget. Approved.", approvedItem.comments)

        // 3. Test Reject Flow
        val devTargetId = devReq!!.id
        val rejectResult = repository.rejectGate(devTargetId, "Secondary vendor TVS clamp voltage exceeds tolerance.")
        assertTrue(rejectResult.isSuccess)

        val rejectedItem = repository.approvals.value.find { it.id == devTargetId }!!
        assertEquals("Rejected", rejectedItem.outcome)
        assertEquals("Secondary vendor TVS clamp voltage exceeds tolerance.", rejectedItem.comments)

        // 4. Test Return for Rework Flow
        val poTargetId = poReq!!.id
        val returnResult = repository.returnGate(poTargetId, "Please re-negotiate 45-day payment terms with Quectel.")
        assertTrue(returnResult.isSuccess)

        val returnedItem = repository.approvals.value.find { it.id == poTargetId }!!
        assertEquals("Returned", returnedItem.outcome)
        assertEquals("Please re-negotiate 45-day payment terms with Quectel.", returnedItem.comments)

        // 5. Verify Immutable Audit Trail Entries
        val auditLogs = repository.auditLogs.value
        val latestApproveAudit = auditLogs.find { it.action == "APPROVE_GOVERNANCE_GATE" }
        assertNotNull("Audit entry for approval must be recorded", latestApproveAudit)
        assertTrue(latestApproveAudit!!.reason.contains("Verified against SMT Line 2"))

        val latestRejectAudit = auditLogs.find { it.action == "REJECT_GOVERNANCE_GATE" }
        assertNotNull("Audit entry for rejection must be recorded", latestRejectAudit)

        val latestReturnAudit = auditLogs.find { it.action == "RETURN_FOR_REWORK" }
        assertNotNull("Audit entry for return must be recorded", latestReturnAudit)
    }

    @Test
    fun testWebConfiguredTravelPolicyVerification() {
        val rules = repository.travelPolicyRules.value
        assertTrue("Travel policy rules must be present", rules.isNotEmpty())
        val airRule = rules.find { it.ruleId == "POL-TRV-01" }!!
        assertEquals(6500.0, airRule.maxPerDiemInr, 0.01)
        assertEquals(15000.0, airRule.requiresPreApprovalAboveInr, 0.01)
        assertEquals("v2.4-WebAuthority", airRule.webConfigVersion)
    }

    @Test
    fun testCanonical12DepartmentArchitectureAndNormalization() {
        // 1. Verify all canonical departments are represented
        val canonicalCodes = listOf("SAL", "EHW", "FW", "SW", "MECH", "PUR", "QA", "EMF", "MMF", "FIN", "SIT", "HROPS")
        val roles = DepartmentRole.entries.map { it.code }
        for (code in canonicalCodes) {
            assertTrue("Canonical department '$code' must exist in DepartmentRole entries", roles.contains(code))
        }

        // 2. Verify legacy roles are preserved without blanket deletion
        assertTrue("Legacy ESW must be preserved", roles.contains("ESW"))
        assertTrue("Legacy TST must be preserved", roles.contains("TST"))
        assertTrue("ESW must be marked legacy", DepartmentRole.ESW.isLegacy)
        assertTrue("TST must be marked legacy", DepartmentRole.TST.isLegacy)

        // 3. Normalization through fromCode helper
        assertEquals(DepartmentRole.FW, DepartmentRole.fromCode("EFW"))
        assertEquals(DepartmentRole.FW, DepartmentRole.fromCode("fw"))
        assertEquals(DepartmentRole.SW, DepartmentRole.fromCode("CSW"))
        assertEquals(DepartmentRole.SW, DepartmentRole.fromCode("sw"))
        assertEquals(DepartmentRole.HROPS, DepartmentRole.fromCode("HR"))
        assertEquals(DepartmentRole.HROPS, DepartmentRole.fromCode("ops"))
        assertEquals(DepartmentRole.ADMIN, DepartmentRole.fromCode("UNKNOWN_DEPT"))
    }

    @Test
    fun testOperationalStageGateAdvancementAndPrerequisites() {
        // Current seed starts at Stage 4 (In Progress)
        val initialStages = repository.apqpStages.value
        val stage4 = initialStages.find { it.stageNumber == 4 }!!
        assertEquals("In Progress", stage4.status)

        // 1. Idempotent advance: advancing to same current stage returns success
        val sameStageResult = repository.verifyAndAdvanceStageGate(4, "Re-triggering stage 4 check")
        assertTrue("Idempotent check must succeed", sameStageResult.isSuccess)

        // 2. Cannot skip stages (jumping from 4 to 6 is denied)
        val skipResult = repository.verifyAndAdvanceStageGate(6, "Attempting illegal stage jump")
        assertTrue("Skipping stages must fail", skipResult.isFailure)
        assertTrue(skipResult.exceptionOrNull() is IllegalStateException)

        // 3. Stage 4 Prerequisite check: advancing to Stage 5 requires approved EHW-SCH-01
        // Initially EHW-SCH-01 is 'Under Review'
        val advanceTo5Blocked = repository.verifyAndAdvanceStageGate(5, "Attempting stage 5 without approved schematics")
        assertTrue("Advancing to stage 5 without approved schematics must fail", advanceTo5Blocked.isFailure)
        assertTrue(advanceTo5Blocked.exceptionOrNull()?.message?.contains("EHW-SCH-01") == true)

        // 4. Now approve EHW-SCH-01 and verify advance to Stage 5 succeeds
        val docApproveResult = repository.updateControlledDocStatus("HLPL-AI4T-01-EHW-SCH-01", "Approved")
        assertTrue("Updating document status must succeed", docApproveResult.isSuccess)

        val advanceTo5Allowed = repository.verifyAndAdvanceStageGate(5, "Schematic approved, unlocking SMT Pilot")
        assertTrue("Advancing to stage 5 must succeed after schematics approval", advanceTo5Allowed.isSuccess)

        val updatedStages = repository.apqpStages.value
        assertEquals("Approved", updatedStages.find { it.stageNumber == 4 }!!.status)
        assertEquals("In Progress", updatedStages.find { it.stageNumber == 5 }!!.status)

        // 5. Stage regression is permitted (e.g. back to stage 3 for engineering rework)
        val regressionResult = repository.verifyAndAdvanceStageGate(3, "Engineering rework triggered by client ECO")
        assertTrue("Stage regression must be permitted", regressionResult.isSuccess)
        val regressedStages = repository.apqpStages.value
        assertEquals("In Progress", regressedStages.find { it.stageNumber == 3 }!!.status)
        assertEquals("Pending", regressedStages.find { it.stageNumber == 4 }!!.status)

        // 6. Verify audit trail was recorded for gate events
        val auditLogs = repository.auditLogs.value
        assertTrue("Must record STAGE_GATE_ADVANCE audit", auditLogs.any { it.action == "STAGE_GATE_ADVANCE" })
        assertTrue("Must record STAGE_GATE_REGRESSION audit", auditLogs.any { it.action == "STAGE_GATE_REGRESSION" })
    }

    @Test
    fun testPrivateAttachmentVaultBucketSecurity() {
        val buckets = com.example.myapplication.data.repository.PrivateStorageRepositoryImpl.PRIVATE_BUCKETS
        assertTrue("controlled-documents must be an approved private bucket", buckets.contains("controlled-documents"))
        assertTrue("project-drive must be an approved private bucket", buckets.contains("project-drive"))
        assertFalse("public or arbitrary buckets must not be approved", buckets.contains("public-files"))
        assertEquals("Max signed URL expiry must be 60 seconds", 60, com.example.myapplication.data.repository.PrivateStorageRepositoryImpl.MAX_EXPIRY_SECONDS)
    }
}



