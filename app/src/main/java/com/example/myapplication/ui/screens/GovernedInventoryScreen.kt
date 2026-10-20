package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.AssetEquipment
import com.example.myapplication.data.model.AuditLogEntry
import com.example.myapplication.data.model.CalibrationCertificate
import com.example.myapplication.data.model.CycleCountRecord
import com.example.myapplication.data.model.EmployeeDirectoryRecord
import com.example.myapplication.data.model.GatePassRecord
import com.example.myapplication.data.model.GrnRecord
import com.example.myapplication.data.model.MaintenanceLogRecord
import com.example.myapplication.data.model.MaterialAllocation
import com.example.myapplication.data.model.PartComponent
import com.example.myapplication.data.model.PickingItem
import com.example.myapplication.data.model.ReceivingDraftVoucher
import com.example.myapplication.data.model.VisitorLogEntry
import com.example.myapplication.data.model.WireHarness
import com.example.myapplication.data.repository.ScmsRepository

enum class InventoryPreset(val label: String, val targetDept: String) {
    UNIVERSAL("Universal", "All"),
    EHW("EHW (Hardware)", "Embedded Hardware"),
    PUR("PUR (Purchasing)", "Purchasing & SCM"),
    MECH("MECH (Mechanical)", "Mechanical")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GovernedInventoryScreen(
    scmsRepository: ScmsRepository
) {
    val currentRole by scmsRepository.currentRole.collectAsState()
    val parts by scmsRepository.parts.collectAsState()
    val harnesses by scmsRepository.harnesses.collectAsState()
    val allocations by scmsRepository.allocations.collectAsState()
    val grnRecords by scmsRepository.grnRecords.collectAsState()
    val assets by scmsRepository.assets.collectAsState()
    val auditLogs by scmsRepository.auditLogs.collectAsState()
    val receivingDrafts by scmsRepository.receivingDrafts.collectAsState()
    val cycleCounts by scmsRepository.cycleCounts.collectAsState()
    val pickList by scmsRepository.pickList.collectAsState()
    val maintenanceLogs by scmsRepository.maintenanceLogs.collectAsState()
    val calibrationCerts by scmsRepository.calibrationCerts.collectAsState()
    val visitorLogs by scmsRepository.visitorLogs.collectAsState()
    val gatePasses by scmsRepository.gatePasses.collectAsState()
    val employeeDirectory by scmsRepository.employeeDirectory.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var activePreset by remember { mutableStateOf(InventoryPreset.UNIVERSAL) }

    var selectedPartForDetail by remember { mutableStateOf<PartComponent?>(null) }
    var showRegisterModal by remember { mutableStateOf(false) }
    var showIssueModal by remember { mutableStateOf(false) }
    var showReceivingModal by remember { mutableStateOf(false) }
    var showMaintenanceModal by remember { mutableStateOf(false) }
    var showVisitorModal by remember { mutableStateOf(false) }

    val tabs = listOf(
        "1. Parts Register",
        "2. Wire Harnesses",
        "3. Allocations",
        "4. GRN & IQC Hold",
        "5. Cycle & Pick",
        "6. Receiving Drafts",
        "7. Plant & Maintenance",
        "8. Security & Gatehouse",
        "9. Staff & Skill Matrix",
        "10. Audit Ledger"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Governed Inventory",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "IATF 16949 • HLPL-AI4T-01",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { showReceivingModal = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Receive", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                    FilledTonalButton(
                        onClick = { showRegisterModal = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Register", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { showIssueModal = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Outbox, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Issue", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Governed Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Content Area per Tab
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0 -> PartsRegisterTab(
                    parts = parts,
                    preset = activePreset,
                    onPresetChange = { activePreset = it },
                    onPartClick = { selectedPartForDetail = it }
                )
                1 -> WireHarnessesTab(harnesses = harnesses)
                2 -> AllocationsTab(
                    allocations = allocations,
                    onReturn = { alloc, qty -> scmsRepository.returnMaterial(alloc.id, qty) }
                )
                3 -> GrnIqcHoldTab(
                    grnRecords = grnRecords,
                    onRelease = { grn ->
                        scmsRepository.releaseIqcHold(
                            grn.grnNumber,
                            "QA-IR-2026-088",
                            currentRole.leadName
                        )
                    }
                )
                4 -> CycleCountAndPickingTab(
                    cycleCounts = cycleCounts,
                    pickList = pickList,
                    onRecordCount = { record -> scmsRepository.recordCycleCount(record) },
                    onConfirmPick = { pickId, qty -> scmsRepository.confirmPickLine(pickId, qty) }
                )
                5 -> ReceivingDraftsTab(
                    receivingDrafts = receivingDrafts,
                    onOpenNewDraft = { showReceivingModal = true }
                )
                6 -> PlantAndMaintenanceTab(
                    assets = assets,
                    maintenanceLogs = maintenanceLogs,
                    calibrationCerts = calibrationCerts,
                    onOpenLogTicket = { showMaintenanceModal = true },
                    onSignOffCalibration = { certId ->
                        scmsRepository.signOffCalibration(certId, currentRole.leadName)
                    }
                )
                7 -> SecurityAndGatehouseTab(
                    gatePasses = gatePasses,
                    visitorLogs = visitorLogs,
                    onOpenVisitorModal = { showVisitorModal = true },
                    onCheckOutVisitor = { visitorId ->
                        scmsRepository.checkOutVisitor(visitorId)
                    },
                    onVerifyGatePass = { passNumber ->
                        scmsRepository.verifyGatePass(passNumber, currentRole.leadName)
                    }
                )
                8 -> StaffAndSkillMatrixTab(
                    directory = employeeDirectory
                )
                9 -> AuditLogTab(auditLogs = auditLogs)
            }
        }
    }

    // 5-Tab Deep Part Inspection Modal
    selectedPartForDetail?.let { part ->
        DeepPartInspectionSheet(
            part = part,
            onDismiss = { selectedPartForDetail = null }
        )
    }

    // Register Part with Mouser Auto-Fill Dialog
    if (showRegisterModal) {
        RegisterPartModal(
            scmsRepository = scmsRepository,
            onDismiss = { showRegisterModal = false }
        )
    }

    // Issue Material Dialog
    if (showIssueModal) {
        IssueMaterialModal(
            parts = parts,
            scmsRepository = scmsRepository,
            issuer = currentRole.leadName,
            onDismiss = { showIssueModal = false }
        )
    }

    // Stage 5B Receiving Draft Modal
    if (showReceivingModal) {
        ReceivingDraftModal(
            scmsRepository = scmsRepository,
            inspectorName = currentRole.leadName,
            onDismiss = { showReceivingModal = false }
        )
    }

    // Stage 7 Plant Maintenance Ticket Modal
    if (showMaintenanceModal) {
        LogMaintenanceModal(
            assets = assets,
            technicianName = currentRole.leadName,
            onDismiss = { showMaintenanceModal = false },
            onSubmit = { record ->
                scmsRepository.logMaintenanceTicket(record)
                showMaintenanceModal = false
            }
        )
    }

    // Stage 8 Security Visitor Check-In Modal
    if (showVisitorModal) {
        CheckInVisitorModal(
            hostName = currentRole.leadName,
            onDismiss = { showVisitorModal = false },
            onSubmit = { visitor ->
                scmsRepository.checkInVisitor(visitor)
                showVisitorModal = false
            }
        )
    }
}

@Composable
fun PartsRegisterTab(
    parts: List<PartComponent>,
    preset: InventoryPreset,
    onPresetChange: (InventoryPreset) -> Unit,
    onPartClick: (PartComponent) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredParts = remember(parts, searchQuery) {
        if (searchQuery.isBlank()) parts
        else {
            val q = searchQuery.trim().lowercase()
            parts.filter { part ->
                part.mpn.lowercase().contains(q) ||
                part.description.lowercase().contains(q) ||
                part.manufacturer.lowercase().contains(q) ||
                part.category.lowercase().contains(q) ||
                part.storageLocation.lowercase().contains(q) ||
                part.binCoordinate.lowercase().contains(q) ||
                part.packageFootprint.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                placeholder = {
                    Text("Search MPN, description, manufacturer, bin...", style = MaterialTheme.typography.bodyMedium)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search Parts")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text(
                        text = "Preset:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(InventoryPreset.entries.toTypedArray()) { p ->
                    FilterChip(
                        selected = preset == p,
                        onClick = { onPresetChange(p) },
                        label = { Text(p.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredParts.size} parts found",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "Clear search",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { searchQuery = "" }
                    )
                }
            }
        }
        if (filteredParts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No components found",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No parts match '$searchQuery' in ${preset.label}" else "No parts registered in this category",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredParts) { part ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPartClick(part) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = part.mpn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${part.category} / ${part.packageFootprint}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Text(
                        text = part.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset-Adaptive Column Information
                    when (preset) {
                        InventoryPreset.UNIVERSAL -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "On-Hand: ${part.onHand} | Released: ${part.released}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Bin: ${part.binCoordinate} (${part.storageLocation})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        InventoryPreset.EHW -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "⚡ ${part.voltage} | ${part.current}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1565C0)
                                )
                                Text(
                                    text = "AEC: ${part.aecRating}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                        InventoryPreset.PUR -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Cost: $${part.unitCostUsd} (₹${part.unitCostInr}) | LT: ${part.leadTimeWeeks}w",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    text = "Mouser Stock: ${part.mouserStock}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        InventoryPreset.MECH -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Dim: ${part.dimensions} | Mass: ${part.massGrams}g",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "IP: ${part.ipRating} | Force: ${part.matingForce}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun WireHarnessesTab(harnesses: List<WireHarness>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(harnesses) { harness ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = harness.assemblyId,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = harness.status,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = harness.name,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wire: ${harness.wireGauge} | Pin count: ${harness.pinCount} | IP: ${harness.ipRating}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Connectors: ${harness.connectors} (Retention: ${harness.matingRetentionForce})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AllocationsTab(
    allocations: List<MaterialAllocation>,
    onReturn: (MaterialAllocation, Int) -> Unit
) {
    var returnAllocationTarget by remember { mutableStateOf<MaterialAllocation?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allocations) { alloc ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = alloc.partMpn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Issued: ${alloc.issuedQuantity} pcs (Ret: ${alloc.returnedQuantity})",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Recipient: ${alloc.recipient} (${alloc.department})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Time: ${alloc.timestamp} | Issuer: ${alloc.issuer} | Project: ${alloc.projectId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (alloc.issuedQuantity > alloc.returnedQuantity) {
                        Button(
                            onClick = { onReturn(alloc, alloc.issuedQuantity - alloc.returnedQuantity) },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text("Full Return (${alloc.issuedQuantity - alloc.returnedQuantity} pcs)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GrnIqcHoldTab(
    grnRecords: List<GrnRecord>,
    onRelease: (GrnRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IATF 16949 Invariant: Inward stock enters 100% IQC Quarantine Hold (IQC-HOLD-STAGE-01) with Qty Accepted = 0 until QA release.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }

        items(grnRecords) { grn ->
            val isPending = grn.status == "Pending IQC"
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isPending) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${grn.grnNumber} (PO: ${grn.poNumber})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = if (isPending) Color(0xFFFFE082) else Color(0xFFC8E6C9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = grn.status,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPending) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                        }
                    }
                    Text(
                        text = "Vendor: ${grn.vendorName} | Lot: ${grn.lotNumber} | DateCode: ${grn.dateCode}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Billed: ${grn.totalBilledQty} | Accepted: ${grn.quantityAccepted} | Held in IQC: ${grn.quantityHeld}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Bin: ${grn.assignedBin} | Time: ${grn.timestamp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    grn.qaInspectionRef?.let { ref ->
                        Text(
                            text = "QA Release Ref: $ref",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isPending) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { onRelease(grn) },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QA Release Disposition (QA-IR-2026-088)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlantAndMaintenanceTab(
    assets: List<AssetEquipment>,
    maintenanceLogs: List<MaintenanceLogRecord>,
    calibrationCerts: List<CalibrationCertificate>,
    onOpenLogTicket: () -> Unit,
    onSignOffCalibration: (String) -> Unit
) {
    var subSection by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Equipment Fleet (${assets.size})", "Maintenance PM (${maintenanceLogs.size})", "Calibration Certs (${calibrationCerts.size})")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
        TabRow(
            selectedTabIndex = subSection,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = subSection == idx,
                    onClick = { subSection = idx },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (subSection == idx) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (subSection) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assets) { asset ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "${asset.name} (${asset.assetTag})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        color = if (asset.status == "Operational") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = asset.status,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (asset.status == "Operational") Color(0xFF2E7D32) else Color(0xFFE65100),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Category: ${asset.category} | Serial: ${asset.serialNumber}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Location: ${asset.location} | Custodian: ${asset.custodian}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Compatible Parts: ${asset.compatiblePartNumbers.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Last Cal: ${asset.lastCalibrationDate} | Next Due: ${asset.calibrationDue}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IATF 16949 §8.5.1 Plant Maintenance Log",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalButton(
                            onClick = onOpenLogTicket,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Ticket", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(maintenanceLogs) { log ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${log.logId}: ${log.title}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            color = when (log.priority) {
                                                "Critical" -> Color(0xFFFFEBEE)
                                                "High" -> Color(0xFFFFF3E0)
                                                else -> Color(0xFFE3F2FD)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = log.priority,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = when (log.priority) {
                                                    "Critical" -> Color(0xFFC62828)
                                                    "High" -> Color(0xFFE65100)
                                                    else -> Color(0xFF1565C0)
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Asset: ${log.assetName} (${log.assetTag}) | Type: ${log.maintenanceType}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Technician: ${log.technicianName} | Scheduled: ${log.scheduledDate} | Stoppage: ${log.stoppageHours} hrs",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Remarks: ${log.remarks}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = when (log.status) {
                                            "Resolved" -> Color(0xFFE8F5E9)
                                            "In Progress" -> Color(0xFFFFF8E1)
                                            else -> Color(0xFFECEFF1)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Status: ${log.status}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (log.status) {
                                                "Resolved" -> Color(0xFF2E7D32)
                                                "In Progress" -> Color(0xFFF57F17)
                                                else -> Color(0xFF37474F)
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(calibrationCerts) { cert ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${cert.certId}: ${cert.instrumentName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = when (cert.status) {
                                            "Valid / Certified" -> Color(0xFFE8F5E9)
                                            "Expiring (< 30d)" -> Color(0xFFFFF3E0)
                                            else -> Color(0xFFFFEBEE)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = cert.status,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (cert.status) {
                                                "Valid / Certified" -> Color(0xFF2E7D32)
                                                "Expiring (< 30d)" -> Color(0xFFE65100)
                                                else -> Color(0xFFC62828)
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Asset Tag: ${cert.assetTag} | Agency: ${cert.standardAgency}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Calibrated: ${cert.calibrationDate} | Valid Until: ${cert.validUntil}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Uncertainty: ${cert.uncertaintySpec}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                if (cert.status != "Valid / Certified") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { onSignOffCalibration(cert.certId) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sign-Off Calibration (+12 Mo)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityAndGatehouseTab(
    gatePasses: List<GatePassRecord>,
    visitorLogs: List<VisitorLogEntry>,
    onOpenVisitorModal: () -> Unit,
    onCheckOutVisitor: (String) -> Unit,
    onVerifyGatePass: (String) -> Unit
) {
    var subSection by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Gate Passes (${gatePasses.size})", "Visitor Register (${visitorLogs.size})")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
        TabRow(
            selectedTabIndex = subSection,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = subSection == idx,
                    onClick = { subSection = idx },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (subSection == idx) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (subSection) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gatePasses) { pass ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = pass.passNumber,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        color = when (pass.status) {
                                            "Cleared & Gate Passed" -> Color(0xFFE8F5E9)
                                            "Inspection Hold" -> Color(0xFFFFEBEE)
                                            else -> Color(0xFFE3F2FD)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = pass.status,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (pass.status) {
                                                "Cleared & Gate Passed" -> Color(0xFF2E7D32)
                                                "Inspection Hold" -> Color(0xFFC62828)
                                                else -> Color(0xFF1565C0)
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Type: ${pass.passType} | Vehicle: ${pass.vehicleNumber} (${pass.transporterName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Driver: ${pass.driverName} | Ref Doc: ${pass.referenceDoc}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Items: ${pass.itemSummary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Authorized By: ${pass.authorizedBy} | Time: ${pass.clearedAt}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (pass.status != "Cleared & Gate Passed") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { onVerifyGatePass(pass.passNumber) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Verify & Clear Gate Pass", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Security Gatehouse Visitor Register",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalButton(
                            onClick = onOpenVisitorModal,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check-In", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visitorLogs) { entry ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = entry.fullName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Surface(
                                            color = if (entry.status == "Active On-Site") Color(0xFFE8F5E9) else Color(0xFFECEFF1),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = entry.status,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (entry.status == "Active On-Site") Color(0xFF2E7D32) else Color(0xFF455A64),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Company: ${entry.company} | Badge: ${entry.badgeNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Host: ${entry.hostEmployee} | Purpose: ${entry.purpose}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "In: ${entry.checkInTime}${entry.checkOutTime?.let { " | Out: $it" } ?: ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (entry.status == "Active On-Site") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = { onCheckOutVisitor(entry.visitorId) },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp).align(Alignment.End)
                                        ) {
                                            Text("Check Out Visitor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffAndSkillMatrixTab(
    directory: List<EmployeeDirectoryRecord>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("All") }

    val departments = listOf("All", "Embedded HW", "Purchasing & SCM", "Quality Assurance", "Stores & Logistics", "SMT Operations")

    val filteredList = directory.filter { emp ->
        val matchesDept = selectedDept == "All" || emp.deptName.contains(selectedDept, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                emp.name.contains(searchQuery, ignoreCase = true) ||
                emp.employeeId.contains(searchQuery, ignoreCase = true) ||
                emp.certifications.any { it.contains(searchQuery, ignoreCase = true) }
        matchesDept && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search staff name, ID, or certification...", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(departments) { dept ->
                FilterChip(
                    selected = selectedDept == dept,
                    onClick = { selectedDept = dept },
                    label = { Text(dept, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList) { emp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        text = emp.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${emp.employeeId} • ${emp.deptName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = when {
                                    emp.skillMatrixLevel.startsWith("L4") -> Color(0xFFEDE7F6)
                                    emp.skillMatrixLevel.startsWith("L3") -> Color(0xFFE8F5E9)
                                    emp.skillMatrixLevel.startsWith("L2") -> Color(0xFFE3F2FD)
                                    else -> Color(0xFFFFF3E0)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = emp.skillMatrixLevel,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        emp.skillMatrixLevel.startsWith("L4") -> Color(0xFF512DA8)
                                        emp.skillMatrixLevel.startsWith("L3") -> Color(0xFF2E7D32)
                                        emp.skillMatrixLevel.startsWith("L2") -> Color(0xFF1565C0)
                                        else -> Color(0xFFE65100)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Role: ${emp.roleTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Contact: ${emp.email} | ${emp.phone}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "IATF 16949 Competency Certifications:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(emp.certifications) { cert ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = cert,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMaintenanceModal(
    assets: List<AssetEquipment>,
    technicianName: String,
    onDismiss: () -> Unit,
    onSubmit: (MaintenanceLogRecord) -> Unit
) {
    var selectedAssetTag by remember { mutableStateOf(assets.firstOrNull()?.assetTag ?: "EQ-SMT-001") }
    var selectedAssetName by remember { mutableStateOf(assets.firstOrNull()?.name ?: "Yamaha YSM20R Pick & Place") }
    var title by remember { mutableStateOf("Routine PM & Nozzle Calibration") }
    var maintenanceType by remember { mutableStateOf("Preventive PM") }
    var priority by remember { mutableStateOf("Normal") }
    var stoppageHours by remember { mutableStateOf("0.5") }
    var remarks by remember { mutableStateOf("Scheduled monthly PM cycle per IATF 16949 §8.5.1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Equipment Maintenance Ticket") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Ticket Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = selectedAssetTag,
                    onValueChange = { selectedAssetTag = it },
                    label = { Text("Asset Tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maintenanceType,
                        onValueChange = { maintenanceType = it },
                        label = { Text("Type") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priority,
                        onValueChange = { priority = it },
                        label = { Text("Priority") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = stoppageHours,
                    onValueChange = { stoppageHours = it },
                    label = { Text("Stoppage Hours") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks & Action Items") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val record = MaintenanceLogRecord(
                        logId = "PM-2026-${(100..999).random()}",
                        assetTag = selectedAssetTag,
                        assetName = selectedAssetName,
                        maintenanceType = maintenanceType,
                        title = title,
                        technicianName = technicianName,
                        status = "Scheduled",
                        priority = priority,
                        scheduledDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
                        stoppageHours = stoppageHours.toDoubleOrNull() ?: 0.0,
                        remarks = remarks
                    )
                    onSubmit(record)
                }
            ) {
                Text("Submit Ticket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CheckInVisitorModal(
    hostName: String,
    onDismiss: () -> Unit,
    onSubmit: (VisitorLogEntry) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var host by remember { mutableStateOf(hostName) }
    var purpose by remember { mutableStateOf("Supplier Quality Review") }
    var badgeNumber by remember { mutableStateOf("SEC-VIS-${(100..999).random()}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gatehouse Visitor Check-In") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Visitor Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company / Organization") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host Employee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Purpose of Visit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = badgeNumber,
                    onValueChange = { badgeNumber = it },
                    label = { Text("Security Badge Assigned") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        val entry = VisitorLogEntry(
                            visitorId = "VIS-2026-${(100..999).random()}",
                            fullName = fullName,
                            company = if (company.isBlank()) "Independent Contractor" else company,
                            hostEmployee = host,
                            purpose = purpose,
                            badgeNumber = badgeNumber,
                            checkInTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date()),
                            status = "Active On-Site"
                        )
                        onSubmit(entry)
                    }
                },
                enabled = fullName.isNotBlank()
            ) {
                Text("Check-In Visitor")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AuditLogTab(auditLogs: List<AuditLogEntry>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(auditLogs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = when (log.action) {
                                    "QuarantineHold" -> Color(0xFFFFF3E0)
                                    "QualityRelease" -> Color(0xFFE8F5E9)
                                    "Issue" -> Color(0xFFFFEBEE)
                                    else -> Color(0xFFE3F2FD)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = log.action,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (log.action) {
                                        "QuarantineHold" -> Color(0xFFE65100)
                                        "QualityRelease" -> Color(0xFF2E7D32)
                                        "Issue" -> Color(0xFFC62828)
                                        else -> Color(0xFF1565C0)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${log.partMpn} (Δ ${if (log.quantityDelta > 0) "+${log.quantityDelta}" else "${log.quantityDelta}"})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = log.reason,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${log.timestamp} | Actor: ${log.actor}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepPartInspectionSheet(
    part: PartComponent,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Overview", "EHW Tech", "PUR Com", "MECH Phys", "Traceability")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = part.mpn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${part.manufacturer} | ${part.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            TabRow(selectedTabIndex = selectedSubTab) {
                subTabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedSubTab == idx,
                        onClick = { selectedSubTab = idx },
                        text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedSubTab) {
                0 -> {
                    // Overview
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Description: ${part.description}", style = MaterialTheme.typography.bodyMedium)
                        Text("On-Hand Stock: ${part.onHand} pcs", fontWeight = FontWeight.Bold)
                        Text("Released Stock: ${part.released} pcs", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        Text("Held in IQC Quarantine: ${part.iqcHold} pcs", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        Text("Active Allocations: ${part.allocated} pcs", fontWeight = FontWeight.Medium)
                        Text("Location / Bin: ${part.storageLocation} / ${part.binCoordinate}")
                        Text("Reorder Buffer: ${part.reorderBuffer} pcs")
                    }
                }
                1 -> {
                    // EHW Technical
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Voltage Limits: ${part.voltage}", style = MaterialTheme.typography.bodyMedium)
                        Text("Operating Current: ${part.current}")
                        Text("Automotive AEC Standard: ${part.aecRating}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Text("Clock / Frequency: ${part.clockSpeed}")
                        Text("Temperature Envelope: ${part.tempRange}")
                        Text("Package Footprint: ${part.packageFootprint}")
                    }
                }
                2 -> {
                    // PUR Commercial
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tentative Unit Cost: $${part.unitCostUsd} (₹${part.unitCostInr})", fontWeight = FontWeight.Bold)
                        Text("Mouser Distributor Stock: ${part.mouserStock} pcs")
                        Text("Factory Lead Time: ${part.leadTimeWeeks} weeks")
                        Text("Coverage vs 50k Volume: ${part.volumeCoveragePct}%", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        Text("Volume Price Tiers: 1: $${part.unitCostUsd} | 1k: $${String.format("%.2f", part.unitCostUsd * 0.92)} | 10k: $${String.format("%.2f", part.unitCostUsd * 0.85)}")
                    }
                }
                3 -> {
                    // MECH Physical
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Physical Dimensions: ${part.dimensions}")
                        Text("Mass / Weight: ${part.massGrams} g")
                        Text("Ingress Protection: ${part.ipRating}", fontWeight = FontWeight.Bold)
                        Text("Wire Gauge / Size: ${part.wireGauge}")
                        Text("Mating Retention Force: ${part.matingForce}")
                    }
                }
                4 -> {
                    // Traceability
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Compatible Machinery: ${part.compatibleMachines.joinToString(", ")}", fontWeight = FontWeight.Medium)
                        Text("IATF 16949 Dual-Source Status: Approved & Verified")
                        Text("Datasheet Reference: ${part.datasheetUrl}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RegisterPartModal(
    scmsRepository: ScmsRepository,
    onDismiss: () -> Unit
) {
    var mpnInput by remember { mutableStateOf("STM32G0B1VCT6") }
    var descInput by remember { mutableStateOf("") }
    var mfgInput by remember { mutableStateOf("") }
    var packageInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Electronics") }
    var costUsdInput by remember { mutableStateOf("4.85") }
    var qtyInput by remember { mutableStateOf("1000") }
    var binInput by remember { mutableStateOf("A-12-04") }
    var isMouserFetched by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Controlled Part Registration (§1.2)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = mpnInput,
                        onValueChange = { mpnInput = it },
                        label = { Text("Part Number (MPN)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val found = scmsRepository.lookupMouserCatalog(mpnInput)
                            if (found != null) {
                                descInput = found.description
                                mfgInput = found.manufacturer
                                packageInput = found.packageFootprint
                                categoryInput = found.category
                                costUsdInput = found.unitCostUsd.toString()
                                binInput = found.binCoordinate
                                isMouserFetched = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text("⚡ Mouser")
                    }
                }

                if (isMouserFetched) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "✓ Mouser Catalog Verified: Technical & Commercial parameters auto-populated",
                            modifier = Modifier.padding(6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                OutlinedTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = mfgInput,
                        onValueChange = { mfgInput = it },
                        label = { Text("Manufacturer") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = packageInput,
                        onValueChange = { packageInput = it },
                        label = { Text("Package") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it },
                        label = { Text("Initial Stock") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = binInput,
                        onValueChange = { binInput = it },
                        label = { Text("Storage Bin") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val part = PartComponent(
                        mpn = mpnInput.trim(),
                        description = descInput.ifBlank { "Automotive Component" },
                        manufacturer = mfgInput.ifBlank { "Standard SCM" },
                        packageFootprint = packageInput.ifBlank { "Standard" },
                        category = categoryInput,
                        onHand = qtyInput.toIntOrNull() ?: 100,
                        released = qtyInput.toIntOrNull() ?: 100,
                        iqcHold = 0,
                        allocated = 0,
                        storageLocation = "Warehouse Zone A",
                        binCoordinate = binInput.ifBlank { "A-01-01" },
                        reorderBuffer = 500,
                        unitCostUsd = costUsdInput.toDoubleOrNull() ?: 1.0,
                        unitCostInr = (costUsdInput.toDoubleOrNull() ?: 1.0) * 83.5
                    )
                    scmsRepository.registerPart(part)
                    onDismiss()
                }
            ) {
                Text("Save Part")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun IssueMaterialModal(
    parts: List<PartComponent>,
    scmsRepository: ScmsRepository,
    issuer: String,
    onDismiss: () -> Unit
) {
    var selectedMpn by remember { mutableStateOf(parts.firstOrNull()?.mpn ?: "") }
    var selectedRecipient by remember { mutableStateOf("Avery Vale (EHW · Hardware Lead)") }
    var issueQtyInput by remember { mutableStateOf("25") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val projectTeamRoster = listOf(
        "Avery Vale (EHW · Hardware Lead)",
        "Elena Rostova (EHW · RF Engineer)",
        "Devon Lane (ESW · Firmware Lead)",
        "Samira Holt (MECH · Mechanical Lead)",
        "Casey Rowan (EMF · SMT Line Manager)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Issue Project Material (§1.2)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Server-Scoped Verification: Recipient must be an active project member for HLPL-AI4T-01.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = selectedMpn,
                    onValueChange = { selectedMpn = it },
                    label = { Text("Component MPN") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Recipient (Active Project Roster):", style = MaterialTheme.typography.labelSmall)
                projectTeamRoster.forEach { member ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRecipient = member }
                            .padding(vertical = 2.dp),
                        color = if (selectedRecipient == member) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = member,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                OutlinedTextField(
                    value = issueQtyInput,
                    onValueChange = { issueQtyInput = it },
                    label = { Text("Quantity to Issue (pcs)") },
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(text = "⚠ $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = issueQtyInput.toIntOrNull() ?: 1
                    val result = scmsRepository.issueMaterial(
                        mpn = selectedMpn,
                        recipient = selectedRecipient.substringBefore(" ("),
                        dept = selectedRecipient.substringAfter("(").substringBefore(")"),
                        quantity = qty,
                        issuer = issuer
                    )
                    result.fold(
                        onSuccess = { onDismiss() },
                        onFailure = { errorMessage = it.message }
                    )
                }
            ) {
                Text("Confirm Issuance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CycleCountAndPickingTab(
    cycleCounts: List<CycleCountRecord>,
    pickList: List<PickingItem>,
    onRecordCount: (CycleCountRecord) -> Unit,
    onConfirmPick: (String, Int) -> Unit
) {
    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Cycle Counts, 1 = Picking
    var isBlindCountMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sub-navigation pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeSubTab = 0 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeSubTab == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, if (activeSubTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Cycle Counts (${cycleCounts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (activeSubTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeSubTab = 1 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeSubTab == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, if (activeSubTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Work Order Picking (${pickList.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (activeSubTab == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        if (activeSubTab == 0) {
            // Blind Count Toggle Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isBlindCountMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = if (isBlindCountMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Blind Count Audit Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isBlindCountMode) "IATF 16949 Blind Mode Active: System on-hand quantities hidden to prevent operator confirmation bias."
                                else "Standard Mode: Expected system stock visible for verification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isBlindCountMode,
                            onCheckedChange = { isBlindCountMode = it }
                        )
                    }
                }
            }

            // Cycle Count Items
            items(cycleCounts) { record ->
                var countInput by remember { mutableStateOf(record.countedQty?.toString() ?: "") }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = record.binCoordinate,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            val (statusBg, statusFg) = when (record.status) {
                                "Matched" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
                                "Discrepancy" -> Color(0xFFFEF3C7) to Color(0xFFB45309)
                                else -> Color(0xFFF1F5F9) to Color(0xFF64748B)
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                                Text(
                                    text = record.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusFg,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = record.mpn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = record.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("System Expected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (isBlindCountMode) "••••• (Blind)" else "${record.systemQty} pcs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Counted Qty", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = record.countedQty?.let { "$it pcs" } ?: "Not Counted",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (record.countedQty == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text("Variance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = record.variance?.let { if (it > 0) "+$it" else "$it" } ?: "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        record.variance == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                        record.variance == 0 -> Color(0xFF15803D)
                                        else -> Color(0xFFB45309)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = countInput,
                                onValueChange = { countInput = it },
                                label = { Text("Counted Qty", style = MaterialTheme.typography.labelSmall) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                            FilledTonalButton(
                                onClick = {
                                    val counted = countInput.toIntOrNull() ?: record.systemQty
                                    val variance = counted - record.systemQty
                                    val newStatus = if (variance == 0) "Matched" else "Discrepancy"
                                    onRecordCount(
                                        record.copy(
                                            countedQty = counted,
                                            variance = variance,
                                            status = newStatus,
                                            counterName = "Count Operator"
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Record", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Picking Tab Content
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Work Order Pick List: WO-2026-AI4T-001",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pilot Production Batch (100 units) • SMT Line 2 Kitting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(pickList) { pickItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = pickItem.binCoordinate,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                if (pickItem.isConfirmed) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                        Text(
                                            text = "✓ Picked",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7)) {
                                        Text(
                                            text = "Pending Pick",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = pickItem.mpn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Required: ${pickItem.requiredQty} pcs • Picked: ${pickItem.pickedQty} pcs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!pickItem.isConfirmed) {
                            FilledTonalButton(
                                onClick = { onConfirmPick(pickItem.pickId, pickItem.requiredQty) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick 100", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Confirmed", tint = Color(0xFF15803D), modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceivingDraftsTab(
    receivingDrafts: List<ReceivingDraftVoucher>,
    onOpenNewDraft: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mobile Inwarding Drafts (Stage 5B)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Offline-safe vouchers for incoming shipments. Official GRN numbering and quarantine staging are finalized upon online server synchronization.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onOpenNewDraft,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Draft", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(receivingDrafts) { draft ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = draft.voucherId,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7)) {
                            Text(
                                text = draft.status,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${draft.mpn} (${draft.quantityReceived} pcs)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(text = "${draft.poNumber} • ${draft.vendorName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Reel ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(draft.reelId, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Lot / Date Code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${draft.lotNumber} (${draft.dateCode})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("MSL Rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(draft.mslRating, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Inspector: ${draft.inspectorName} • ${draft.packageCondition}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        if (draft.esdCompliant) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDCFCE7)) {
                                Text(
                                    text = "ESD Checked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF15803D),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceivingDraftModal(
    scmsRepository: ScmsRepository,
    inspectorName: String,
    onDismiss: () -> Unit
) {
    val purchaseOrders by scmsRepository.purchaseOrders.collectAsState()
    var selectedPo by remember { mutableStateOf(purchaseOrders.firstOrNull()?.poNumber ?: "HLPL-PO-2026-042") }
    var selectedVendor by remember { mutableStateOf(purchaseOrders.firstOrNull()?.vendorName ?: "Mouser Electronics India") }
    var mpnInput by remember { mutableStateOf("STM32G0B1VCT6") }
    var lotInput by remember { mutableStateOf("LOT-2026-MOU-0882") }
    var batchInput by remember { mutableStateOf("B-9940") }
    var dateCodeInput by remember { mutableStateOf("2639") }
    var reelInput by remember { mutableStateOf("REEL-ST-99121") }
    var qtyInput by remember { mutableStateOf("2500") }
    var mslRating by remember { mutableStateOf("MSL-3 (168 hrs)") }
    var esdCompliant by remember { mutableStateOf(true) }
    var packageCondition by remember { mutableStateOf("Intact / Verified") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CallReceived, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Draft Inwarding Voucher", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Stage 5B Mobile Receiving", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select Approved PO:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(purchaseOrders) { po ->
                        val isSelected = selectedPo == po.poNumber
                        Surface(
                            modifier = Modifier.clickable {
                                selectedPo = po.poNumber
                                selectedVendor = po.vendorName
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = po.poNumber,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = mpnInput,
                    onValueChange = { mpnInput = it },
                    label = { Text("Component MPN", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lotInput,
                        onValueChange = { lotInput = it },
                        label = { Text("Lot Number", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dateCodeInput,
                        onValueChange = { dateCodeInput = it },
                        label = { Text("Date Code", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reelInput,
                        onValueChange = { reelInput = it },
                        label = { Text("Reel ID", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it },
                        label = { Text("Quantity (pcs)", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ESD Bag Sealed & Intact:", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = esdCompliant, onCheckedChange = { esdCompliant = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val voucher = ReceivingDraftVoucher(
                        voucherId = "RCV-DRAFT-2026-${(100..999).random()}",
                        poNumber = selectedPo,
                        vendorName = selectedVendor,
                        mpn = mpnInput,
                        lotNumber = lotInput,
                        batchCode = batchInput,
                        dateCode = dateCodeInput,
                        reelId = reelInput,
                        quantityReceived = qtyInput.toIntOrNull() ?: 100,
                        mslRating = mslRating,
                        esdCompliant = esdCompliant,
                        packageCondition = packageCondition,
                        inspectorName = inspectorName,
                        timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())
                    )
                    scmsRepository.saveReceivingDraft(voucher)
                    onDismiss()
                }
            ) {
                Text("Save Draft Voucher")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun GovernedInventoryScreenPreview() {
    com.example.myapplication.ui.theme.MyApplicationTheme {
        GovernedInventoryScreen(
            scmsRepository = com.example.myapplication.data.repository.ScmsRepositoryImpl()
        )
    }
}


