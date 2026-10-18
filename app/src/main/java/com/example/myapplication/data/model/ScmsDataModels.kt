package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

/**
 * Automotive Department and Persona Roles according to SCMS Master Plan §2 and §3.
 */
enum class DepartmentRole(
    val code: String,
    val title: String,
    val leadName: String,
    val isClient: Boolean = false
) {
    ADMIN("ADMIN", "Executive Admin / MD", "Executive Command Center"),
    SAL("SAL", "Sales & Business Development", "Priya Sharma"),
    EHW("EHW", "Embedded Hardware Engineering", "Avery Vale"),
    ESW("ESW", "Firmware & Software Engineering", "Devon Lane"),
    MECH("MECH", "Mechanical Engineering", "Samira Holt"),
    PUR("PUR", "Purchasing & SCM", "Morgan Reed"),
    QA("QA", "Quality Assurance & APQP", "Jordan Blake"),
    EMF("EMF", "Electronics Manufacturing", "Casey Rowan"),
    TST("TST", "EOL Testing & Jigs", "Mina Cross"),
    FIN("FIN", "Accounts & Finance", "Priya Voss"),
    SIT("SIT", "Service & IT Infrastructure", "Kai Morgan"),
    HROPS("HROPS", "HR & Operations", "Taylor Quinn"),
    CLIENT("CLIENT", "Client OEM Portal (Tata Motors)", "Rohan Verma", isClient = true)
}

/**
 * 9-Stage Automotive APQP / Stage-Gate Lifecycle Stepper per Master Plan §5.
 */
@Serializable
data class ApqpStage(
    val stageNumber: Int,
    val name: String,
    val description: String,
    val status: String, // "Approved", "In Progress", "Pending", "Locked"
    val isLocked: Boolean = false,
    val prerequisiteDoc: String? = null,
    val isBlockedByPrerequisite: Boolean = false
)

/**
 * Governed Inventory Component / Part Record per Master Plan §4 and SCMS KB Module 1.
 */
@Serializable
data class PartComponent(
    val mpn: String,
    val description: String,
    val manufacturer: String,
    val packageFootprint: String,
    val category: String,
    val subcategory: String = "",
    val onHand: Int,
    val released: Int,
    val iqcHold: Int,
    val allocated: Int,
    val storageLocation: String,
    val binCoordinate: String,
    val reorderBuffer: Int,
    val uom: String = "pcs",
    val unitCostUsd: Double,
    val unitCostInr: Double,
    // EHW Specs
    val voltage: String = "-",
    val current: String = "-",
    val aecRating: String = "-",
    val clockSpeed: String = "-",
    val tempRange: String = "-40°C to +105°C",
    // MECH Specs
    val dimensions: String = "-",
    val massGrams: Double = 0.0,
    val ipRating: String = "IP67",
    val wireGauge: String = "-",
    val matingForce: String = "-",
    // PUR Specs
    val leadTimeWeeks: Int = 4,
    val mouserStock: Int = 10000,
    val volumeCoveragePct: Int = 100,
    // Links & Compatibility
    val datasheetUrl: String = "https://www.mouser.in",
    val mouserUrl: String = "https://www.mouser.in",
    val compatibleMachines: List<String> = emptyList(),
    val isProvisional: Boolean = false
)

/**
 * Wire Harness Assembly (Tab 2 of Governed Inventory).
 */
@Serializable
data class WireHarness(
    val assemblyId: String,
    val name: String,
    val projectId: String,
    val wireGauge: String,
    val connectors: String,
    val pinCount: Int,
    val matingRetentionForce: String,
    val ipRating: String,
    val status: String
)

/**
 * Project-Team Material Issuance & Returns (Tab 3 of Governed Inventory).
 */
@Serializable
data class MaterialAllocation(
    val id: String,
    val partMpn: String,
    val recipient: String,
    val department: String,
    val projectId: String,
    val issuedQuantity: Int,
    val returnedQuantity: Int = 0,
    val timestamp: String,
    val issuer: String
)

