package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.ProjectMetadata
import com.example.myapplication.data.repository.ScmsRepository
import com.example.myapplication.ui.components.SmartUploadModalBottomSheet
import com.example.myapplication.ui.viewmodel.MainViewModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray

/**
 * Tab 3: Central Records & Workflows Hub
 * Dynamically scopes operational records, POs, Engineering BOM, Governed Inventory,
 * and QA Ingestion Gate strictly based on the active Department Role and access level.
 */
data class DeptHubTab(
    val title: String,
    val content: @Composable () -> Unit
)

@Composable
fun DepartmentRecordsHubScreen(
    scmsRepository: ScmsRepository,
    isAuthenticated: Boolean = false,
    sharedSnapshot: JsonObject? = null,
    lifecycleWorkspace: JsonObject? = null,
    selectedProjectId: String = "HLPL-AI4T-01",
    viewDepartment: String? = null,
    mainViewModel: MainViewModel? = null,
    projectCatalog: List<ProjectMetadata> = emptyList(),
    onOpenVault: (() -> Unit)? = null,
    onOpenBusinessWorkspace: ((String) -> Unit)? = null
) {
    val currentRole by scmsRepository.currentRole.collectAsState()
    // Admin's department selector changes the displayed context only. Build a read-only,
    // in-memory projection for child screens while retaining the authenticated membership.
    val workspaceSnapshot = remember(sharedSnapshot, viewDepartment, isAuthenticated) {
        if (!isAuthenticated || viewDepartment == null || sharedSnapshot == null) sharedSnapshot
        else {
            val focusedRecords = com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "records")
                .filter { com.example.myapplication.data.model.SharedAccess.text(it, "department_id") == viewDepartment }
            val focusedTasks = com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "tasks")
                .filter { com.example.myapplication.data.model.SharedAccess.text(it, "department_id") == viewDepartment }
            val focusedDocuments = com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "documents")
                .filter { com.example.myapplication.data.model.SharedAccess.text(it, "department_id") == viewDepartment }
            val focusedApprovals = com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "approvals")
                .filter {
                    com.example.myapplication.data.model.SharedAccess.text(it, "reviewer_department_id") == viewDepartment ||
                        com.example.myapplication.data.model.SharedAccess.text(it, "department_id") == viewDepartment
                }
            JsonObject(sharedSnapshot + mapOf(
                "records" to JsonArray(focusedRecords),
                "tasks" to JsonArray(focusedTasks),
                "documents" to JsonArray(focusedDocuments),
                "approvals" to JsonArray(focusedApprovals)
            ))
        }
    }
    val focusedRole = remember(currentRole, sharedSnapshot, isAuthenticated, viewDepartment) {
        val membership = sharedSnapshot?.get("membership") as? JsonObject
        val code = when {
            viewDepartment != null -> com.example.myapplication.data.model.SharedAccess.departments(sharedSnapshot)
                .firstOrNull { com.example.myapplication.data.model.SharedAccess.text(it, "id") == viewDepartment }
                ?.let { com.example.myapplication.data.model.SharedAccess.text(it, "code") }
            isAuthenticated && membership?.let { com.example.myapplication.data.model.SharedAccess.text(it, "role") } == "admin" -> "ADMIN"
            isAuthenticated -> membership?.let { com.example.myapplication.data.model.SharedAccess.text(it, "department_code") }
            else -> currentRole.code
        }.orEmpty()
        val normalizedCode = when (code.uppercase()) {
            "HR", "OPS", "HR/OPS" -> "HROPS"
            "EFW" -> "FW"
            "CSW" -> "SW"
            "OEM" -> "CLIENT"
            else -> code.uppercase()
        }
        DepartmentRole.entries.firstOrNull { it.code.equals(normalizedCode, ignoreCase = true) } ?: DepartmentRole.CLIENT
    }

    // Department-scoped tabs: adapts dynamically to the active department's operational domain
    val currentRoleTabs = remember(focusedRole, isAuthenticated, workspaceSnapshot, lifecycleWorkspace, viewDepartment, selectedProjectId, projectCatalog) {
        when (focusedRole) {
            DepartmentRole.SAL -> listOf(
                DeptHubTab("Sales pipeline & SOR") {
                    DocumentControlScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        projectCatalog = projectCatalog,
                        selectedProjectId = selectedProjectId,
                        viewDepartment = viewDepartment,
                        sharedSnapshot = workspaceSnapshot,
                        lifecycleWorkspace = lifecycleWorkspace,
                        initialTopTab = 4
                    )
                },
                DeptHubTab("Sales documents & directory") {
                    DocumentControlScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        projectCatalog = projectCatalog,
                        selectedProjectId = selectedProjectId,
                        viewDepartment = viewDepartment,
                        sharedSnapshot = workspaceSnapshot,
                        lifecycleWorkspace = lifecycleWorkspace,
                        initialTopTab = 0
                    )
                }
            )
            DepartmentRole.CLIENT -> listOf(
                DeptHubTab("Client portal") {
                    DocumentControlScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        projectCatalog = projectCatalog,
                        selectedProjectId = selectedProjectId,
                        viewDepartment = viewDepartment,
                        sharedSnapshot = workspaceSnapshot,
                        lifecycleWorkspace = lifecycleWorkspace,
                        initialTopTab = 1
                    )
                },
                DeptHubTab("Client documents") {
                    DocumentControlScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        projectCatalog = projectCatalog,
                        selectedProjectId = selectedProjectId,
                        viewDepartment = viewDepartment,
                        sharedSnapshot = workspaceSnapshot,
                        lifecycleWorkspace = lifecycleWorkspace,
                        initialTopTab = 0
                    )
                }
            )
            DepartmentRole.PUR -> listOf(
                DeptHubTab("1. POs & Orders") {
                    SmartBomAndPurchasingScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot,
                        initialSubTab = 3,
                        visibleTabs = listOf(3)
                    )
                },
                DeptHubTab("2. Vendors (AVL)") {
                    SmartBomAndPurchasingScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot,
                        initialSubTab = 2,
                        visibleTabs = listOf(2)
                    )
                },
                DeptHubTab("3. Purchasing SCM Review") {
                    SmartBomAndPurchasingScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot,
                        initialSubTab = 1,
                        visibleTabs = listOf(1)
                    )
                },
                DeptHubTab("4. Stores & Receiving") {
                    GovernedInventoryScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot,
                        lifecycleWorkspace = lifecycleWorkspace,
                        mainViewModel = mainViewModel,
                        viewDepartment = viewDepartment,
                        selectedProjectId = selectedProjectId
                    )
                }
            )

            DepartmentRole.EHW, DepartmentRole.MECH -> listOf(
                DeptHubTab("1. Engineering Dual BOM") {
                    SmartBomAndPurchasingScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot,
                        initialSubTab = 0,
                        visibleTabs = listOf(0)
                    )
                },
                DeptHubTab("2. Lab Assets & Calibration") {
                    AdaptiveIngestionAndQualityScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot
                    )
                },
                DeptHubTab("3. Component Stores") {
                    GovernedInventoryScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot,
                        lifecycleWorkspace = lifecycleWorkspace,
                        mainViewModel = mainViewModel,
                        viewDepartment = viewDepartment,
                        selectedProjectId = selectedProjectId
                    )
                },
                DeptHubTab("4. Ingestion Gate") {
                    AdaptiveIngestionAndQualityScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot
                    )
                }
            )

            DepartmentRole.EMF -> listOf(
                DeptHubTab("1. SMT Profiles & SOPs") {
                    AdaptiveIngestionAndQualityScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot
                    )
                },
                DeptHubTab("2. Manufacturing BOM (mBOM)") {
                    SmartBomAndPurchasingScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot,
                        initialSubTab = 0,
                        visibleTabs = listOf(0)
                    )
                },
                DeptHubTab("3. Plant Stores & Feeders") {
                    GovernedInventoryScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot,
                        lifecycleWorkspace = lifecycleWorkspace,
                        mainViewModel = mainViewModel,
                        viewDepartment = viewDepartment,
                        selectedProjectId = selectedProjectId
                    )
                },
                DeptHubTab("4. Line Quality & IQC") {
                    AdaptiveIngestionAndQualityScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot
                    )
                }
            )

            DepartmentRole.QA -> listOf(
                DeptHubTab("1. Incoming IQC Inspection") {
                    AdaptiveIngestionAndQualityScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot
                    )
                },
                DeptHubTab("2. Gauge & Calibration Ledger") {
                    AdaptiveIngestionAndQualityScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot
                    )
                },
                DeptHubTab("3. Inventory Quality Audit") {
                    GovernedInventoryScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot,
                        lifecycleWorkspace = lifecycleWorkspace,
                        mainViewModel = mainViewModel,
                        viewDepartment = viewDepartment,
                        selectedProjectId = selectedProjectId
                    )
                },
                DeptHubTab("4. Ingestion Gate") {
                    AdaptiveIngestionAndQualityScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot
                    )
                }
            )

            DepartmentRole.ADMIN -> listOf(
                DeptHubTab("All BOM & Purchasing") {
                    SmartBomAndPurchasingScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot,
                        initialSubTab = 0,
                        visibleTabs = listOf(0, 1, 2, 3)
                    )
                },
                DeptHubTab("All Stores & Inventory") {
                    GovernedInventoryScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot,
                        lifecycleWorkspace = lifecycleWorkspace,
                        mainViewModel = mainViewModel,
                        viewDepartment = viewDepartment,
                        selectedProjectId = selectedProjectId
                    )
                },
                DeptHubTab("All Quality & Ingestion") {
                    AdaptiveIngestionAndQualityScreen(
                        scmsRepository = scmsRepository,
                        isAuthenticated = isAuthenticated,
                        sharedSnapshot = workspaceSnapshot
                    )
                }
            )

            else -> buildList {
                add(DeptHubTab("Department records") {
                    val rows = com.example.myapplication.data.model.SharedAccess.rows(workspaceSnapshot, "records")
                    val deptId = com.example.myapplication.data.model.SharedAccess.rows(workspaceSnapshot, "departments")
                        .firstOrNull { com.example.myapplication.data.model.SharedAccess.text(it, "code").equals(focusedRole.code, true) }
                        ?.let { com.example.myapplication.data.model.SharedAccess.text(it, "id") }
                    val visible = if (focusedRole == DepartmentRole.ADMIN && viewDepartment == null) rows else rows.filter { record ->
                        val ownerId = com.example.myapplication.data.model.SharedAccess.text(record, "department_id")
                        val ownerCode = com.example.myapplication.data.model.SharedAccess.text(record, "department_code")
                        (ownerId.isBlank() && ownerCode.isBlank()) || ownerId == deptId || ownerCode.equals(focusedRole.code, true)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("${focusedRole.title} · ${visible.size} records", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Records shown here come from the signed-in shared workspace. Creation remains subject to the configured department permission.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (visible.isEmpty()) item { Text("No shared records are available in this department scope yet.", style = MaterialTheme.typography.bodySmall) }
                        items(visible) { record ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(com.example.myapplication.data.model.SharedAccess.text(record, "title").ifBlank { com.example.myapplication.data.model.SharedAccess.text(record, "reference") }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${com.example.myapplication.data.model.SharedAccess.text(record, "record_type")} · ${com.example.myapplication.data.model.SharedAccess.text(record, "status").ifBlank { "Status unavailable" }}", style = MaterialTheme.typography.bodySmall)
                                    Text(com.example.myapplication.data.model.SharedAccess.text(record, "reference"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                })
                add(DeptHubTab("Documents & Vault") {
                    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Department documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Open the authenticated Vault to browse controlled files, upload originals as Drafts, and review available document actions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { onOpenVault?.invoke() }, enabled = onOpenVault != null) { Text("Open Documents & Vault") }
                    }
                })
                if (focusedRole in setOf(DepartmentRole.FIN, DepartmentRole.HROPS, DepartmentRole.SIT)) {
                    add(DeptHubTab(if (focusedRole == DepartmentRole.HROPS) "People & assignments" else if (focusedRole == DepartmentRole.SIT) "Tickets & service" else "Expenses & budgets") {
                        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${focusedRole.title} workspace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Open the authenticated workspace for shared requests, tickets, profile, and assignments.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { onOpenBusinessWorkspace?.invoke(if (focusedRole == DepartmentRole.HROPS) "profile" else if (focusedRole == DepartmentRole.SIT) "tickets" else "budgets") }, enabled = onOpenBusinessWorkspace != null) { Text("Open department workspace") }
                        }
                    })
                }
            }
        }
    }

    var activeSubTab by remember(focusedRole) { mutableIntStateOf(0) }
    var showSmartUploadModal by remember { mutableStateOf(false) }
    var showCreateRecord by remember { mutableStateOf(false) }
    val membership = sharedSnapshot?.get("membership") as? JsonObject
    val authenticatedAccessRole = membership?.let { com.example.myapplication.data.model.SharedAccess.text(it, "role") }.orEmpty()
    val canCreateSharedRecord = isAuthenticated && authenticatedAccessRole in setOf("admin", "department_lead") && focusedRole != DepartmentRole.CLIENT
    LaunchedEffect(focusedRole, currentRoleTabs.size) {
        if (activeSubTab !in currentRoleTabs.indices) activeSubTab = 0
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Sub-Tab Strip & Quick Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${focusedRole.title} Records Hub",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Department scope: [${focusedRole.code}] · Lead: ${focusedRole.leadName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (canCreateSharedRecord && mainViewModel != null) {
                            FilledTonalButton(
                                onClick = { showCreateRecord = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("New record", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                        // Smart Upload Button (Phase 3 Slice 1)
                        FilledTonalButton(
                            onClick = { if (isAuthenticated) onOpenVault?.invoke() else showSmartUploadModal = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                            text = if (isAuthenticated) "Upload" else "Smart Upload",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Sub-Tabs
                ScrollableTabRow(
                    selectedTabIndex = activeSubTab.coerceIn(0, (currentRoleTabs.size - 1).coerceAtLeast(0)),
                    edgePadding = 0.dp,
                    divider = {},
                    containerColor = Color.Transparent
                ) {
                    currentRoleTabs.forEachIndexed { index, tabItem ->
                        Tab(
                            selected = activeSubTab == index,
                            onClick = { activeSubTab = index },
                            text = {
                                Text(
                                    text = tabItem.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (activeSubTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.5.sp,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }
        }

        // Active Sub-Tab View (Dynamically rendered based on active department role)
        Box(modifier = Modifier.weight(1f)) {
            currentRoleTabs.getOrNull(activeSubTab.coerceIn(0, (currentRoleTabs.size - 1).coerceAtLeast(0)))?.content?.invoke()
        }
    }

    if (showSmartUploadModal && !isAuthenticated) {
        SmartUploadModalBottomSheet(
            scmsRepository = scmsRepository,
            selectedProjectId = selectedProjectId,
            isAuthenticated = isAuthenticated,
            onDismiss = { showSmartUploadModal = false },
            onUploadSuccess = {
                showSmartUploadModal = false
            }
        )
    }
    if (showCreateRecord && mainViewModel != null) {
        SharedRecordCreateDialog(
            viewModel = mainViewModel,
            snapshot = workspaceSnapshot,
            projectCatalog = projectCatalog,
            selectedProjectId = selectedProjectId,
            viewDepartment = viewDepartment,
            onDismiss = { showCreateRecord = false },
            onSaved = { showCreateRecord = false }
        )
    }
}
