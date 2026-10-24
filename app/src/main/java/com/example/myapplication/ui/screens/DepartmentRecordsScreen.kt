package com.example.myapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.AssetEquipment
import com.example.myapplication.data.model.ControlledDoc
import com.example.myapplication.data.model.DvprTestItem
import com.example.myapplication.data.model.PartComponent
import com.example.myapplication.data.model.ProductionWorkOrder
import com.example.myapplication.data.model.PurchaseOrder
import com.example.myapplication.data.model.PurchasingBomReviewItem
import com.example.myapplication.data.model.VendorRecord
import com.example.myapplication.data.model.WireHarness
import com.example.myapplication.data.repository.ScmsRepository
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentRecordsScreen(
    scmsRepository: ScmsRepository,
    onOpenRoleSwitcher: () -> Unit = {},
    sharedBusinessContent: (@Composable (String) -> Unit)? = null
) {
    val currentRole by scmsRepository.currentRole.collectAsState()
    val parts by scmsRepository.parts.collectAsState()
    val harnesses by scmsRepository.harnesses.collectAsState()
    val purchaseOrders by scmsRepository.purchaseOrders.collectAsState()
    val vendors by scmsRepository.approvedVendors.collectAsState()
    val purchasingBom by scmsRepository.purchasingBom.collectAsState()
    val dvprTests by scmsRepository.dvprTests.collectAsState()
    val assets by scmsRepository.assets.collectAsState()
    val clientDeliverables by scmsRepository.clientDeliverables.collectAsState()
    val workOrders by scmsRepository.workOrders.collectAsState()
    val receivingDrafts by scmsRepository.receivingDrafts.collectAsState()
    val departmentAllocations by scmsRepository.departmentAllocations.collectAsState()
    val expenseRequests by scmsRepository.expenseRequests.collectAsState()
    val serviceTickets by scmsRepository.serviceTickets.collectAsState()
    val employeeDirectory by scmsRepository.employeeDirectory.collectAsState()
    val assignedPolicies by scmsRepository.assignedPolicies.collectAsState()
    val assignedTrainings by scmsRepository.assignedTrainings.collectAsState()

    val selectedProject by scmsRepository.selectedProject.collectAsState()
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showCreateRecordModal by remember { mutableStateOf(false) }
    var selectedRecordForInspect by remember { mutableStateOf<Any?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Department-specific sub-tabs definition
    val subTabs = when (currentRole.code) {
        "PUR" -> listOf("Inventory", "BOM Review", "Quotations", "Orders (POs)", "Suppliers (ASL)")
        "EHW" -> listOf("Components", "BOM Review", "Allocations", "Schematics")
        "MECH" -> listOf("Wire Harnesses", "Enclosures & CAD", "Tooling & Ingress")
        "EMF" -> listOf("SMT Lines", "Control Plans", "Work Orders", "Receiving Drafts")
        "QA" -> listOf("DVP&R Matrix", "PPAP Level 3", "Calibration Logs", "NCR Logs")
        "FIN" -> listOf("Dept Budgets", "Expense Claims", "Invoices", "PO Ledger", "Milestone Cashflow")
        "SIT" -> listOf("Service Tickets", "IT Assets", "Field RMA")
        "HROPS" -> listOf("Team Directory", "Company Policies", "Training & Skills", "Facility Assets", "Dept Budgets", "Expense Claims")
        "CLIENT" -> listOf("Deliverables", "Client POs", "Support Tickets")
        else -> listOf("All Inventory", "Service Tickets", "Dept Budgets", "Expense Claims", "Capital Assets", "Orders (POs)", "Wire Harnesses", "DVP&R Tests")
    }

    val activeSubTabIndex = if (selectedSubTab >= subTabs.size) 0 else selectedSubTab

    val sharedSection = when(subTabs[activeSubTabIndex]) {
        "Dept Budgets", "Expense Claims" -> "budgets"
        "Service Tickets", "Support Tickets", "Field RMA" -> "tickets"
        "Team Directory", "Company Policies", "Training & Skills" -> "profile"
        else -> null
    }
    if(sharedBusinessContent != null && sharedSection != null) {
        Column(Modifier.fillMaxSize()) {
            LazyRow { items(subTabs.size) { index ->
                TextButton(onClick = { selectedSubTab = index }) { Text(subTabs[index]) }
            } }
            Box(Modifier.weight(1f)) { sharedBusinessContent(sharedSection) }
        }
        return
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Department & Project Scope Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DEPARTMENT RECORDS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.clickable { onOpenRoleSwitcher() }
                            ) {
                                Text(
                                    text = "${currentRole.code} · ${currentRole.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${currentRole.leadName}'s Record Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // + New Record Action Button
                    Button(
                        onClick = { showCreateRecordModal = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Record", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Project Selector Chips Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Project:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(
                        "HLPL-AI4T-01" to "Tata Ace EV (AI4T-01)",
                        "HLPL-SLCT-01" to "Select AIS-140",
                        "ALL" to "All Projects"
                    ).forEach { (id, label) ->
                        val isSelected = selectedProject == id
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
                            modifier = Modifier.clickable { scmsRepository.selectProject(id) }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Sub-tabs Row
        ScrollableTabRow(
            selectedTabIndex = activeSubTabIndex,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeSubTabIndex == index,
                    onClick = { selectedSubTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (activeSubTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Status banner if any
        statusMessage?.let { msg ->
            Surface(
                color = Color(0xFFE8F5E9),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(msg, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { statusMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Content Area based on currentRole and subTab
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                // INVENTORY / COMPONENTS
                subTabs[activeSubTabIndex].contains("Inventory", ignoreCase = true) ||
                subTabs[activeSubTabIndex].contains("Components", ignoreCase = true) -> {
                    val filteredParts = parts.filter {
                        if (currentRole.code == "EHW") it.category.contains("Active", ignoreCase = true) || it.category.contains("Passive", ignoreCase = true) || it.category.contains("Microcontroller", ignoreCase = true)
                        else true
                    }
                    item {
                        RecordSectionHeader(
                            title = "Controlled Component Inventory",
                            count = filteredParts.size,
                            unit = "components",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(filteredParts) { part ->
                        PartCard(part = part, onClick = { selectedRecordForInspect = part })
                    }
                }

                // BOM REVIEW
                subTabs[activeSubTabIndex].contains("BOM Review", ignoreCase = true) -> {
                    item {
                        RecordSectionHeader(
                            title = "R&D Tentative BOM vs. Purchasing SCM Review",
                            count = purchasingBom.size,
                            unit = "lines",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(purchasingBom) { bomItem ->
                        PurchasingBomCard(bomItem = bomItem, onClick = { selectedRecordForInspect = bomItem })
                    }
                }

                // PURCHASE ORDERS
                subTabs[activeSubTabIndex].contains("Orders", ignoreCase = true) ||
                subTabs[activeSubTabIndex].contains("PO", ignoreCase = true) -> {
                    item {
                        RecordSectionHeader(
                            title = "Controlled Purchase Orders (POs)",
                            count = purchaseOrders.size,
                            unit = "orders",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(purchaseOrders) { po ->
                        PurchaseOrderCard(po = po, onClick = { selectedRecordForInspect = po })
                    }
                }

                // SUPPLIERS / ASL
                subTabs[activeSubTabIndex].contains("Suppliers", ignoreCase = true) ||
                subTabs[activeSubTabIndex].contains("ASL", ignoreCase = true) -> {
                    item {
                        RecordSectionHeader(
                            title = "Approved Supplier List (ASL) & Vendor Governance",
                            count = vendors.size,
                            unit = "suppliers",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(vendors) { vendor ->
                        VendorCard(vendor = vendor, onClick = { selectedRecordForInspect = vendor })
                    }
                }

                // WIRE HARNESSES
                subTabs[activeSubTabIndex].contains("Harness", ignoreCase = true) -> {
                    val filteredHarnesses = harnesses.filter {
                        selectedProject == "ALL" || it.projectId == selectedProject
                    }
                    item {
                        RecordSectionHeader(
                            title = "Automotive Wire Harness Assemblies",
                            count = filteredHarnesses.size,
                            unit = "harnesses",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(filteredHarnesses) { harness ->
                        WireHarnessCard(harness = harness, onClick = { selectedRecordForInspect = harness })
                    }
                }

                // DVP&R TESTS
                subTabs[activeSubTabIndex].contains("DVP&R", ignoreCase = true) -> {
                    item {
                        RecordSectionHeader(
                            title = "Automotive Hardware DVP&R Test Matrix",
                            count = dvprTests.size,
                            unit = "tests",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(dvprTests) { test ->
                        DvprTestCard(test = test, onClick = { selectedRecordForInspect = test })
                    }
                }

// SERVICE & FIELD SUPPORT TICKETS
                                subTabs[activeSubTabIndex].contains("Directory", ignoreCase = true) ||
                subTabs[activeSubTabIndex].contains("Team", ignoreCase = true) -> {
                    item {
                        RecordSectionHeader(
                            title = "Personnel & Team Directory",
                            count = employeeDirectory.size,
                            unit = "Employees",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(employeeDirectory) { emp ->
                        EmployeeDirectoryCard(emp = emp, onClick = { selectedRecordForInspect = emp })
                    }
                }

                subTabs[activeSubTabIndex].contains("Policies", ignoreCase = true) ||
                subTabs[activeSubTabIndex].contains("Policy", ignoreCase = true) -> {
                    item {
                        RecordSectionHeader(
                            title = "Organizational & Compliance Policies",
                            count = assignedPolicies.size,
                            unit = "Policies",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(assignedPolicies) { pol ->
                        AssignedPolicyCard(policy = pol, onClick = { selectedRecordForInspect = pol })
                    }
                }

                subTabs[activeSubTabIndex].contains("Training", ignoreCase = true) ||
                subTabs[activeSubTabIndex].contains("Skill", ignoreCase = true) -> {
                    item {
                        RecordSectionHeader(
                            title = "Training Curriculum & Skills",
                            count = assignedTrainings.size,
                            unit = "Modules",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(assignedTrainings) { trn ->
                        AssignedTrainingCard(training = trn, onClick = { selectedRecordForInspect = trn })
                    }
                }

                subTabs[activeSubTabIndex].contains("Tickets", ignoreCase = true) -> {
                    item {
                        RecordSectionHeader(
                            title = "Service & Field Support Tickets",
                            count = serviceTickets.size,
                            unit = "tickets",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(serviceTickets) { ticket ->
                        ServiceTicketCard(
                            ticket = ticket,
                            isClient = currentRole.isClient,
                            canManage = currentRole == com.example.myapplication.data.model.DepartmentRole.ADMIN || currentRole == com.example.myapplication.data.model.DepartmentRole.SIT,
                            onClick = { selectedRecordForInspect = ticket },
                            onUpdateStatus = { newStatus ->
                                scmsRepository.updateServiceTicketStatus(ticket.id, newStatus)
                            }
                        )
                    }
                }

                // CAPITAL ASSETS
                subTabs[activeSubTabIndex].contains("Assets", ignoreCase = true) -> {
                    item {
                        RecordSectionHeader(
                            title = "SMT & Plant Capital Assets Register",
                            count = assets.size,
                            unit = "assets",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(assets) { asset ->
                        AssetCard(asset = asset, onClick = { selectedRecordForInspect = asset })
                    }
                }

                // DELIVERABLES / CLIENT DELIVERABLES
                subTabs[activeSubTabIndex].contains("Deliverables", ignoreCase = true) -> {
                    item {
                        RecordSectionHeader(
                            title = "OEM Client Milestones & Deliverables",
                            count = clientDeliverables.size,
                            unit = "deliverables",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(clientDeliverables) { deliverable ->
                        ClientDeliverableCard(deliverable = deliverable, onClick = { selectedRecordForInspect = deliverable })
                    }
                }

                // WORK ORDERS (EMF)
                subTabs[activeSubTabIndex].contains("Work Orders", ignoreCase = true) -> {
                    item {
                        RecordSectionHeader(
                            title = "SMT Production Work Orders",
                            count = workOrders.size,
                            unit = "work orders",
                            onAdd = { showCreateRecordModal = true }
                        )
                    }
                    items(workOrders) { wo ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedRecordForInspect = wo },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(wo.workOrderNo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                                        Text(wo.status, color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text(wo.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Batch Qty: ${wo.batchQuantity} pcs · Station: ${wo.currentStationNo} · Completed: ${wo.completedQuantity}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // DEPARTMENT BUDGET ALLOCATIONS
                subTabs[activeSubTabIndex].contains("Budgets", ignoreCase = true) -> {
                    if (currentRole.code == "CLIENT") {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Lock, contentDescription = "Access Restricted", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Access Restricted — Internal Budgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Department budgets and expense claims are internal operational controls. Client accounts do not have access to internal company finances.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        val filteredAllocations = departmentAllocations.filter {
                            val matchesProject = selectedProject == "ALL" || it.projectId == null || it.projectId == selectedProject
                            val matchesDept = if (currentRole.code in listOf("ADMIN", "MD", "FIN")) true else it.departmentCode.equals(currentRole.code, ignoreCase = true)
                            matchesProject && matchesDept
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "SANDBOX DEMO MODE · Changes persist in local session for evaluation. Shared sync requires authenticated Enterprise API session.",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            RecordSectionHeader(
                                title = "Department FY 2026-27 Budget Allocations",
                                count = filteredAllocations.size,
                                unit = "allocations",
                                onAdd = { showCreateRecordModal = true }
                            )
                        }
                        items(filteredAllocations) { alloc ->
                            DepartmentAllocationCard(allocation = alloc, onClick = { selectedRecordForInspect = alloc })
                        }
                    }
                }

                // EXPENSE CLAIMS & REQUISITIONS
                subTabs[activeSubTabIndex].contains("Expense", ignoreCase = true) -> {
                    if (currentRole.code == "CLIENT") {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Lock, contentDescription = "Access Restricted", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Access Restricted — Expense Claims", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Department budgets and expense claims are internal operational controls. Client accounts do not have access to internal company finances.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        val filteredExpenses = expenseRequests.filter {
                            val matchesProject = selectedProject == "ALL" || it.projectId == null || it.projectId == selectedProject
                            val matchesDept = if (currentRole.code in listOf("ADMIN", "MD", "FIN")) true else it.requesterDept.equals(currentRole.code, ignoreCase = true)
                            matchesProject && matchesDept
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "SANDBOX DEMO MODE · Changes persist in local session for evaluation. Shared sync requires authenticated Enterprise API session.",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            RecordSectionHeader(
                                title = "Department Expense Claims & Requisitions",
                                count = filteredExpenses.size,
                                unit = "claims",
                                onAdd = { showCreateRecordModal = true }
                            )
                        }
                        items(filteredExpenses) { expense ->
                            ExpenseRequestCard(
                                expense = expense,
                                currentRoleCode = currentRole.code,
                                onApprove = { approver ->
                                    val res = scmsRepository.approveExpenseRequest(expense.requestId, approver)
                                    statusMessage = if (res.isSuccess) res.getOrNull() else res.exceptionOrNull()?.message
                                },
                                onReject = { reason ->
                                    val res = scmsRepository.rejectExpenseRequest(expense.requestId, reason)
                                    statusMessage = if (res.isSuccess) res.getOrNull() else res.exceptionOrNull()?.message
                                },
                                onClick = { selectedRecordForInspect = expense }
                            )
                        }
                    }
                }

                // DEFAULT / FALLBACK
                else -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${subTabs[activeSubTabIndex]} Records",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ready to record and track data for ${currentRole.title}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { showCreateRecordModal = true },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("+ New ${subTabs[activeSubTabIndex]} Entry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE RECORD DIALOG
    if (showCreateRecordModal) {
        CreateRecordDialog(
            activeDepartment = currentRole.code,
            activeSubTab = subTabs[activeSubTabIndex],
            scmsRepository = scmsRepository,
            onDismiss = { showCreateRecordModal = false },
            onSuccess = { message ->
                showCreateRecordModal = false
                statusMessage = message
            }
        )
    }

    // INSPECTION SHEET
    selectedRecordForInspect?.let { record ->
        ModalBottomSheet(
            onDismissRequest = { selectedRecordForInspect = null },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            RecordDetailInspectionView(
                record = record,
                onClose = { selectedRecordForInspect = null }
            )
        }
    }
}

@Composable
fun RecordSectionHeader(
    title: String,
    count: Int,
    unit: String,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$count $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Specialized Record Cards
// -----------------------------------------------------------------------------

@Composable
fun PartCard(part: PartComponent, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = part.mpn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (part.onHand <= part.reorderBuffer) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = if (part.onHand <= part.reorderBuffer) "Low Stock" else "In Stock",
                        color = if (part.onHand <= part.reorderBuffer) Color(0xFFC62828) else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = part.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bin: ${part.storageLocation} (${part.binCoordinate})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Stock: ${part.onHand} ${part.uom}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PurchasingBomCard(bomItem: PurchasingBomReviewItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bomItem.mpn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (bomItem.costDelta <= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = if (bomItem.costDelta <= 0) "-$${Math.abs(bomItem.costDelta)} Savings" else "+$${bomItem.costDelta} Variance",
                        color = if (bomItem.costDelta <= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = bomItem.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Supplier: ${bomItem.primarySupplier}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Lead: ${bomItem.leadTimeWeeks} wks · Quoted: $${bomItem.quotedCost}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PurchaseOrderCard(po: PurchaseOrder, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(po.poNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE3F2FD)
                ) {
                    Text(
                        text = po.status,
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(po.vendorName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ETA: ${po.expectedDelivery} (${po.lineItemsCount} lines)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Text("$${po.totalAmountUsd}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun VendorCard(vendor: VendorRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(vendor.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = vendor.aslStatus,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text("${vendor.category} · Country: ${vendor.country}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Cert: ${vendor.isoCertification}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                Text(vendor.riskRating, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
        }
    }
}

@Composable
fun WireHarnessCard(harness: WireHarness, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(harness.assemblyId, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                    Text(harness.status, color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
            Text(harness.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pins: ${harness.pinCount} · Gauge: ${harness.wireGauge}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Text(harness.ipRating, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun DvprTestCard(test: DvprTestItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(test.testId, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val statusColor = when (test.status) {
                    "Passed" -> Color(0xFF2E7D32)
                    "In Progress" -> Color(0xFF1565C0)
                    else -> Color(0xFFE65100)
                }
                Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(alpha = 0.1f)) {
                    Text(test.status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
            Text(test.testTitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Standard: ${test.standard}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Text(test.lab, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun AssetCard(asset: AssetEquipment, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(asset.assetTag, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                    Text(asset.status, color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
            Text(asset.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Location: ${asset.location}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Text("Cal Due: ${asset.calibrationDue}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ClientDeliverableCard(deliverable: com.example.myapplication.data.model.ClientDeliverable, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(deliverable.code, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE3F2FD)) {
                    Text(deliverable.status, color = Color(0xFF1565C0), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
            Text(deliverable.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Owner: ${deliverable.owner}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Text(deliverable.date, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Create Record Dialog
// -----------------------------------------------------------------------------

@Composable
fun CreateRecordDialog(
    activeDepartment: String,
    activeSubTab: String,
    scmsRepository: ScmsRepository,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var recordType by remember {
        mutableStateOf(
            when {
                activeSubTab.contains("Order", ignoreCase = true) || activeSubTab.contains("PO", ignoreCase = true) -> "Purchase Order"
                activeSubTab.contains("Harness", ignoreCase = true) -> "Wire Harness"
                activeSubTab.contains("DVP&R", ignoreCase = true) -> "DVP&R Test"
                activeSubTab.contains("Asset", ignoreCase = true) -> "Capital Asset"
                else -> "Part / Component"
            }
        )
    }

    // Form fields
    var field1 by remember { mutableStateOf("") }
    var field2 by remember { mutableStateOf("") }
    var field3 by remember { mutableStateOf("") }
    var field4 by remember { mutableStateOf("") }
    var field5 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create New $recordType", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Record type selector chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val types = listOf("Part / Component", "Purchase Order", "Wire Harness", "DVP&R Test", "Capital Asset")
                    items(types) { type ->
                        val selected = recordType == type
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { recordType = type }
                        ) {
                            Text(
                                text = type,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                when (recordType) {
                    "Purchase Order" -> {
                        OutlinedTextField(
                            value = field1,
                            onValueChange = { field1 = it },
                            label = { Text("PO Number (e.g. HLPL-PO-2026-045)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field2,
                            onValueChange = { field2 = it },
                            label = { Text("Vendor / Supplier Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field3,
                            onValueChange = { field3 = it },
                            label = { Text("Total Amount USD ($)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field4,
                            onValueChange = { field4 = it },
                            label = { Text("Expected Delivery Date") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    "Wire Harness" -> {
                        OutlinedTextField(
                            value = field1,
                            onValueChange = { field1 = it },
                            label = { Text("Assembly ID (e.g. HLPL-HARN-AI4T-BMS-02)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field2,
                            onValueChange = { field2 = it },
                            label = { Text("Harness Name / Description") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field3,
                            onValueChange = { field3 = it },
                            label = { Text("Pin Count (e.g. 16)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field4,
                            onValueChange = { field4 = it },
                            label = { Text("IP Rating (e.g. IP67)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    "DVP&R Test" -> {
                        OutlinedTextField(
                            value = field1,
                            onValueChange = { field1 = it },
                            label = { Text("Test ID (e.g. DVP-ISO-01)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field2,
                            onValueChange = { field2 = it },
                            label = { Text("Automotive Standard (e.g. ISO 7637-2)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field3,
                            onValueChange = { field3 = it },
                            label = { Text("Test Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field4,
                            onValueChange = { field4 = it },
                            label = { Text("Test Lab (e.g. Hexense Internal / ARAI)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    "Capital Asset" -> {
                        OutlinedTextField(
                            value = field1,
                            onValueChange = { field1 = it },
                            label = { Text("Asset Tag (e.g. AST-SMT-004)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field2,
                            onValueChange = { field2 = it },
                            label = { Text("Asset Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field3,
                            onValueChange = { field3 = it },
                            label = { Text("Location (e.g. Greater Noida SMT Line 2)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    else -> { // Part / Component
                        OutlinedTextField(
                            value = field1,
                            onValueChange = { field1 = it },
                            label = { Text("Part Number / MPN") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field2,
                            onValueChange = { field2 = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field3,
                            onValueChange = { field3 = it },
                            label = { Text("Category (e.g. Microcontroller, Sensor)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field4,
                            onValueChange = { field4 = it },
                            label = { Text("Storage Bin (e.g. WH-02-B)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = field5,
                            onValueChange = { field5 = it },
                            label = { Text("Current Stock Qty") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (recordType) {
                        "Purchase Order" -> {
                            val poId = if (field1.isNotBlank()) field1 else "HLPL-PO-${UUID.randomUUID().toString().take(6).uppercase()}"
                            val po = PurchaseOrder(
                                poNumber = poId,
                                vendorName = if (field2.isNotBlank()) field2 else "Authorized Distributor",
                                issueDate = "2026-10-20",
                                expectedDelivery = if (field4.isNotBlank()) field4 else "2026-11-15",
                                totalAmountUsd = field3.toDoubleOrNull() ?: 12500.0,
                                status = "Approved",
                                lineItemsCount = 3
                            )
                            scmsRepository.createPurchaseOrder(po)
                            onSuccess("Successfully created Purchase Order $poId")
                        }
                        "Wire Harness" -> {
                            val hId = if (field1.isNotBlank()) field1 else "HLPL-HARN-AI4T-${UUID.randomUUID().toString().take(4).uppercase()}"
                            val harness = WireHarness(
                                assemblyId = hId,
                                name = if (field2.isNotBlank()) field2 else "Automotive Sealed Loom Assembly",
                                projectId = "HLPL-AI4T-01",
                                wireGauge = "20 AWG TXL Automotive",
                                connectors = "TE Sealed Automotive",
                                pinCount = field3.toIntOrNull() ?: 16,
                                matingRetentionForce = "> 110 N (USCAR-2)",
                                ipRating = if (field4.isNotBlank()) field4 else "IP67",
                                status = "Released"
                            )
                            scmsRepository.createHarness(harness)
                            onSuccess("Successfully registered Wire Harness $hId")
                        }
                        "DVP&R Test" -> {
                            val tId = if (field1.isNotBlank()) field1 else "DVP-${UUID.randomUUID().toString().take(4).uppercase()}"
                            val test = DvprTestItem(
                                testId = tId,
                                standard = if (field2.isNotBlank()) field2 else "AIS-140 Annexure 8",
                                testTitle = if (field3.isNotBlank()) field3 else "Automotive Compliance Test",
                                condition = "Operational verification under automotive nominal voltage",
                                acceptanceCriteria = "No reset, telemetry packet integrity 100%",
                                lab = if (field4.isNotBlank()) field4 else "Internal Lab (Hexense)",
                                status = "Scheduled",
                                technician = "Jordan Blake"
                            )
                            scmsRepository.createDvprTest(test)
                            onSuccess("Successfully logged DVP&R test $tId")
                        }
                        "Capital Asset" -> {
                            val aId = if (field1.isNotBlank()) field1 else "AST-${UUID.randomUUID().toString().take(4).uppercase()}"
                            val asset = AssetEquipment(
                                assetTag = aId,
                                name = if (field2.isNotBlank()) field2 else "SMT Production Asset",
                                category = "Manufacturing Equipment",
                                serialNumber = "SN-2026-${UUID.randomUUID().toString().take(6).uppercase()}",
                                location = if (field3.isNotBlank()) field3 else "Greater Noida SMT Plant 2",
                                custodian = "SMT Supervisor",
                                compatiblePartNumbers = listOf("STM32G0B1VCT6", "EG915N-EU"),
                                lastCalibrationDate = "2026-09-15",
                                calibrationDue = "2027-09-15",
                                status = "Operational"
                            )
                            scmsRepository.createAsset(asset)
                            onSuccess("Successfully registered Capital Asset $aId")
                        }
                        else -> { // Part / Component
                            val pMpn = if (field1.isNotBlank()) field1 else "HLPL-PART-${UUID.randomUUID().toString().take(6).uppercase()}"
                            val qty = field5.toIntOrNull() ?: 1000
                            val part = PartComponent(
                                mpn = pMpn,
                                category = if (field3.isNotBlank()) field3 else "Automotive Passive",
                                description = if (field2.isNotBlank()) field2 else "Automotive Grade Component",
                                manufacturer = "Franchised Automotive Supplier",
                                packageFootprint = "0603 / SMD",
                                onHand = qty,
                                released = qty,
                                iqcHold = 0,
                                allocated = 0,
                                storageLocation = "Greater Noida SMT Plant 2",
                                binCoordinate = if (field4.isNotBlank()) field4 else "WH-02-B",
                                reorderBuffer = 200,
                                uom = "pcs",
                                unitCostUsd = 0.45,
                                unitCostInr = 38.0,
                                aecRating = "AEC-Q200"
                            )
                            scmsRepository.registerPart(part)
                            onSuccess("Successfully registered Part $pMpn")
                        }
                    }
                }
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// -----------------------------------------------------------------------------
// Record Detail Bottom Sheet
// -----------------------------------------------------------------------------

@Composable
fun RecordDetailInspectionView(
    record: Any,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Record Details & Compliance Inspection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        when (record) {
            is PartComponent -> {
                Text("Part Number: ${record.mpn}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Description: ${record.description}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Category: ${record.category} · Manufacturer: ${record.manufacturer}", fontSize = 12.sp)
                Text("Storage Location: ${record.storageLocation} in Bin ${record.binCoordinate}", fontSize = 12.sp)
                Text("Stock: ${record.onHand} ${record.uom} (Buffer: ${record.reorderBuffer})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Compliance: AEC-Q200 = ${record.aecRating}, Temp = ${record.tempRange}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
            is PurchaseOrder -> {
                Text("PO Number: ${record.poNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Vendor: ${record.vendorName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Total Commitment: $${record.totalAmountUsd}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Expected Delivery: ${record.expectedDelivery} · Issued: ${record.issueDate}", fontSize = 12.sp)
                Text("Status: ${record.status} (${record.lineItemsCount} line items)", fontSize = 12.sp)
            }
            is WireHarness -> {
                Text("Harness Assembly: ${record.assemblyId}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Name: ${record.name}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Project: ${record.projectId} · Wire Gauge: ${record.wireGauge}", fontSize = 12.sp)
                Text("Connectors: ${record.connectors} · Pins: ${record.pinCount}", fontSize = 12.sp)
                Text("Retention Force: ${record.matingRetentionForce} · Ingress: ${record.ipRating}", fontSize = 12.sp)
                Text("Status: ${record.status}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
            is DvprTestItem -> {
                Text("Test ID: ${record.testId}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Title: ${record.testTitle}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Standard: ${record.standard}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Lab: ${record.lab}", fontSize = 12.sp)
                Text("Test Condition: ${record.condition}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Text("Acceptance Criteria: ${record.acceptanceCriteria}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Text("Status: ${record.status} · Technician: ${record.technician}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            is AssetEquipment -> {
                Text("Asset Tag: ${record.assetTag}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Name: ${record.name}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Category: ${record.category} · Serial: ${record.serialNumber}", fontSize = 12.sp)
                Text("Facility Location: ${record.location}", fontSize = 12.sp)
                Text("Status: ${record.status} · Cal Due: ${record.calibrationDue}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            else -> {
                Text(record.toString(), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Done")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}


// -----------------------------------------------------------------------------
// Department Budget Allocation Card
// -----------------------------------------------------------------------------
@Composable
fun DepartmentAllocationCard(
    allocation: com.example.myapplication.data.model.DepartmentAllocationItem,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE0F2FE)
                    ) {
                        Text(
                            text = allocation.departmentCode,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = allocation.allocationId,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (allocation.availableAmount > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = "Avail: ₹" + String.format(java.util.Locale.US, "%,.0f", allocation.availableAmount),
                        color = if (allocation.availableAmount > 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = allocation.departmentName + " · " + allocation.category,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            allocation.projectId?.let { proj ->
                Text(
                    text = "Project Scope: " + proj,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Allocated", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                    Text("₹" + String.format(java.util.Locale.US, "%,.0f", allocation.allocatedAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Committed", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                    Text("₹" + String.format(java.util.Locale.US, "%,.0f", allocation.committedAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                }
                Column {
                    Text("Posted Actuals", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                    Text("₹" + String.format(java.util.Locale.US, "%,.0f", allocation.spentAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            val spentRatio = ((allocation.spentAmount + allocation.committedAmount) / allocation.allocatedAmount.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
            androidx.compose.material3.LinearProgressIndicator(
                progress = { spentRatio },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = if (spentRatio > 0.9f) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Department Expense Request Card
// -----------------------------------------------------------------------------
@Composable
fun ExpenseRequestCard(
    expense: com.example.myapplication.data.model.ExpenseRequestItem,
    currentRoleCode: String,
    onApprove: (String) -> Unit = {},
    onReject: (String) -> Unit = {},
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(expense.reference, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(expense.requesterDept, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (expense.status) {
                        "Approved" -> Color(0xFFE8F5E9)
                        "Rejected" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFFFF8E1)
                    }
                ) {
                    Text(
                        text = expense.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (expense.status) {
                            "Approved" -> Color(0xFF2E7D32)
                            "Rejected" -> Color(0xFFC62828)
                            else -> Color(0xFFF57F17)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(expense.purpose, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Claim: ₹" + String.format(java.util.Locale.US, "%,.0f", expense.amount), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Text("By " + expense.requesterName + " · " + expense.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
                expense.projectId?.let { proj ->
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(proj, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }

            expense.travelDetails?.let { trv ->
                Spacer(modifier = Modifier.height(4.dp))
                Text("Travel / Advance: " + trv, fontSize = 10.sp, color = Color(0xFF00838F))
            }

            // Approver Action Row (No self-approval rule enforced)
            if (expense.status.contains("Pending", ignoreCase = true)) {
                val isSelfClaim = expense.requesterName.contains(currentRoleCode, ignoreCase = true)
                val isDeptLead = currentRoleCode.equals(expense.requesterDept, ignoreCase = true)
                val isFin = currentRoleCode.equals("FIN", ignoreCase = true)
                val isAdmin = currentRoleCode in listOf("ADMIN", "MD")

                val canApprove = when (expense.status) {
                    "Pending_Dept_Review", "Pending_Approval" -> (isDeptLead || isFin || isAdmin) && !isSelfClaim
                    "Pending_Finance", "Pending_Admin" -> (isFin || isAdmin) && !isSelfClaim
                    else -> false
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelfClaim) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFF3E0), modifier = Modifier.weight(1f)) {
                            Text(
                                "No self-approval: routed to " + (expense.alternateApprover ?: "Finance Lead"),
                                fontSize = 10.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (canApprove) {
                        OutlinedButton(
                            onClick = { onReject("Policy deviation") },
                            modifier = Modifier.weight(1f).height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Reject", fontSize = 11.sp, color = Color(0xFFC62828))
                        }
                        Button(
                            onClick = { onApprove(currentRoleCode) },
                            modifier = Modifier.weight(1f).height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Approve", fontSize = 11.sp, color = Color.White)
                        }
                    } else {
                        Text(
                            "Awaiting sign-off by: " + expense.currentApproverRole,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeDirectoryCard(emp: com.example.myapplication.data.model.EmployeeDirectoryRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = emp.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = emp.deptCode,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "${emp.roleTitle} · ${emp.email}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Skill: ${emp.skillMatrixLevel}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "ID: ${emp.employeeId}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AssignedPolicyCard(policy: com.example.myapplication.data.model.AssignedPolicyItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = policy.policyCode,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (policy.status == "Acknowledged") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = policy.status,
                        color = if (policy.status == "Acknowledged") Color(0xFF2E7D32) else Color(0xFFE65100),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = policy.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = policy.summary,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Category: ${policy.category}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Deadline: ${policy.deadline}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AssignedTrainingCard(training: com.example.myapplication.data.model.AssignedTrainingItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = training.courseCode,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (training.status == "Completed") Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                ) {
                    Text(
                        text = training.status,
                        color = if (training.status == "Completed") Color(0xFF2E7D32) else Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = training.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Trainer: ${training.trainer}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Duration: ${training.durationHours} hrs",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
