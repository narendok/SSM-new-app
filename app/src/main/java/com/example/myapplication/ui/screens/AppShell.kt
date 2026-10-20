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
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import com.example.myapplication.ui.components.UnifiedSearchModal
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val sharedSnapshot by viewModel.sharedSnapshot.collectAsState()
    val viewDepartment by viewModel.viewDepartment.collectAsState()
    val currentRole by viewModel.scmsRepository.currentRole.collectAsState()
    val approvals by viewModel.scmsRepository.approvals.collectAsState()
    val pendingApprovalsCount = approvals.count { it.outcome == "Pending" }

    var showAuthDialog by remember { mutableStateOf(false) }
    var showSharedWorkspace by remember { mutableStateOf(false) }
    var showRoleSwitcherDialog by remember { mutableStateOf(false) }
    var showScannerModal by remember { mutableStateOf(false) }
    var showAiAssistantModal by remember { mutableStateOf(false) }
    var showApprovalsModal by remember { mutableStateOf(false) }
    var showSearchPopupModal by remember { mutableStateOf(false) }

    if (showSharedWorkspace && authState is AuthState.Authenticated && (authState as AuthState.Authenticated).session.testMembership != null) {
        SharedTestScreen(viewModel, onBack = { showSharedWorkspace = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "SS",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SSM Mobile",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    modifier = Modifier.clickable { showRoleSwitcherDialog = true },
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(Color(0xFF2E7D32))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = currentRole.code,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(0xFF2E7D32))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Synced",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = { showAiAssistantModal = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Enterprise AI Assistant",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = { showScannerModal = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Universal Scanner",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshActiveTab() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (authState is AuthState.Authenticated) {
                        IconButton(onClick = { showSharedWorkspace = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.AccountTree, contentDescription = "Shared TEST records")
                        }
                    }
                    IconButton(
                        onClick = { showAuthDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account / Sign In",
                            tint = if (authState is AuthState.Authenticated) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSearchPopupModal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Unified Search & Voice Control",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Cockpit", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Records") },
                    label = { Text("Records", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Documents") },
                    label = { Text("Docs", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    icon = { Icon(Icons.Default.Forum, contentDescription = "Team & Chat") },
                    label = { Text("Team", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> CommandCenterScreen(
                    scmsRepository = viewModel.scmsRepository,
                    onOpenRoleSwitcher = { showRoleSwitcherDialog = true },
                    authenticatedName = (authState as? AuthState.Authenticated)?.session?.testMembership?.displayName,
                    sharedTasks = if (authState is AuthState.Authenticated) com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "tasks")
                        .filter { viewDepartment == null || com.example.myapplication.data.model.SharedAccess.text(it, "department_id") == viewDepartment }
                        .map { task ->
                            fun t(key: String) = com.example.myapplication.data.model.SharedAccess.text(task, key)
                            com.example.myapplication.data.model.DailyTaskItem(t("id"), t("title"), "", t("priority"), "", t("due_at"), t("status") == "Complete")
                        } else null,
                    onSharedTaskToggle = { id ->
                        com.example.myapplication.data.model.SharedAccess.rows(sharedSnapshot, "tasks").find { com.example.myapplication.data.model.SharedAccess.text(it, "id") == id }?.let { task ->
                            if (com.example.myapplication.data.model.SharedAccess.canUpdateTask(sharedSnapshot, task)) {
                                viewModel.mutateSharedWorkspace("update_task", kotlinx.serialization.json.buildJsonObject {
                                    put("id", kotlinx.serialization.json.JsonPrimitive(id))
                                    put("status", kotlinx.serialization.json.JsonPrimitive(if (com.example.myapplication.data.model.SharedAccess.text(task, "status") == "Complete") "Open" else "Complete"))
                                    put("expected_updated_at", kotlinx.serialization.json.JsonPrimitive(com.example.myapplication.data.model.SharedAccess.text(task, "updated_at")))
                                })
                            }
                        }
                    }
                )
                1 -> DepartmentRecordsScreen(
                    scmsRepository = viewModel.scmsRepository,
                    onOpenRoleSwitcher = { showRoleSwitcherDialog = true }
                )
                2 -> UnifiedSearchScreen(
                    scmsRepository = viewModel.scmsRepository
                )
                3 -> ProjectDocumentsScreen(
                    scmsRepository = viewModel.scmsRepository,
                    onOpenRoleSwitcher = { showRoleSwitcherDialog = true }
                )
                4 -> TeamAndChatScreen(
                    scmsRepository = viewModel.scmsRepository,
                    onOpenRoleSwitcher = { showRoleSwitcherDialog = true },
                    onOpenAuthDialog = { showAuthDialog = true }
                )
            }
            }
        }
    }
    if (showRoleSwitcherDialog && authState is AuthState.Authenticated) {
        androidx.compose.material3.AlertDialog(onDismissRequest = { showRoleSwitcherDialog = false },
            title = { Text("Department view") },
            text = { Column {
                Text("Navigation only. Your signed-in identity and permissions stay unchanged.")
                if ((sharedSnapshot?.get("membership") as? kotlinx.serialization.json.JsonObject)?.get("role")?.toString() == "\"admin\"") {
                    androidx.compose.material3.TextButton(onClick = { viewModel.selectDepartment(null); showRoleSwitcherDialog = false }) { Text("All permitted departments") }
                }
                com.example.myapplication.data.model.SharedAccess.departments(sharedSnapshot).forEach { dept ->
                    androidx.compose.material3.TextButton(onClick = {
                        viewModel.selectDepartment(com.example.myapplication.data.model.SharedAccess.text(dept, "id")); showRoleSwitcherDialog = false
                    }) { Text(com.example.myapplication.data.model.SharedAccess.text(dept, "name")) }
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
                onBack = { showApprovalsModal = false }
            )
        }
    }
}
