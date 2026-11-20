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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.PurchaseOrder
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.PurchasingBomReviewItem
import com.example.myapplication.data.model.SharedAccess
import com.example.myapplication.data.model.SmartBomItem
import com.example.myapplication.data.model.VendorRecord
import com.example.myapplication.data.repository.ScmsRepository
import com.example.myapplication.data.repository.WhatIfSimulationResult
import kotlinx.serialization.json.JsonObject

@Composable
fun SmartBomAndPurchasingScreen(
    scmsRepository: ScmsRepository,
    isAuthenticated: Boolean = false,
    sharedSnapshot: JsonObject? = null,
    initialSubTab: Int = 0,
    visibleTabs: List<Int>? = null
) {
    val currentRole by scmsRepository.currentRole.collectAsState()
    val revisions by scmsRepository.bomRevisions.collectAsState()
    val activeRevisionId by scmsRepository.activeBomRevisionId.collectAsState()
    val selectedProject by scmsRepository.selectedProject.collectAsState()
    val purchasingBom by scmsRepository.purchasingBom.collectAsState()
    val approvedVendors by scmsRepository.approvedVendors.collectAsState()
    val purchaseOrders by scmsRepository.purchaseOrders.collectAsState()
    val serverBomRows = remember(sharedSnapshot, isAuthenticated) {
        if (!isAuthenticated) emptyList() else {
            val membership = sharedSnapshot?.get("membership") as? JsonObject
            val memberRole = membership?.let { SharedAccess.text(it, "role") }.orEmpty()
            val memberDepartmentCode = membership?.let { SharedAccess.text(it, "department_code") }.orEmpty()
            val permittedDepartmentId = SharedAccess.departments(sharedSnapshot)
                .firstOrNull { SharedAccess.text(it, "code").equals(memberDepartmentCode, ignoreCase = true) }
                ?.let { SharedAccess.text(it, "id") }
                .orEmpty()

            SharedAccess.rows(sharedSnapshot, "records").filter { row ->
                val isPurchasingRecord = SharedAccess.text(row, "record_type") in listOf("BOM", "Purchase order", "Supplier")
                val departmentMatches = memberRole == "admin" ||
                    (permittedDepartmentId.isNotBlank() && SharedAccess.text(row, "department_id") == permittedDepartmentId)
                isPurchasingRecord && departmentMatches
            }
        }
    }

    val membership = sharedSnapshot?.get("membership") as? JsonObject
    val memberRole = membership?.let { SharedAccess.text(it, "role") }.orEmpty()
    val activeDepartmentCode = if (isAuthenticated) {
        membership?.let { SharedAccess.text(it, "department_code") }.orEmpty()
    } else currentRole.code
    val canSeeAllPurchasingAreas = if (isAuthenticated) memberRole == "admin" else currentRole == DepartmentRole.ADMIN
    val rolePermittedTabs = when {
        canSeeAllPurchasingAreas -> listOf(0, 1, 2, 3)
        activeDepartmentCode.equals("PUR", ignoreCase = true) -> listOf(1, 2, 3)
        activeDepartmentCode.equals("EHW", ignoreCase = true) ||
            activeDepartmentCode.equals("MECH", ignoreCase = true) ||
            activeDepartmentCode.equals("EMF", ignoreCase = true) -> listOf(0)
        else -> emptyList()
    }
    val tabsToShow = (visibleTabs ?: rolePermittedTabs).filter { it in rolePermittedTabs }.distinct()

    if (tabsToShow.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Not part of this department workspace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "BOM and Purchasing views are available only through an explicitly mapped department profile. Your current profile has no access to these routes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedTopTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) } // 0: Smart BOM Engine, 1: Purchasing SCM Review, 2: AVL, 3: POs
    var showWhatIfModal by remember { mutableStateOf(false) }
    var showEcoModal by remember { mutableStateOf(false) }
    var showDiffModal by remember { mutableStateOf(false) }

    val activeRev = revisions.find { it.revisionId == activeRevisionId } ?: revisions.first()

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row: compact 1-line labels (rendered only if more than 1 tab)
        if (tabsToShow.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = tabsToShow.indexOf(selectedTopTab).coerceAtLeast(0),
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                tabsToShow.forEach { tabIdx ->
                    val tabName = when (tabIdx) {
                        0 -> "Dual BOM"
                        1 -> "Purchasing"
                        2 -> "Vendors (AVL)"
                        3 -> "POs"
                        else -> "Tab $tabIdx"
                    }
                    Tab(
                        selected = selectedTopTab == tabIdx,
                        onClick = { selectedTopTab = tabIdx },
                        text = { Text(tabName, fontWeight = FontWeight.Bold, maxLines = 1) }
                    )
                }
            }
        }

        when (selectedTopTab) {
            0 -> {
                // Smart BOM Content
                LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header & Revision Selector
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BOM Revision Architecture",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = if (activeRev.isLocked) Color(0xFFC8E6C9) else Color(0xFFFFF9C4),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (activeRev.isLocked) "Local Baseline" else "Exploratory Draft",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeRev.isLocked) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                revisions.forEach { rev ->
                                    FilterChip(
                                        selected = rev.revisionId == activeRevisionId,
                                        onClick = { scmsRepository.setActiveBomRevision(rev.revisionId) },
                                        label = { Text(rev.revisionId, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { showWhatIfModal = true },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Simulate", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { showDiffModal = true },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Difference, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Diff", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                                if (!activeRev.isLocked) {
                                    Button(
                                        onClick = { showEcoModal = true },
                                        modifier = Modifier.weight(1.1f).height(32.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Freeze ECO", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                // BOM Table Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BOM Items (${activeRev.items.size} lines) | Rev: ${activeRev.revisionId}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val totalUnitCost = activeRev.items.sumOf { it.qtyPerUnit * it.unitCostUsd }
                        Text(
                            text = "Unit Cost: $${String.format("%.3f", totalUnitCost)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (isAuthenticated) {
                    val liveBoms = serverBomRows.filter { SharedAccess.text(it, "record_type") == "BOM" }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Shared TEST BOM Records (${liveBoms.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Scoped by server membership on TEST Supabase; local demo Dual BOM lines are hidden.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (liveBoms.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "No BOM records returned for this TEST account.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(liveBoms) { row ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${SharedAccess.text(row, "reference").ifBlank { "BOM" }} · ${SharedAccess.text(row, "title")}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = SharedAccess.text(row, "status").ifBlank { "Active" },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Stage: ${SharedAccess.text(row, "stage").ifBlank { "Draft" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                // BOM Items
                items(activeRev.items) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.itemNo}. ${item.mpn}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (item.isProvisional) {
                                    Surface(
                                        color = Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "⚠ Provisional Candidate",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${item.aecRating} (Sample Spec)",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Text(text = item.description, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "RefDes: ${item.refDes} | Qty: ${item.qtyPerUnit} pcs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${item.unitCostUsd}/pc | LT: ${item.leadTimeWeeks}w",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            item.substituteMpn?.let { sub ->
                                Text(
                                    text = "Approved Alternate: $sub",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1565C0)
                                )
                            }
                        }
                    }
                }
            }
            }
            }
            1 -> {
                // Purchasing SCM Review
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Purchasing SCM R&D BOM Review (§4 of Knowledge Base)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D47A1)
                                )
                                Text(
                                    text = "Evaluates R&D target costs against supplier quotations for HLPL-AI4T-01. Flags long lead times and establishes dual-source alternates.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1565C0)
                                )
                            }
                        }
                    }

                    items(purchasingBom) { line ->
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
                                        text = line.mpn,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (line.isLongLead) {
                                        Surface(
                                            color = Color(0xFFFFEBEE),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "⚠ Long Lead (${line.leadTimeWeeks}w)",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFC62828),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Lead Time: ${line.leadTimeWeeks}w",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }
                                Text(text = line.description, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Target: $${line.targetCost} -> Quoted: $${line.quotedCost}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Savings: ${line.costSavingsPct}% (-$${String.format("%.2f", -line.costDelta)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Supplier: ${line.primarySupplier} | Dual-Source: ${line.dualSourceAlternate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                // Approved Vendor List (AVL)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Approved Vendor List (AVL / ASL Register)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (isAuthenticated) {
                                        "Automotive Franchised Distributors & Tier-1 Suppliers (Shared TEST)."
                                    } else {
                                        "Automotive Franchised Distributors & Tier-1 Suppliers (Local TEST)."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    if (isAuthenticated) {
                        val liveSuppliers = serverBomRows.filter { SharedAccess.text(it, "record_type") == "Supplier" }
                        item {
                            Text(
                                text = "Shared TEST Suppliers (${liveSuppliers.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (liveSuppliers.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "No Supplier records permitted for this TEST account.",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(liveSuppliers) { row ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${SharedAccess.text(row, "reference").ifBlank { "SUP" }} · ${SharedAccess.text(row, "title")}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                                                Text(
                                                    text = SharedAccess.text(row, "status").ifBlank { "Active" },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Stage: ${SharedAccess.text(row, "stage").ifBlank { "AVL Review" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                    items(approvedVendors) { vendor ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = vendor.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Code: ${vendor.vendorCode} | Origin: ${vendor.country}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = if (vendor.aslStatus == "Verified ASL") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = vendor.aslStatus,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (vendor.aslStatus == "Verified ASL") Color(0xFF2E7D32) else Color(0xFFE65100)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(text = "Category: ${vendor.category}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Cert: ${vendor.isoCertification}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Risk: ${vendor.riskRating} | ${vendor.paymentTerms}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
            3 -> {
                // Purchase Orders (PO)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Procurement Purchase Orders",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Tracking Active Inward Deliveries and Pilot Build Releases for HLPL-AI4T-01.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    if (isAuthenticated) {
                        val livePos = serverBomRows.filter { SharedAccess.text(it, "record_type") == "Purchase order" }
                        item {
                            Text(
                                text = "Shared TEST Purchase Orders (${livePos.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (livePos.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "No Purchase order records permitted for this TEST account.",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(livePos) { row ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${SharedAccess.text(row, "reference").ifBlank { "PO" }} · ${SharedAccess.text(row, "title")}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                                                Text(
                                                    text = SharedAccess.text(row, "status").ifBlank { "Open" },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Stage: ${SharedAccess.text(row, "stage").ifBlank { "Procurement" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                    items(purchaseOrders) { po ->
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = po.poNumber,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Vendor: ${po.vendorName}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Surface(
                                        color = when (po.status) {
                                            "Approved" -> Color(0xFFE8F5E9)
                                            "Partially Inwarded" -> Color(0xFFE3F2FD)
                                            else -> Color(0xFFFFF3E0)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = po.status,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (po.status) {
                                                "Approved" -> Color(0xFF2E7D32)
                                                "Partially Inwarded" -> Color(0xFF1976D2)
                                                else -> Color(0xFFE65100)
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Issued: ${po.issueDate} | ETA: ${po.expectedDelivery}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$${po.totalAmountUsd} (${po.lineItemsCount} lines)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
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

    // What-If Simulation Dialog
    if (showWhatIfModal) {
        WhatIfSimulatorDialog(
            scmsRepository = scmsRepository,
            onDismiss = { showWhatIfModal = false }
        )
    }

    // Revision Diff Dialog
    if (showDiffModal) {
        AlertDialog(
            onDismissRequest = { showDiffModal = false },
            title = { Text("Revision Diff Engine (§1.B)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Comparing Base [Rev A ECO-2026-004] vs Target [v0.3-tentative]:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "+1 Added Part: C0603C104K5RACTU (0.1uF 50V Decoupling Array)",
                            modifier = Modifier.padding(6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "-1 Removed Part: GRM188R71H104KA93D (Murata Decoupling)",
                            modifier = Modifier.padding(6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC62828)
                        )
                    }
                    Text(
                        text = "Net Unit Cost Delta: -$0.020 per board unit savings",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiffModal = false }) { Text("Close") }
            }
        )
    }

    // ECO Promotion Gate Dialog (With Strict Provisional Check)
    if (showEcoModal) {
        var ecoNumberInput by remember { mutableStateOf("ECO-2026-005") }
        var rationaleInput by remember { mutableStateOf("Transition to validated pilot production baseline") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEcoModal = false },
            title = { Text("Governed ECO Promotion Gate (§1.B)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "IATF 16949 §8.3.6 Invariant: Baseline freeze requires formal ECO. Any unverified provisional candidate components are strictly blocked from release.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = ecoNumberInput,
                        onValueChange = { ecoNumberInput = it },
                        label = { Text("ECO Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rationaleInput,
                        onValueChange = { rationaleInput = it },
                        label = { Text("Engineering Rationale") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let {
                        Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "⚠ $it",
                                modifier = Modifier.padding(8.dp),
                                color = Color(0xFFC62828),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = scmsRepository.promoteBomRevision(
                            targetRevision = "Rev B $ecoNumberInput",
                            ecoNumber = ecoNumberInput,
                            rationale = rationaleInput
                        )
                        result.fold(
                            onSuccess = { showEcoModal = false },
                            onFailure = { errorMessage = it.message }
                        )
                    }
                ) {
                    Text("Confirm ECO Freeze")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEcoModal = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun WhatIfSimulatorDialog(
    scmsRepository: ScmsRepository,
    onDismiss: () -> Unit
) {
    var buildQty by remember { mutableIntStateOf(5000) }
    var simulationResult by remember { mutableStateOf(scmsRepository.runWhatIfSimulation(buildQty)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dynamic 'What-If' Stock Simulator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Simulates component requirements against active released inventory without physical stock reservation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Build Target:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                buildQty = 1000
                                simulationResult = scmsRepository.runWhatIfSimulation(buildQty)
                            },
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) { Text("1,000") }
                        Button(
                            onClick = {
                                buildQty = 5000
                                simulationResult = scmsRepository.runWhatIfSimulation(buildQty)
                            },
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) { Text("5,000") }
                    }
                }

                Surface(
                    color = if (simulationResult.shortageLinesCount > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = if (simulationResult.shortageLinesCount > 0) {
                                "⚠ Shortage on ${simulationResult.shortageLinesCount} line(s)!"
                            } else {
                                "✓ 100% Stock Coverage Available"
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (simulationResult.shortageLinesCount > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Total Deficit Cost: $${String.format("%.2f", simulationResult.totalDeficitUsd)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(simulationResult.lineStatus) { line ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            color = if (line.isShortage) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = line.mpn, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (line.isShortage) "Deficit: -${line.deficitQty} pcs" else "Buffer: +${line.availableReleasedQty - line.requiredQty}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (line.isShortage) Color(0xFFC62828) else Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Req: ${line.requiredQty} | Avail Released: ${line.availableReleasedQty}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun SmartBomAndPurchasingScreenPreview() {
    com.example.myapplication.ui.theme.MyApplicationTheme {
        SmartBomAndPurchasingScreen(
            scmsRepository = com.example.myapplication.data.repository.ScmsRepositoryImpl()
        )
    }
}