/**
 * Goods Received Note & IQC Quarantine Hold Record (Tab 4 of Governed Inventory).
 */
@Serializable
data class GrnRecord(
    val grnNumber: String,
    val poNumber: String,
    val vendorName: String,
    val lotNumber: String,
    val dateCode: String,
    val totalBilledQty: Int,
    val quantityAccepted: Int, // Zero on arrival
    val quantityHeld: Int,     // 100% on arrival
    val assignedBin: String,   // e.g. "IQC-HOLD-STAGE-01"
    val status: String,        // "Pending IQC", "Released", "Quarantined"
    val qaInspectionRef: String? = null,
    val timestamp: String
)

/**
 * Governed Plant Asset & Machinery Fleet Record (Tab 5 of Governed Inventory).
 */
@Serializable
data class AssetEquipment(
    val assetTag: String,
    val serialNumber: String,
    val name: String,
    val category: String, // "SMT Line", "Wire Harness Tooling", "Testing Jig", "IMTE Instrument"
    val location: String,
    val custodian: String,
    val compatiblePartNumbers: List<String>,
    val lastCalibrationDate: String,
    val calibrationDue: String,
    val status: String // "Operational", "Calibration Due", "Maintenance"
)

/**
 * Stock Movement & Audit Log Entry (Tab 6 of Governed Inventory).
 */
@Serializable
data class AuditLogEntry(
    val id: String,
    val timestamp: String,
    val actor: String,
    val action: String, // "Receipt", "Issue", "Return", "Adjustment", "QuarantineHold", "QualityRelease"
    val partMpn: String,
    val quantityDelta: Int,
    val reason: String,
    val lotNumber: String? = null
)

/**
 * Dual-Lifecycle Smart BOM Item.
 */
@Serializable
data class SmartBomItem(
    val itemNo: Int,
    val mpn: String,
    val description: String,
    val refDes: String,
    val qtyPerUnit: Int,
    val unitCostUsd: Double,
    val leadTimeWeeks: Int,
    val supplier: String,
    val isProvisional: Boolean,
    val substituteMpn: String? = null,
    val aecRating: String = "AEC-Q100"
)

/**
 * Dual-Lifecycle Smart BOM Revision.
 */
@Serializable
data class BomRevision(
    val revisionId: String, // "v0.3-tentative" or "Rev A ECO-2026-004"
    val title: String,
    val isLocked: Boolean,
    val effectiveDate: String,
    val ecoNumber: String? = null,
    val items: List<SmartBomItem>
)

/**
 * Adaptive Ingestion Row (Staging Gate & 3-way reconciliation).
 */
@Serializable
data class IngestionRow(
    val id: String,
    val vendorName: String,
    val mpn: String,
    val orderedQty: Int,
    val billedQty: Int,
    val unitPrice: Double,
    val vendorStatus: String, // "Verified ASL", "Unregistered Candidate"
    val catalogStatus: String, // "Exact Match", "Substitute Match", "Unmatched Candidate"
    val priceVariancePct: Double,
    val qtyVariance: Int,
    val distributorPreset: String,
    val isProcessed: Boolean = false
)

/**
 * Quality Assurance DVP&R Pre-Compliance Certification Item per IATF 16949 §8.3.4.
 */
@Serializable
data class DvprTestItem(
    val testId: String,
    val standard: String,
    val testTitle: String,
    val condition: String,
    val acceptanceCriteria: String,
    val lab: String, // "Internal Lab (XYZ Pvt Ltd)", "Outsourced Lab (ARAI Pune)", "Outsourced Lab (ICAT Manesar)"
    val status: String, // "Passed", "In Progress", "Scheduled"
    val technician: String,
    val signOffTimestamp: String? = null
)

/**
 * Purchasing SCM R&D BOM Review Item.
 */
@Serializable
data class PurchasingBomReviewItem(
    val mpn: String,
    val description: String,
    val targetCost: Double,
    val quotedCost: Double,
    val costDelta: Double,
    val costSavingsPct: Double,
    val leadTimeWeeks: Int,
    val isLongLead: Boolean,
    val primarySupplier: String,
    val dualSourceAlternate: String
)

