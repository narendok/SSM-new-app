package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.ui.text.style.TextOverflow
import com.example.myapplication.ui.components.UnifiedSearchModal
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.AuthState
import com.example.myapplication.ui.components.AuthDialog
import com.example.myapplication.ui.components.EnterpriseAiAssistantModal
import com.example.myapplication.ui.components.RoleSwitcherDialog
import com.example.myapplication.ui.components.SyncBadge
import com.example.myapplication.ui.components.UniversalScannerModal
import com.example.myapplication.ui.viewmodel.MainViewModel
import kotlinx.serialization.json.contentOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val sharedSnapshot by viewModel.sharedSnapshot.collectAsState()
    val lifecycleWorkspace by viewModel.lifecycleWorkspace.collectAsState()
    val lifecycleWorkspaceError by viewModel.lifecycleWorkspaceError.collectAsState()
    val lifecycleWorkspaceBusy by viewModel.lifecycleWorkspaceBusy.collectAsState()
    val sharedError by viewModel.sharedError.collectAsState()
    val sharedBusy by viewModel.sharedBusy.collectAsState()
    val viewDepartment by viewModel.viewDepartment.collectAsState()
    val currentRole by viewModel.scmsRepository.currentRole.collectAsState()
    val approvals by viewModel.scmsRepository.approvals.collectAsState()
    val selectedProject by viewModel.scmsRepository.selectedProject.collectAsState()
    val projectCatalog = if (authState is AuthState.Authenticated) {
        com.example.myapplication.data.model.SharedAccess.projects(sharedSnapshot)
    } else com.example.myapplication.data.model.AutomotiveProjects.all
    val pendingApprovalsCount = approvals.count { it.outcome == "Pending" }

    val orgConfig by viewModel.orgConfig.collectAsState()
    val customerChanges by viewModel.customerChanges.collectAsState()
    val isAuth = authState is AuthState.Authenticated
    val activeProjectFilter = if (isAuth && projectCatalog.none { it.id == selectedProject }) "ALL" else selectedProject
    val activeMembership = sharedSnapshot?.get("membership") as? kotlinx.serialization.json.JsonObject
    val activeMembershipRole = activeMembership?.let { com.example.myapplication.data.model.SharedAccess.text(it, "role") }.orEmpty()
    val activeMembershipDepartmentRaw = activeMembership?.let { com.example.myapplication.data.model.SharedAccess.text(it, "department_code") }.orEmpty().uppercase()
    val activeMembershipDepartment = when (activeMembershipDepartmentRaw) {
        "EFW" -> "FW"
        "CSW" -> "SW"
        "HR", "OPS", "HR/OPS" -> "HROPS"
        else -> activeMembershipDepartmentRaw
    }
    val activeMembershipDepartmentIds = buildSet {
        activeMembership?.let { com.example.myapplication.data.model.SharedAccess.text(it, "department_id") }
            ?.takeIf(String::isNotBlank)?.let(::add)
        (activeMembership?.get("department_ids") as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
            ?.filter(String::isNotBlank)?.forEach(::add)
        com.example.myapplication.data.model.SharedAccess.departments(sharedSnapshot)
            .filter { com.example.myapplication.data.model.SharedAccess.text(it, "code").equals(activeMembershipDepartment, true) }
            .map { com.example.myapplication.data.model.SharedAccess.text(it, "id") }
            .filter(String::isNotBlank).forEach(::add)
    }
    fun matchesActiveDepartment(row: kotlinx.serialization.json.JsonObject): Boolean {
        if (!isAuth) return true
        if (activeMembershipRole == "admin") return viewDepartment == null ||
            com.example.myapplication.data.model.SharedAccess.text(row, "department_id") == viewDepartment ||
                com.example.myapplication.data.model.SharedAccess.text(row, "reviewer_department_id") == viewDepartment
        return com.example.myapplication.data.model.SharedAccess.text(row, "department_id") in activeMembershipDepartmentIds ||
            com.example.myapplication.data.model.SharedAccess.text(row, "reviewer_department_id") in activeMembershipDepartmentIds ||
            com.example.myapplication.data.model.SharedAccess.text(row, "department_code").equals(activeMembershipDepartment, true) ||
            com.example.myapplication.data.model.SharedAccess.text(row, "reviewer_department_code").equals(activeMembershipDepartment, true)
    }
    val sharedRecordsCount = if (isAuth) {
        com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "records").count(::matchesActiveDepartment)
    } else null
    val sharedApprovalsCount = if (isAuth) {
        com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "approvals")
            .count { com.example.myapplication.data.model.SharedAccess.canDecide(sharedSnapshot, it) }
            .let { total ->
                if (viewDepartment == null || (sharedSnapshot?.get("membership") as? kotlinx.serialization.json.JsonObject)
                        ?.let { com.example.myapplication.data.model.SharedAccess.text(it, "role") } != "admin") total
                else com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "approvals")
                    .count { approval ->
                        com.example.myapplication.data.model.SharedAccess.canDecide(sharedSnapshot, approval) &&
                            com.example.myapplication.data.model.SharedAccess.text(approval, "reviewer_department_id") == viewDepartment
                    }
            }
    } else null
    val lifecycleStagesCount = if (isAuth) {
        (((lifecycleWorkspace?.get("revision") as? kotlinx.serialization.json.JsonObject)
            ?.get("definition") as? kotlinx.serialization.json.JsonObject)
            ?.get("stages") as? kotlinx.serialization.json.JsonArray)?.size ?: 0
    } else null

    var showAuthDialog by remember { mutableStateOf(false) }
    var showSharedWorkspace by remember { mutableStateOf(false) }
    var showBusinessWorkspace by remember { mutableStateOf(false) }
    var businessInitialView by remember { mutableStateOf("budgets") }
    var showRoleSwitcherDialog by remember { mutableStateOf(false) }
    var showAdminDeptDropdown by remember { mutableStateOf(false) }
    var showScannerModal by remember { mutableStateOf(false) }
    var showAiAssistantModal by remember { mutableStateOf(false) }
    var showApprovalsModal by remember { mutableStateOf(false) }
    var showSearchPopupModal by remember { mutableStateOf(false) }

    if (showSharedWorkspace && authState is AuthState.Authenticated && (authState as AuthState.Authenticated).session.testMembership != null) {
        SharedTestScreen(viewModel, onBack = { showSharedWorkspace = false })
        return
    }

    if (showBusinessWorkspace && isAuth) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (businessInitialView == "profile") "My HR Profile" else "Expenses, Travel & Services",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showBusinessWorkspace = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to dashboard"
                            )
                        }
                    }
                )
            }
        ) { bizPadding ->
            Box(modifier = Modifier.padding(bizPadding)) {
                SharedBusinessScreen(viewModel = viewModel, initialView = businessInitialView)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val serverRoleRaw = (sharedSnapshot?.get("membership") as? kotlinx.serialization.json.JsonObject)
                        ?.get("role")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }.orEmpty()
                    val membership = sharedSnapshot?.get("membership") as? kotlinx.serialization.json.JsonObject
                    val serverDepartmentCode = membership?.let { com.example.myapplication.data.model.SharedAccess.text(it, "department_code") }
                        .orEmpty().uppercase().let { code -> when (code) {
                            "EFW" -> "FW"
                            "CSW" -> "SW"
                            "HR", "OPS", "HR/OPS" -> "HROPS"
                            else -> code
                        } }
                    val isAdmin = (!isAuth && currentRole == com.example.myapplication.data.model.DepartmentRole.ADMIN) || serverRoleRaw == "admin"
                    val activeRoleCode = when {
                        !isAuth -> currentRole.code
                        serverRoleRaw == "admin" -> "ADMIN"
                        serverRoleRaw == "department_lead" -> "LEAD"
                        serverRoleRaw == "viewer" -> "VIEW"
                        serverRoleRaw == "member" -> "USER"
                        serverRoleRaw.isNotBlank() -> serverRoleRaw.take(5).uppercase()
                        else -> currentRole.code
                    }
                    val activeDeptLabel = when {
                        isAuth && !isAdmin -> serverDepartmentCode.ifBlank { "My Dept" }
                        !isAdmin -> activeRoleCode
                        viewDepartment != null -> {
                            if (isAuth) {
                                val depts = com.example.myapplication.data.model.SharedAccess.departments(sharedSnapshot)
                                if (depts.isEmpty()) {
                                    "Loading..."
                                } else {
                                    val match = depts.firstOrNull { com.example.myapplication.data.model.SharedAccess.text(it, "id") == viewDepartment }
                                    match?.let {
                                        val code = com.example.myapplication.data.model.SharedAccess.text(it, "code")
                                        if (code.isNotBlank()) code else com.example.myapplication.data.model.SharedAccess.text(it, "name")
                                    } ?: "Unknown Dept"
                                }
                            } else {
                                val localMatch = com.example.myapplication.data.model.DepartmentRole.entries.firstOrNull { it.code.equals(viewDepartment, ignoreCase = true) }
                                localMatch?.code ?: "Unknown Dept"
                            }
                        }
                        else -> "Admin · All"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "SS",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            text = "SSM One",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Box {
                            Surface(
                            modifier = Modifier.clickable(enabled = isAdmin || !isAuth) {
                                    if (isAdmin) showAdminDeptDropdown = true else showRoleSwitcherDialog = true
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isAuth) Color(0xFF1565C0) else Color(0xFF2E7D32))
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = activeDeptLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    if (isAdmin) {
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Department",
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            if (isAdmin || !isAuth) {
                                DropdownMenu(
                                    expanded = showAdminDeptDropdown,
                                    onDismissRequest = { showAdminDeptDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Departments (Executive Admin)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.selectDepartment(null)
                                            if (!isAuth) {
                                                viewModel.scmsRepository.switchRole(com.example.myapplication.data.model.DepartmentRole.ADMIN)
                                            }
                                            showAdminDeptDropdown = false
                                        }
                                    )
                                    HorizontalDivider()
                                    if (isAuth) {
                                        val configuredDepts = com.example.myapplication.data.model.SharedAccess.departments(sharedSnapshot)
                                        if (configuredDepts.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Loading departments...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                onClick = { showAdminDeptDropdown = false },
                                                enabled = false
                                            )
                                        } else {
                                            configuredDepts.forEach { dept ->
                                                val deptId = com.example.myapplication.data.model.SharedAccess.text(dept, "id")
                                                val deptCode = com.example.myapplication.data.model.SharedAccess.text(dept, "code").ifBlank { "DEPT" }
                                                val deptName = com.example.myapplication.data.model.SharedAccess.text(dept, "name")
                                                val label = if (deptName.isNotBlank()) "$deptCode · $deptName" else deptCode
                                                DropdownMenuItem(
                                                    text = { Text(label, fontSize = 12.sp) },
                                                    onClick = {
                                                        viewModel.selectDepartment(deptId)
                                                        showAdminDeptDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        com.example.myapplication.data.model.DepartmentRole.entries
                                            .filter { it != com.example.myapplication.data.model.DepartmentRole.ADMIN && !it.isClient }
                                            .forEach { role ->
                                                DropdownMenuItem(
                                                    text = { Text("${role.code} · ${role.title}", fontSize = 12.sp) },
                                                    onClick = {
                                                        viewModel.selectDepartment(role.code)
                                                        viewModel.scmsRepository.switchRole(role)
                                                        showAdminDeptDropdown = false
                                                    }
                                                )
                                            }
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiAssistantModal = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Enterprise AI Assistant",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = { showScannerModal = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Universal Scanner",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshActiveTab() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = { showAuthDialog = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account / Sign In",
                            tint = if (isAuth) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { showSearchPopupModal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Unified Search & Voice Control",
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Work") },
                    label = { Text("Work", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Records") },
                    label = { Text("Records", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Vault") },
                    label = { Text("Vault", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Keep project scope visible anywhere it filters project-linked records.
            if (selectedTab == 0 || selectedTab == 1 || selectedTab == 2) {
                com.example.myapplication.ui.components.ProjectFilterDropdown(
                    selectedProjectId = activeProjectFilter,
                    onProjectSelected = { viewModel.scmsRepository.selectProject(it) },
                    projects = projectCatalog
                )
            }
            Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> CommandCenterScreen(
                    scmsRepository = viewModel.scmsRepository,
                    onOpenRoleSwitcher = { showRoleSwitcherDialog = true },
                    authenticatedName = (authState as? AuthState.Authenticated)?.session?.testMembership?.displayName,
                    isAuthenticated = authState is AuthState.Authenticated,
                    projectCatalog = projectCatalog,
                    sharedTasks = if (authState is AuthState.Authenticated) com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "tasks")
                        .filter(::matchesActiveDepartment)
                        .map { task ->
                            fun t(key: String) = com.example.myapplication.data.model.SharedAccess.text(task, key)
                            val status = t("status")
                            val projectId = com.example.myapplication.data.model.SharedAccess.projectIdForTask(sharedSnapshot, task)
                            val deptCode = t("department_code").ifBlank {
                                com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "departments")
                                    .firstOrNull { com.example.myapplication.data.model.SharedAccess.text(it, "id") == t("department_id") }
                                    ?.let { com.example.myapplication.data.model.SharedAccess.text(it, "code") }
                                    .orEmpty()
                            }
                            com.example.myapplication.data.model.DailyTaskItem(
                                id = t("id"),
                                title = t("title"),
                                dept = deptCode,
                                priority = t("priority").ifBlank { "Normal" },
                                assignee = t("assignee_name").ifBlank { t("assignee") }.ifBlank { "Dept queue" },
                                dueDate = t("due_at").take(10).ifBlank { "Unscheduled" },
                                isCompleted = status.equals("Complete", true) || status.equals("Completed", true),
                                lane = com.example.myapplication.data.model.SharedAccess.taskLane(status),
                                projectId = projectId
                            )
                        } else null,
                    onSharedTaskToggle = { id ->
                        com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "tasks").find { com.example.myapplication.data.model.SharedAccess.text(it, "id") == id }?.let { task ->
                            val nextStatus = if (com.example.myapplication.data.model.SharedAccess.text(task, "status").equals("Complete", true)) "Open" else "Complete"
                            if (com.example.myapplication.data.model.SharedAccess.canUpdateTask(sharedSnapshot, task) &&
                                com.example.myapplication.data.model.SharedAccess.canSetTaskStatus(task, nextStatus)) {
                                viewModel.mutateSharedWorkspace("update_task", kotlinx.serialization.json.buildJsonObject {
                                    put("id", kotlinx.serialization.json.JsonPrimitive(id))
                                    put("status", kotlinx.serialization.json.JsonPrimitive(nextStatus))
                                    put("expected_updated_at", kotlinx.serialization.json.JsonPrimitive(com.example.myapplication.data.model.SharedAccess.text(task, "updated_at")))
                                })
                            }
                        }
                    },
                    onSharedTaskMove = { id, status ->
                        com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "tasks").find { com.example.myapplication.data.model.SharedAccess.text(it, "id") == id }?.let { task ->
                            if (com.example.myapplication.data.model.SharedAccess.canUpdateTask(sharedSnapshot, task) &&
                                com.example.myapplication.data.model.SharedAccess.canSetTaskStatus(task, status)) {
                                viewModel.mutateSharedWorkspace("update_task", kotlinx.serialization.json.buildJsonObject {
                                    put("id", kotlinx.serialization.json.JsonPrimitive(id))
                                    put("status", kotlinx.serialization.json.JsonPrimitive(status))
                                    put("expected_updated_at", kotlinx.serialization.json.JsonPrimitive(com.example.myapplication.data.model.SharedAccess.text(task, "updated_at")))
                                })
                            }
                        }
                    },
                    sharedRecordsCount = sharedRecordsCount,
                    sharedApprovalsCount = sharedApprovalsCount,
                    lifecycleStagesCount = lifecycleStagesCount,
                    onOpenBusinessWorkspace = if (isAuth) ({ section -> businessInitialView = section; showBusinessWorkspace = true }) else null,
                    sharedSnapshot = sharedSnapshot,
                    viewDepartment = viewDepartment,
                    lifecycleWorkspace = lifecycleWorkspace,
                    onOpenDepartmentRecords = { viewModel.selectTab(2) },
                    onOpenApprovals = { showApprovalsModal = true },
                    canReopenSharedTask = { id ->
                        val task = com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "tasks")
                            .firstOrNull { com.example.myapplication.data.model.SharedAccess.text(it, "id") == id }
                        task != null && com.example.myapplication.data.model.SharedAccess.canUpdateTask(sharedSnapshot, task)
                    },
                    isSharedBusy = isAuth && sharedBusy
                )
                1 -> ProjectAnalysisScreen(
                    scmsRepository = viewModel.scmsRepository,
                    selectedProjectId = activeProjectFilter,
                    projectCatalog = projectCatalog,
                    isAuthenticated = isAuth,
                    sharedSnapshot = sharedSnapshot,
                    lifecycleWorkspace = lifecycleWorkspace,
                    viewDepartment = viewDepartment,
                    onOpenBusinessWorkspace = { section -> businessInitialView = section; showBusinessWorkspace = true },
                    onOpenDepartmentRecords = { viewModel.selectTab(2) },
                    onOpenApprovals = { showApprovalsModal = true }
                )
                2 -> DepartmentRecordsHubScreen(
                    scmsRepository = viewModel.scmsRepository,
                    isAuthenticated = isAuth,
                    sharedSnapshot = sharedSnapshot,
                    lifecycleWorkspace = lifecycleWorkspace,
                    selectedProjectId = activeProjectFilter,
                    viewDepartment = viewDepartment,
                    mainViewModel = viewModel,
                    projectCatalog = projectCatalog,
                    onOpenVault = { viewModel.selectTab(3) },
                    onOpenBusinessWorkspace = { section -> businessInitialView = section; showBusinessWorkspace = true }
                )
                3 -> DocumentControlScreen(
                    scmsRepository = viewModel.scmsRepository,
                    isAuthenticated = isAuth,
                    projectCatalog = projectCatalog,
                    selectedProjectId = activeProjectFilter,
                    viewDepartment = viewDepartment,
                    sharedSnapshot = sharedSnapshot,
                    lifecycleWorkspace = lifecycleWorkspace,
                    lifecycleWorkspaceError = lifecycleWorkspaceError,
                    lifecycleWorkspaceBusy = lifecycleWorkspaceBusy,
                    orgConfig = orgConfig,
                    customerChanges = customerChanges,
                    onRefreshLifecycleWorkspace = { viewModel.refreshLifecycleWorkspace() },
                    onLinkReusableAsset = { projectId, assetRevisionId, assessment ->
                        viewModel.linkReusableAsset(projectId, assetRevisionId, assessment)
                    },
                    loadOriginalRevisions = { documentId -> viewModel.controlledOriginalRevisions(documentId) },
                    originalDownload = { fileId -> viewModel.controlledOriginalDownload(fileId) },
                    createSharedDocument = { projectId, departmentId, title, documentType, originalName, contentType, bytes ->
                        viewModel.createSharedDocumentWithOriginal(
                            projectId,
                            departmentId,
                            title,
                            documentType,
                            originalName,
                            contentType,
                            bytes
                        )
                    },
                    onOpenBusinessWorkspace = if (isAuth) ({ businessInitialView = "budgets"; showBusinessWorkspace = true }) else null,
                    onOpenTickets = if (isAuth) ({ businessInitialView = "tickets"; showBusinessWorkspace = true }) else null
                )
                4 -> MyProfileAndWorkflowsScreen(
                    viewModel = viewModel,
                    onOpenRoleSwitcher = { showRoleSwitcherDialog = true },
                    onOpenAuthDialog = { showAuthDialog = true },
                    onOpenDataWorkspace = if (isAuth) ({ showSharedWorkspace = true }) else null,
                    onOpenBusinessWorkspace = if (isAuth) ({ section -> businessInitialView = section; showBusinessWorkspace = true }) else null
                )
            }
            }
        }
    }
    if (showRoleSwitcherDialog && authState is AuthState.Authenticated &&
        (sharedSnapshot?.get("membership") as? kotlinx.serialization.json.JsonObject)?.let {
            com.example.myapplication.data.model.SharedAccess.text(it, "role") == "admin"
        } == true) {
        androidx.compose.material3.AlertDialog(onDismissRequest = { showRoleSwitcherDialog = false },
            title = { Text("Department view") },
            text = { Column {
                Text("Navigation only. Your signed-in identity and permissions stay unchanged.")
                if ((sharedSnapshot?.get("membership") as? kotlinx.serialization.json.JsonObject)?.get("role")?.toString() == "\"admin\"") {
                    androidx.compose.material3.TextButton(onClick = { viewModel.selectDepartment(null); showRoleSwitcherDialog = false }) { Text("All permitted departments") }
                    com.example.myapplication.data.model.SharedAccess.departments(sharedSnapshot).forEach { dept ->
                        androidx.compose.material3.TextButton(onClick = {
                            viewModel.selectDepartment(com.example.myapplication.data.model.SharedAccess.text(dept, "id")); showRoleSwitcherDialog = false
                        }) { Text(com.example.myapplication.data.model.SharedAccess.text(dept, "name")) }
                    }
                }
            } }, confirmButton = { androidx.compose.material3.TextButton(onClick = { showRoleSwitcherDialog = false }) { Text("Close") } })
    }
    if (showRoleSwitcherDialog && authState !is AuthState.Authenticated) {
        RoleSwitcherDialog(
            currentRole = currentRole,
            onRoleSelected = { role ->
                viewModel.switchRole(role)
                showRoleSwitcherDialog = false
            },
            onDismiss = { showRoleSwitcherDialog = false }
        )
    }

    if (showAuthDialog) {
        AuthDialog(
            authRepository = viewModel.authRepository,
            onSignIn = viewModel::signInWithPassword,
            authState = authState,
            onDismiss = { showAuthDialog = false }
        )
    }

    if (showScannerModal) {
        UniversalScannerModal(
            scmsRepository = viewModel.scmsRepository,
            onNavigateToRoute = { route ->
                showScannerModal = false
                when (route) {
                    "approvals" -> showApprovalsModal = true
                    "inventory", "records", "receiving", "assets", "bom", "qa", "quality" -> viewModel.selectTab(1)
                    "search" -> viewModel.selectTab(2)
                    "documents", "docs", "security" -> viewModel.selectTab(3)
                    "team", "leads", "hr", "chat" -> viewModel.selectTab(4)
                    else -> viewModel.selectTab(0)
                }
            },
            onDismiss = { showScannerModal = false }
        )
    }

    if (showAiAssistantModal) {
        EnterpriseAiAssistantModal(
            scmsRepository = viewModel.scmsRepository,
            onDismiss = { showAiAssistantModal = false }
        )
    }

    if (showSearchPopupModal) {
        UnifiedSearchModal(
            scmsRepository = viewModel.scmsRepository,
            isAuthenticated = authState is AuthState.Authenticated,
            onDismiss = { showSearchPopupModal = false }
        )
    }

    if (showApprovalsModal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showApprovalsModal = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ApprovalsScreen(
                scmsRepository = viewModel.scmsRepository,
                isAuthenticated = authState is AuthState.Authenticated,
                onBack = { showApprovalsModal = false }
            )
        }
    }
}
