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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import com.example.myapplication.ui.components.SmartUploadModalBottomSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.myapplication.data.model.ClientDeliverable
import com.example.myapplication.data.model.ControlledDoc
import com.example.myapplication.data.model.CustomerAccount
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.ProjectMetadata
import com.example.myapplication.data.model.SalesOpportunity
import com.example.myapplication.data.model.SharedAccess
import com.example.myapplication.data.repository.ScmsRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.util.UUID

internal fun visibleDocumentControlTabIndex(
    logicalTab: Int,
    canViewClientPortal: Boolean,
    canViewSalesCrm: Boolean
): Int {
    val visibleTabs = buildList {
        add(0)
        if (canViewClientPortal) add(1)
        add(2)
        add(3)
        if (canViewSalesCrm) add(4)
    }
    return visibleTabs.indexOf(logicalTab).coerceAtLeast(0)
}

@Composable
fun DocumentControlScreen(
    scmsRepository: ScmsRepository,
    isAuthenticated: Boolean = false,
    projectCatalog: List<ProjectMetadata> = emptyList(),
    selectedProjectId: String = "ALL",
    viewDepartment: String? = null,
    sharedSnapshot: JsonObject? = null,
    lifecycleWorkspace: JsonObject? = null,
    lifecycleWorkspaceError: String? = null,
    lifecycleWorkspaceBusy: Boolean = false,
    orgConfig: JsonObject? = null,
    customerChanges: JsonObject? = null,
    onRefreshLifecycleWorkspace: (() -> Unit)? = null,
    onLinkReusableAsset: ((String, String, String) -> Unit)? = null,
    loadOriginalRevisions: (suspend (String) -> JsonArray)? = null,
    originalDownload: (suspend (String) -> String)? = null,
    createSharedDocument: (suspend (String, String, String, String, String, String, ByteArray) -> String)? = null,
    onOpenBusinessWorkspace: (() -> Unit)? = null,
    onOpenTickets: (() -> Unit)? = null,
    initialTopTab: Int = 0
) {
    val documents by scmsRepository.documents.collectAsState()
    val customers by scmsRepository.customers.collectAsState()
    val salesOpportunities by scmsRepository.salesOpportunities.collectAsState()
    val clientDeliverables by scmsRepository.clientDeliverables.collectAsState()
    val currentRole by scmsRepository.currentRole.collectAsState()
    val localSelectedProject by scmsRepository.selectedProject.collectAsState()
    val effectiveProjectId = if (isAuthenticated) selectedProjectId else localSelectedProject
    val activeProjectObj = projectCatalog.firstOrNull { it.id == effectiveProjectId }
        ?: com.example.myapplication.data.model.AutomotiveProjects.all.firstOrNull { it.id == effectiveProjectId }

    val membership = sharedSnapshot?.get("membership") as? JsonObject
    val membershipRole = membership?.let { SharedAccess.text(it, "role") }.orEmpty()
    val membershipDepartment = membership?.let { SharedAccess.text(it, "department_code") }.orEmpty()
    val isAdminProfile = if (isAuthenticated) membershipRole == "admin" else currentRole == DepartmentRole.ADMIN
    val isSalesProfile = if (isAuthenticated) membershipDepartment.equals("SAL", true) else currentRole == DepartmentRole.SAL
    val canViewSalesCrm = isAdminProfile || isSalesProfile
    val canManageCustomers = !isAuthenticated && (isAdminProfile || isSalesProfile)
    val canViewClientPortal = if (isAuthenticated) isAdminProfile || membershipRole == "viewer" || membershipRole.equals("client", true) || membershipDepartment.equals("CLIENT", true)
        else isAdminProfile || currentRole == DepartmentRole.CLIENT

    var selectedTopTab by remember(initialTopTab) { mutableIntStateOf(initialTopTab.coerceIn(0, 4)) } // 0: Docs, 1: Client Portal, 2: Security, 3: Directory, 4: CRM
    var showSignedUrlModal by remember { mutableStateOf(false) }
    var selectedDocForSignedUrl by remember { mutableStateOf<ControlledDoc?>(null) }
    var signedUrlResult by remember { mutableStateOf<String?>(null) }
    var showAddCustomerModal by remember { mutableStateOf(false) }
    var showSupportTicketModal by remember { mutableStateOf(false) }
    var showSmartUploadModal by remember { mutableStateOf(false) }
    var ticketTitle by remember { mutableStateOf("") }
    var ticketDesc by remember { mutableStateOf("") }
    var ticketConfirmationMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(canViewSalesCrm, canViewClientPortal) {
        if ((!canViewSalesCrm && selectedTopTab == 4) || (!canViewClientPortal && selectedTopTab == 1)) selectedTopTab = 0
    }
    LaunchedEffect(canManageCustomers) {
        if (!canManageCustomers) showAddCustomerModal = false
    }

    // The tab state uses stable logical IDs (Docs=0, Client=1, Security=2,
    // Directory=3, CRM=4), while ScrollableTabRow expects the index among the
    // tabs actually rendered. For a Sales lead, Client Portal is hidden, so
    // CRM is logical tab 4 but visible position 3.
    val activeTab = if ((selectedTopTab == 4 && !canViewSalesCrm) || (selectedTopTab == 1 && !canViewClientPortal)) 0 else selectedTopTab
    val selectedVisibleTabIndex = visibleDocumentControlTabIndex(activeTab, canViewClientPortal, canViewSalesCrm)
    LaunchedEffect(canViewSalesCrm, canViewClientPortal) {
        if ((selectedTopTab == 4 && !canViewSalesCrm) || (selectedTopTab == 1 && !canViewClientPortal)) selectedTopTab = 0
    }

    var newCustName by remember { mutableStateOf("") }
    var newCustDivision by remember { mutableStateOf("") }
    var newCustContact by remember { mutableStateOf("") }
    var newCustEmail by remember { mutableStateOf("") }
    var newCustLocation by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row: compact 1-line labels
        ScrollableTabRow(
            selectedTabIndex = selectedVisibleTabIndex,
            edgePadding = 10.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Tab(
                selected = selectedTopTab == 0,
                onClick = { selectedTopTab = 0 },
                text = { Text("Docs", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, maxLines = 1) }
            )
            if (canViewClientPortal) {
                Tab(
                    selected = selectedTopTab == 1,
                    onClick = { selectedTopTab = 1 },
                    text = { Text("Client Portal", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, maxLines = 1) }
                )
            }
            Tab(
                selected = selectedTopTab == 2,
                onClick = { selectedTopTab = 2 },
                text = { Text("Security", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, maxLines = 1) }
            )
            Tab(
                selected = selectedTopTab == 3,
                onClick = { selectedTopTab = 3 },
                text = { Text("Directory", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, maxLines = 1) }
            )
            if (canViewSalesCrm) {
                Tab(
                    selected = selectedTopTab == 4,
                    onClick = { selectedTopTab = 4 },
                    text = { Text("CRM", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, maxLines = 1) }
                )
            }
        }

        when (activeTab) {
            0 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Controlled Docs Vault (38 Families)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FilledTonalButton(
                                onClick = { showSmartUploadModal = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Smart Upload", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        // Delegate directly to the canonical ProjectDocumentsScreen (server controlled originals + 9-stage project view filter)
                        ProjectDocumentsScreen(
                            scmsRepository = scmsRepository,
                            isAuthenticated = isAuthenticated,
                            projectCatalog = projectCatalog,
                            selectedProjectId = effectiveProjectId,
                            viewDepartment = viewDepartment,
                            sharedSnapshot = sharedSnapshot,
                            lifecycleWorkspace = lifecycleWorkspace,
                            lifecycleWorkspaceError = lifecycleWorkspaceError,
                            lifecycleWorkspaceBusy = lifecycleWorkspaceBusy,
                            onRefreshLifecycleWorkspace = onRefreshLifecycleWorkspace,
                            onLinkReusableAsset = onLinkReusableAsset,
                            loadOriginalRevisions = loadOriginalRevisions,
                            originalDownload = originalDownload,
                            createSharedDocument = createSharedDocument
                        )
                    }
                }
            }
            1 -> {
                // Client Portal
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = activeProjectObj?.title ?: "Client Collaboration Portal",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (!isAuthenticated) {
                                        Button(
                                            onClick = { showSupportTicketModal = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("+ Ticket", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isAuthenticated) "Authenticated TEST · ${activeProjectObj?.client?.ifBlank { activeProjectObj.title } ?: "Department Scope"}" else "${activeProjectObj?.client?.ifBlank { activeProjectObj.title } ?: "Northstar Field Systems (TEST)"} · ${activeProjectObj?.status ?: "Local TEST"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    if (isAuthenticated) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Tickets use the authenticated shared workspace.",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    onOpenTickets?.let { openTickets ->
                                        OutlinedButton(
                                            onClick = openTickets,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) { Text("Tickets", style = MaterialTheme.typography.labelSmall) }
                                    }
                                }
                            }
                        }
                        val serverChangeRows = (customerChanges?.get("requests") as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
                        item {
                            Text(
                                text = "Customer Change Requests (${serverChangeRows.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (serverChangeRows.isEmpty()) {
                            item {
                                Text(
                                    "No customer change requests returned in your authorized TEST scope.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(serverChangeRows) { ccr ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            SharedAccess.text(ccr, "title").ifBlank { SharedAccess.text(ccr, "reference") },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${SharedAccess.text(ccr, "reference")} · ${SharedAccess.text(ccr, "status")}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val filteredDeliverables = clientDeliverables.filter {
                            effectiveProjectId == "ALL" || it.projectId == effectiveProjectId
                        }
                        items(filteredDeliverables) { deliv ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = deliv.code,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = deliv.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = deliv.status,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Security & Storage Vault
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Android Keystore Security Status",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Token Storage: EncryptedSharedPreferences (AES256_GCM)")
                                Text("• Keystore Master Key: _androidx_security_master_key_")
                                Text("• Plaintext SharedPreferences: Permanently Disabled")
                                Text("• Target Supabase Endpoint: ${com.example.myapplication.BuildConfig.SUPABASE_URL}")
                                Text("• OAuth Provider: Google Workspace PKCE (S256)")
                                Text("• Deep Link Callback: com.example.myapplication://auth-callback")
                                Text("• Service Keys / DB Passwords in APK: 0 (Zero)")
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Private Storage Policy",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("All CAD files (.stp), schematics (.pdf), DVP&R test records, and customer quotations are protected under ISO/IEC 27001:2022.")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("To access, client generates an HMAC-signed token expiring in exactly 300 seconds.")
                            }
                        }
                    }
                }
            }
            3 -> {
                // Company Directory & Legal Hub
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isAuthenticated) {
                        val orgDepts = (orgConfig?.get("departments") as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
                        val orgLeads = (orgConfig?.get("leads") as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
                        val multiDeptLive = orgLeads.any { it.containsKey("departmentIds") }
                        item {
                            Surface(
                                color = Color(0xFFEDE7F6),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "Live TEST Organization Directory (${orgDepts.size} Depts · ${orgLeads.size} Leads)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4A148C)
                                    )
                                    Text(
                                        text = if (multiDeptLive) "Multi-department lead schema live." else "Server uses singular departmentId per lead (migration 20261110090000 not yet active on live TEST).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6A1B9A)
                                    )
                                    onOpenBusinessWorkspace?.let { openBiz ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        FilledTonalButton(
                                            onClick = openBiz,
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("Open Expenses, Travel & Services", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        val deptCodeById = orgDepts.associate { SharedAccess.text(it, "id") to SharedAccess.text(it, "code") }
                        val deptNameById = orgDepts.associate {
                            SharedAccess.text(it, "id") to listOfNotNull(
                                SharedAccess.text(it, "code").ifBlank { null },
                                SharedAccess.text(it, "name").ifBlank { null }
                            ).joinToString(" · ")
                        }
                        items(orgLeads) { lead ->
                            val deptId = SharedAccess.text(lead, "departmentId")
                            val displayName = SharedAccess.text(lead, "name")
                                .ifBlank { SharedAccess.text(lead, "displayName") }
                                .ifBlank { SharedAccess.text(lead, "email") }
                                .ifBlank { "Unnamed Lead" }
                            val roleLabel = SharedAccess.text(lead, "role").ifBlank { "Department Lead" }
                            val badgeText = deptCodeById[deptId]?.ifBlank { null } ?: "LEAD"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = badgeText.take(4).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        val leadSubtitle = listOfNotNull(
                                            deptNameById[deptId]?.ifBlank { null },
                                            roleLabel
                                        ).joinToString(" · ")
                                        Text(
                                            leadSubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Surface(
                                color = Color(0xFFEDE7F6),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Organization Directory (Local TEST Sample)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4A148C)
                                    )
                                    Text(
                                        text = "Sign in to load live TEST departments & leads from get_organization_configuration.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6A1B9A)
                                    )
                                }
                            }
                        }

                        items(DepartmentRole.entries) { role ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (role.isClient) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = role.code.take(3),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = role.leadName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = role.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            4 -> {
                // Sales CRM & Customer Register (Master Plan §4 & §6)
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sales CRM & Customer Register (§4)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Automotive OEM Client Accounts, Volume Commitments, and Enquiries.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                if (canManageCustomers) {
                                    Button(
                                        onClick = { showAddCustomerModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("+ Customer", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    if (isAuthenticated) {
                        val serverCrmRows = SharedAccess.rows(sharedSnapshot, "records").filter {
                            SharedAccess.text(it, "record_type") in listOf("Project", "Sales", "Customer")
                        }
                        val serverChangeRows = (customerChanges?.get("requests") as? JsonArray)
                            ?.mapNotNull { it as? JsonObject }.orEmpty()
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Shared TEST Commercial Pipeline · ${serverCrmRows.size} records · ${serverChangeRows.size} change requests",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Scoped by server membership on TEST Supabase; local fictional OEM accounts are hidden.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (serverCrmRows.isEmpty()) {
                            item {
                                Text(
                                    text = "No commercial or project records permitted for this TEST account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        } else {
                            items(serverCrmRows) { row ->
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
                                                text = "${SharedAccess.text(row, "reference").ifBlank { SharedAccess.text(row, "record_type") }} · ${SharedAccess.text(row, "title")}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                                            text = "Type: ${SharedAccess.text(row, "record_type")} · Stage: ${SharedAccess.text(row, "stage").ifBlank { "Active" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                    item {
                        Text(
                            text = "Client Accounts (${customers.size} Registered)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(customers) { cust ->
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
                                            text = cust.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${cust.division} · ${cust.location}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Surface(
                                        color = if (cust.tierRating.contains("Tier-1")) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = cust.tierRating,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cust.tierRating.contains("Tier-1")) Color(0xFF2E7D32) else Color(0xFF1976D2)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Contact: ${cust.contactPerson} | ${cust.email} | ${cust.phone}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "NDA: ${cust.ndaStatus}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (cust.ndaStatus.contains("Active")) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                    if (cust.activeProjects.isNotEmpty()) {
                                        Text(
                                            text = "Projects: ${cust.activeProjects.joinToString()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sales Opportunities & Active Enquiries (${salesOpportunities.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(salesOpportunities) { opp ->
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
                                            text = opp.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Client: ${opp.clientName} | SOR: ${opp.sorDocId}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Surface(
                                        color = when (opp.status) {
                                            "Approved", "Active" -> Color(0xFFE8F5E9)
                                            "Feasibility Review" -> Color(0xFFE3F2FD)
                                            else -> Color(0xFFFFF3E0)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = opp.status,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (opp.status) {
                                                "Approved", "Active" -> Color(0xFF2E7D32)
                                                "Feasibility Review" -> Color(0xFF1976D2)
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
                                        text = "Volume: ${opp.volume} units",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Projected Value: ${opp.projectedValueInr}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
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

    // Signed URL Result Dialog
    signedUrlResult?.let { url ->
        AlertDialog(
            onDismissRequest = { signedUrlResult = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Private Signed URL Generated")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Document: ${selectedDocForSignedUrl?.docId}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "TTL: 300 Seconds (Expires automatically)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = url,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { signedUrlResult = null }) { Text("Done") }
            }
        )
    }

    // Add Customer Account Dialog
    if (showAddCustomerModal) {
        AlertDialog(
            onDismissRequest = { showAddCustomerModal = false },
            title = { Text("Register Customer Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCustName,
                        onValueChange = { newCustName = it },
                        label = { Text("Company Name (e.g. Hero Electric)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustDivision,
                        onValueChange = { newCustDivision = it },
                        label = { Text("Division (e.g. EV Commercial Fleet)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustContact,
                        onValueChange = { newCustContact = it },
                        label = { Text("Contact Person") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustEmail,
                        onValueChange = { newCustEmail = it },
                        label = { Text("Corporate Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustLocation,
                        onValueChange = { newCustLocation = it },
                        label = { Text("Plant / Headquarter Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustName.isNotBlank()) {
                            val newCust = CustomerAccount(
                                id = "CUST-${UUID.randomUUID().toString().take(6).uppercase()}",
                                name = newCustName,
                                division = newCustDivision.ifBlank { "Commercial EV Division" },
                                contactPerson = newCustContact.ifBlank { "Procurement Lead" },
                                email = newCustEmail.ifBlank { "contact@${newCustName.lowercase().replace(" ", "")}.com" },
                                phone = "+91 22 4000 0000",
                                location = newCustLocation.ifBlank { "India" },
                                activeProjects = emptyList(),
                                ndaStatus = "Active & Executed",
                                tierRating = "Tier-1 OEM"
                            )
                            scmsRepository.addCustomer(newCust)
                            newCustName = ""
                            newCustDivision = ""
                            newCustContact = ""
                            newCustEmail = ""
                            newCustLocation = ""
                            showAddCustomerModal = false
                        }
                    },
                    enabled = newCustName.isNotBlank()
                ) {
                    Text("Register Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (!isAuthenticated && showSupportTicketModal) {
        AlertDialog(
            onDismissRequest = { showSupportTicketModal = false },
            title = {
                Text(
                    text = "New Client Support Ticket",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Client: Northstar Field Systems · Fleet Division (TEST)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Dispatched directly to System Engineering & EHW Lead (Avery Vale)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = ticketTitle,
                        onValueChange = { ticketTitle = it },
                        label = { Text("Ticket Subject / Query") },
                        placeholder = { Text("e.g. AIS-140 Antenna Connector Tolerance") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ticketDesc,
                        onValueChange = { ticketDesc = it },
                        label = { Text("Description & Technical Query") },
                        placeholder = { Text("Provide details or query for the engineering squad...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ticketTitle.isNotBlank()) {
                            val result = scmsRepository.submitClientTicket(ticketTitle, ticketDesc)
                            result.onSuccess { tId ->
                                ticketConfirmationMsg = "Ticket #$tId created and logged to Engineering Lead dispatch."
                            }
                            ticketTitle = ""
                            ticketDesc = ""
                            showSupportTicketModal = false
                        }
                    },
                    enabled = ticketTitle.isNotBlank()
                ) {
                    Text("Submit Ticket")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportTicketModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ticketConfirmationMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { ticketConfirmationMsg = null },
            title = { Text("Ticket Dispatched", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { ticketConfirmationMsg = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showSmartUploadModal) {
        SmartUploadModalBottomSheet(
            scmsRepository = scmsRepository,
            selectedProjectId = effectiveProjectId,
            isAuthenticated = isAuthenticated,
            onDismiss = { showSmartUploadModal = false },
            onUploadSuccess = {
                showSmartUploadModal = false
            }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun DocumentControlScreenPreview() {
    com.example.myapplication.ui.theme.MyApplicationTheme {
        DocumentControlScreen(
            scmsRepository = com.example.myapplication.data.repository.ScmsRepositoryImpl()
        )
    }
}