/**
 * Controlled Document Record per Formula: HLPL-[PROJ_ID]-[DEPT]-[DOC_TYPE]-[REV].
 */
@Serializable
data class ControlledDoc(
    val docId: String,
    val title: String,
    val dept: String,
    val stage: String,
    val owner: String,
    val revision: String,
    val status: String, // "Draft", "Under Review", "Approved", "Released"
    val confidentiality: String = "Confidential",
    val storageBucket: String = "controlled-documents",
    val storagePath: String = ""
)

/**
 * RACI Matrix Row.
 */
@Serializable
data class RaciRow(
    val stageNo: Int,
    val stageName: String,
    val responsible: String,
    val accountable: String,
    val consulted: String,
    val informed: String
)

/**
 * Daily Task & Action Item.
 */
@Serializable
data class DailyTaskItem(
    val id: String,
    val title: String,
    val dept: String,
    val priority: String, // "Critical", "High", "Normal", "Low"
    val assignee: String,
    val dueDate: String,
    val isCompleted: Boolean,
    val isOverdue: Boolean = false
)

// ==============================================================================
// Additional Master Plan Modules: Feasibility, Sales CRM, AVL, and Client Portal
// ==============================================================================

/**
 * 6 HOD Department Feasibility Review per Master Plan §4 & §5.
 */
@Serializable
data class FeasibilityReview(
    val deptCode: String,
    val deptName: String,
    val leadName: String,
    val verdict: String, // "FEASIBLE", "FEASIBLE_WITH_CONDITIONS", "INFEASIBLE"
    val scopeSummary: String,
    val internalRemarks: String,   // Confidential internal review
    val clientHighlights: String,  // Shared client-facing highlights
    val keyParameters: Map<String, String>,
    val signedAt: String,
    val signatureHash: String
)

/**
 * Customer Account in Customer Register per Master Plan §4 & §6.
 */
@Serializable
data class CustomerAccount(
    val id: String,
    val name: String,
    val division: String,
    val contactPerson: String,
    val email: String,
    val phone: String,
    val location: String,
    val activeProjects: List<String>,
    val ndaStatus: String, // "Active & Executed", "Pending Renewal"
    val tierRating: String = "Tier-1 OEM"
)

/**
 * Sales Opportunity & Enquiry per Master Plan §4.
 */
@Serializable
data class SalesOpportunity(
    val id: String,
    val title: String,
    val clientName: String,
    val volume: Int,
    val projectedValueInr: String,
    val status: String, // "Contract Locked", "Feasibility Review", "Enquiry Intake"
    val sorDocId: String,
    val isOpportunity: Boolean = true // true = Active Opportunity, false = Pending Enquiry
)

/**
 * Approved Vendor List (AVL) Record per Master Plan §5 & SCM.
 */
@Serializable
data class VendorRecord(
    val vendorCode: String,
    val name: String,
    val category: String,
    val country: String,
    val aslStatus: String, // "Verified ASL", "Under Evaluation", "Conditional"
    val isoCertification: String,
    val riskRating: String, // "Low Risk", "Medium Risk", "Critical Supplier"
    val paymentTerms: String
)

/**
 * Purchase Order Record for Procurement.
 */
@Serializable
data class PurchaseOrder(
    val poNumber: String,
    val vendorName: String,
    val issueDate: String,
    val expectedDelivery: String,
    val totalAmountUsd: Double,
    val status: String, // "Approved", "Partially Inwarded", "Pending Delivery"
    val lineItemsCount: Int
)

/**
 * Client OEM Portal Deliverable per Master Plan §3.
 */
@Serializable
data class ClientDeliverable(
    val id: String,
    val code: String,
    val title: String,
    val category: String,
    val standard: String,
    val status: String,
    val date: String,
    val owner: String,
    val description: String
)

/**
 * Stage 5B: Local Receiving Draft Inspection Voucher per Master Plan §3.3.
 */
