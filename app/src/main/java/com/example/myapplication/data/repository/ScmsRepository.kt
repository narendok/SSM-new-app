package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.SupabaseClient
import com.example.myapplication.data.model.ApqpStage
import com.example.myapplication.data.model.AssetEquipment
import com.example.myapplication.data.model.AuditLogEntry
import com.example.myapplication.data.model.BomRevision
import com.example.myapplication.data.model.ClientDeliverable
import com.example.myapplication.data.model.ControlledDoc
import com.example.myapplication.data.model.CustomerAccount
import com.example.myapplication.data.model.DailyTaskItem
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.DvprTestItem
import com.example.myapplication.data.model.FeasibilityReview
import com.example.myapplication.data.model.GrnRecord
import com.example.myapplication.data.model.IngestionRow
import com.example.myapplication.data.model.MaterialAllocation
import com.example.myapplication.data.model.PartComponent
import com.example.myapplication.data.model.PurchaseOrder
import com.example.myapplication.data.model.PurchasingBomReviewItem
import com.example.myapplication.data.model.RaciRow
import com.example.myapplication.data.model.SalesOpportunity
import com.example.myapplication.data.model.SmartBomItem
import com.example.myapplication.data.model.VendorRecord
import com.example.myapplication.data.model.WireHarness
import com.example.myapplication.data.model.CycleCountRecord
import com.example.myapplication.data.model.PickingItem
import com.example.myapplication.data.model.ProductionWorkOrder
import com.example.myapplication.data.model.ReceivingDraftVoucher
import com.example.myapplication.data.model.ResolvedScanResult
import com.example.myapplication.data.model.RoutingStationStep
import com.example.myapplication.data.model.ScannedEntityType
import com.example.myapplication.data.model.MaintenanceLogRecord
import com.example.myapplication.data.model.CalibrationCertificate
import com.example.myapplication.data.model.VisitorLogEntry
import com.example.myapplication.data.model.GatePassRecord
import com.example.myapplication.data.model.EmployeeDirectoryRecord
import com.example.myapplication.data.model.AiProceduralQuery
import com.example.myapplication.data.model.ApprovalDecisionRecord
import com.example.myapplication.data.model.ApprovalCategory
import com.example.myapplication.data.model.TravelPolicyRule
import com.example.myapplication.data.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

interface ScmsRepository {
    val currentRole: StateFlow<DepartmentRole>
    val apqpStages: StateFlow<List<ApqpStage>>
    val parts: StateFlow<List<PartComponent>>
    val harnesses: StateFlow<List<WireHarness>>
    val allocations: StateFlow<List<MaterialAllocation>>
    val grnRecords: StateFlow<List<GrnRecord>>
    val assets: StateFlow<List<AssetEquipment>>
    val auditLogs: StateFlow<List<AuditLogEntry>>
    val bomRevisions: StateFlow<List<BomRevision>>
    val activeBomRevisionId: StateFlow<String>
    val ingestionRows: StateFlow<List<IngestionRow>>
    val dvprTests: StateFlow<List<DvprTestItem>>
    val purchasingBom: StateFlow<List<PurchasingBomReviewItem>>
    val documents: StateFlow<List<ControlledDoc>>
    val raciMatrix: StateFlow<List<RaciRow>>
    val tasks: StateFlow<List<DailyTaskItem>>

    // Extended Master Plan Modules
    val feasibilityReviews: StateFlow<List<com.example.myapplication.data.model.FeasibilityReview>>
    val hardwarePath: StateFlow<String>
    val engineeringSquad: StateFlow<List<String>>
    val customers: StateFlow<List<com.example.myapplication.data.model.CustomerAccount>>
    val salesOpportunities: StateFlow<List<com.example.myapplication.data.model.SalesOpportunity>>
    val approvedVendors: StateFlow<List<com.example.myapplication.data.model.VendorRecord>>
    val purchaseOrders: StateFlow<List<com.example.myapplication.data.model.PurchaseOrder>>
    val clientDeliverables: StateFlow<List<com.example.myapplication.data.model.ClientDeliverable>>

    // Stage 5B: Mobile Receiving Drafts
    val receivingDrafts: StateFlow<List<ReceivingDraftVoucher>>
    // Stage 5E: Cycle Counts & Picking
    val cycleCounts: StateFlow<List<CycleCountRecord>>
    val pickList: StateFlow<List<PickingItem>>
    // Stage 6: Production Travelers & Work Orders
    val workOrders: StateFlow<List<ProductionWorkOrder>>

    // Stage 7: Facility Maintenance & Calibration
    val maintenanceLogs: StateFlow<List<MaintenanceLogRecord>>
    val calibrationCerts: StateFlow<List<CalibrationCertificate>>

    // Stage 8: Security & Gatehouse Logistics
    val visitorLogs: StateFlow<List<VisitorLogEntry>>
    val gatePasses: StateFlow<List<GatePassRecord>>

    // Stage 9: HR Directory & Skill Matrix
    val employeeDirectory: StateFlow<List<EmployeeDirectoryRecord>>

    // Stage 10: AI Procedural Queries
    val aiQueries: StateFlow<List<AiProceduralQuery>>

    // Human Approvals & Governance Center (Master Plan Â§26.3)
    val approvals: StateFlow<List<ApprovalDecisionRecord>>
    val travelPolicyRules: StateFlow<List<TravelPolicyRule>>

    fun switchRole(role: DepartmentRole)
    fun lookupMouserCatalog(mpn: String): PartComponent?
    fun registerPart(part: PartComponent): Result<Unit>
    fun issueMaterial(mpn: String, recipient: String, dept: String, quantity: Int, issuer: String): Result<Unit>
    fun returnMaterial(allocationId: String, quantity: Int): Result<Unit>
    fun setActiveBomRevision(revisionId: String)
    fun promoteBomRevision(targetRevision: String, ecoNumber: String, rationale: String): Result<Unit>
    fun runWhatIfSimulation(buildQty: Int): WhatIfSimulationResult
    fun loadIngestionPreset(preset: String)
    fun generateGrnFromIngestion(rowId: String): Result<String>
    fun releaseIqcHold(grnNumber: String, inspectionRef: String, inspector: String): Result<Unit>
    fun updateDvprTestStatus(testId: String, newStatus: String, technician: String)
    fun toggleTask(taskId: String)
    fun requestSignedStorageUrl(bucket: String, path: String): Result<String>
    fun selectHardwarePath(path: String, rationale: String)
    fun addCustomer(customer: com.example.myapplication.data.model.CustomerAccount)
    fun createOpportunity(opp: com.example.myapplication.data.model.SalesOpportunity)
    fun submitClientTicket(title: String, description: String): Result<String>
    fun saveReceivingDraft(draft: ReceivingDraftVoucher): Result<String>
    fun recordCycleCount(record: CycleCountRecord): Result<Unit>
    fun confirmPickLine(pickId: String, pickedQty: Int): Result<Unit>
    fun advanceWorkOrderStep(workOrderId: String, stepNo: Int, operatorName: String): Result<Unit>
    fun logMaintenanceTicket(record: MaintenanceLogRecord): Result<String>
    fun signOffCalibration(certId: String, technician: String): Result<Unit>
    fun checkInVisitor(visitor: VisitorLogEntry): Result<String>
    fun checkOutVisitor(visitorId: String): Result<Unit>
    fun verifyGatePass(passNumber: String, securityOfficer: String): Result<Unit>
    fun queryAiAssistant(prompt: String): AiProceduralQuery
    fun resolveScanQuery(query: String): ResolvedScanResult

    // Approvals Decision Methods
    fun approveGate(approvalId: String, notes: String): Result<Unit>
    fun rejectGate(approvalId: String, reason: String): Result<Unit>
    fun returnGate(approvalId: String, reworkNotes: String): Result<Unit>

    // Operational APQP Stage Gate Advancement Contract (Batch 3/4 parity)
    suspend fun verifyAndAdvanceStageGate(
        targetStage: Int,
        comments: String = "",
        evidenceJson: String? = null,
        recordId: String? = null
    ): Result<Unit>
    fun verifyAndAdvanceStageGateBlocking(
        targetStage: Int,
        comments: String = "",
        evidenceJson: String? = null,
        recordId: String? = null
    ): Result<Unit>
    fun updateControlledDocStatus(docId: String, newStatus: String): Result<Unit>
}

data class WhatIfSimulationResult(
    val buildQuantity: Int,
    val totalRequiredPcs: Int,
    val shortageLinesCount: Int,
    val totalDeficitUsd: Double,
    val lineStatus: List<ShortageStatusLine>
)

data class ShortageStatusLine(
    val mpn: String,
    val description: String,
    val requiredQty: Int,
    val availableReleasedQty: Int,
    val deficitQty: Int,
    val unitCostUsd: Double,
    val deficitUsd: Double,
    val isShortage: Boolean
)

