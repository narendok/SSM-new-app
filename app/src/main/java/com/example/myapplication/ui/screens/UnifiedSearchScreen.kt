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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.myapplication.data.model.ApprovalDecisionRecord
import com.example.myapplication.data.model.ControlledDoc
import com.example.myapplication.data.model.DailyTaskItem
import com.example.myapplication.data.model.PartComponent
import com.example.myapplication.data.model.PurchaseOrder
import com.example.myapplication.data.model.WireHarness
import com.example.myapplication.data.repository.ScmsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSearchScreen(
    scmsRepository: ScmsRepository,
    initialQuery: String = "",
    onRecordSelected: (Any) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedDepartment by remember { mutableStateOf("All") }
    var selectedStage by remember { mutableStateOf("All") }
    var isVoiceActive by remember { mutableStateOf(false) }
    var selectedRecordForInspect by remember { mutableStateOf<Any?>(null) }

    val parts by scmsRepository.parts.collectAsState()
    val documents by scmsRepository.documents.collectAsState()
    val purchaseOrders by scmsRepository.purchaseOrders.collectAsState()
    val tasks by scmsRepository.tasks.collectAsState()
    val approvals by scmsRepository.approvals.collectAsState()
    val harnesses by scmsRepository.harnesses.collectAsState()

    val q = searchQuery.trim().lowercase()

    // Filtered result sets
    val matchedParts = parts.filter { part ->
        (selectedDepartment == "All" || selectedDepartment == "PUR" || selectedDepartment == "EHW") &&
        (q.isEmpty() || part.mpn.lowercase().contains(q) || part.description.lowercase().contains(q) || part.category.lowercase().contains(q))
    }

    val matchedDocs = documents.filter { doc ->
        (selectedDepartment == "All" || doc.dept.equals(selectedDepartment, ignoreCase = true)) &&
        (selectedStage == "All" || doc.stage.contains(selectedStage, ignoreCase = true)) &&
        (q.isEmpty() || doc.docId.lowercase().contains(q) || doc.title.lowercase().contains(q) || doc.dept.lowercase().contains(q))
    }

    val matchedPos = purchaseOrders.filter { po ->
        (selectedDepartment == "All" || selectedDepartment == "PUR" || selectedDepartment == "FIN") &&
        (q.isEmpty() || po.poNumber.lowercase().contains(q) || po.vendorName.lowercase().contains(q) || po.status.lowercase().contains(q))
    }

    val matchedTasks = tasks.filter { task ->
        (selectedDepartment == "All" || task.dept.equals(selectedDepartment, ignoreCase = true)) &&
        (q.isEmpty() || task.title.lowercase().contains(q) || task.assignee.lowercase().contains(q) || task.dept.lowercase().contains(q))
    }

    val matchedApprovals = approvals.filter { approval ->
        (selectedDepartment == "All" || approval.department.equals(selectedDepartment, ignoreCase = true)) &&
        (q.isEmpty() || approval.title.lowercase().contains(q) || approval.requester.lowercase().contains(q) || approval.id.lowercase().contains(q))
    }

    val matchedHarnesses = harnesses.filter { h ->
        (selectedDepartment == "All" || selectedDepartment == "MECH") &&
        (q.isEmpty() || h.assemblyId.lowercase().contains(q) || h.name.lowercase().contains(q))
    }

    val totalMatches = matchedParts.size + matchedDocs.size + matchedPos.size + matchedTasks.size + matchedApprovals.size + matchedHarnesses.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // FIXED TOP SEARCH BAR CONTAINER
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Fixed Search Box with Voice Control
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isVoiceActive) "Listening... speak now" else "Search parts, docs, POs, tasks, approvals...",
                            fontSize = 13.sp,
                            color = if (isVoiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    isVoiceActive = !isVoiceActive
                                    if (isVoiceActive && searchQuery.isEmpty()) {
                                        searchQuery = "STM32" // Voice trigger simulated query
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isVoiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isVoiceActive) Icons.Default.Mic else Icons.Default.MicOff,
                                            contentDescription = "Voice Search",
                                            tint = if (isVoiceActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isVoiceActive) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Voice Command Active · Simulating input: 'STM32'",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FILTER CHIPS: Record Type Filter Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text(
                            text = "Filter:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    val categories = listOf(
                        "All" to "All ($totalMatches)",
                        "Parts" to "Parts (${matchedParts.size})",
                        "Docs" to "Docs (${matchedDocs.size})",
                        "Orders" to "POs (${matchedPos.size})",
                        "Tasks" to "Tasks (${matchedTasks.size})",
                        "Approvals" to "Approvals (${matchedApprovals.size})",
                        "Harness" to "Harnesses (${matchedHarnesses.size})"
                    )
                    items(categories) { (cat, label) ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)) else null,
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Department & Stage quick filters
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dept:",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    listOf("All", "EHW", "PUR", "MECH", "QA", "EMF", "FIN").forEach { dept ->
                        val isDept = selectedDepartment == dept
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDept) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            border = if (!isDept) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
                            modifier = Modifier.clickable { selectedDepartment = dept }
                        ) {
                            Text(
                                text = dept,
                                fontSize = 10.sp,
                                fontWeight = if (isDept) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDept) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // RESULTS CONTENT FEED
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Summary count banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (q.isEmpty()) "Browsing All Department Records" else "Search Results for '$searchQuery'",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$totalMatches found",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // PARTS RESULTS
            if ((selectedCategory == "All" || selectedCategory == "Parts") && matchedParts.isNotEmpty()) {
                item {
                    SearchGroupHeader(title = "Parts & Materials", count = matchedParts.size, icon = Icons.Default.Inventory)
                }
                items(matchedParts) { part ->
                    SearchResultCard(
                        tag = "PART",
                        title = part.mpn,
                        subtitle = part.description,
                        meta1 = "Category: ${part.category} · Bin: ${part.binCoordinate}",
                        meta2 = "Stock: ${part.onHand} ${part.uom}",
                        status = if (part.onHand <= part.reorderBuffer) "Low Stock" else "In Stock",
                        statusColor = if (part.onHand <= part.reorderBuffer) Color(0xFFC62828) else Color(0xFF2E7D32),
                        onClick = { selectedRecordForInspect = part }
                    )
                }
            }

            // DOCUMENTS RESULTS
            if ((selectedCategory == "All" || selectedCategory == "Docs") && matchedDocs.isNotEmpty()) {
                item {
                    SearchGroupHeader(title = "Controlled Documents", count = matchedDocs.size, icon = Icons.Default.Description)
                }
                items(matchedDocs) { doc ->
                    SearchResultCard(
                        tag = "DOC",
                        title = doc.docId,
                        subtitle = doc.title,
                        meta1 = "Dept: ${doc.dept} · Stage: ${doc.stage} · Owner: ${doc.owner}",
                        meta2 = "Rev: ${doc.revision}",
                        status = doc.status,
                        statusColor = if (doc.status == "Approved") Color(0xFF2E7D32) else Color(0xFF1565C0),
                        onClick = { selectedRecordForInspect = doc }
                    )
                }
            }

            // PURCHASE ORDERS RESULTS
            if ((selectedCategory == "All" || selectedCategory == "Orders") && matchedPos.isNotEmpty()) {
                item {
                    SearchGroupHeader(title = "Purchase Orders", count = matchedPos.size, icon = Icons.Default.ShoppingCart)
                }
                items(matchedPos) { po ->
                    SearchResultCard(
                        tag = "PO",
                        title = po.poNumber,
                        subtitle = po.vendorName,
                        meta1 = "ETA: ${po.expectedDelivery} · Issued: ${po.issueDate}",
                        meta2 = "$${po.totalAmountUsd}",
                        status = po.status,
                        statusColor = Color(0xFF1565C0),
                        onClick = { selectedRecordForInspect = po }
                    )
                }
            }

            // TASKS RESULTS
            if ((selectedCategory == "All" || selectedCategory == "Tasks") && matchedTasks.isNotEmpty()) {
                item {
                    SearchGroupHeader(title = "Daily Operational Tasks", count = matchedTasks.size, icon = Icons.Default.Assignment)
                }
                items(matchedTasks) { task ->
                    SearchResultCard(
                        tag = "TASK",
                        title = task.id,
                        subtitle = task.title,
                        meta1 = "Dept: ${task.dept} · Assignee: ${task.assignee} · Due: ${task.dueDate}",
                        meta2 = "Priority: ${task.priority}",
                        status = if (task.isCompleted) "Completed" else task.lane,
                        statusColor = if (task.isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100),
                        onClick = { selectedRecordForInspect = task }
                    )
                }
            }

            // APPROVALS RESULTS
            if ((selectedCategory == "All" || selectedCategory == "Approvals") && matchedApprovals.isNotEmpty()) {
                item {
                    SearchGroupHeader(title = "Approvals & Decision Gates", count = matchedApprovals.size, icon = Icons.Default.Gavel)
                }
                items(matchedApprovals) { approval ->
                    SearchResultCard(
                        tag = "APPR",
                        title = approval.id,
                        subtitle = approval.title,
                        meta1 = "Dept: ${approval.department} · Requester: ${approval.requester}",
                        meta2 = "Risk: ${approval.riskLevel}",
                        status = approval.outcome,
                        statusColor = when (approval.outcome) {
                            "Approved" -> Color(0xFF2E7D32)
                            "Pending" -> Color(0xFFC62828)
                            else -> Color(0xFFE65100)
                        },
                        onClick = { selectedRecordForInspect = approval }
                    )
                }
            }

            // WIRE HARNESSES RESULTS
            if ((selectedCategory == "All" || selectedCategory == "Harness") && matchedHarnesses.isNotEmpty()) {
                item {
                    SearchGroupHeader(title = "Wire Harness Assemblies", count = matchedHarnesses.size, icon = Icons.Default.Inventory)
                }
                items(matchedHarnesses) { h ->
                    SearchResultCard(
                        tag = "HARN",
                        title = h.assemblyId,
                        subtitle = h.name,
                        meta1 = "Pins: ${h.pinCount} · Gauge: ${h.wireGauge} · Ingress: ${h.ipRating}",
                        meta2 = "Project: ${h.projectId}",
                        status = h.status,
                        statusColor = Color(0xFF2E7D32),
                        onClick = { selectedRecordForInspect = h }
                    )
                }
            }

            if (totalMatches == 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No records match '$searchQuery'", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Try searching for STM32, Quectel, PO, AIS-140, or clear filters.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
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
fun SearchGroupHeader(title: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(6.dp))
        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
        }
    }
}

@Composable
fun SearchResultCard(
    tag: String,
    title: String,
    subtitle: String,
    meta1: String,
    meta2: String,
    status: String,
    statusColor: Color,
    onClick: () -> Unit
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
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(meta1, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(6.dp))
                Text(meta2, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
