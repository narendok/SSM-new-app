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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.myapplication.data.model.ApprovalCategory
import com.example.myapplication.data.model.ApprovalDecisionRecord
import com.example.myapplication.data.model.TravelPolicyRule
import com.example.myapplication.data.repository.ScmsRepository
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ApprovalsScreen(
    scmsRepository: ScmsRepository,
    onBack: (() -> Unit)? = null
) {
    val approvals by scmsRepository.approvals.collectAsState()
    val travelPolicyRules by scmsRepository.travelPolicyRules.collectAsState()
    val currentRole by scmsRepository.currentRole.collectAsState()

    var statusFilter by remember { mutableStateOf("ALL") }
    var selectedCategory by remember { mutableStateOf<ApprovalCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedApprovalForAction by remember { mutableStateOf<ApprovalDecisionRecord?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "APPROVE", "REJECT", "RETURN"
    var actionReasonNotes by remember { mutableStateOf("") }
    var showTravelPolicyDetailModal by remember { mutableStateOf(false) }

    val filteredApprovals = approvals.filter { record ->
        val matchesStatus = (statusFilter == "ALL") || record.outcome.equals(statusFilter, ignoreCase = true)
        val matchesCategory = (selectedCategory == null) || (record.category == selectedCategory)
        val matchesQuery = if (searchQuery.isBlank()) true else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            record.title.lowercase(Locale.ROOT).contains(q) ||
                    record.recordId.lowercase(Locale.ROOT).contains(q) ||
                    record.requester.lowercase(Locale.ROOT).contains(q) ||
                    record.project.lowercase(Locale.ROOT).contains(q) ||
                    record.expenseReasonDetails.lowercase(Locale.ROOT).contains(q) ||
                    record.department.lowercase(Locale.ROOT).contains(q)
        }
        matchesStatus && matchesCategory && matchesQuery
    }

    val pendingCount = approvals.count { it.outcome == "Pending" }
    val approvedCount = approvals.count { it.outcome == "Approved" }
    val rejectedReturnedCount = approvals.count { it.outcome == "Rejected" || it.outcome == "Returned" }

    val inrFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Header Bar with Signer Context
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) {
                            IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Approvals & Governance Center",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Department Sign-offs & Authorizations",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    // Signer Context Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${currentRole.code} Lead: ${currentRole.leadName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 10.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 2. Summary Metric Cards Strip
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ApprovalMetricPill(
                            title = "Pending",
                            count = pendingCount,
                            color = Color(0xFFF57C00),
                            isSelected = statusFilter == "Pending",
                            onClick = { statusFilter = if (statusFilter == "Pending") "ALL" else "Pending" }
                        )
                    }
                    item {
                        ApprovalMetricPill(
                            title = "Approved",
                            count = approvedCount,
                            color = Color(0xFF2E7D32),
                            isSelected = statusFilter == "Approved",
                            onClick = { statusFilter = if (statusFilter == "Approved") "ALL" else "Approved" }
                        )
                    }
                    item {
                        ApprovalMetricPill(
                            title = "Rejected / Rework",
                            count = rejectedReturnedCount,
                            color = Color(0xFFC62828),
                            isSelected = statusFilter == "Rejected",
                            onClick = { statusFilter = if (statusFilter == "Rejected") "ALL" else "Rejected" }
                        )
                    }
                    item {
                        Surface(
                            modifier = Modifier.clickable { showTravelPolicyDetailModal = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE0F7FA),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF80DEEA))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlightTakeoff,
                                    contentDescription = null,
                                    tint = Color(0xFF00838F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Travel Policy: Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF006064)
                                    )
                                    Text(
                                        text = "Web Configured Rules",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF00838F)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Category Filter Chips Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Filter by Approval Scope:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            Surface(
                                modifier = Modifier.clickable { selectedCategory = null },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedCategory == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selectedCategory == null) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Text(
                                    text = "All Types (${approvals.size})",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategory == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        items(ApprovalCategory.values()) { cat ->
                            val isSelected = selectedCategory == cat
                            val count = approvals.count { it.category == cat }
                            Surface(
                                modifier = Modifier.clickable {
                                    selectedCategory = if (isSelected) null else cat
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(cat.badgeColorHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(cat.badgeColorHex) else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(cat.badgeColorHex))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${cat.label} ($count)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(cat.badgeColorHex) else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Live Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by title, PO, traveler, project or expense reason...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            // 5. Approvals List
            if (filteredApprovals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Approvals Matching Criteria",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "All requested transactions in this filter have been signed off or cleared.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredApprovals, key = { it.id }) { item ->
                    ApprovalRecordCard(
                        record = item,
                        inrFormat = inrFormat,
                        onApprove = {
                            selectedApprovalForAction = item
                            actionType = "APPROVE"
                            actionReasonNotes = "Sign-off verified and approved."
                        },
                        onReject = {
                            selectedApprovalForAction = item
                            actionType = "REJECT"
                            actionReasonNotes = ""
                        },
                        onReturn = {
                            selectedApprovalForAction = item
                            actionType = "RETURN"
                            actionReasonNotes = ""
                        }
                    )
                }
            }
        }
    }

    // Modal Action Confirmation Dialog
    selectedApprovalForAction?.let { target ->
        AlertDialog(
            onDismissRequest = {
                selectedApprovalForAction = null
                actionType = null
                actionReasonNotes = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (actionType) {
                            "APPROVE" -> Icons.Default.CheckCircle
                            "REJECT" -> Icons.Default.Warning
                            else -> Icons.Default.Undo
                        },
                        contentDescription = null,
                        tint = when (actionType) {
                            "APPROVE" -> Color(0xFF2E7D32)
                            "REJECT" -> Color(0xFFC62828)
                            else -> Color(0xFFE65100)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (actionType) {
                            "APPROVE" -> "Approve Governance Gate"
                            "REJECT" -> "Formally Reject Gate"
                            else -> "Return for Rework"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = target.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ref: ${target.recordId} · Department: ${target.department} · Project: ${target.project}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (target.amountInr > 0) {
                        Text(
                            text = "Financial Impact: ${inrFormat.format(target.amountInr)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    OutlinedTextField(
                        value = actionReasonNotes,
                        onValueChange = { actionReasonNotes = it },
                        label = {
                            Text(
                                text = when (actionType) {
                                    "APPROVE" -> "Approval Notes / Governance Remarks"
                                    "REJECT" -> "Mandatory Rejection Root Cause Reason"
                                    else -> "Specific Rework Instructions for Teammate"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Text(
                        text = "Action will be cryptographically attributed to ${currentRole.leadName} (${currentRole.title}) in the immutable audit log.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (actionType) {
                            "APPROVE" -> scmsRepository.approveGate(target.id, actionReasonNotes)
                            "REJECT" -> scmsRepository.rejectGate(target.id, actionReasonNotes)
                            "RETURN" -> scmsRepository.returnGate(target.id, actionReasonNotes)
                        }
                        selectedApprovalForAction = null
                        actionType = null
                        actionReasonNotes = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (actionType) {
                            "APPROVE" -> Color(0xFF2E7D32)
                            "REJECT" -> Color(0xFFC62828)
                            else -> Color(0xFFE65100)
                        }
                    ),
                    enabled = (actionType == "APPROVE") || actionReasonNotes.isNotBlank()
                ) {
                    Text(
                        text = when (actionType) {
                            "APPROVE" -> "Confirm Sign-off"
                            "REJECT" -> "Confirm Rejection"
                            else -> "Return to Teammate"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedApprovalForAction = null
                    actionType = null
                    actionReasonNotes = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Travel Policy Rule Details Modal
    if (showTravelPolicyDetailModal) {
        AlertDialog(
            onDismissRequest = { showTravelPolicyDetailModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlightTakeoff,
                        contentDescription = null,
                        tint = Color(0xFF00838F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Customizable Travel & Expense Policy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color(0xFFE0F7FA),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Policy Configuration Authority: Web Portal (§26.3). Policy limits and thresholds are defined on the web portal by Corporate HR/OPS & Finance and verified automatically on mobile companion.",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF006064),
                            fontSize = 11.sp
                        )
                    }

                    travelPolicyRules.forEach { rule ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${rule.categoryName} (${rule.ruleId})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "• Max Daily Per-Diem: ₹${rule.maxPerDiemInr}\n" +
                                            "• Pre-Approval Trigger: Above ₹${rule.requiresPreApprovalAboveInr}\n" +
                                            "• Allowed Lodging: ${rule.allowedHotelTier}\n" +
                                            "• Advance Notice: ${rule.flightAdvanceNoticeDays} days required\n" +
                                            "• Sync Version: ${rule.webConfigVersion}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTravelPolicyDetailModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ApprovalMetricPill(
    title: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$title:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun ApprovalRecordCard(
    record: ApprovalDecisionRecord,
    inrFormat: NumberFormat,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onReturn: () -> Unit
) {
    var expandedDetails by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (record.outcome) {
                "Approved" -> Color(0xFF2E7D32).copy(alpha = 0.4f)
                "Rejected" -> Color(0xFFC62828).copy(alpha = 0.4f)
                "Returned" -> Color(0xFFE65100).copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Category Badge + Risk Level + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(record.category.badgeColorHex).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(record.category.badgeColorHex).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = record.category.label,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(record.category.badgeColorHex),
                            fontSize = 10.5.sp
                        )
                    }

                    // Risk Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (record.riskLevel) {
                            "Critical" -> Color(0xFFFFEBEE)
                            "High" -> Color(0xFFFFF3E0)
                            else -> Color(0xFFE8F5E9)
                        }
                    ) {
                        Text(
                            text = record.riskLevel.uppercase(),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (record.riskLevel) {
                                "Critical" -> Color(0xFFC62828)
                                "High" -> Color(0xFFE65100)
                                else -> Color(0xFF2E7D32)
                            },
                            fontSize = 9.5.sp
                        )
                    }
                }

                // Outcome Status Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (record.outcome) {
                        "Approved" -> Color(0xFFE8F5E9)
                        "Rejected" -> Color(0xFFFFEBEE)
                        "Returned" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFFFF8E1)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (record.outcome) {
                            "Approved" -> Color(0xFFC8E6C9)
                            "Rejected" -> Color(0xFFFFCDD2)
                            "Returned" -> Color(0xFFFFE0B2)
                            else -> Color(0xFFFFECB3)
                        }
                    )
                ) {
                    Text(
                        text = record.outcome,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (record.outcome) {
                            "Approved" -> Color(0xFF2E7D32)
                            "Rejected" -> Color(0xFFC62828)
                            "Returned" -> Color(0xFFE65100)
                            else -> Color(0xFFF57C00)
                        },
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Record ID
            Text(
                text = record.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${record.recordId} · Requester: ${record.requester} (${record.department}) · Project: ${record.project}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            // Financial Amount or Deviation Cost
            if (record.amountInr > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Amount: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = inrFormat.format(record.amountInr),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (record.vendorOrBeneficiary.isNotBlank()) {
                            Text(
                                text = " · ${record.vendorOrBeneficiary}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Description
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = record.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.5.sp
            )

            // Expandable Detailed Expense Reason & Travel Policy Notes
            AnimatedVisibility(visible = expandedDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (record.expenseReasonDetails.isNotBlank()) {
                        Surface(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Detailed Rationale & Justification:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF37474F)
                                )
                                Text(
                                    text = record.expenseReasonDetails,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF263238),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (record.category == ApprovalCategory.TRAVEL) {
                        Surface(
                            color = if (record.travelPolicyCompliant) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (record.travelPolicyCompliant) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (record.travelPolicyCompliant) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (record.travelPolicyCompliant) "Travel Policy Compliant (Web Rules)" else "Travel Policy Warning",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (record.travelPolicyCompliant) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    Text(
                                        text = record.travelPolicyNotes,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // IATF Clause & Signer Metadata
                    Text(
                        text = "Governance Standard: ${record.iatfClause}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp
                    )

                    if (record.signedBy != null) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Signed by: ${record.signedBy} at ${record.signedAt}\nNotes: ${record.comments}",
                                modifier = Modifier.padding(6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1B5E20),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            // Footer: Toggle Details + Action Buttons (if pending)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { expandedDetails = !expandedDetails },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (expandedDetails) "Hide Details ▲" else "View Full Reason ▼",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (record.outcome == "Pending") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onReturn,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Rework", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onReject,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Reject", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                        }

                        Button(
                            onClick = onApprove,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Approve", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
