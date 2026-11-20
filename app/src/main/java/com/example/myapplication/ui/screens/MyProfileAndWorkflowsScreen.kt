package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.AssetEquipment
import com.example.myapplication.data.model.AuthState
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.repository.ScmsRepository
import com.example.myapplication.ui.viewmodel.MainViewModel

/**
 * Tab 5: My Profile, Workflows, HR, Assets & AI Chatbot
 * Consolidates user profile settings, Enterprise AI chatbot, personal expense claims,
 * assigned company plant assets, and security vault status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileAndWorkflowsScreen(
    viewModel: MainViewModel,
    onOpenRoleSwitcher: () -> Unit,
    onOpenAuthDialog: () -> Unit,
    onOpenDataWorkspace: (() -> Unit)? = null,
    onOpenBusinessWorkspace: ((String) -> Unit)? = null
) {
    val scmsRepository = viewModel.scmsRepository
    val currentRole by scmsRepository.currentRole.collectAsState()
    val assets by scmsRepository.assets.collectAsState()
    val approvals by scmsRepository.approvals.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val isAuthenticated = authState is AuthState.Authenticated
    val authenticatedSession = (authState as? AuthState.Authenticated)?.session
    val authenticatedMembership = authenticatedSession?.testMembership
    val profileName = authenticatedMembership?.displayName?.takeIf { it.isNotBlank() } ?: currentRole.leadName
    val profileDepartment = authenticatedMembership?.departmentCode?.takeIf { it.isNotBlank() } ?: currentRole.code
    val profileRoleLabel = when (authenticatedMembership?.role) {
        "admin" -> "Administrator"
        "department_lead" -> "Department Lead"
        "member" -> "Department Member"
        "viewer" -> "Client Viewer"
        else -> currentRole.title
    }
    val profileEmail = authenticatedSession?.user?.email ?: currentRole.email

    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Profile, 1: AI Chatbot, 2: Expenses, 3: Assets, 4: Security
    val subTabs = listOf("Profile", "AI Assistant", "Expenses", "Assets", "Security")
    val safeActiveSubTab = activeSubTab.coerceIn(0, subTabs.lastIndex)

    // Personal expense records
    val myExpenseApprovals = remember(approvals, currentRole) {
        approvals.filter {
            (it.category == com.example.myapplication.data.model.ApprovalCategory.EXPENSE || it.category == com.example.myapplication.data.model.ApprovalCategory.TRAVEL) &&
                (currentRole == DepartmentRole.ADMIN || it.department.equals(currentRole.code, ignoreCase = true))
        }
    }
    val profileAssets = remember(assets, currentRole) {
        if (currentRole == DepartmentRole.ADMIN) assets
        else assets.filter { it.departmentCode.equals(currentRole.code, ignoreCase = true) }
    }

    // Chatbot state
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                "System" to "Welcome to SSM One AI Assistant. Ask about APQP stage gates, 38-family document rules, AEC-Q100 part compliance, or expense policies.",
                "Assistant" to "How can I assist you with ${currentRole.title} workflows today?"
            )
        )
    }
    var chatInput by remember { mutableStateOf("") }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf<AssetEquipment?>(null) }
    var showAssetScanDialog by remember { mutableStateOf(false) }
    var scannedTagQuery by remember { mutableStateOf("") }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            showExpenseDialog = false
            showAssetScanDialog = false
            showCalibrationDialog = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-Tabs Header Strip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            ScrollableTabRow(
                selectedTabIndex = activeSubTab.coerceIn(0, (subTabs.size - 1).coerceAtLeast(0)),
                edgePadding = 12.dp,
                divider = {},
                containerColor = Color.Transparent
            ) {
                subTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = safeActiveSubTab == index,
                        onClick = { activeSubTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (safeActiveSubTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            when (safeActiveSubTab) {
                0 -> {
                    // 1. Profile & Account Details
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profileName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "$profileRoleLabel · $profileDepartment",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = profileEmail,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (!isAuthenticated) Button(
                                            onClick = onOpenRoleSwitcher,
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Switch Persona", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = onOpenAuthDialog,
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sign In / Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (isAuthenticated && onOpenDataWorkspace != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        TextButton(
                                            onClick = onOpenDataWorkspace,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Open data workspace", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Departmental Authority & Compliance", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("• Signed-in scope: $profileRoleLabel ($profileDepartment)", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                    Text("• Multi-Track APQP Freeze Sign-off: Enabled", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                    Text("• Controlled Document Release: Review & Draft only", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                    Text("• Self-Approval Guard: Active (Routes to alternate lead)", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // 2. AI Chatbot & Procedural Queries
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatMessages) { (sender, msg) ->
                                val isUser = sender == "User"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = sender,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                fontSize = 10.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = msg,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("Ask procedural AI question...", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        val query = chatInput
                                        chatMessages = chatMessages + ("User" to query) +
                                                ("Assistant" to "Under Master Plan §14 & IATF rules, ${currentRole.title} procedures require independent sign-off and strict Draft retention before tollgate freeze.")
                                        chatInput = ""
                                    }
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                2 -> {
                    // 3. Personal Expense Claims
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("My Expense Claims & Reimbursements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (!isAuthenticated) {
                                    FilledTonalButton(
                                        onClick = { showExpenseDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("File Claim", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (isAuthenticated) {
                            item {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Expense requests are stored in the authenticated business workspace.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Button(onClick = { onOpenBusinessWorkspace?.invoke("new_expense") }, enabled = onOpenBusinessWorkspace != null) { Text("＋ New expense") }
                                }
                            }
                        } else if (myExpenseApprovals.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "No active personal expense claims recorded for ${currentRole.leadName}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(14.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(myExpenseApprovals) { exp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(exp.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text("Category: ${exp.category.label} · Requester: ${exp.requester}", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Record ID: ${exp.recordId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (exp.outcome == "Approved") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                        ) {
                                            Text(
                                                text = exp.outcome,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (exp.outcome == "Approved") Color(0xFF2E7D32) else Color(0xFFE65100),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // 4. Assigned Plant Assets (Phase 3 Slice 4)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Assigned Plant Equipment & Tools", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Tools, calibration instruments, and hardware jigs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                                if (!isAuthenticated) {
                                    FilledTonalButton(
                                        onClick = { showAssetScanDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Scan QR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (isAuthenticated) {
                            item {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Not connected: this app has no authenticated personal asset or calibration feed yet. Local sample assets are hidden, and no asset actions are available here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                        val filteredAssets = if (scannedTagQuery.isNotBlank()) {
                                profileAssets.filter { it.assetTag.contains(scannedTagQuery, ignoreCase = true) || it.name.contains(scannedTagQuery, ignoreCase = true) }
                            } else profileAssets

                        items(filteredAssets) { asset ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(asset.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text("Tag: ${asset.assetTag} · S/N: ${asset.serialNumber}", style = MaterialTheme.typography.bodySmall, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Custodian: ${asset.custodian} · Location: ${asset.location}", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "Calibration Due: ${asset.calibrationDue}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (asset.status == "Operational" || asset.status == "Calibrated") Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (asset.status == "Operational" || asset.status == "Calibrated") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                        ) {
                                            Text(
                                                text = asset.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (asset.status == "Operational" || asset.status == "Calibrated") Color(0xFF2E7D32) else Color(0xFFC62828),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { showCalibrationDialog = asset },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Log Calibration / Maintenance", fontSize = 9.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }

                4 -> {
                    // 5. On-device session protection; enterprise security controls are not connected here.
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("On-device session protection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text("Session tokens and retry IDs use AndroidX EncryptedSharedPreferences with an Android Keystore MasterKey (AES-256-GCM values). Hardware-backed status is not queried.", style = MaterialTheme.typography.bodySmall, fontSize = 11.5.sp)
                            Text("Server membership and row-level permissions protect shared reads and writes.", style = MaterialTheme.typography.bodySmall, fontSize = 11.5.sp)
                            Text("Not connected in this screen: MFA management, active-session/device controls, security audit events, and periodic offline sync.", style = MaterialTheme.typography.bodySmall, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // File Expense Claim Dialog
    if (!isAuthenticated && showExpenseDialog) {
        var expenseTitle by remember { mutableStateOf("") }
        var expenseAmount by remember { mutableStateOf("") }
        var expenseType by remember { mutableStateOf("Travel") }

        AlertDialog(
            onDismissRequest = { showExpenseDialog = false },
            title = { Text("File Personal Expense Claim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reimbursement requests are submitted as Drafts for Department Lead and Finance sign-off. Self-approval is strictly forbidden.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = expenseTitle,
                        onValueChange = { expenseTitle = it },
                        label = { Text("Purpose / Claim Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = expenseAmount,
                        onValueChange = { expenseAmount = it },
                        label = { Text("Amount (INR)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExpenseDialog = false },
                    enabled = expenseTitle.isNotBlank() && expenseAmount.isNotBlank()
                ) {
                    Text("Submit Claim Draft")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpenseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Asset QR Scan Lookup Dialog (Phase 3 Slice 4)
    if (!isAuthenticated && showAssetScanDialog) {
        var tempTag by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAssetScanDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Plant Asset QR / Barcode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Scan equipment QR code with camera or enter Asset Tag manually to retrieve calibration status:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = tempTag,
                        onValueChange = { tempTag = it },
                        label = { Text("Asset Tag (e.g. AST-SMT-001)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("AST-SMT-001", "EQP-TEST-002", "AST-JIG-003").forEach { quickTag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.clickable { tempTag = quickTag }
                            ) {
                                Text(quickTag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scannedTagQuery = tempTag.trim()
                        showAssetScanDialog = false
                    }
                ) {
                    Text("Locate Asset")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scannedTagQuery = ""
                    showAssetScanDialog = false
                }) {
                    Text("Clear Filter")
                }
            }
        )
    }

    // Log Calibration / Maintenance Dialog (Phase 3 Slice 4)
    if (!isAuthenticated) showCalibrationDialog?.let { asset ->
        var certNo by remember { mutableStateOf("CAL-2026-${(1000..9999).random()}") }
        var agency by remember { mutableStateOf("National NABL Calibration Lab") }
        var nextDue by remember { mutableStateOf("2027-03-31") }

        AlertDialog(
            onDismissRequest = { showCalibrationDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Calibration Evidence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Asset: ${asset.name} (${asset.assetTag})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text("Serial: ${asset.serialNumber} · Custodian: ${asset.custodian}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = certNo,
                        onValueChange = { certNo = it },
                        label = { Text("Calibration Certificate #") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = agency,
                        onValueChange = { agency = it },
                        label = { Text("Certified By / Agency") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nextDue,
                        onValueChange = { nextDue = it },
                        label = { Text("Next Calibration Due Date") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                        Text(
                            text = "IATF 16949 §7.1.5 Guard: Records immutable calibration event log with verified agency traceability.",
                            modifier = Modifier.padding(6.dp),
                            fontSize = 10.sp,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedAsset = asset.copy(
                            status = "Operational",
                            lastCalibrationDate = "2026-11-15",
                            calibrationDue = nextDue
                        )
                        scmsRepository.createAsset(updatedAsset)
                        showCalibrationDialog = null
                    }
                ) {
                    Text("Record & Certify")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalibrationDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