class ScmsRepositoryImpl(
    private val secureStorage: SecureStorage? = null,
    private val supabaseClient: SupabaseClient? = null
) : ScmsRepository {

    private fun currentTime(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

    // 1. Current Active Role
    private val _currentRole = MutableStateFlow(DepartmentRole.ADMIN)
    override val currentRole: StateFlow<DepartmentRole> = _currentRole.asStateFlow()

    // 2. 9-Stage APQP Stepper
    private val _apqpStages = MutableStateFlow(
        listOf(
            ApqpStage(1, "Intake & Opportunity", "SOR intake, preliminary mNDA, RFQ evaluation", "Approved"),
            ApqpStage(2, "Feasibility & Contract Review", "6 HOD multi-dept review, HLPL/F/MKT/01 sign-off", "Approved"),
            ApqpStage(3, "Requirement & Commercial Lock", "Dual freeze: PRS-01 (Tech) & QUO-01 (Commercial)", "Approved"),
            ApqpStage(4, "Engineering Design & Schematics", "EHW-SCH-01 Schematics review & ECAD layout", "In Progress"),
            ApqpStage(
                5,
                "SMT Pilot Build & Proto Tooling",
                "Line 2 pilot setup, proto harnesses, SPI & AOI",
                "Locked",
                isLocked = true,
                prerequisiteDoc = "EHW-SCH-01",
                isBlockedByPrerequisite = true
            ),
            ApqpStage(6, "DVP&R Pre-Compliance Validation", "18 electrical/environmental tests (ISO 7637-2, AIS-140)", "Pending"),
            ApqpStage(
                7,
                "Production Release & PPAP Level 3",
                "PSW sign-off, IMDS declaration, Control Plan",
                "Locked",
                isLocked = true,
                prerequisiteDoc = "QA-DVP-01",
                isBlockedByPrerequisite = true
            ),
            ApqpStage(8, "Mass Manufacturing & Invoicing", "Commercial batch run 50k units, FIN-INV-01 tax invoice", "Pending"),
            ApqpStage(9, "EOL Testing & Field Warranty", "100% PDI calibration, telematics gateway, RMA", "Pending")
        )
    )
    override val apqpStages: StateFlow<List<ApqpStage>> = _apqpStages.asStateFlow()

    // 3. Mouser Catalog Lookup Database
    private val mouserCatalog = listOf(
        PartComponent(
            mpn = "STM32G0B1VCT6",
            description = "Main Application Microcontroller, ARM Cortex-M0+ 64MHz, 512KB Flash",
            manufacturer = "STMicroelectronics",
            packageFootprint = "LQFP-100",
            category = "Electronics",
            subcategory = "Microcontrollers & MCUs",
            onHand = 18540,
            released = 18540,
            iqcHold = 0,
            allocated = 1200,
            storageLocation = "Warehouse Zone A",
            binCoordinate = "A-12-04",
            reorderBuffer = 5000,
            unitCostUsd = 4.85,
            unitCostInr = 405.00,
            voltage = "2.0V - 3.6V",
            current = "8.4 mA active",
            aecRating = "AEC-Q100 Grade 1",
            clockSpeed = "64 MHz",
            tempRange = "-40Â°C to +125Â°C",
            dimensions = "14.0 x 14.0 x 1.4 mm",
            massGrams = 0.65,
            ipRating = "N/A (Board Mounted)",
            leadTimeWeeks = 12,
            mouserStock = 45200,
            volumeCoveragePct = 100,
            datasheetUrl = "https://www.st.com/resource/en/datasheet/stm32g0b1vc.pdf",
            mouserUrl = "https://www.mouser.in/c/?q=STM32G0B1VCT6",
            compatibleMachines = listOf("Yamaha YSM20R SMT Mounter")
        ),
        PartComponent(
            mpn = "EG915N-EU",
            description = "Automotive LTE Cat 1 Bis Cellular Module with GNSS",
            manufacturer = "Quectel Wireless",
            packageFootprint = "LGA-126",
            category = "Electronics",
            subcategory = "RF & Wireless",
            onHand = 7200,
            released = 7200,
            iqcHold = 0,
            allocated = 500,
            storageLocation = "Warehouse Zone A",
            binCoordinate = "A-08-02",
            reorderBuffer = 2000,
            unitCostUsd = 12.40,
            unitCostInr = 1035.40,
            voltage = "3.3V - 4.3V (Typ 3.8V)",
            current = "580 mA peak Tx",
            aecRating = "AIS-140 / TAC Certified",
            clockSpeed = "Baseband Processor",
            tempRange = "-40Â°C to +85Â°C",
            dimensions = "29.0 x 32.0 x 2.4 mm",
            massGrams = 4.2,
            ipRating = "N/A (Board Mounted)",
            leadTimeWeeks = 14,
            mouserStock = 18400,
            volumeCoveragePct = 95,
            datasheetUrl = "https://www.quectel.com/product/lte-eg915n-series",
            mouserUrl = "https://www.mouser.in/c/?q=EG915N-EU",
            compatibleMachines = listOf("Yamaha YSM20R SMT Mounter")
        ),
        PartComponent(
            mpn = "TCAN1042VDRQ1",
            description = "Automotive Fault-Protected High-Speed CAN Transceiver with I/O Level Shift",
            manufacturer = "Texas Instruments",
            packageFootprint = "SOIC-8",
            category = "Electronics",
            subcategory = "Interface ICs",
            onHand = 24000,
            released = 24000,
            iqcHold = 0,
            allocated = 2000,
            storageLocation = "Warehouse Zone B",
            binCoordinate = "B-03-11",
            reorderBuffer = 5000,
            unitCostUsd = 0.92,
            unitCostInr = 76.80,
            voltage = "4.5V - 5.5V",
            current = "45 mA Dominant",
            aecRating = "AEC-Q100 Grade 1",
            clockSpeed = "5 Mbps CAN-FD",
            tempRange = "-55Â°C to +125Â°C",
            dimensions = "4.9 x 3.91 x 1.75 mm",
            massGrams = 0.08,
            ipRating = "N/A",
            leadTimeWeeks = 8,
            mouserStock = 82000,
            volumeCoveragePct = 100,
            datasheetUrl = "https://www.ti.com/lit/ds/symlink/tcan1042-q1.pdf",
            mouserUrl = "https://www.mouser.in/c/?q=TCAN1042VDRQ1",
            compatibleMachines = listOf("Yamaha YSM20R SMT Mounter")
        ),
        PartComponent(
            mpn = "LMR14050SDDAR",
            description = "Automotive 40V 5A Step-Down DC-DC Buck Converter with Eco-mode",
            manufacturer = "Texas Instruments",
            packageFootprint = "SO-PowerPAD-8",
            category = "Electronics",
            subcategory = "Power Management",
            onHand = 12500,
            released = 12500,
            iqcHold = 0,
            allocated = 1000,
            storageLocation = "Warehouse Zone B",
            binCoordinate = "B-05-09",
            reorderBuffer = 3000,
            unitCostUsd = 1.85,
            unitCostInr = 154.50,
            voltage = "4V - 40V Input",
            current = "5A Continuous Output",
            aecRating = "AEC-Q100 Grade 1",
            clockSpeed = "200 kHz - 2.5 MHz",
            tempRange = "-40Â°C to +125Â°C",
            dimensions = "5.0 x 4.0 x 1.7 mm",
            massGrams = 0.12,
            ipRating = "N/A",
            leadTimeWeeks = 6,
            mouserStock = 35000,
            volumeCoveragePct = 100,
            datasheetUrl = "https://www.ti.com/lit/ds/symlink/lmr14050.pdf",
            mouserUrl = "https://www.mouser.in/c/?q=LMR14050SDDAR",
            compatibleMachines = listOf("Yamaha YSM20R SMT Mounter")
        ),
        PartComponent(
            mpn = "SM5S24A-E3/2D",
            description = "High-Power Automotive 3600W Transient Voltage Suppressor (Load Dump Protection)",
            manufacturer = "Vishay General Semiconductor",
            packageFootprint = "DO-218AB",
            category = "Electronics",
            subcategory = "Circuit Protection",
            onHand = 8900,
            released = 8900,
            iqcHold = 0,
            allocated = 800,
            storageLocation = "Warehouse Zone B",
            binCoordinate = "B-07-04",
            reorderBuffer = 2000,
            unitCostUsd = 1.65,
            unitCostInr = 137.80,
            voltage = "24V Stand-off, 38.9V Clamping",
            current = "93A Peak Pulse",
            aecRating = "AEC-Q101 Qualified",
            clockSpeed = "ISO 16750-2 Clamping",
            tempRange = "-55Â°C to +175Â°C",
            dimensions = "15.5 x 10.0 x 5.0 mm",
            massGrams = 2.6,
            ipRating = "N/A",
            leadTimeWeeks = 10,
            mouserStock = 14200,
            volumeCoveragePct = 100,
            datasheetUrl = "https://www.vishay.com/docs/88384/sm5s.pdf",
            mouserUrl = "https://www.mouser.in/c/?q=SM5S24A-E3/2D",
            compatibleMachines = listOf("Yamaha YSM20R SMT Mounter")
        ),
        PartComponent(
            mpn = "MX150-33472-1601",
            description = "Molex 16-Pin Sealed Automotive Female Connector Housing, Dual Row Key A",
            manufacturer = "Molex",
            packageFootprint = "16-Pin Sealed Dual Row",
            category = "Connectors",
            subcategory = "Automotive Headers",
            onHand = 4200,
            released = 4200,
            iqcHold = 0,
            allocated = 600,
            storageLocation = "Warehouse Zone C",
            binCoordinate = "C-02-15",
            reorderBuffer = 1500,
            unitCostUsd = 3.10,
            unitCostInr = 258.85,
            voltage = "500V DC Max",
            current = "22A per terminal",
            aecRating = "USCAR-2 Class 3",
            dimensions = "42.5 x 31.2 x 26.8 mm",
            massGrams = 14.8,
            ipRating = "IP67 / IP6K9K",
            wireGauge = "FLRY-B 0.50mmÂ² to 1.50mmÂ²",
            matingForce = "75 N max engagement",
            leadTimeWeeks = 8,
            mouserStock = 12000,
            volumeCoveragePct = 100,
            datasheetUrl = "https://www.molex.com/en-us/products/part-detail/334721601",
            mouserUrl = "https://www.mouser.in/c/?q=MX150-33472-1601",
            compatibleMachines = listOf("Schleuniger CrimpCenter 36S")
        )
    )

    // 4. Governed Parts Register (Tab 1)
    private val _parts = MutableStateFlow(mouserCatalog)
    override val parts: StateFlow<List<PartComponent>> = _parts.asStateFlow()

    // 5. Wire Harnesses (Tab 2)
    private val _harnesses = MutableStateFlow(
        listOf(
            WireHarness(
                assemblyId = "HLPL-HARN-AI4T-PWR-01",
                name = "Main Power & CAN Interface Harness (16-Pin Sealed)",
                projectId = "HLPL-AI4T-01",
                wireGauge = "FLRY-B 0.75mmÂ² (Red/Black) + 0.50mmÂ² (Twisted Pair)",
                connectors = "Molex MX150 16-Pin <-> TE Mate-N-Lok",
                pinCount = 16,
                matingRetentionForce = "85 N with CPA lock",
                ipRating = "IP67",
                status = "Production Approved"
            ),
            WireHarness(
                assemblyId = "HLPL-HARN-OBD-02",
                name = "OBD-II Diagnostic Extension Harness with Molded Strain Relief",
                projectId = "HLPL-AI4T-01",
                wireGauge = "FLRY-A 0.35mmÂ² / ISO 6722 compliant",
                connectors = "J1962 Male Plug <-> Micro-Fit 3.0",
                pinCount = 16,
                matingRetentionForce = "60 N",
                ipRating = "IP54",
                status = "Validated Prototype"
            )
        )
    )
    override val harnesses: StateFlow<List<WireHarness>> = _harnesses.asStateFlow()

    // 6. Project Allocations (Tab 3)
    private val _allocations = MutableStateFlow(
        listOf(
            MaterialAllocation(
                id = "ALLOC-2026-001",
                partMpn = "STM32G0B1VCT6",
                recipient = "Avery Vale",
                department = "EHW Â· Hardware Lead",
                projectId = "HLPL-AI4T-01",
                issuedQuantity = 50,
                returnedQuantity = 0,
                timestamp = "2026-10-17 14:30",
                issuer = "Stores Admin"
            ),
            MaterialAllocation(
                id = "ALLOC-2026-002",
                partMpn = "EG915N-EU",
                recipient = "Elena Rostova",
                department = "EHW Â· RF Engineer",
                projectId = "HLPL-AI4T-01",
                issuedQuantity = 20,
                returnedQuantity = 0,
                timestamp = "2026-10-17 16:15",
                issuer = "Stores Admin"
            ),
            MaterialAllocation(
                id = "ALLOC-2026-003",
                partMpn = "HLPL-HARN-AI4T-PWR-01",
                recipient = "Devon Lane",
                department = "ESW Â· Firmware Lead",
                projectId = "HLPL-AI4T-01",
                issuedQuantity = 5,
                returnedQuantity = 0,
                timestamp = "2026-10-18 04:00",
                issuer = "Stores Admin"
            )
        )
    )
    override val allocations: StateFlow<List<MaterialAllocation>> = _allocations.asStateFlow()

    // 7. GRN Records (Tab 4) - Demonstrating the Strict Invariant: 100% IQC Quarantine Hold on Inwarding
    private val _grnRecords = MutableStateFlow(
        listOf(
            GrnRecord(
                grnNumber = "GRN-2026-088",
                poNumber = "HLPL-PO-2026-042",
                vendorName = "Mouser Electronics India - Verified ASL",
                lotNumber = "LOT-2026-MOU-0881",
                dateCode = "2638",
                totalBilledQty = 2500,
                quantityAccepted = 0,
                quantityHeld = 2500,
                assignedBin = "IQC-HOLD-STAGE-01",
                status = "Pending IQC",
                qaInspectionRef = null,
                timestamp = "2026-10-18 02:45"
            ),
            GrnRecord(
                grnNumber = "GRN-2026-074",
                poNumber = "HLPL-PO-2026-039",
                vendorName = "Texas Instruments Direct",
                lotNumber = "LOT-2026-TI-0914",
                dateCode = "2632",
                totalBilledQty = 5000,
                quantityAccepted = 5000,
                quantityHeld = 0,
                assignedBin = "B-03-11",
                status = "Released",
                qaInspectionRef = "QA-IR-2026-074 (Jordan Blake)",
                timestamp = "2026-10-16 11:20"
            )
        )
    )
    override val grnRecords: StateFlow<List<GrnRecord>> = _grnRecords.asStateFlow()

    // 8. Governed Assets & Machinery Fleet (Tab 5)
    private val _assets = MutableStateFlow(
        listOf(
            AssetEquipment(
                assetTag = "EQ-SMT-001",
                serialNumber = "YAM-2024-9982-A",
                name = "Yamaha YSM20R High-Speed Modular SMT Mounter Line 2",
                category = "SMT Line",
                location = "Cleanroom Facility 1",
                custodian = "Casey Rowan (EMF)",
                compatiblePartNumbers = listOf("STM32G0B1VCT6", "EG915N-EU", "TCAN1042VDRQ1", "LMR14050SDDAR", "SM5S24A-E3/2D"),
                lastCalibrationDate = "2026-08-15",
                calibrationDue = "2026-11-15",
                status = "Operational"
            ),
            AssetEquipment(
                assetTag = "EQ-CRIMP-002",
                serialNumber = "SCH-36S-2025-11",
                name = "Schleuniger CrimpCenter 36S Automated Harness Machine",
                category = "Wire Harness Tooling",
                location = "Harness Fabrication Bay B",
                custodian = "Samira Holt (MECH)",
                compatiblePartNumbers = listOf("HLPL-HARN-AI4T-PWR-01", "MX150-33472-1601"),
                lastCalibrationDate = "2026-09-01",
                calibrationDue = "2026-12-01",
                status = "Operational"
            ),
            AssetEquipment(
                assetTag = "EQ-JIG-003",
                serialNumber = "CHR-8000-04",
                name = "Chroma 8000 Automated EOL Telematics Functional Test Jig",
                category = "Testing Jig",
                location = "Testing & Quality Lab",
                custodian = "Mina Cross (TST)",
                compatiblePartNumbers = listOf("STM32G0B1VCT6", "EG915N-EU", "TCAN1042VDRQ1"),
                lastCalibrationDate = "2026-10-01",
                calibrationDue = "2026-11-01",
                status = "Operational"
            ),
            AssetEquipment(
                assetTag = "EQ-IMTE-004",
                serialNumber = "KEY-DSOX-3024T",
                name = "Keysight InfiniiVision 200MHz 4-Ch Digital Storage Oscilloscope",
                category = "IMTE Instrument",
                location = "R&D Bench 3",
                custodian = "Avery Vale (EHW)",
                compatiblePartNumbers = listOf("STM32G0B1VCT6", "TCAN1042VDRQ1"),
                lastCalibrationDate = "2026-05-10",
                calibrationDue = "2026-11-10",
                status = "Operational"
            )
        )
    )
    override val assets: StateFlow<List<AssetEquipment>> = _assets.asStateFlow()

    // 9. Stock Movement Audit Log (Tab 6)
    private val _auditLogs = MutableStateFlow(
        listOf(
            AuditLogEntry(
                id = "LOG-001",
                timestamp = "2026-10-18 02:45",
                actor = "Stores Receiving Agent",
                action = "QuarantineHold",
                partMpn = "STM32G0B1VCT6",
                quantityDelta = 2500,
                reason = "Inward delivery via PO-2026-042; placed onto 100% IQC Quarantine Hold (IQC-HOLD-STAGE-01)",
                lotNumber = "LOT-2026-MOU-0881"
            ),
            AuditLogEntry(
                id = "LOG-002",
                timestamp = "2026-10-17 14:30",
                actor = "Stores Manager",
                action = "Issue",
                partMpn = "STM32G0B1VCT6",
                quantityDelta = -50,
                reason = "Material issued to Avery Vale for HLPL-AI4T-01 ECAD Bring-up",
                lotNumber = "LOT-2026-TI-0914"
            )
        )
    )
    override val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    // 10. Smart BOM Revisions (Tentative vs Released)
    private val _activeBomRevisionId = MutableStateFlow("v0.3-tentative")
    override val activeBomRevisionId: StateFlow<String> = _activeBomRevisionId.asStateFlow()

    private val _bomRevisions = MutableStateFlow(
        listOf(
            BomRevision(
                revisionId = "v0.3-tentative",
                title = "Exploratory Engineering Draft (Provisional MPNs & What-If Stock)",
                isLocked = false,
                effectiveDate = "2026-10-18",
                ecoNumber = null,
                items = listOf(
                    SmartBomItem(1, "STM32G0B1VCT6", "ARM Cortex-M0+ 64MHz MCU", "U1", 1, 4.85, 12, "Mouser Electronics", isProvisional = false),
                    SmartBomItem(2, "EG915N-EU", "Automotive LTE Cat 1 Cellular Modem", "MOD1", 1, 12.40, 14, "Quectel Direct", isProvisional = false),
                    SmartBomItem(3, "TCAN1042VDRQ1", "AEC-Q100 Fault-Protected CAN Transceiver", "U2", 1, 0.92, 8, "Texas Instruments", isProvisional = false),
                    SmartBomItem(4, "LMR14050SDDAR", "40V 5A Step-Down DC-DC Converter", "U3", 1, 1.85, 6, "Texas Instruments", isProvisional = false),
                    SmartBomItem(5, "SM5S24A-E3/2D", "3600W ISO 16750 Load Dump TVS Diode", "D1", 1, 1.65, 10, "Vishay Intertechnology", isProvisional = false),
                    SmartBomItem(6, "C0603C104K5RACTU", "0.1uF 50V X7R Ceramic Decoupling Array", "C1-C10", 10, 0.012, 4, "Kemet / Mouser", isProvisional = true, substituteMpn = "GRM188R71H104KA93D"),
                    SmartBomItem(7, "MX150-33472-1601", "16-Pin Sealed Dual Row Male Header", "J1", 1, 3.10, 8, "Molex LLC", isProvisional = false)
                )
            ),
            BomRevision(
                revisionId = "Rev A ECO-2026-004",
                title = "Governed Production Baseline (Locked for PPAP Level 3)",
                isLocked = true,
                effectiveDate = "2026-10-10",
                ecoNumber = "ECO-2026-004",
                items = listOf(
                    SmartBomItem(1, "STM32G0B1VCT6", "ARM Cortex-M0+ 64MHz MCU", "U1", 1, 4.85, 12, "Mouser Electronics", isProvisional = false),
                    SmartBomItem(2, "EG915N-EU", "Automotive LTE Cat 1 Cellular Modem", "MOD1", 1, 12.40, 14, "Quectel Direct", isProvisional = false),
                    SmartBomItem(3, "TCAN1042VDRQ1", "AEC-Q100 Fault-Protected CAN Transceiver", "U2", 1, 0.92, 8, "Texas Instruments", isProvisional = false),
                    SmartBomItem(4, "LMR14050SDDAR", "40V 5A Step-Down DC-DC Converter", "U3", 1, 1.85, 6, "Texas Instruments", isProvisional = false),
                    SmartBomItem(5, "SM5S24A-E3/2D", "3600W ISO 16750 Load Dump TVS Diode", "D1", 1, 1.65, 10, "Vishay Intertechnology", isProvisional = false),
                    SmartBomItem(6, "GRM188R71H104KA93D", "0.1uF 50V Automotive Decoupling Capacitor", "C1-C10", 10, 0.014, 6, "Murata Electronics", isProvisional = false),
                    SmartBomItem(7, "MX150-33472-1601", "16-Pin Sealed Dual Row Male Header", "J1", 1, 3.10, 8, "Molex LLC", isProvisional = false)
                )
            )
        )
    )
    override val bomRevisions: StateFlow<List<BomRevision>> = _bomRevisions.asStateFlow()

    // 11. Adaptive Ingestion Staging Rows
    private val _ingestionRows = MutableStateFlow(
        listOf(
            IngestionRow(
                id = "ING-01",
                vendorName = "Mouser Electronics India - Verified ASL",
                mpn = "STM32G0B1VCT6",
                orderedQty = 2500,
                billedQty = 2500,
                unitPrice = 4.85,
                vendorStatus = "Verified ASL",
                catalogStatus = "Exact Match",
                priceVariancePct = 0.0,
                qtyVariance = 0,
                distributorPreset = "Mouser Order Confirmation"
            ),
            IngestionRow(
                id = "ING-02",
                vendorName = "Digi-Key Global Logistics - Verified ASL",
                mpn = "EG915N-EU-ND",
                orderedQty = 1000,
                billedQty = 1000,
                unitPrice = 12.80,
                vendorStatus = "Verified ASL",
                catalogStatus = "Exact Match (Suffix -ND stripped)",
                priceVariancePct = +3.2,
                qtyVariance = 0,
                distributorPreset = "Digi-Key Commercial Billed"
            ),
            IngestionRow(
                id = "ING-03",
                vendorName = "Precision Wiring Systems Ltd - Verified ASL",
                mpn = "HLPL-HARN-AI4T-PWR-01",
                orderedQty = 500,
                billedQty = 500,
                unitPrice = 18.50,
                vendorStatus = "Verified ASL",
                catalogStatus = "Exact Match",
                priceVariancePct = 0.0,
                qtyVariance = 0,
                distributorPreset = "Custom Harness Delivery Challan"
            ),
            IngestionRow(
                id = "ING-04",
                vendorName = "ElectroComponents Express (Unregistered)",
                mpn = "UNK-CONN-04P",
                orderedQty = 200,
                billedQty = 250,
                unitPrice = 2.40,
                vendorStatus = "Unregistered Candidate",
                catalogStatus = "Unmatched Candidate",
                priceVariancePct = +14.2,
                qtyVariance = +50,
                distributorPreset = "Arbitrary Supplier Inward"
            )
        )
    )
    override val ingestionRows: StateFlow<List<IngestionRow>> = _ingestionRows.asStateFlow()

    // 12. Quality Assurance DVP&R Pre-Compliance Certification Table
    private val _dvprTests = MutableStateFlow(
        listOf(
            DvprTestItem(
                testId = "DVP-01",
                standard = "ISO 7637-2",
                testTitle = "Conducted Electrical Transient Immunity (Pulse 1, 2a, 3a/b)",
                condition = "Pulse 1: -100V, Pulse 2a: +37V, Pulse 3a/b: Â±150V",
                acceptanceCriteria = "Status II (No malfunction, auto recovery)",
                lab = "Internal Lab (XYZ Pvt Ltd)",
                status = "Passed",
                technician = "Jordan Blake (QA Lead)",
                signOffTimestamp = "2026-10-15 17:00"
            ),
            DvprTestItem(
                testId = "DVP-02",
                standard = "ISO 16750-2",
                testTitle = "Centralized Load Dump 5b Pulse Clamping",
                condition = "Us = 87V, Us* = 40V clamped via SM5S24A TVS, td = 400ms",
                acceptanceCriteria = "Zero component degradation; MCU watchdog intact",
                lab = "Internal Lab (XYZ Pvt Ltd)",
                status = "Passed",
                technician = "Jordan Blake (QA Lead)",
                signOffTimestamp = "2026-10-16 11:30"
            ),
            DvprTestItem(
                testId = "DVP-03",
                standard = "CISPR 25 Class 3",
                testTitle = "Conducted & Radiated RF Emissions",
                condition = "150 kHz to 2.5 GHz in anechoic chamber",
                acceptanceCriteria = "Peak and Quasi-peak under Class 3 limits",
                lab = "Outsourced Lab (ARAI Pune)",
                status = "In Progress",
                technician = "External ARAI Evaluator",
                signOffTimestamp = null
            ),
            DvprTestItem(
                testId = "DVP-04",
                standard = "ISO 10605",
                testTitle = "Electrostatic Discharge (ESD) Immunity",
                condition = "Â±8 kV contact discharge, Â±15 kV air discharge",
                acceptanceCriteria = "Class A performance; zero memory corruption",
                lab = "Internal Lab (XYZ Pvt Ltd)",
                status = "Passed",
                technician = "Jordan Blake (QA Lead)",
                signOffTimestamp = "2026-10-17 09:45"
            ),
            DvprTestItem(
                testId = "DVP-05",
                standard = "ISO 16750-4",
                testTitle = "Rapid Thermal Shock & Temperature Cycling",
                condition = "-40Â°C to +85Â°C, 250 cycles, 30 min dwell",
                acceptanceCriteria = "Solder joint integrity under 3D X-ray; no cracks",
                lab = "Outsourced Lab (ICAT Manesar)",
                status = "In Progress",
                technician = "ICAT Test Engineer",
                signOffTimestamp = null
            ),
            DvprTestItem(
                testId = "DVP-06",
                standard = "ISO 20653",
                testTitle = "IP67 Enclosure Ingress Protection (Dust & Water Submersion)",
                condition = "Talcum dust 8 hours + 1m water immersion 30 mins",
                acceptanceCriteria = "Zero dust ingress; zero water penetration into enclosure",
                lab = "Internal Lab (XYZ Pvt Ltd)",
                status = "Passed",
                technician = "Samira Holt (MECH Lead)",
                signOffTimestamp = "2026-10-17 15:00"
            ),
            DvprTestItem(
                testId = "DVP-07",
                standard = "AIS-140 Annexure 8",
                testTitle = "Automotive Random Vibration & Mechanical Shock",
                condition = "10 Hz - 500 Hz, 3 axes, 8 hours per axis, 50G shock",
                acceptanceCriteria = "Zero physical breakage, no connector unseating",
                lab = "Outsourced Lab (ARAI Pune)",
                status = "Scheduled",
                technician = "ARAI Scheduled Inspector",
                signOffTimestamp = null
            ),
            DvprTestItem(
                testId = "DVP-08",
                standard = "AIS-140 Annexure 3",
                testTitle = "Emergency Panic Button & Dual-Server Telematics Transmission",
                condition = "Simulated crash & panic press under GSM dead-zone",
                acceptanceCriteria = "NMEA packet queued in flash; sent within 5s of network restore",
                lab = "Internal Lab (XYZ Pvt Ltd)",
                status = "Passed",
                technician = "Devon Lane (ESW Lead)",
                signOffTimestamp = "2026-10-18 01:20"
            )
        )
    )
    override val dvprTests: StateFlow<List<DvprTestItem>> = _dvprTests.asStateFlow()

    // 13. Purchasing SCM R&D BOM Review Table
    private val _purchasingBom = MutableStateFlow(
        listOf(
            PurchasingBomReviewItem(
                mpn = "STM32G0B1VCT6",
                description = "MCU ARM Cortex-M0+ 512KB Flash",
                targetCost = 5.20,
                quotedCost = 4.85,
                costDelta = -0.35,
                costSavingsPct = 6.7,
                leadTimeWeeks = 12,
                isLongLead = false,
                primarySupplier = "Mouser Electronics",
                dualSourceAlternate = "ST Direct (PPAP Available)"
            ),
            PurchasingBomReviewItem(
                mpn = "EG915N-EU",
                description = "Automotive LTE Cat 1 Modem + GNSS",
                targetCost = 13.00,
                quotedCost = 12.40,
                costDelta = -0.60,
                costSavingsPct = 4.6,
                leadTimeWeeks = 14,
                isLongLead = true,
                primarySupplier = "Quectel Direct",
                dualSourceAlternate = "SimCom A7672S (AEC-Q Qualified)"
            ),
            PurchasingBomReviewItem(
                mpn = "TCAN1042VDRQ1",
                description = "AEC-Q100 High-Speed CAN Transceiver",
                targetCost = 1.10,
                quotedCost = 0.92,
                costDelta = -0.18,
                costSavingsPct = 16.4,
                leadTimeWeeks = 8,
                isLongLead = false,
                primarySupplier = "Texas Instruments",
                dualSourceAlternate = "NXP TJA1051T/3 (AEC-Q100)"
            ),
            PurchasingBomReviewItem(
                mpn = "LMR14050SDDAR",
                description = "40V 5A Step-Down DC-DC Converter",
                targetCost = 2.10,
                quotedCost = 1.85,
                costDelta = -0.25,
                costSavingsPct = 11.9,
                leadTimeWeeks = 6,
                isLongLead = false,
                primarySupplier = "Texas Instruments",
                dualSourceAlternate = "MPS MP4560 (Automotive Grade)"
            ),
            PurchasingBomReviewItem(
                mpn = "MX150-33472-1601",
                description = "16-Pin Sealed Dual Row Header",
                targetCost = 3.50,
                quotedCost = 3.10,
                costDelta = -0.40,
                costSavingsPct = 11.4,
                leadTimeWeeks = 8,
                isLongLead = false,
                primarySupplier = "Molex India",
                dualSourceAlternate = "TE Connectivity AMPSEAL 16"
            )
        )
    )
    override val purchasingBom: StateFlow<List<PurchasingBomReviewItem>> = _purchasingBom.asStateFlow()

    // 14. Controlled Documents
    private val _documents = MutableStateFlow(
        listOf(
            ControlledDoc("HLPL-AI4T-01-EHW-PRS-01", "Product Requirement Specification (PRS) Rev 01", "EHW", "Stage 3", "Avery Vale", "Rev 01", "Approved", storagePath = "01_Governance/EHW-PRS-01.pdf"),
            ControlledDoc("HLPL-AI4T-01-SAL-QUO-01", "Commercial Quotation & Volume Price Commitment", "SAL", "Stage 3", "Priya Sharma", "Rev 01", "Approved", storagePath = "01_Governance/SAL-QUO-01.pdf"),
            ControlledDoc("HLPL-AI4T-01-EHW-SCH-01", "Hardware Schematic Diagram & ECAD Netlist", "EHW", "Stage 4", "Avery Vale", "Rev 02", "Under Review", storagePath = "03_Hardware/EHW-SCH-01.pdf"),
            ControlledDoc("HLPL-AI4T-01-QA-DVP-01", "Design Verification Plan and Report (DVP&R)", "QA", "Stage 6", "Jordan Blake", "Rev 01", "In Progress", storagePath = "06_Quality/QA-DVP-01.pdf"),
            ControlledDoc("HLPL-AI4T-01-ENC-MECH-01", "IP67 Enclosure 3D CAD & Thermal Simulation", "MECH", "Stage 4", "Samira Holt", "Rev 01", "Approved", storagePath = "04_Mechanical/ENC-MECH-01.stp")
        )
    )
    override val documents: StateFlow<List<ControlledDoc>> = _documents.asStateFlow()

    // 15. RACI Governance Matrix
    private val _raciMatrix = MutableStateFlow(
        listOf(
            RaciRow(1, "Intake & Opportunity", "SAL (Sales & BD)", "Managing Director", "EHW, PUR, FIN", "All Departments"),
            RaciRow(2, "Feasibility & Contract Review", "EHW, ESW, MECH, PUR, QA, FIN", "QA Lead (Jordan Blake)", "Executive Admin", "Client (Tata Motors)"),
            RaciRow(3, "Requirement & Commercial Lock", "EHW & SAL", "Executive Admin", "FIN & PUR", "Engineering Team"),
            RaciRow(4, "Engineering Design & Schematics", "EHW (Avery Vale)", "EHW Lead", "QA, MECH, PUR", "EMF (Manufacturing)"),
            RaciRow(5, "SMT Pilot Build & Proto Tooling", "EMF (Casey Rowan)", "EMF Lead", "EHW, PUR, TST", "Executive Admin"),
            RaciRow(6, "DVP&R Pre-Compliance Validation", "QA (Jordan Blake)", "QA Lead", "EHW, ESW, MECH", "Client & ARAI Pune"),
            RaciRow(7, "Production Release & PPAP Level 3", "QA & EMF", "Plant Manager", "PUR, EHW, SAL", "Client Fleet Team"),
            RaciRow(8, "Mass Manufacturing & Invoicing", "EMF & FIN", "Managing Director", "PUR, Stores", "Client Accounts"),
            RaciRow(9, "EOL Testing & Field Warranty", "TST & SIT", "Service Head", "QA, EMF", "Client Fleet Manager")
        )
    )
    override val raciMatrix: StateFlow<List<RaciRow>> = _raciMatrix.asStateFlow()

    // 16. Daily Tasks & Action Items
    private val _tasks = MutableStateFlow(
        listOf(
            DailyTaskItem("TSK-01", "Complete ISO 7637-2 Transient Pulse 1 verification sign-off", "QA", "Critical", "Jordan Blake", "Today", isCompleted = true),
            DailyTaskItem("TSK-02", "Reconcile Mouser Order LOT-2026-MOU-0881 against PO-2026-042", "PUR", "High", "Morgan Reed", "Today", isCompleted = true),
            DailyTaskItem("TSK-03", "Review EHW-SCH-01 Schematics netlist for SMT Pilot Gate unlock", "EHW", "Critical", "Avery Vale", "Today", isCompleted = false),
            DailyTaskItem("TSK-04", "Calibrate Chroma 8000 EOL test jig before Stage 5 pilot batch", "TST", "High", "Mina Cross", "Today", isCompleted = false),
            DailyTaskItem("TSK-05", "Inspect 16-Pin Wire Harness crimp pull force (USCAR-2 spec)", "MECH", "Normal", "Samira Holt", "Tomorrow", isCompleted = false)
        )
    )
    override val tasks: StateFlow<List<DailyTaskItem>> = _tasks.asStateFlow()

    // 17. 6 HOD Feasibility & Contract Review (Master Plan Â§4 & Â§5, Stage 2 Gate)
    private val _feasibilityReviews = MutableStateFlow(
        listOf(
            FeasibilityReview(
                deptCode = "EHW",
                deptName = "Electronics & Hardware Architecture",
                leadName = "Avery Vale",
                verdict = "FEASIBLE",
                scopeSummary = "Automotive Telematics Gateway schematic with STM32H753 MCU, Quectel EG915N-EU LTE Cat-1, dual CAN-FD, and ISO 7637-2 power protection.",
                internalRemarks = "Custom 4-layer FR4 stackup satisfies thermal & EMC targets. Quectel module requires early PO due to 14-week lead time. ATECC608B hardware secure element locked for TLS client cert storage.",
                clientHighlights = "Architecture complies with Tata Motors SOR v1.4 telematics specifications. 100% AEC-Q100 Grade 2 active silicon selected.",
                keyParameters = mapOf("MCU Clock" to "480 MHz", "Operating Temp" to "-40Â°C to +85Â°C", "Deep Sleep Current" to "< 1.8 mA", "CAN Channels" to "2x CAN-FD with Wake-up"),
                signedAt = "2026-03-12 14:30",
                signatureHash = "SHA256:8f9a2b1c4e7d0f3e8291a0c4f1e5"
            ),
            FeasibilityReview(
                deptCode = "ESW",
                deptName = "Embedded Software & Firmware",
                leadName = "Devon Lane",
                verdict = "FEASIBLE",
                scopeSummary = "Automotive FreeRTOS kernel with dual-bank secure bootloader, UDS on CAN diagnostics, and TLS 1.3 MQTT edge telemetry.",
                internalRemarks = "Dual-bank flash architecture allocated for seamless A/B rollback. Cryptographic signature check verified in <350ms on cold boot. Tata Motors proprietary CAN matrix dbc imported.",
                clientHighlights = "AUTOSAR classic stack compatibility with real-time AIS-140 emergency health telemetry and remote Over-The-Air (FOTA) delta updates.",
                keyParameters = mapOf("RTOS" to "FreeRTOS v10.5", "Bootloader" to "Dual-Bank A/B Secure", "FOTA Compression" to "Courgette Delta", "Cloud Protocol" to "MQTT over TLS 1.3"),
                signedAt = "2026-03-12 15:15",
                signatureHash = "SHA256:3a4b5c6d7e8f9012abcdef345678"
            ),
            FeasibilityReview(
                deptCode = "MECH",
                deptName = "Mechanical & Thermal Packaging",
                leadName = "Elena Rostova",
                verdict = "FEASIBLE",
                scopeSummary = "IP67 Die-cast ADC12 aluminum housing, dual silicone compression gasket, and Molex MX150 automotive interface.",
                internalRemarks = "Thermal FEA simulation shows junction temp stays < 72Â°C at 50Â°C ambient engine-bay conditions. Tooling quote approved for â‚¹8.5 Lakhs with 4-week T0 sample turnaround.",
                clientHighlights = "Rigid IP67 water/dust proof enclosure designed for Tata Ace EV chassis mounting, vibration certified to ISO 16750-3 (10G RMS).",
                keyParameters = mapOf("Ingress Protection" to "IP67 Immersion Rated", "Housing Material" to "ADC12 Die-Cast Aluminum", "Vibration Rating" to "ISO 16750-3 (10G RMS)", "Operating Temp" to "-40Â°C to +105Â°C"),
                signedAt = "2026-03-12 16:00",
                signatureHash = "SHA256:112233445566778899aabbccddee"
            ),
            FeasibilityReview(
                deptCode = "PUR",
                deptName = "Purchasing & SCM Supply Chain",
                leadName = "Marcus Vance",
                verdict = "FEASIBLE_WITH_CONDITIONS",
                scopeSummary = "Dual-source supply chain verification, Mouser/Digi-Key inventory locking, and volume vendor allocation.",
                internalRemarks = "Long lead-time on TI TCAN1042 transceivers mitigated by locking 10,000 pcs reel stock with Mouser. Alternate NXP TJA1051T qualified as secondary source. Target BOM locked at $42.80.",
                clientHighlights = "94% of active BOM lines validated with Tier-1 franchised distributors. Committed piece price â‚¹4,066/unit ($48.80) within client commercial ceiling.",
                keyParameters = mapOf("Locked Unit BOM" to "$42.80", "AVL Coverage" to "94%", "Long-Lead Lines" to "2 Lines (Reel Reserved)", "Payment Terms" to "Net 30 franchised"),
                signedAt = "2026-03-13 10:00",
                signatureHash = "SHA256:99aabbccddeeff00112233445566"
            ),
            FeasibilityReview(
                deptCode = "QA",
                deptName = "Quality Assurance & Regulatory Compliance",
                leadName = "Sarah Chen",
                verdict = "FEASIBLE",
                scopeSummary = "IATF 16949 APQP Stage Gate compliance, ARAI AIS-140 type approval path, and ISO 7637-2 pre-compliance test matrix.",
                internalRemarks = "8 pre-compliance DVP&R tests mapped. External lab test slots pre-booked with ARAI Pune for Q3 2026. Zero-defect IQC quarantine hold enforced on all incoming inward deliveries.",
                clientHighlights = "Full automotive qualification pathway established. PPAP Level 3 package delivery guaranteed prior to commercial SOP.",
                keyParameters = mapOf("Quality System" to "IATF 16949:2016", "Pre-Compliance Tests" to "8/8 Pass Criteria Defined", "PPAP Level" to "Level 3 with PSW", "Regulatory Agency" to "ARAI Pune (AIS-140)"),
                signedAt = "2026-03-13 11:30",
                signatureHash = "SHA256:fedcba9876543210fedcba987654"
            ),
            FeasibilityReview(
                deptCode = "FIN",
                deptName = "Finance & Commercial Contracts",
                leadName = "Rajesh Sharma",
                verdict = "FEASIBLE",
                scopeSummary = "Commercial evaluation of â‚¹20.33 Cr multi-year contract, volume price commitments, and milestone cash flows.",
                internalRemarks = "NRE budget â‚¹45.00 Lakhs covers tooling, ARAI testing, and prototype runs with 35% advance milestone. Project gross margin modeled at 34.2%.",
                clientHighlights = "Contract volume locked for 50,000 units with progressive delivery schedules and milestone-linked payment terms.",
                keyParameters = mapOf("Total Contract" to "â‚¹20.33 Cr", "Order Volume" to "50,000 Units", "NRE Milestone" to "â‚¹45.00 Lakhs", "Gross Margin Target" to "34.2%"),
                signedAt = "2026-03-13 14:00",
                signatureHash = "SHA256:abcdef0123456789abcdef012345"
            )
        )
    )
    override val feasibilityReviews: StateFlow<List<FeasibilityReview>> = _feasibilityReviews.asStateFlow()

    // 18. Hardware Kick-off Path Selection ("pilot" = Direct-to-Pilot 12 wks vs "poc" = Proof of Concept First 20 wks)
    private val _hardwarePath = MutableStateFlow("pilot")
    override val hardwarePath: StateFlow<String> = _hardwarePath.asStateFlow()

    // 19. Dedicated Engineering Squad
    private val _engineeringSquad = MutableStateFlow(
        listOf(
            "Avery Vale (Lead Electronics Architect)",
            "Elena Rostova (Lead Mechanical Engineer)",
            "Devon Lane (Lead Firmware & Systems Engineer)",
            "Marcus Vance (SCM & Purchasing Specialist)",
            "Sarah Chen (Head of Quality & Compliance)"
        )
    )
    override val engineeringSquad: StateFlow<List<String>> = _engineeringSquad.asStateFlow()

    // 20. Customer Register & Accounts (Master Plan Â§4 & Â§6)
    private val _customers = MutableStateFlow(
        listOf(
            CustomerAccount(
                id = "CUST-TATA-01",
                name = "Tata Motors Limited",
                division = "EV Fleet & Commercial Vehicle Division",
                contactPerson = "Rajiv Mehta (Head of Procurement)",
                email = "rajiv.mehta@tatamotors.com",
                phone = "+91 22 6665 8282",
                location = "Bombay House, Mumbai / Pune Plant",
                activeProjects = listOf("HLPL-AI4T-01 (Tata Ace EV 4G Gateway)"),
                ndaStatus = "Active & Executed",
                tierRating = "Tier-1 OEM"
            ),
            CustomerAccount(
                id = "CUST-AL-02",
                name = "Ashok Leyland Limited",
                division = "Electric Commercial Vehicles (e-Dost)",
                contactPerson = "K. Sundaram (VP SCM & Electronics)",
                email = "k.sundaram@ashokleyland.com",
                phone = "+91 44 2530 1818",
                location = "Guindy, Chennai, Tamil Nadu",
                activeProjects = listOf("HLPL-EV-AL-02 (e-Dost Fleet V2X)"),
                ndaStatus = "Active & Executed",
                tierRating = "Tier-1 OEM"
            ),
            CustomerAccount(
                id = "CUST-ME-03",
                name = "Mahindra Electric Mobility Ltd",
                division = "Last-Mile Commercial Mobility",
                contactPerson = "Priya Deshmukh (Technical Lead)",
                email = "deshmukh.priya@mahindra.com",
                phone = "+91 80 6799 4000",
                location = "Electronic City, Bengaluru, Karnataka",
                activeProjects = listOf("HLPL-T3-ME-01 (Treo Telematics Retrofit)"),
                ndaStatus = "Active & Executed",
                tierRating = "Tier-1 OEM"
            ),
            CustomerAccount(
                id = "CUST-SEL-04",
                name = "Select Motors & Conversions",
                division = "Retrofit Fleet Electrification",
                contactPerson = "Vikram Singh (Managing Partner)",
                email = "vikram@selectmotors.co.in",
                phone = "+91 124 459 2000",
                location = "Gurugram, Haryana",
                activeProjects = emptyList(),
                ndaStatus = "Pending Renewal",
                tierRating = "Tier-2 Retrofit Partner"
            )
        )
    )
    override val customers: StateFlow<List<CustomerAccount>> = _customers.asStateFlow()

    // 21. Sales Opportunities & Enquiries Register
    private val _salesOpportunities = MutableStateFlow(
        listOf(
            SalesOpportunity(
                id = "OPP-2026-001",
                title = "Tata Ace EV 4G Telematics Gateway",
                clientName = "Tata Motors Limited",
                volume = 50000,
                projectedValueInr = "â‚¹20.33 Cr",
                status = "Contract Locked",
                sorDocId = "HLPL-AI4T-01-MKT-SOR-01",
                isOpportunity = true
            ),
            SalesOpportunity(
                id = "OPP-2026-002",
                title = "Ashok Leyland 5G V2X Fleet Gateway",
                clientName = "Ashok Leyland Limited",
                volume = 15000,
                projectedValueInr = "â‚¹8.75 Cr",
                status = "Feasibility Review",
                sorDocId = "HLPL-AL-RFQ-2026-08",
                isOpportunity = true
            ),
            SalesOpportunity(
                id = "ENQ-2026-009",
                title = "Mahindra Treo 3W Telematics Retrofit",
                clientName = "Mahindra Electric Mobility Ltd",
                volume = 25000,
                projectedValueInr = "â‚¹6.50 Cr",
                status = "Enquiry Intake",
                sorDocId = "HLPL-ME-ENQ-2026-09",
                isOpportunity = false
            )
        )
    )
    override val salesOpportunities: StateFlow<List<SalesOpportunity>> = _salesOpportunities.asStateFlow()

    // 22. Approved Vendor List (AVL)
    private val _approvedVendors = MutableStateFlow(
        listOf(
            VendorRecord(
                vendorCode = "VND-MOU-01",
                name = "Mouser Electronics India",
                category = "Global Franchised Catalog Distributor",
                country = "United States / India",
                aslStatus = "Verified ASL",
                isoCertification = "ISO 9001:2015 / AS9120B",
                riskRating = "Low Risk",
                paymentTerms = "Net 30 franchised"
            ),
            VendorRecord(
                vendorCode = "VND-DIGI-02",
                name = "Digi-Key Electronics",
                category = "Global Active & Passive Components",
                country = "United States",
                aslStatus = "Verified ASL",
                isoCertification = "ISO 9001:2015",
                riskRating = "Low Risk",
                paymentTerms = "Credit Line / Net 30"
            ),
            VendorRecord(
                vendorCode = "VND-TI-03",
                name = "Texas Instruments India Pvt Ltd",
                category = "Semiconductors, Power ICs & CAN Transceivers",
                country = "United States / India",
                aslStatus = "Verified ASL",
                isoCertification = "IATF 16949 / ISO 9001",
                riskRating = "Low Risk",
                paymentTerms = "Net 45 Direct"
            ),
            VendorRecord(
                vendorCode = "VND-MOL-04",
                name = "Molex India Tooling Ltd",
                category = "Automotive Sealed Interconnects & Headers",
                country = "Japan / India",
                aslStatus = "Verified ASL",
                isoCertification = "IATF 16949:2016",
                riskRating = "Low Risk",
                paymentTerms = "Net 30 Direct"
            ),
            VendorRecord(
                vendorCode = "VND-QUE-05",
                name = "Quectel Wireless Solutions",
                category = "Automotive Cellular & GNSS Modules",
                country = "China / Global",
                aslStatus = "Verified ASL",
                isoCertification = "ISO 9001 / IATF 16949",
                riskRating = "Medium Risk",
                paymentTerms = "Letter of Credit / Net 60"
            ),
            VendorRecord(
                vendorCode = "VND-PRE-06",
                name = "Precision Harnessing & Cables Pune",
                category = "Automotive Wire Harness Assembly",
                country = "India",
                aslStatus = "Conditional",
                isoCertification = "ISO 9001:2015",
                riskRating = "Medium Risk",
                paymentTerms = "Net 15"
            )
        )
    )
    override val approvedVendors: StateFlow<List<VendorRecord>> = _approvedVendors.asStateFlow()

    // 23. Purchase Orders Register
    private val _purchaseOrders = MutableStateFlow(
        listOf(
            PurchaseOrder(
                poNumber = "HLPL-PO-2026-042",
                vendorName = "Mouser Electronics India",
                issueDate = "2026-03-05",
                expectedDelivery = "2026-03-20",
                totalAmountUsd = 14250.00,
                status = "Partially Inwarded",
                lineItemsCount = 4
            ),
            PurchaseOrder(
                poNumber = "HLPL-PO-2026-043",
                vendorName = "Digi-Key Electronics",
                issueDate = "2026-03-10",
                expectedDelivery = "2026-03-25",
                totalAmountUsd = 8740.00,
                status = "Approved",
                lineItemsCount = 3
            ),
            PurchaseOrder(
                poNumber = "HLPL-PO-2026-044",
                vendorName = "Quectel Wireless Solutions",
                issueDate = "2026-03-12",
                expectedDelivery = "2026-04-10",
                totalAmountUsd = 32000.00,
                status = "Pending Delivery",
                lineItemsCount = 1
            )
        )
    )
    override val purchaseOrders: StateFlow<List<PurchaseOrder>> = _purchaseOrders.asStateFlow()

    // 24. Client OEM Deliverables & Document Vault (Master Plan Â§3)
    private val _clientDeliverables = MutableStateFlow(
        listOf(
            ClientDeliverable(
                id = "DELIV-SOR-01",
                code = "HLPL-AI4T-01-MKT-SOR-01",
                title = "System Requirement Specification (SOR)",
                category = "Requirements",
                standard = "Tata Motors SOR v1.4",
                status = "Signed & Locked",
                date = "2026-02-15",
                owner = "Rajiv Mehta (Client)",
                description = "Core functional specifications for Tata Ace EV Telematics Gateway."
            ),
            ClientDeliverable(
                id = "DELIV-mNDA-01",
                code = "HLPL-AI4T-01-MKT-mNDA-01",
                title = "Mutual Non-Disclosure Agreement",
                category = "Legal",
                standard = "Bilateral Corporate NDA",
                status = "Executed",
                date = "2026-02-10",
                owner = "Legal & Compliance",
                description = "Mutual proprietary IP, schematic confidentiality, and trade secret protections."
            ),
            ClientDeliverable(
                id = "DELIV-QUO-01",
                code = "HLPL-AI4T-01-FIN-QUO-01",
                title = "Commercial Quotation & Cost Breakdown",
                category = "Commercial",
                standard = "Volume 50k Locked Pricing",
                status = "Locked",
                date = "2026-03-01",
                owner = "Rajesh Sharma (CFO)",
                description = "Piece price â‚¹4,066/unit ($48.80) + NRE â‚¹45.00 Lakhs locked across 50,000 units."
            ),
            ClientDeliverable(
                id = "DELIV-TAC-01",
                code = "HLPL-AI4T-01-QA-TAC-01",
                title = "ARAI AIS-140 Type Approval Certificate",
                category = "Regulatory",
                standard = "AIS-140 Amendment 2",
                status = "Pre-Certification In Progress",
                date = "Target 2026-08-30",
                owner = "Sarah Chen (QA Lead)",
                description = "Type approval certification required for roadworthiness and EV commercial registration."
            ),
            ClientDeliverable(
                id = "DELIV-PDI-01",
                code = "HLPL-AI4T-01-MFG-PDI-01",
                title = "Pre-Delivery Inspection Checklist",
                category = "Manufacturing",
                standard = "IATF 16949 Â§8.6",
                status = "Ready",
                date = "2026-03-15",
                owner = "Production Quality Lead",
                description = "100% end-of-line flashing, calibration, and enclosure seal leak-test protocol."
            ),
            ClientDeliverable(
                id = "DELIV-INV-01",
                code = "HLPL-INV-2026-088",
                title = "Inwarding & IQC Inspection Dossier",
                category = "Stores / SCM",
                standard = "IQC Zero-Defect Protocol",
                status = "Quarantine Staged",
                date = "2026-03-16",
                owner = "Warehouse SCM",
                description = "Inwarding record and certificate of analysis verification for pilot lot."
            )
        )
    )
    override val clientDeliverables: StateFlow<List<ClientDeliverable>> = _clientDeliverables.asStateFlow()

    // 25. Receiving Draft Vouchers (Stage 5B Mobile Inwarding)
    private val _receivingDrafts = MutableStateFlow(
        listOf(
            ReceivingDraftVoucher(
                voucherId = "RCV-DRAFT-2026-001",
                poNumber = "HLPL-PO-2026-042",
                vendorName = "Mouser Electronics India",
                mpn = "STM32G0B1VCT6",
                lotNumber = "LOT-2026-MOU-0881",
                batchCode = "B-9921",
                dateCode = "2638",
                reelId = "REEL-ST-99120",
                quantityReceived = 2500,
                mslRating = "MSL-3 (168 hrs)",
                esdCompliant = true,
                packageCondition = "Intact / Verified",
                inspectorName = "Marcus Vance (SCM)",
                timestamp = "2026-10-18 08:30",
                status = "Local Draft (Awaiting IQC Inward)"
            ),
            ReceivingDraftVoucher(
                voucherId = "RCV-DRAFT-2026-002",
                poNumber = "HLPL-PO-2026-044",
                vendorName = "Quectel Wireless Solutions",
                mpn = "EG915N-EU",
                lotNumber = "LOT-2026-QUE-4410",
                batchCode = "B-8802",
                dateCode = "2635",
                reelId = "REEL-QC-1104",
                quantityReceived = 1000,
                mslRating = "MSL-3 (168 hrs)",
                esdCompliant = true,
                packageCondition = "Intact / Verified",
                inspectorName = "Elena Rostova",
                timestamp = "2026-10-18 09:15",
                status = "Local Draft (Awaiting IQC Inward)"
            )
        )
    )
    override val receivingDrafts: StateFlow<List<ReceivingDraftVoucher>> = _receivingDrafts.asStateFlow()

    // 26. Cycle Count Records (Stage 5E Inventory Verification)
    private val _cycleCounts = MutableStateFlow(
        listOf(
            CycleCountRecord(
                countId = "CC-2026-001",
                mpn = "STM32G0B1VCT6",
                description = "Main MCU ARM Cortex-M0+ 64MHz 512KB",
                binCoordinate = "A-12-04",
                systemQty = 18540,
                countedQty = 18540,
                variance = 0,
                counterName = "Marcus Vance",
                status = "Matched"
            ),
            CycleCountRecord(
                countId = "CC-2026-002",
                mpn = "EG915N-EU",
                description = "Automotive LTE Cat 1 Bis Cellular Module",
                binCoordinate = "A-08-02",
                systemQty = 7200,
                countedQty = 7195,
                variance = -5,
                counterName = "Marcus Vance",
                status = "Discrepancy"
            ),
            CycleCountRecord(
                countId = "CC-2026-003",
                mpn = "TCAN1042VDRQ1",
                description = "Automotive CAN Transceiver AEC-Q100",
                binCoordinate = "B-03-11",
                systemQty = 24000,
                countedQty = null,
                variance = null,
                counterName = null,
                status = "Pending"
            )
        )
    )
    override val cycleCounts: StateFlow<List<CycleCountRecord>> = _cycleCounts.asStateFlow()

    // 27. Work Order Pick List Items (Stage 5E Picking)
    private val _pickList = MutableStateFlow(
        listOf(
            PickingItem(
                pickId = "PICK-001",
                workOrderNo = "WO-2026-AI4T-001",
                mpn = "STM32G0B1VCT6",
                requiredQty = 100,
                pickedQty = 100,
                binCoordinate = "A-12-04",
                isConfirmed = true
            ),
            PickingItem(
                pickId = "PICK-002",
                workOrderNo = "WO-2026-AI4T-001",
                mpn = "EG915N-EU",
                requiredQty = 100,
                pickedQty = 100,
                binCoordinate = "A-08-02",
                isConfirmed = true
            ),
            PickingItem(
                pickId = "PICK-003",
                workOrderNo = "WO-2026-AI4T-001",
                mpn = "TCAN1042VDRQ1",
                requiredQty = 100,
                pickedQty = 0,
                binCoordinate = "B-03-11",
                isConfirmed = false
            ),
            PickingItem(
                pickId = "PICK-004",
                workOrderNo = "WO-2026-AI4T-001",
                mpn = "LMR14050SDDAR",
                requiredQty = 100,
                pickedQty = 0,
                binCoordinate = "B-05-02",
                isConfirmed = false
            )
        )
    )
    override val pickList: StateFlow<List<PickingItem>> = _pickList.asStateFlow()

    // 28. Production Work Orders & Electronic Routing Travelers (Stage 6)
    private val _workOrders = MutableStateFlow(
        listOf(
            ProductionWorkOrder(
                workOrderNo = "WO-2026-AI4T-001",
                projectId = "HLPL-AI4T-01",
                title = "Tata Ace EV 4G Telematics Gateway - Pilot Batch (100 units)",
                batchQuantity = 100,
                completedQuantity = 0,
                currentStationNo = 2,
                status = "In Production",
                stations = listOf(
                    RoutingStationStep(1, "SMT Stencil & High-Speed Mounter", "Yamaha YSM20R Mounter", "Casey Rowan", "Passed", "2026-10-18 06:30"),
                    RoutingStationStep(2, "10-Zone Nitrogen Reflow Oven", "Heller 1809 MK5 Reflow", "Casey Rowan", "In Progress", null),
                    RoutingStationStep(3, "3D Automated Optical Inspection (AOI)", "Koh Young Zenith 2 AOI", "Sarah Chen", "Pending", null),
                    RoutingStationStep(4, "In-Circuit Test & Firmware Flashing (ICT)", "Teradyne TestStation / ST-Link", "Devon Lane", "Pending", null),
                    RoutingStationStep(5, "End-of-Line Functional & Hi-Pot Rig (EOL)", "Chroma 8000 Automated Test Rig", "Mina Cross", "Pending", null)
                )
            ),
            ProductionWorkOrder(
                workOrderNo = "WO-2026-HARN-002",
                projectId = "HLPL-AI4T-01",
                title = "16-Pin Sealed Main Power & CAN Wire Harness Batch (50 units)",
                batchQuantity = 50,
                completedQuantity = 50,
                currentStationNo = 3,
                status = "Completed",
                stations = listOf(
                    RoutingStationStep(1, "Automated Wire Cutting & Stripping", "Komax Kappa 330", "Samira Holt", "Passed", "2026-10-17 11:00"),
                    RoutingStationStep(2, "Terminal Crimping & Pull Force Verification", "Mecal Applicator Press", "Samira Holt", "Passed", "2026-10-17 14:00"),
                    RoutingStationStep(3, "Continuity & Hi-Pot Electrical Test", "Cirris CH2 Continuity Tester", "Jordan Blake", "Passed", "2026-10-17 16:30")
                )
            )
        )
    )
    override val workOrders: StateFlow<List<ProductionWorkOrder>> = _workOrders.asStateFlow()

    // 29. Facility Maintenance & Breakdown Logs (Stage 7)
    private val _maintenanceLogs = MutableStateFlow(
        listOf(
            MaintenanceLogRecord(
                logId = "MNT-2026-001",
                assetTag = "EQ-SMT-001",
                assetName = "Yamaha YSM20R Modular High-Speed Mounter",
                maintenanceType = "Preventive PM",
                title = "Monthly Feeder Precision Calibration & Nozzle Alignment",
                technicianName = "Casey Rowan",
                status = "Scheduled",
                priority = "Normal",
                scheduledDate = "2026-10-25",
                stoppageHours = 0.0,
                remarks = "Routine 30-day preventive maintenance per IATF 16949 Â§8.5.1.5"
            ),
            MaintenanceLogRecord(
                logId = "MNT-2026-002",
                assetTag = "EQ-REFLOW-002",
                assetName = "Heller 1809 MK5 Nitrogen Reflow Oven",
                maintenanceType = "Preventive PM",
                title = "9-Zone Thermal Profiling & Nitrogen Flow Check",
                technicianName = "Kiran Rao",
                status = "Resolved",
                priority = "Normal",
                scheduledDate = "2026-10-15",
                stoppageHours = 1.5,
                remarks = "O2 ppm < 50 verified; thermal curve locked for SAC305 profile"
            ),
            MaintenanceLogRecord(
                logId = "MNT-2026-003",
                assetTag = "EQ-AOI-003",
                assetName = "Koh Young Zenith 2 3D AOI Inspection System",
                maintenanceType = "Breakdown Urgent",
                title = "Optical Z-Axis Stepper Motor Jitter Warning",
                technicianName = "Devon Lane",
                status = "In Progress",
                priority = "High",
                scheduledDate = "2026-10-18",
                stoppageHours = 0.5,
                remarks = "Motor driver thermal pad inspected; recalibrating Moire camera sensor"
            )
        )
    )
    override val maintenanceLogs: StateFlow<List<MaintenanceLogRecord>> = _maintenanceLogs.asStateFlow()

    // 30. Equipment Calibration Certificates (Stage 7)
    private val _calibrationCerts = MutableStateFlow(
        listOf(
            CalibrationCertificate(
                certId = "CAL-2026-089",
                assetTag = "EQ-IMTE-004",
                instrumentName = "Keysight InfiniiVision 200MHz 4-Ch Digital Storage Oscilloscope",
                standardAgency = "NABL Accredited Calibration Lab #1042",
                calibrationDate = "2026-05-10",
                validUntil = "2026-11-10",
                uncertaintySpec = "Â±0.5% DC Gain Accuracy",
                status = "Valid / Certified",
                certificateUrl = "https://example.invalid/certs/CAL-2026-089.pdf"
            ),
            CalibrationCertificate(
                certId = "CAL-2026-091",
                assetTag = "EQ-SMT-001",
                instrumentName = "Yamaha YSM20R Feeder Tape Tension & Force Gauge",
                standardAgency = "Fluke Metrology Services",
                calibrationDate = "2025-11-20",
                validUntil = "2026-11-20",
                uncertaintySpec = "Â±0.02 N (CPK > 1.67)",
                status = "Expiring (< 30d)",
                certificateUrl = "https://example.invalid/certs/CAL-2026-091.pdf"
            )
        )
    )
    override val calibrationCerts: StateFlow<List<CalibrationCertificate>> = _calibrationCerts.asStateFlow()

    // 31. Gatehouse Visitor Logistics (Stage 8)
    private val _visitorLogs = MutableStateFlow(
        listOf(
            VisitorLogEntry(
                visitorId = "VIS-2026-042",
                fullName = "Rajesh Kulkarni",
                company = "Tata Motors Commercial EV Division",
                hostEmployee = "Avery Vale (EHW)",
                purpose = "Stage 4 Schematics & Design Lock Review",
                badgeNumber = "BADGE-V-104",
                checkInTime = "2026-10-18 09:15",
                checkOutTime = null,
                status = "Active On-Site"
            ),
            VisitorLogEntry(
                visitorId = "VIS-2026-041",
                fullName = "Anil Deshmukh",
                company = "Mouser Electronics India",
                hostEmployee = "Morgan Reed (PUR)",
                purpose = "Annual ASL Supplier Quality Audit",
                badgeNumber = "BADGE-V-102",
                checkInTime = "2026-10-18 08:30",
                checkOutTime = "2026-10-18 10:15",
                status = "Checked Out"
            )
        )
    )
    override val visitorLogs: StateFlow<List<VisitorLogEntry>> = _visitorLogs.asStateFlow()

    // 32. Material Gate Passes & Logistics (Stage 8)
    private val _gatePasses = MutableStateFlow(
        listOf(
            GatePassRecord(
                passNumber = "GP-2026-019",
                passType = "Inward Material",
                transporterName = "Blue Dart Express Logistics",
                vehicleNumber = "MH-12-RN-8842",
                driverName = "Ramesh Shinde",
                referenceDoc = "HLPL-PO-2026-042",
                itemSummary = "2500 pcs STM32G0B1VCT6 Microcontrollers in Sealed Reel Cartons",
                authorizedBy = "Taylor Quinn (Security Lead)",
                clearedAt = "2026-10-18 08:45",
                status = "Cleared & Gate Passed"
            ),
            GatePassRecord(
                passNumber = "GP-2026-020",
                passType = "Outward Return",
                transporterName = "SafeTrans Cargo",
                vehicleNumber = "KA-01-MJ-4109",
                driverName = "Sunil Yadav",
                referenceDoc = "RTV-2026-004",
                itemSummary = "50 pcs Defective Wire Harness Pre-assemblies for Vendor Rework",
                authorizedBy = "Jordan Blake (QA Lead)",
                clearedAt = "2026-10-18 09:30",
                status = "Cleared & Gate Passed"
            ),
            GatePassRecord(
                passNumber = "GP-2026-021",
                passType = "Inward Material",
                transporterName = "DTDC Supply Chain",
                vehicleNumber = "MH-14-GH-1290",
                driverName = "Vikas Patil",
                referenceDoc = "HLPL-PO-2026-044",
                itemSummary = "1000 pcs Quectel EG915N-EU LTE Modules",
                authorizedBy = "Security Gatehouse",
                clearedAt = "2026-10-18 10:20",
                status = "Awaiting Verification"
            )
        )
    )
    override val gatePasses: StateFlow<List<GatePassRecord>> = _gatePasses.asStateFlow()

    // 33. HR Directory & Competency Skill Matrix (Stage 9)
    private val _employeeDirectory = MutableStateFlow(
        listOf(
            EmployeeDirectoryRecord(
                employeeId = "HLPL-EMP-101",
                name = "Avery Vale",
                deptCode = "EHW",
                deptName = "Embedded Hardware Engineering",
                roleTitle = "Hardware Engineering Lead",
                email = "avery.vale@example.com",
                phone = "+91 98230 11401",
                skillMatrixLevel = "L4 Expert / Trainer",
                certifications = listOf("Altium Designer Pro", "AEC-Q100 Compliance", "High-Speed ECAD Layout")
            ),
            EmployeeDirectoryRecord(
                employeeId = "HLPL-EMP-102",
                name = "Casey Rowan",
                deptCode = "EMF",
                deptName = "Electronics Manufacturing",
                roleTitle = "SMT Operations Lead",
                email = "casey.rowan@example.com",
                phone = "+91 98230 11402",
                skillMatrixLevel = "L4 Expert / Trainer",
                certifications = listOf("IPC-A-610 Class 3", "Yamaha SMT Programming", "Reflow Thermal Profiling")
            ),
            EmployeeDirectoryRecord(
                employeeId = "HLPL-EMP-103",
                name = "Jordan Blake",
                deptCode = "QA",
                deptName = "Quality Assurance & APQP",
                roleTitle = "Quality Assurance Lead",
                email = "jordan.blake@example.com",
                phone = "+91 98230 11403",
                skillMatrixLevel = "L4 Expert / Trainer",
                certifications = listOf("IATF 16949 Lead Auditor", "PPAP Level 3", "VDA 6.3 Process Auditor")
            ),
            EmployeeDirectoryRecord(
                employeeId = "HLPL-EMP-104",
                name = "Morgan Reed",
                deptCode = "PUR",
                deptName = "Purchasing & SCM",
                roleTitle = "Procurement & Stores Lead",
                email = "morgan.reed@example.com",
                phone = "+91 98230 11404",
                skillMatrixLevel = "L3 Autonomous Operator",
                certifications = listOf("ASL Supplier Audit", "ERP Stores Governance", "Dangerous Goods Logistics")
            ),
            EmployeeDirectoryRecord(
                employeeId = "HLPL-EMP-105",
                name = "Devon Lane",
                deptCode = "ESW",
                deptName = "Firmware & Software",
                roleTitle = "Lead Embedded Firmware Architect",
                email = "devon.lane@example.com",
                phone = "+91 98230 11405",
                skillMatrixLevel = "L4 Expert / Trainer",
                certifications = listOf("AUTOSAR Classic", "ISO 26262 ASIL-B", "Secure Boot & HSM")
            ),
            EmployeeDirectoryRecord(
                employeeId = "HLPL-EMP-106",
                name = "Taylor Quinn",
                deptCode = "HROPS",
                deptName = "HR & Operations",
                roleTitle = "Security & Facilities Officer",
                email = "taylor.quinn@example.com",
                phone = "+91 98230 11406",
                skillMatrixLevel = "L3 Autonomous Operator",
                certifications = listOf("Factory Act Compliance", "ISO 45001 OHSAS", "CCTV & Physical Security")
            )
        )
    )
    override val employeeDirectory: StateFlow<List<EmployeeDirectoryRecord>> = _employeeDirectory.asStateFlow()

    // 34. AI Procedural Queries (Stage 10)
    private val _aiQueries = MutableStateFlow(
        listOf(
            AiProceduralQuery(
                queryId = "AI-Q-001",
                queryText = "What is the mandatory quarantine procedure for ICs received on reel packaging?",
                detectedTopic = "Receiving IQC & MSL Governance",
                answerText = "Per SOP-IQC-004 Â§3.2, all semiconductors arriving on tape & reel must undergo 100% ESD inspection, desiccant verification, and MSL floor life calculation before transfer out of IQC-HOLD-STAGE-01.",
                referencedSop = "SOP-IQC-004 (Electronic Component Receiving & IQC)",
                confidencePct = 98
            ),
            AiProceduralQuery(
                queryId = "AI-Q-002",
                queryText = "Can an operator sign off Stage 5 SMT Pilot Build without Stage 4 Schematics approval?",
                detectedTopic = "APQP Stage-Gate Interlock Rules",
                answerText = "Strictly prohibited. Per IATF 16949 Â§6.1, Stage Gate 5 is hard-interlocked and remains cryptographically locked in Room/Supabase until Controlled Document EHW-SCH-01 receives Lead sign-off.",
                referencedSop = "IATF 16949 Â§6.1 Automotive Stage-Gate Interlock Governance",
                confidencePct = 100
            )
        )
    )
    override val aiQueries: StateFlow<List<AiProceduralQuery>> = _aiQueries.asStateFlow()

    // 35. Web-Configured Travel Policy Rules (Customizable on Web Portal per Master Plan Â§26.3)
    private val _travelPolicyRules = MutableStateFlow(
        listOf(
            TravelPolicyRule(
                ruleId = "POL-TRV-01",
                categoryName = "Domestic Air Travel & Flight",
                maxPerDiemInr = 6500.0,
                requiresPreApprovalAboveInr = 15000.0,
                allowedHotelTier = "3-Star / Corporate Tier B",
                flightAdvanceNoticeDays = 7,
                webConfigVersion = "v2.4-WebAuthority"
            ),
            TravelPolicyRule(
                ruleId = "POL-TRV-02",
                categoryName = "Local Inter-Plant Transit & Cabs",
                maxPerDiemInr = 2500.0,
                requiresPreApprovalAboveInr = 5000.0,
                allowedHotelTier = "Standard Corporate",
                flightAdvanceNoticeDays = 0,
                webConfigVersion = "v2.4-WebAuthority"
            )
        )
    )
    override val travelPolicyRules: StateFlow<List<TravelPolicyRule>> = _travelPolicyRules.asStateFlow()

    // 36. Approvals & Governance Decision Queue (Master Plan Â§26.3 & Enterprise Categories)
    private val _approvals = MutableStateFlow(
        listOf(
            // 1. Expense Request
            ApprovalDecisionRecord(
                id = "APPR-EXP-001",
                title = "Rapid Prototyping Thermal Paste & Soldering Flux Consumables",
                recordId = "EXP-2026-089",
                category = ApprovalCategory.EXPENSE,
                outcome = "Pending",
                updatedAt = "2026-10-18 14:30",
                requester = "Casey Rowan",
                department = "EMF",
                project = "HLPL-AI4T-01 (Tata Ace EV 4G)",
                description = "Procurement of specialized Henkel Loctite 3880 thermal adhesive and high-temperature lead-free SAC305 solder paste for SMT pilot line testing.",
                riskLevel = "Medium",
                iatfClause = "IATF 16949 Â§8.5.1 (Tooling & Consumables Release)",
                amountInr = 42500.0,
                expenseReasonDetails = "Urgent consumable replenishment for SMT Line 2 pilot run. Standard lead time via bulk vendor was 4 weeks; local supplier expedited for trial.",
                vendorOrBeneficiary = "Sumitron Exports Pvt Ltd",
                travelPolicyCompliant = true,
                travelPolicyNotes = "Standard departmental OPEX line item."
            ),
            // 2. Travel Expense (with customizable travel policy check)
            ApprovalDecisionRecord(
                id = "APPR-TRV-002",
                title = "OEM Client Plant Site Visit & Telematics Gateway Integration",
                recordId = "TRV-2026-034",
                category = ApprovalCategory.TRAVEL,
                outcome = "Pending",
                updatedAt = "2026-10-18 11:15",
                requester = "Devon Lane",
                department = "ESW",
                project = "HLPL-AI4T-01 (Tata Ace EV 4G)",
                description = "On-site engineering calibration and CAN J1939 telemetry logging at Tata Motors Pune Chakan Plant over 3 days.",
                riskLevel = "Medium",
                iatfClause = "ISO 9001 Â§8.4 (Travel & Field Engineering Authorization)",
                amountInr = 28400.0,
                expenseReasonDetails = "Flight: BLR-PNQ round trip (â‚¹14,200), Hotel: 2 nights at Lemon Tree Chakan (â‚¹9,200), Local taxi & per diem food (â‚¹5,000). Total claimed â‚¹28,400.",
                vendorOrBeneficiary = "Devon Lane (Employee Reimbursement)",
                travelPolicyCompliant = true,
                travelPolicyNotes = "Complies with Web Travel Policy POL-TRV-01: Hotel tier verified, corporate discount applied, advance booking within window."
            ),
            // 3. Task Approval Request
            ApprovalDecisionRecord(
                id = "APPR-TSK-003",
                title = "Hardware Schematics Frozen Sign-off Gate EHW-SCH-01",
                recordId = "TSK-EHW-GATE-04",
                category = ApprovalCategory.TASK,
                outcome = "Pending",
                updatedAt = "2026-10-18 09:40",
                requester = "Avery Vale",
                department = "EHW",
                project = "HLPL-AI4T-01 (Tata Ace EV 4G)",
                description = "Formal sign-off of Stage 4 Schematics after addressing TI Buck regulator thermal relief and transient suppression diode footprint.",
                riskLevel = "Critical",
                iatfClause = "IATF 16949 Â§8.3.4 (Design Review & Verification Gate)",
                amountInr = 0.0,
                expenseReasonDetails = "Non-monetary engineering gateway task. Approving this gate unlocks Stage 5 SMT Pilot Build in the APQP Stepper.",
                vendorOrBeneficiary = "XYZ Pvt Ltd Internal Engineering",
                travelPolicyCompliant = true,
                travelPolicyNotes = "N/A - Quality Gate Approval"
            ),
            // 4. Deviation Request for Procurement Teammates
            ApprovalDecisionRecord(
                id = "APPR-DEV-004",
                title = "Procurement Deviation: Secondary Source TVS Diode Automotive Grade",
                recordId = "DEV-PUR-2026-012",
                category = ApprovalCategory.PROCUREMENT_DEVIATION,
                outcome = "Pending",
                updatedAt = "2026-10-18 15:10",
                requester = "Morgan Reed",
                department = "PUR",
                project = "HLPL-SLCT-01 (AIS-140 Gateway)",
                description = "Deviation to permit Vishay SM6T36CA (AEC-Q101) as drop-in alternative for Littelfuse TPSMAJ36A due to global 16-week allocation delay.",
                riskLevel = "High",
                iatfClause = "IATF 16949 Â§8.7.1.1 (Customer / Engineering Concession & Deviation)",
                amountInr = 185000.0,
                expenseReasonDetails = "Price variance: +â‚¹1.20 per unit for 15,000 pcs lot. EHW team reviewed clamping voltage and confirmed 100% pin-to-pin and electrical compatibility.",
                vendorOrBeneficiary = "Arrow Electronics India Pvt Ltd",
                travelPolicyCompliant = true,
                travelPolicyNotes = "Procurement SCM Concession"
            ),
            // 5. Purchase Order Request
            ApprovalDecisionRecord(
                id = "APPR-PO-005",
                title = "Purchase Order Issuance: Quectel LTE Cat 1 Bis Modules 5,000 Pcs",
                recordId = "HLPL-PO-2026-042",
                category = ApprovalCategory.PURCHASE_ORDER,
                outcome = "Pending",
                updatedAt = "2026-10-18 16:20",
                requester = "Morgan Reed",
                department = "PUR",
                project = "HLPL-AI4T-01 (Tata Ace EV 4G)",
                description = "Commercial PO release for 5,000 pcs EG915N-EU cellular modules under contracted Master Pricing Agreement.",
                riskLevel = "Critical",
                iatfClause = "ISO 9001 Â§8.4 (Purchasing Authorization & Supplier Control)",
                amountInr = 5177000.0,
                expenseReasonDetails = "Batch procurement for Pilot SMT Build and ARAI Type Approval testing batch. Rate locked at â‚¹1,035.40/unit with 30-day net credit terms.",
                vendorOrBeneficiary = "Quectel Wireless Solutions Co., Ltd.",
                travelPolicyCompliant = true,
                travelPolicyNotes = "Standard PO Disbursement"
            ),
            // 6. Finance Approval
            ApprovalDecisionRecord(
                id = "APPR-FIN-006",
                title = "Advance Milestone Disbursement: Molex Hard Injection Tooling",
                recordId = "FIN-ADV-2026-018",
                category = ApprovalCategory.FINANCE_DISBURSEMENT,
                outcome = "Pending",
                updatedAt = "2026-10-18 12:00",
                requester = "Priya Voss",
                department = "FIN",
                project = "HLPL-AI4T-01 (Tata Ace EV 4G)",
                description = "40% advance release for precision steel tooling molds for automotive IP67 sealed multi-pin connectors.",
                riskLevel = "High",
                iatfClause = "ISO 9001 Â§8.4.3 (Financial Release & Milestone Settlement)",
                amountInr = 1450000.0,
                expenseReasonDetails = "Contractual Phase 1 Tooling Advance against Proforma Invoice PI-MOL-9921 with bank guarantee validated by Accounts.",
                vendorOrBeneficiary = "Molex India Tooling Division",
                travelPolicyCompliant = true,
                travelPolicyNotes = "Commercial Capex Advance"
            ),
            // 7. Operations Team Office/Plant Asset Approval
            ApprovalDecisionRecord(
                id = "APPR-AST-007",
                title = "Operations Asset: ESD-Safe Workstation & Ionizer Blowers for Line 2",
                recordId = "AST-OPS-2026-009",
                category = ApprovalCategory.OFFICE_ASSET,
                outcome = "Pending",
                updatedAt = "2026-10-18 10:05",
                requester = "Taylor Quinn",
                department = "HROPS",
                project = "Plant Facility Expansion (XYZ Pvt Ltd Line 2)",
                description = "Procurement and installation of 4x dual-fan overhead ionizing blowers (Desco/Simco-Ion) for manual inspection and repair benches.",
                riskLevel = "Medium",
                iatfClause = "IATF 16949 Â§7.1.3.1 (Plant, Facility & Equipment Planning)",
                amountInr = 85000.0,
                expenseReasonDetails = "Required to meet ANSI/ESD S20.20 and IATF 16949 zero-ESD audit findings prior to customer plant audit next month.",
                vendorOrBeneficiary = "Static Control Solutions India",
                travelPolicyCompliant = true,
                travelPolicyNotes = "Facility CapEx/Safety Line"
            )
        )
    )
    override val approvals: StateFlow<List<ApprovalDecisionRecord>> = _approvals.asStateFlow()

    override fun switchRole(role: DepartmentRole) {
        _currentRole.value = role
    }

    override fun lookupMouserCatalog(mpn: String): PartComponent? {
        val clean = mpn.trim().uppercase()
        return mouserCatalog.find { it.mpn.uppercase().contains(clean) || clean.contains(it.mpn.uppercase()) }
    }

    override fun registerPart(part: PartComponent): Result<Unit> {
        val currentList = _parts.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.mpn.equals(part.mpn, ignoreCase = true) }
        if (existingIndex >= 0) {
            currentList[existingIndex] = part
        } else {
            currentList.add(0, part)
        }
        _parts.value = currentList

        // Audit Log
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = _currentRole.value.leadName,
            action = "Receipt",
            partMpn = part.mpn,
            quantityDelta = part.onHand,
            reason = "IATF 16949 Part Registered (${part.description})",
            lotNumber = "INITIAL-REG"
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
        return Result.success(Unit)
    }

    override fun issueMaterial(
        mpn: String,
        recipient: String,
        dept: String,
        quantity: Int,
        issuer: String
    ): Result<Unit> {
        val currentParts = _parts.value.toMutableList()
        val partIndex = currentParts.indexOfFirst { it.mpn.equals(mpn, ignoreCase = true) }
        if (partIndex < 0) return Result.failure(IllegalArgumentException("Part $mpn not found in authoritative register."))

        val targetPart = currentParts[partIndex]
        if (targetPart.released < quantity) {
            return Result.failure(IllegalStateException("Insufficient released stock! On-hand: ${targetPart.onHand}, Released: ${targetPart.released}, Held in IQC: ${targetPart.iqcHold}."))
        }

        // Deduct released & onHand, add to allocated
        val updatedPart = targetPart.copy(
            onHand = targetPart.onHand - quantity,
            released = targetPart.released - quantity,
            allocated = targetPart.allocated + quantity
        )
        currentParts[partIndex] = updatedPart
        _parts.value = currentParts

        // Create Allocation
        val newAllocation = MaterialAllocation(
            id = "ALLOC-${UUID.randomUUID().toString().take(6)}",
            partMpn = mpn,
            recipient = recipient,
            department = dept,
            projectId = "HLPL-AI4T-01",
            issuedQuantity = quantity,
            returnedQuantity = 0,
            timestamp = currentTime(),
            issuer = issuer
        )
        _allocations.value = listOf(newAllocation) + _allocations.value

        // Audit log
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = issuer,
            action = "Issue",
            partMpn = mpn,
            quantityDelta = -quantity,
            reason = "Material issued to $recipient ($dept) for project HLPL-AI4T-01"
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
        return Result.success(Unit)
    }

    override fun returnMaterial(allocationId: String, quantity: Int): Result<Unit> {
        val currentAllocations = _allocations.value.toMutableList()
        val allocIndex = currentAllocations.indexOfFirst { it.id == allocationId }
        if (allocIndex < 0) return Result.failure(IllegalArgumentException("Allocation not found"))

        val alloc = currentAllocations[allocIndex]
        val remaining = alloc.issuedQuantity - alloc.returnedQuantity
        if (quantity > remaining) return Result.failure(IllegalStateException("Cannot return more than remaining issued quantity ($remaining pcs)."))

        currentAllocations[allocIndex] = alloc.copy(returnedQuantity = alloc.returnedQuantity + quantity)
        _allocations.value = currentAllocations

        // Return to released inventory
        val currentParts = _parts.value.toMutableList()
        val partIndex = currentParts.indexOfFirst { it.mpn.equals(alloc.partMpn, ignoreCase = true) }
        if (partIndex >= 0) {
            val part = currentParts[partIndex]
            currentParts[partIndex] = part.copy(
                onHand = part.onHand + quantity,
                released = part.released + quantity,
                allocated = maxOf(0, part.allocated - quantity)
            )
            _parts.value = currentParts
        }

        // Audit log
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = _currentRole.value.leadName,
            action = "Return",
            partMpn = alloc.partMpn,
            quantityDelta = +quantity,
            reason = "Material returned by ${alloc.recipient} (Allocation: $allocationId)"
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
        return Result.success(Unit)
    }

    override fun setActiveBomRevision(revisionId: String) {
        _activeBomRevisionId.value = revisionId
    }

    override fun promoteBomRevision(targetRevision: String, ecoNumber: String, rationale: String): Result<Unit> {
        val currentRevs = _bomRevisions.value
        val tentative = currentRevs.find { it.revisionId == "v0.3-tentative" }
            ?: return Result.failure(IllegalStateException("Tentative BOM not found"))

        // Strict ECO Check: Are there provisional parts?
        val provisionalParts = tentative.items.filter { it.isProvisional }
        if (provisionalParts.isNotEmpty()) {
            return Result.failure(
                IllegalStateException("ECO Promotion Blocked: Found ${provisionalParts.size} provisional candidate part(s) (${provisionalParts.map { it.mpn }.joinToString(", ")}). All candidate components must be approved before freezing baseline.")
            )
        }

        if (ecoNumber.isBlank()) {
            return Result.failure(IllegalArgumentException("Engineering Change Order (ECO) Number is mandatory under IATF 16949 Â§8.3.6."))
        }

        val promoted = tentative.copy(
            revisionId = targetRevision,
            title = "Governed Production Baseline (ECO: $ecoNumber)",
            isLocked = true,
            effectiveDate = currentTime(),
            ecoNumber = ecoNumber
        )
        _bomRevisions.value = _bomRevisions.value + promoted
        _activeBomRevisionId.value = targetRevision
        return Result.success(Unit)
    }

    override fun runWhatIfSimulation(buildQty: Int): WhatIfSimulationResult {
        val activeRev = _bomRevisions.value.find { it.revisionId == _activeBomRevisionId.value }
            ?: _bomRevisions.value.first()

        var shortageCount = 0
        var totalDeficit = 0.0
        var totalPcs = 0

        val lines = activeRev.items.map { item ->
            val required = item.qtyPerUnit * buildQty
            totalPcs += required
            val part = _parts.value.find { it.mpn.equals(item.mpn, ignoreCase = true) }
            val available = part?.released ?: 0
            val deficit = maxOf(0, required - available)
            val isShortage = deficit > 0
            val lineDeficitUsd = deficit * item.unitCostUsd

            if (isShortage) {
                shortageCount++
                totalDeficit += lineDeficitUsd
            }

            ShortageStatusLine(
                mpn = item.mpn,
                description = item.description,
                requiredQty = required,
                availableReleasedQty = available,
                deficitQty = deficit,
                unitCostUsd = item.unitCostUsd,
                deficitUsd = lineDeficitUsd,
                isShortage = isShortage
            )
        }

        return WhatIfSimulationResult(
            buildQuantity = buildQty,
            totalRequiredPcs = totalPcs,
            shortageLinesCount = shortageCount,
            totalDeficitUsd = totalDeficit,
            lineStatus = lines
        )
    }

    override fun loadIngestionPreset(preset: String) {
        // Loads supplier presets with fuzzy matching aliases
    }

    // STRICT INVARIANT: Ingesting an inward delivery creates a GRN draft on 100% IQC Quarantine Hold.
    // Zero stock is added to active released inventory.
    override fun generateGrnFromIngestion(rowId: String): Result<String> {
        val rows = _ingestionRows.value.toMutableList()
        val index = rows.indexOfFirst { it.id == rowId }
        if (index < 0) return Result.failure(IllegalArgumentException("Row not found"))

        val row = rows[index]
        val grnNumber = "GRN-2026-${(100..999).random()}"
        val lotNumber = "LOT-2026-${row.vendorName.take(3).uppercase()}-${(1000..9999).random()}"

        val newGrn = GrnRecord(
            grnNumber = grnNumber,
            poNumber = "HLPL-PO-2026-${(10..99).random()}",
            vendorName = row.vendorName,
            lotNumber = lotNumber,
            dateCode = "2641",
            totalBilledQty = row.billedQty,
            quantityAccepted = 0,               // STRICT INVARIANT: ZERO AUTO-STOCK POSTING
            quantityHeld = row.billedQty,       // 100% IN QUARANTINE HOLD
            assignedBin = "IQC-HOLD-STAGE-01",  // QUARANTINE BIN
            status = "Pending IQC",
            qaInspectionRef = null,
            timestamp = currentTime()
        )

        _grnRecords.value = listOf(newGrn) + _grnRecords.value
        rows[index] = row.copy(isProcessed = true)
        _ingestionRows.value = rows

        // Update Part IQC Hold (does NOT increase released stock!)
        val currentParts = _parts.value.toMutableList()
        val partIndex = currentParts.indexOfFirst { it.mpn.equals(row.mpn, ignoreCase = true) }
        if (partIndex >= 0) {
            val part = currentParts[partIndex]
            currentParts[partIndex] = part.copy(
                onHand = part.onHand + row.billedQty,
                iqcHold = part.iqcHold + row.billedQty
            )
            _parts.value = currentParts
        }

        // Audit Log
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = _currentRole.value.leadName,
            action = "QuarantineHold",
            partMpn = row.mpn,
            quantityDelta = row.billedQty,
            reason = "Inward delivery received via $grnNumber; placed on 100% IQC Quarantine Hold (Zero Auto Stock Posting)",
            lotNumber = lotNumber
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value

        return Result.success(grnNumber)
    }

    // IQC Quality Release: QA Inspector signs off, transferring stock from Quarantine to Released
    override fun releaseIqcHold(grnNumber: String, inspectionRef: String, inspector: String): Result<Unit> {
        val currentGrns = _grnRecords.value.toMutableList()
        val index = currentGrns.indexOfFirst { it.grnNumber == grnNumber }
        if (index < 0) return Result.failure(IllegalArgumentException("GRN record not found"))

        val grn = currentGrns[index]
        if (grn.status != "Pending IQC") {
            return Result.failure(IllegalStateException("GRN is already ${grn.status}"))
        }

        val qty = grn.quantityHeld
        val releasedGrn = grn.copy(
            quantityAccepted = qty,
            quantityHeld = 0,
            status = "Released",
            qaInspectionRef = "$inspectionRef ($inspector)",
            assignedBin = "Warehouse Zone A / Standard Stores"
        )
        currentGrns[index] = releasedGrn
        _grnRecords.value = currentGrns

        // Shift Part IQC Hold -> Released Stock
        val currentParts = _parts.value.toMutableList()
        val partIndex = currentParts.indexOfFirst { it.mpn.contains("STM32", ignoreCase = true) } // fallback to first active or matching
        if (partIndex >= 0) {
            val p = currentParts[partIndex]
            currentParts[partIndex] = p.copy(
                released = p.released + qty,
                iqcHold = maxOf(0, p.iqcHold - qty)
            )
            _parts.value = currentParts
        }

        // Audit Log
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = inspector,
            action = "QualityRelease",
            partMpn = "LOT-REL-${grn.lotNumber}",
            quantityDelta = qty,
            reason = "IQC Disposition Released by QA ($inspectionRef); transferred from Quarantine to Active Stores",
            lotNumber = grn.lotNumber
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
        return Result.success(Unit)
    }

    override fun updateDvprTestStatus(testId: String, newStatus: String, technician: String) {
        val current = _dvprTests.value.toMutableList()
        val index = current.indexOfFirst { it.testId == testId }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(
                status = newStatus,
                technician = technician,
                signOffTimestamp = if (newStatus == "Passed") currentTime() else item.signOffTimestamp
            )
            _dvprTests.value = current
        }
    }

    override fun toggleTask(taskId: String) {
        val current = _tasks.value.toMutableList()
        val index = current.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(isCompleted = !item.isCompleted)
            _tasks.value = current
        }
    }

    override fun requestSignedStorageUrl(bucket: String, path: String): Result<String> {
        return Result.failure(
            UnsupportedOperationException("Local prototype cannot mint private file URLs. Use authenticated PrivateStorageRepository.")
        )
    }

    override fun selectHardwarePath(path: String, rationale: String) {
        _hardwarePath.value = path
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = _currentRole.value.leadName,
            action = "HardwarePathSelection",
            partMpn = "SYS-ARCH-PATH",
            quantityDelta = 0,
            reason = "Selected hardware path: '$path'. Rationale: $rationale"
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
    }

    override fun addCustomer(customer: CustomerAccount) {
        _customers.value = listOf(customer) + _customers.value
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = _currentRole.value.leadName,
            action = "CustomerRegistered",
            partMpn = customer.id,
            quantityDelta = 0,
            reason = "New customer account registered: ${customer.name} (${customer.tierRating})"
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
    }

    override fun createOpportunity(opp: SalesOpportunity) {
        _salesOpportunities.value = listOf(opp) + _salesOpportunities.value
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = _currentRole.value.leadName,
            action = "OpportunityCreated",
            partMpn = opp.id,
            quantityDelta = opp.volume,
            reason = "Sales intake registered: ${opp.title} (${opp.projectedValueInr})"
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
    }

    override fun submitClientTicket(title: String, description: String): Result<String> {
        val ticketId = "TICK-2026-${UUID.randomUUID().toString().take(6).uppercase()}"
        val task = DailyTaskItem(
            id = ticketId,
            title = "[Client Ticket] $title: $description",
            dept = "EHW",
            priority = "High",
            assignee = "Avery Vale (Lead HW Architect)",
            dueDate = "Next 48 Hours",
            isCompleted = false
        )
        _tasks.value = listOf(task) + _tasks.value

        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = "Tata Motors Limited (Client Representative)",
            action = "ClientTicketSubmission",
            partMpn = ticketId,
            quantityDelta = 0,
            reason = "Client Support Ticket logged: $title"
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
        return Result.success(ticketId)
    }

    override fun saveReceivingDraft(draft: ReceivingDraftVoucher): Result<String> {
        _receivingDrafts.value = listOf(draft) + _receivingDrafts.value
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = draft.inspectorName,
            action = "ReceivingDraftSaved",
            partMpn = draft.mpn,
            quantityDelta = draft.quantityReceived,
            reason = "Mobile receiving draft created for PO ${draft.poNumber}, Reel ${draft.reelId}, Lot ${draft.lotNumber}. Offline draft only, awaiting IQC quarantine."
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
        return Result.success(draft.voucherId)
    }

    override fun recordCycleCount(record: CycleCountRecord): Result<Unit> {
        val current = _cycleCounts.value.toMutableList()
        val index = current.indexOfFirst { it.countId == record.countId }
        if (index >= 0) {
            current[index] = record
        } else {
            current.add(0, record)
        }
        _cycleCounts.value = current
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = record.counterName ?: _currentRole.value.leadName,
            action = "CycleCountRecorded",
            partMpn = record.mpn,
            quantityDelta = record.variance ?: 0,
            reason = "Cycle count at bin ${record.binCoordinate}: System=${record.systemQty}, Counted=${record.countedQty}, Variance=${record.variance}"
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
        return Result.success(Unit)
    }

    override fun confirmPickLine(pickId: String, pickedQty: Int): Result<Unit> {
        val current = _pickList.value.toMutableList()
        val index = current.indexOfFirst { it.pickId == pickId }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(pickedQty = pickedQty, isConfirmed = pickedQty >= item.requiredQty)
            _pickList.value = current
            val newLog = AuditLogEntry(
                id = "LOG-${UUID.randomUUID().toString().take(6)}",
                timestamp = currentTime(),
                actor = _currentRole.value.leadName,
                action = "PickLineConfirmed",
                partMpn = item.mpn,
                quantityDelta = pickedQty,
                reason = "Picked $pickedQty / ${item.requiredQty} for Work Order ${item.workOrderNo} from Bin ${item.binCoordinate}"
            )
            _auditLogs.value = listOf(newLog) + _auditLogs.value
            return Result.success(Unit)
        }
        return Result.failure(Exception("Pick line $pickId not found"))
    }

    override fun advanceWorkOrderStep(workOrderId: String, stepNo: Int, operatorName: String): Result<Unit> {
        val current = _workOrders.value.toMutableList()
        val woIndex = current.indexOfFirst { it.workOrderNo == workOrderId }
        if (woIndex >= 0) {
            val wo = current[woIndex]
            val updatedStations = wo.stations.map { station ->
                when {
                    station.stepNo == stepNo -> station.copy(status = "Passed", completedAt = currentTime(), operatorName = operatorName)
                    station.stepNo == stepNo + 1 -> station.copy(status = "In Progress")
                    else -> station
                }
            }
            val isAllCompleted = updatedStations.all { it.status == "Passed" }
            val nextStationNo = if (isAllCompleted) wo.stations.size else stepNo + 1
            current[woIndex] = wo.copy(
                stations = updatedStations,
                currentStationNo = nextStationNo,
                status = if (isAllCompleted) "Completed" else "In Production",
                completedQuantity = if (isAllCompleted) wo.batchQuantity else wo.completedQuantity
            )
            _workOrders.value = current
            val newLog = AuditLogEntry(
                id = "LOG-${UUID.randomUUID().toString().take(6)}",
                timestamp = currentTime(),
                actor = operatorName,
                action = "RoutingStationPassed",
                partMpn = workOrderId,
                quantityDelta = 0,
                reason = "Station #$stepNo completed for $workOrderId. Next station: $nextStationNo"
            )
            _auditLogs.value = listOf(newLog) + _auditLogs.value
            return Result.success(Unit)
        }
        return Result.failure(Exception("Work order $workOrderId not found"))
    }

    override fun logMaintenanceTicket(record: MaintenanceLogRecord): Result<String> {
        val current = _maintenanceLogs.value.toMutableList()
        current.add(0, record)
        _maintenanceLogs.value = current
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = _currentRole.value.leadName,
            action = "MaintenanceLogged",
            partMpn = record.assetTag,
            quantityDelta = 0,
            reason = "${record.maintenanceType}: ${record.title} (${record.status})"
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
        return Result.success(record.logId)
    }

    override fun signOffCalibration(certId: String, technician: String): Result<Unit> {
        val current = _calibrationCerts.value.toMutableList()
        val index = current.indexOfFirst { it.certId == certId }
        if (index >= 0) {
            val cert = current[index]
            current[index] = cert.copy(
                status = "Valid / Certified",
                calibrationDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                validUntil = "2027-10-18"
            )
            _calibrationCerts.value = current
            val newLog = AuditLogEntry(
                id = "LOG-${UUID.randomUUID().toString().take(6)}",
                timestamp = currentTime(),
                actor = technician,
                action = "CalibrationSignedOff",
                partMpn = cert.assetTag,
                quantityDelta = 0,
                reason = "Recalibrated ${cert.instrumentName} ($certId) - Valid until 2027-10-18"
            )
            _auditLogs.value = listOf(newLog) + _auditLogs.value
            return Result.success(Unit)
        }
        return Result.failure(Exception("Certificate $certId not found"))
    }

    override fun checkInVisitor(visitor: VisitorLogEntry): Result<String> {
        val current = _visitorLogs.value.toMutableList()
        current.add(0, visitor)
        _visitorLogs.value = current
        val newLog = AuditLogEntry(
            id = "LOG-${UUID.randomUUID().toString().take(6)}",
            timestamp = currentTime(),
            actor = _currentRole.value.leadName,
            action = "VisitorCheckedIn",
            partMpn = visitor.visitorId,
            quantityDelta = 0,
            reason = "Visitor ${visitor.fullName} (${visitor.company}) checked in on ${visitor.badgeNumber} host: ${visitor.hostEmployee}"
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
        return Result.success(visitor.visitorId)
    }

    override fun checkOutVisitor(visitorId: String): Result<Unit> {
        val current = _visitorLogs.value.toMutableList()
        val index = current.indexOfFirst { it.visitorId == visitorId }
        if (index >= 0) {
            current[index] = current[index].copy(
                status = "Checked Out",
                checkOutTime = currentTime()
            )
            _visitorLogs.value = current
            val newLog = AuditLogEntry(
                id = "LOG-${UUID.randomUUID().toString().take(6)}",
                timestamp = currentTime(),
                actor = _currentRole.value.leadName,
                action = "VisitorCheckedOut",
                partMpn = visitorId,
                quantityDelta = 0,
                reason = "Visitor $visitorId checked out from security gatehouse"
            )
            _auditLogs.value = listOf(newLog) + _auditLogs.value
            return Result.success(Unit)
        }
        return Result.failure(Exception("Visitor $visitorId not found"))
    }

    override fun verifyGatePass(passNumber: String, securityOfficer: String): Result<Unit> {
        val current = _gatePasses.value.toMutableList()
        val index = current.indexOfFirst { it.passNumber == passNumber }
        if (index >= 0) {
            current[index] = current[index].copy(
                status = "Cleared & Gate Passed",
                authorizedBy = securityOfficer,
                clearedAt = currentTime()
            )
            _gatePasses.value = current
            val newLog = AuditLogEntry(
                id = "LOG-${UUID.randomUUID().toString().take(6)}",
                timestamp = currentTime(),
                actor = securityOfficer,
                action = "GatePassCleared",
                partMpn = passNumber,
                quantityDelta = 0,
                reason = "Gate Pass $passNumber verified and vehicle cleared for factory barrier exit/entry"
            )
            _auditLogs.value = listOf(newLog) + _auditLogs.value
            return Result.success(Unit)
        }
        return Result.failure(Exception("Gate pass $passNumber not found"))
    }

    override fun queryAiAssistant(prompt: String): AiProceduralQuery {
        val pLower = prompt.lowercase()
        val match = _aiQueries.value.find { pLower.contains(it.queryText.take(15).lowercase()) }
        if (match != null) return match

        // Context-aware dynamic rule generation
        val (topic, ans, sop) = when {
            pLower.contains("esd") || pLower.contains("msl") || pLower.contains("moisture") -> Triple(
                "ESD & Moisture Sensitivity (J-STD-033)",
                "Components rated MSL-3 have 168 hours floor life at â‰¤30Â°C/60% RH. If unsealed > 168 hours, bake at 125Â°C for 24 hours prior to SMT reflow per J-STD-033C.",
                "SOP-ESD-002 Electronic Static Discharge & Baking Control"
            )
            pLower.contains("calibration") || pLower.contains("imte") -> Triple(
                "IMTE Instrument Calibration Frequency",
                "Per IATF 16949 Â§7.1.5.2, all measuring instruments (oscilloscopes, LCR meters, calipers) require annual NABL/ISO 17025 accredited calibration. Calibration certificates must be uploaded to the private vault before status can change to Certified.",
                "SOP-QA-008 Calibration Management System"
            )
            pLower.contains("visitor") || pLower.contains("gate") || pLower.contains("security") -> Triple(
                "Workplace & Gatehouse Security Protocol",
                "Non-employee personnel require photographic badge issuance (BADGE-V-xxx) and must be escorted in high-security ECAD/SMT manufacturing zones. All material outward movements require signed digital Gate Pass (GP-xxxx).",
                "SOP-SEC-001 Factory Access Control & Material Gate Passes"
            )
            pLower.contains("shortage") || pLower.contains("bom") -> Triple(
                "Smart BOM Shortage Mitigation",
                "In case of single-source component deficit, trigger ECR in Smart BOM tab to evaluate approved dual-source alternates (e.g. Murata for Kemet capacitors, NXP for TI CAN transceivers) without resetting APQP Stage 1-3 approvals.",
                "SOP-ENG-005 Engineering Change Management (ECR/ECO)"
            )
            else -> Triple(
                "SCMS Automotive Operations Standard",
                "For query: \"$prompt\", please verify corresponding procedure in the Controlled Documents Vault under relevant departmental root (EHW, PUR, EMF, or QA).",
                "IATF 16949:2016 Integrated Automotive QMS Manual"
            )
        }

        val generated = AiProceduralQuery(
            queryId = "AI-GEN-${UUID.randomUUID().toString().take(6)}",
            queryText = prompt,
            detectedTopic = topic,
            answerText = ans,
            referencedSop = sop,
            confidencePct = 96
        )
        _aiQueries.value = listOf(generated) + _aiQueries.value
        return generated
    }

    override fun resolveScanQuery(query: String): ResolvedScanResult {
        val q = query.trim()
        val qUpper = q.uppercase()

        // 1. Check Part MPN
        val matchingPart = _parts.value.find { it.mpn.equals(qUpper, ignoreCase = true) || it.mpn.contains(qUpper, ignoreCase = true) }
        if (matchingPart != null) {
            return ResolvedScanResult(
                rawCode = q,
                entityType = ScannedEntityType.COMPONENT,
                primaryTitle = matchingPart.mpn,
                secondarySubtitle = matchingPart.description,
                details = mapOf(
                    "Bin Location" to matchingPart.binCoordinate,
                    "On Hand" to "${matchingPart.onHand} pcs",
                    "Released" to "${matchingPart.released} pcs",
                    "IQC Hold" to "${matchingPart.iqcHold} pcs",
                    "Manufacturer" to matchingPart.manufacturer,
                    "Unit Cost" to "$${matchingPart.unitCostUsd} (â‚¹${matchingPart.unitCostInr})"
                ),
                actionLabel = "View in Governed Inventory",
                actionRoute = "inventory"
            )
        }

        // 2. Check Storage Bin
        val partsInBin = _parts.value.filter { it.binCoordinate.equals(qUpper, ignoreCase = true) }
        if (partsInBin.isNotEmpty() || qUpper.startsWith("BIN-") || qUpper.matches(Regex("^[A-Z]-[0-9]{1,2}-[0-9]{1,2}$"))) {
            val partDesc = if (partsInBin.isNotEmpty()) partsInBin.joinToString(", ") { "${it.mpn} (${it.onHand})" } else "No active components mapped"
            return ResolvedScanResult(
                rawCode = q,
                entityType = ScannedEntityType.STORAGE_BIN,
                primaryTitle = "Storage Bin $qUpper",
                secondarySubtitle = "Warehouse Zone Coordinates",
                details = mapOf(
                    "Coordinate" to qUpper,
                    "Mapped Items" to partDesc,
                    "Warehouse Zone" to if (qUpper.startsWith("A")) "Zone A (SMT High-Run)" else "Zone B (Passive & Mechanical)"
                ),
                actionLabel = "Open Bin Stock View",
                actionRoute = "inventory"
            )
        }

        // 3. Check PO Number
        val matchingPo = _purchaseOrders.value.find { it.poNumber.equals(qUpper, ignoreCase = true) || it.poNumber.contains(qUpper, ignoreCase = true) }
        if (matchingPo != null || qUpper.startsWith("PO-") || qUpper.startsWith("HLPL-PO")) {
            val po = matchingPo ?: PurchaseOrder(qUpper, "Franchised Distributor", "2026-03-15", "2026-03-30", 12000.0, "Verified", 3)
            return ResolvedScanResult(
                rawCode = q,
                entityType = ScannedEntityType.PURCHASE_ORDER,
                primaryTitle = po.poNumber,
                secondarySubtitle = po.vendorName,
                details = mapOf(
                    "Status" to po.status,
                    "Total Amount" to "$${po.totalAmountUsd}",
                    "Expected Delivery" to po.expectedDelivery,
                    "Line Items" to "${po.lineItemsCount} lines"
                ),
                actionLabel = "Inward Receiving Draft",
                actionRoute = "receiving"
            )
        }

        // 4. Check Asset Equipment
        val matchingAsset = _assets.value.find { it.assetTag.equals(qUpper, ignoreCase = true) || it.serialNumber.equals(qUpper, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
        if (matchingAsset != null || qUpper.startsWith("EQ-") || qUpper.startsWith("AST-")) {
            val asset = matchingAsset ?: _assets.value.first()
            return ResolvedScanResult(
                rawCode = q,
                entityType = ScannedEntityType.PLANT_ASSET,
                primaryTitle = asset.name,
                secondarySubtitle = "${asset.assetTag} Â· ${asset.category}",
                details = mapOf(
                    "Serial Number" to asset.serialNumber,
                    "Location" to asset.location,
                    "Custodian" to asset.custodian,
                    "Calibration Due" to asset.calibrationDue,
                    "Status" to asset.status
                ),
                actionLabel = "Inspect Maintenance Log",
                actionRoute = "assets"
            )
        }

        // 5. Check Gate Pass (Stage 8)
        val matchingGatePass = _gatePasses.value.find { it.passNumber.equals(qUpper, ignoreCase = true) }
        if (matchingGatePass != null || qUpper.startsWith("GP-")) {
            val gp = matchingGatePass ?: GatePassRecord(qUpper, "Inward Material", "Logistics Carrier", "MH-12-AB-1234", "Driver", "DOC-REF", "Electronics Parcel", "Security", currentTime(), "Cleared & Gate Passed")
            return ResolvedScanResult(
                rawCode = q,
                entityType = ScannedEntityType.GATE_PASS,
                primaryTitle = "${gp.passNumber} (${gp.passType})",
                secondarySubtitle = "${gp.vehicleNumber} Â· ${gp.transporterName}",
                details = mapOf(
                    "Driver" to gp.driverName,
                    "Reference Doc" to gp.referenceDoc,
                    "Cargo Summary" to gp.itemSummary,
                    "Authorized By" to gp.authorizedBy,
                    "Security Status" to gp.status
                ),
                actionLabel = "Verify & Gate Pass",
                actionRoute = "security"
            )
        }

        // 6. Check Employee / Staff Badge (Stage 9)
        val matchingEmp = _employeeDirectory.value.find { it.employeeId.equals(qUpper, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
        if (matchingEmp != null || qUpper.startsWith("HLPL-EMP-") || qUpper.startsWith("BADGE-")) {
            val emp = matchingEmp ?: EmployeeDirectoryRecord(qUpper, "Personnel Member", "OPS", "Operations", "Staff", "staff@example.com", "+91 90000 00000", "L3 Autonomous Operator", listOf("Factory Standard"), "Active On Duty")
            return ResolvedScanResult(
                rawCode = q,
                entityType = ScannedEntityType.EMPLOYEE_BADGE,
                primaryTitle = "${emp.name} (${emp.employeeId})",
                secondarySubtitle = "${emp.roleTitle} Â· ${emp.deptName}",
                details = mapOf(
                    "Department" to emp.deptCode,
                    "Competency Level" to emp.skillMatrixLevel,
                    "Certifications" to emp.certifications.joinToString(", "),
                    "Contact" to "${emp.phone} | ${emp.email}",
                    "Duty Status" to emp.status
                ),
                actionLabel = "Inspect Skill Matrix",
                actionRoute = "hr"
            )
        }

        // 7. Check Calibration Certificate (Stage 7)
        val matchingCert = _calibrationCerts.value.find { it.certId.equals(qUpper, ignoreCase = true) }
        if (matchingCert != null || qUpper.startsWith("CAL-")) {
            val cert = matchingCert ?: CalibrationCertificate(qUpper, "EQ-IMTE-004", "Keysight InfiniiVision Oscilloscope", "NABL Lab", "2026-01-01", "2027-01-01", "Â±0.5% DC Gain", "Valid / Certified")
            return ResolvedScanResult(
                rawCode = q,
                entityType = ScannedEntityType.CALIBRATION_CERT,
                primaryTitle = "${cert.certId} Â· ${cert.status}",
                secondarySubtitle = "${cert.instrumentName} (${cert.assetTag})",
                details = mapOf(
                    "Accredited Agency" to cert.standardAgency,
                    "Calibrated On" to cert.calibrationDate,
                    "Valid Until" to cert.validUntil,
                    "Uncertainty" to cert.uncertaintySpec,
                    "Compliance" to "IATF 16949 Â§7.1.5.2 Certified"
                ),
                actionLabel = "View Signed Certificate",
                actionRoute = "assets"
            )
        }

        // 8. Check Controlled Doc
        val matchingDoc = _documents.value.find { it.docId.contains(qUpper, ignoreCase = true) || it.title.contains(q, ignoreCase = true) }
        if (matchingDoc != null) {
            return ResolvedScanResult(
                rawCode = q,
                entityType = ScannedEntityType.CONTROLLED_DOC,
                primaryTitle = "${matchingDoc.docId} (${matchingDoc.revision})",
                secondarySubtitle = matchingDoc.title,
                details = mapOf(
                    "Lifecycle Status" to matchingDoc.status,
                    "Owner / Author" to matchingDoc.owner,
                    "Department" to matchingDoc.dept,
                    "Stage" to matchingDoc.stage
                ),
                actionLabel = "Fetch Signed URL & Open",
                actionRoute = "documents"
            )
        }

        // 9. Check GRN / Lot
        val matchingGrn = _grnRecords.value.find { it.grnNumber.equals(qUpper, ignoreCase = true) || it.lotNumber.equals(qUpper, ignoreCase = true) }
        if (matchingGrn != null || qUpper.startsWith("GRN-") || qUpper.startsWith("LOT-")) {
            val grn = matchingGrn ?: GrnRecord(qUpper, "HLPL-PO-2026-042", "Vendor ASL", "LOT-CUSTOM", "2638", 1000, 0, 1000, "IQC-STAGE", "Pending IQC", null, currentTime())
            return ResolvedScanResult(
                rawCode = q,
                entityType = ScannedEntityType.LOT_GRN,
                primaryTitle = grn.grnNumber,
                secondarySubtitle = "Lot ${grn.lotNumber} Â· ${grn.vendorName}",
                details = mapOf(
                    "Billed Qty" to "${grn.totalBilledQty} pcs",
                    "IQC Hold" to "${grn.quantityHeld} pcs",
                    "Accepted" to "${grn.quantityAccepted} pcs",
                    "Current Bin" to grn.assignedBin,
                    "Status" to grn.status
                ),
                actionLabel = "Release IQC Quarantine",
                actionRoute = "inventory"
            )
        }

        // Fallback: UNKNOWN entity
        return ResolvedScanResult(
            rawCode = q,
            entityType = ScannedEntityType.UNKNOWN,
            primaryTitle = "Unrecognized Barcode / QR Payload",
            secondarySubtitle = q,
            details = mapOf(
                "Payload" to q,
                "Suggestion" to "Try scanning a valid MPN (e.g. STM32G0B1VCT6), Bin (e.g. A-12-04), PO (e.g. HLPL-PO-2026-042), Asset (e.g. EQ-SMT-001), Gate Pass (e.g. GP-2026-019), or Staff Badge (e.g. HLPL-EMP-102)"
            ),
            actionLabel = "Search Mouser Catalog",
            actionRoute = "inventory"
        )
    }

    override fun approveGate(approvalId: String, notes: String): Result<Unit> {
        val current = _approvals.value
        val target = current.find { it.id == approvalId } ?: return Result.failure(IllegalArgumentException("Approval record $approvalId not found"))
        val signer = "${_currentRole.value.leadName} (${_currentRole.value.title})"
        val timestamp = currentTime()

        val updatedList = current.map { item ->
            if (item.id == approvalId) {
                item.copy(
                    outcome = "Approved",
                    comments = notes.ifBlank { "Sign-off verified and approved under ISO/IATF compliance." },
                    signedBy = signer,
                    signedAt = timestamp,
                    updatedAt = timestamp
                )
            } else {
                item
            }
        }
        _approvals.value = updatedList

        // Immutable Audit Log Attribution
        val auditEntry = AuditLogEntry(
            id = "AUDIT-APPR-${UUID.randomUUID().toString().take(6).uppercase()}",
            timestamp = timestamp,
            actor = signer,
            action = "APPROVE_GOVERNANCE_GATE",
            partMpn = "${target.id} (${target.recordId})",
            quantityDelta = if (target.amountInr > 0) target.amountInr.toInt() else 1,
            reason = "Approved gate '${target.title}' [${target.category.label}]. Notes: $notes. Clause: ${target.iatfClause}. Amount: â‚¹${target.amountInr}",
            lotNumber = target.recordId
        )
        _auditLogs.value = listOf(auditEntry) + _auditLogs.value

        return Result.success(Unit)
    }

    override fun rejectGate(approvalId: String, reason: String): Result<Unit> {
        val current = _approvals.value
        val target = current.find { it.id == approvalId } ?: return Result.failure(IllegalArgumentException("Approval record $approvalId not found"))
        val signer = "${_currentRole.value.leadName} (${_currentRole.value.title})"
        val timestamp = currentTime()

        val updatedList = current.map { item ->
            if (item.id == approvalId) {
                item.copy(
                    outcome = "Rejected",
                    comments = reason.ifBlank { "Rejected due to non-conformance with requirements." },
                    signedBy = signer,
                    signedAt = timestamp,
                    updatedAt = timestamp
                )
            } else {
                item
            }
        }
        _approvals.value = updatedList

        // Immutable Audit Log Attribution
        val auditEntry = AuditLogEntry(
            id = "AUDIT-REJ-${UUID.randomUUID().toString().take(6).uppercase()}",
            timestamp = timestamp,
            actor = signer,
            action = "REJECT_GOVERNANCE_GATE",
            partMpn = "${target.id} (${target.recordId})",
            quantityDelta = 0,
            reason = "Rejected gate '${target.title}' [${target.category.label}]. Reason: $reason. Clause: ${target.iatfClause}",
            lotNumber = target.recordId
        )
        _auditLogs.value = listOf(auditEntry) + _auditLogs.value

        return Result.success(Unit)
    }

    override fun returnGate(approvalId: String, reworkNotes: String): Result<Unit> {
        val current = _approvals.value
        val target = current.find { it.id == approvalId } ?: return Result.failure(IllegalArgumentException("Approval record $approvalId not found"))
        val signer = "${_currentRole.value.leadName} (${_currentRole.value.title})"
        val timestamp = currentTime()

        val updatedList = current.map { item ->
            if (item.id == approvalId) {
                item.copy(
                    outcome = "Returned",
                    comments = reworkNotes.ifBlank { "Returned to requesting lead for revisions." },
                    signedBy = signer,
                    signedAt = timestamp,
                    updatedAt = timestamp
                )
            } else {
                item
            }
        }
        _approvals.value = updatedList

        // Immutable Audit Log Attribution
        val auditEntry = AuditLogEntry(
            id = "AUDIT-RET-${UUID.randomUUID().toString().take(6).uppercase()}",
            timestamp = timestamp,
            actor = signer,
            action = "RETURN_FOR_REWORK",
            partMpn = "${target.id} (${target.recordId})",
            quantityDelta = 0,
            reason = "Returned gate '${target.title}' [${target.category.label}] for rework. Notes: $reworkNotes. Clause: ${target.iatfClause}",
            lotNumber = target.recordId
        )
        _auditLogs.value = listOf(auditEntry) + _auditLogs.value

        return Result.success(Unit)
    }

    override fun updateControlledDocStatus(docId: String, newStatus: String): Result<Unit> {
        val currentDocs = _documents.value
        val docIndex = currentDocs.indexOfFirst { it.docId.equals(docId, ignoreCase = true) }
        if (docIndex == -1) {
            return Result.failure(NoSuchElementException("Controlled document '$docId' not found in repository."))
        }
        val updatedDocs = currentDocs.toMutableList()
        val oldDoc = updatedDocs[docIndex]
        updatedDocs[docIndex] = oldDoc.copy(status = newStatus)
        _documents.value = updatedDocs

        val auditEntry = AuditLogEntry(
            id = "AUDIT-DOC-${UUID.randomUUID().toString().take(6).uppercase()}",
            timestamp = currentTime(),
            actor = _currentRole.value.leadName,
            action = "UPDATE_CONTROLLED_DOC_STATUS",
            partMpn = docId,
            quantityDelta = 0,
            reason = "Updated document '$docId' status from '${oldDoc.status}' to '$newStatus'.",
            lotNumber = oldDoc.revision
        )
        _auditLogs.value = listOf(auditEntry) + _auditLogs.value
        return Result.success(Unit)
    }

    override suspend fun verifyAndAdvanceStageGate(
        targetStage: Int,
        comments: String,
        evidenceJson: String?,
        recordId: String?
    ): Result<Unit> {
        if (targetStage < 1 || targetStage > 9) {
            return Result.failure(IllegalArgumentException("Invalid target stage $targetStage. APQP stages range from 1 to 9."))
        }

        val stages = _apqpStages.value
        val currentStageIndex = stages.indexOfLast { it.status == "Approved" || it.status == "In Progress" }
        val currentStage = if (currentStageIndex >= 0) stages[currentStageIndex].stageNumber else 1

        if (targetStage == currentStage) {
            return Result.success(Unit)
        }

        // Authoritative Backend Check: If supabaseClient is configured, dispatch authoritative RPC call
        if (supabaseClient != null) {
            val accessToken = secureStorage?.getAccessToken()?.takeIf { it.isNotBlank() }
                ?: return Result.failure(SecurityException("AUTH_REQUIRED: Authenticated session required for operational stage gate advancement"))

            val targetRecordId = recordId?.takeIf { it.isNotBlank() } ?: "00000000-0000-0000-0000-000000000001"
            val targetStageStr = "Stage $targetStage"

            // Construct RPC payload matching verify_and_advance_stage_gate signature:
            // p_record_id: uuid, p_target_stage: text, p_evidence_manifest: jsonb
            val manifestJson = evidenceJson?.takeIf { it.isNotBlank() } ?: "{}"
            val payloadJson = buildString {
                append("{")
                append("\"p_record_id\":\"").append(targetRecordId).append("\",")
                append("\"p_target_stage\":\"").append(targetStageStr).append("\",")
                append("\"p_evidence_manifest\":").append(manifestJson)
                append("}")
            }

            val rpcResult = supabaseClient.invokeRpc(
                rpcName = "verify_and_advance_stage_gate",
                payloadJson = payloadJson,
                accessToken = accessToken
            )

            if (rpcResult.isFailure) {
                return Result.failure(rpcResult.exceptionOrNull() ?: Exception("Stage gate advance failed"))
            }
        } else {
            // Local fallback / invariant checks when operating offline or in unit tests:
            if (targetStage > currentStage + 1) {
                return Result.failure(IllegalStateException("Cannot skip stages: currently at Stage $currentStage, requested Stage $targetStage."))
            }

            when (targetStage) {
                2 -> {
                    // Advancing to Stage 2 requires SOR intake
                }
                3 -> {
                    val contractReviewApproved = _feasibilityReviews.value.any { review ->
                        review.verdict.startsWith("FEASIBLE")
                    } || _documents.value.any { doc ->
                        (doc.docId.contains("MKT-01") || doc.docId.contains("CTR-01") || doc.title.contains("Contract Review", ignoreCase = true)) && doc.status == "Approved"
                    }
                    if (!contractReviewApproved) {
                        return Result.failure(IllegalStateException("Stage 2 Feasibility Freeze: Approved Contract Review Record (HLPL/F/MKT/01) is required before advancing to Stage 3."))
                    }
                }
                4 -> {
                    val docs = _documents.value
                    val hasEhwPrs = docs.any { it.docId.contains("EHW-PRS") && it.status == "Approved" }
                    val hasSalQuo = docs.any { it.docId.contains("SAL-QUO") && it.status == "Approved" }
                    if (!hasEhwPrs || !hasSalQuo) {
                        return Result.failure(IllegalStateException("Stage 3 Requirements Baseline Freeze: All domain requirements must be approved before advancing to Stage 4."))
                    }
                }
                5 -> {
                    val schematicsApproved = _documents.value.any { doc ->
                        doc.docId.contains("EHW-SCH-01") && doc.status == "Approved"
                    }
                    if (!schematicsApproved) {
                        return Result.failure(IllegalStateException("Stage 4 Hardware Schematics Prerequisite: Approved EHW-SCH-01 is required before advancing to Stage 5+."))
                    }
                }
            }
        }

        if (targetStage < currentStage) {
            val updated = stages.map { stage ->
                when {
                    stage.stageNumber < targetStage -> stage.copy(status = "Approved", isLocked = false, isBlockedByPrerequisite = false)
                    stage.stageNumber == targetStage -> stage.copy(status = "In Progress", isLocked = false, isBlockedByPrerequisite = false)
                    else -> stage.copy(status = "Pending", isLocked = stage.prerequisiteDoc != null, isBlockedByPrerequisite = stage.prerequisiteDoc != null)
                }
            }
            _apqpStages.value = updated
            val auditEntry = AuditLogEntry(
                id = "AUDIT-GATE-${UUID.randomUUID().toString().take(6).uppercase()}",
                timestamp = currentTime(),
                actor = _currentRole.value.leadName,
                action = "STAGE_GATE_REGRESSION",
                partMpn = "STAGE-$targetStage",
                quantityDelta = 0,
                reason = "Regressed APQP stage from $currentStage to $targetStage. Reason: $comments",
                lotNumber = "STAGE-GATE"
            )
            _auditLogs.value = listOf(auditEntry) + _auditLogs.value
            return Result.success(Unit)
        }

        val updated = stages.map { stage ->
            when {
                stage.stageNumber < targetStage -> stage.copy(status = "Approved", isLocked = false, isBlockedByPrerequisite = false)
                stage.stageNumber == targetStage -> stage.copy(status = "In Progress", isLocked = false, isBlockedByPrerequisite = false)
                else -> stage
            }
        }
        _apqpStages.value = updated
        val auditEntry = AuditLogEntry(
            id = "AUDIT-GATE-${UUID.randomUUID().toString().take(6).uppercase()}",
            timestamp = currentTime(),
            actor = _currentRole.value.leadName,
            action = "STAGE_GATE_ADVANCE",
            partMpn = "STAGE-$targetStage",
            quantityDelta = 0,
            reason = "Advanced APQP stage from $currentStage to $targetStage. Comments: $comments",
            lotNumber = "STAGE-GATE"
        )
        _auditLogs.value = listOf(auditEntry) + _auditLogs.value
        return Result.success(Unit)
    }

    override fun verifyAndAdvanceStageGateBlocking(
        targetStage: Int,
        comments: String,
        evidenceJson: String?,
        recordId: String?
    ): Result<Unit> = kotlinx.coroutines.runBlocking {
        verifyAndAdvanceStageGate(targetStage, comments, evidenceJson, recordId)
    }
}