@Serializable
data class ReceivingDraftVoucher(
    val voucherId: String,
    val poNumber: String,
    val vendorName: String,
    val mpn: String,
    val lotNumber: String,
    val batchCode: String,
    val dateCode: String,
    val reelId: String,
    val quantityReceived: Int,
    val mslRating: String, // "MSL-1", "MSL-3 (168 hrs)", "MSL-4"
    val esdCompliant: Boolean,
    val packageCondition: String, // "Intact / Verified", "Damaged Carton", "Seal Broken"
    val inspectorName: String,
    val timestamp: String,
    val status: String = "Local Draft (Awaiting IQC Inward)"
)

/**
 * Stage 5E: Cycle Count Inspection Record per Master Plan §3.3.
 */
@Serializable
data class CycleCountRecord(
    val countId: String,
    val mpn: String,
    val description: String,
    val binCoordinate: String,
    val systemQty: Int,
    val countedQty: Int? = null,
    val variance: Int? = null,
    val counterName: String? = null,
    val status: String = "Pending" // "Pending", "Matched", "Discrepancy"
)

/**
 * Stage 5E: Work Order Pick List Item per Master Plan §3.3.
 */
@Serializable
data class PickingItem(
    val pickId: String,
    val workOrderNo: String,
    val mpn: String,
    val requiredQty: Int,
    val pickedQty: Int,
    val binCoordinate: String,
    val isConfirmed: Boolean
)

/**
 * Stage 6: Production Traveler Work Order per Master Plan §3.4.
 */
@Serializable
data class ProductionWorkOrder(
    val workOrderNo: String,
    val projectId: String,
    val title: String,
    val batchQuantity: Int,
    val completedQuantity: Int,
    val currentStationNo: Int,
    val status: String, // "In Production", "QA Hold", "Completed"
    val stations: List<RoutingStationStep>
)

/**
 * Stage 6: Route Station Step in Electronic Traveler.
 */
@Serializable
data class RoutingStationStep(
    val stepNo: Int,
    val stepName: String,
    val machineOrJig: String,
    val operatorName: String,
    val status: String, // "Passed", "In Progress", "Pending", "Locked"
    val completedAt: String? = null
)

/**
 * Entity Types for Universal QR / Barcode Scanner.
 */
enum class ScannedEntityType {
    COMPONENT,
    STORAGE_BIN,
    PURCHASE_ORDER,
    PLANT_ASSET,
    CONTROLLED_DOC,
    LOT_GRN,
    GATE_PASS,
    EMPLOYEE_BADGE,
    CALIBRATION_CERT,
    UNKNOWN
}

/**
 * Resolved Entity payload for Universal Scanner.
 */
@Serializable
data class ResolvedScanResult(
    val rawCode: String,
    val entityType: ScannedEntityType,
    val primaryTitle: String,
    val secondarySubtitle: String,
    val details: Map<String, String>,
    val actionLabel: String,
    val actionRoute: String
)

/**
 * Stage 7: Facility Equipment Maintenance & Breakdown Ticket per Master Plan §3.5.
 */
@Serializable
data class MaintenanceLogRecord(
    val logId: String,
    val assetTag: String,
    val assetName: String,
    val maintenanceType: String, // "Preventive PM", "Breakdown Urgent", "Calibration Routine"
    val title: String,
    val technicianName: String,
    val status: String, // "Scheduled", "In Progress", "Resolved", "Awaiting Parts"
    val priority: String, // "Critical", "High", "Normal"
    val scheduledDate: String,
    val stoppageHours: Double = 0.0,
    val remarks: String
)

/**
 * Stage 7: Equipment Calibration Certificate & Validity per Master Plan §3.5.
 */
@Serializable
data class CalibrationCertificate(
    val certId: String,
    val assetTag: String,
    val instrumentName: String,
    val standardAgency: String,
    val calibrationDate: String,
    val validUntil: String,
    val uncertaintySpec: String,
    val status: String, // "Valid / Certified", "Expiring (< 30d)", "Expired"
    val certificateUrl: String = "https://example.invalid/certs"
)

/**
 * Stage 8: Gatehouse Visitor Log & Security Badge per Master Plan §3.6.
 */
@Serializable
data class VisitorLogEntry(
    val visitorId: String,
    val fullName: String,
    val company: String,
    val hostEmployee: String,
    val purpose: String,
    val badgeNumber: String,
    val checkInTime: String,
    val checkOutTime: String? = null,
    val status: String // "Active On-Site", "Checked Out"
)

/**
 * Stage 8: Material Gate Pass & Vehicle Logistics per Master Plan §3.6.
 */
@Serializable
data class GatePassRecord(
    val passNumber: String,
    val passType: String, // "Inward Material", "Outward Return", "Capital Equipment Transfer"
    val transporterName: String,
    val vehicleNumber: String,
    val driverName: String,
    val referenceDoc: String,
    val itemSummary: String,
    val authorizedBy: String,
    val clearedAt: String,
    val status: String // "Cleared & Gate Passed", "Inspection Hold", "Awaiting Verification"
)

/**
 * Stage 9: HR Directory & Operator Skill Matrix per Master Plan §3.7.
 */
@Serializable
data class EmployeeDirectoryRecord(
    val employeeId: String,
    val name: String,
    val deptCode: String,
    val deptName: String,
    val roleTitle: String,
    val email: String,
    val phone: String,
    val skillMatrixLevel: String, // "L4 Expert / Trainer", "L3 Autonomous Operator", "L2 Supervised", "L1 Trainee"
    val certifications: List<String>,
    val status: String = "Active On Duty"
)

/**
 * Stage 10: AI Procedural & Voice Assistant Query per Master Plan §3.10.
 */
@Serializable
data class AiProceduralQuery(
    val queryId: String,
    val queryText: String,
    val detectedTopic: String,
    val answerText: String,
    val referencedSop: String,
    val confidencePct: Int
)

/**
 * Functional Categories for Human Approvals Center per Master Plan §26.3 & Enterprise Ops.
 */
enum class ApprovalCategory(val label: String, val badgeColorHex: Long) {
    EXPENSE("Expense Request", 0xFFE65100),
    TRAVEL("Travel & Stay Expense", 0xFF00838F),
    TASK("Task Approval", 0xFF2E7D32),
    PROCUREMENT_DEVIATION("Procurement Deviation", 0xFFC2185B),
    PURCHASE_ORDER("Purchase Order (PO)", 0xFF1565C0),
    FINANCE_DISBURSEMENT("Finance & Advance", 0xFF6A1B9A),
    OFFICE_ASSET("Office & Plant Asset", 0xFF455A64)
}

/**
 * Enterprise Human Approval & Governance Gate Decision Record (Master Plan §26.3).
 * Captures detailed expense reasons, budget impact, procurement deviations, and IATF/ISO governance.
 */
@Serializable
data class ApprovalDecisionRecord(
    val id: String,
    val title: String,
    val recordId: String,
    val category: ApprovalCategory,
    val outcome: String, // "Pending", "Approved", "Rejected", "Returned"
    val updatedAt: String,
    val requester: String,
    val department: String,
    val project: String,
    val description: String,
    val riskLevel: String, // "Critical", "High", "Medium", "Low"
    val iatfClause: String,
    val amountInr: Double = 0.0,
    val expenseReasonDetails: String = "",
    val vendorOrBeneficiary: String = "",
    val comments: String? = null,
    val signedBy: String? = null,
    val signedAt: String? = null,
    // Customizable travel policy check indicators (policy rule configured on web portal)
    val travelPolicyCompliant: Boolean = true,
    val travelPolicyNotes: String = ""
)

/**
 * Web-Configured Travel & Expense Policy Rules for Verification in Mobile (Master Plan §26.3).
 */
@Serializable
data class TravelPolicyRule(
    val ruleId: String,
    val categoryName: String,
    val maxPerDiemInr: Double,
    val requiresPreApprovalAboveInr: Double,
    val allowedHotelTier: String,
    val flightAdvanceNoticeDays: Int,
    val webConfigVersion: String = "v2.4-WebAuthority"
)
