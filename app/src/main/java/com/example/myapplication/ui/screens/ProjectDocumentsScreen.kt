package com.example.myapplication.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ClientDeliverable
import com.example.myapplication.data.model.ControlledDoc
import com.example.myapplication.data.model.ProjectMetadata
import com.example.myapplication.data.model.SharedAccess
import com.example.myapplication.data.model.SharedDocumentItem
import com.example.myapplication.data.repository.ScmsRepository
import com.example.myapplication.ui.components.ProjectFilterDropdown
import com.example.myapplication.ui.components.resolveProjectSelection
import kotlinx.serialization.json.JsonObject
import java.util.UUID
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

private data class StageDocumentDeliverable(
    val stage: Int,
    val dept: String,
    val code: String,
    val title: String,
    val kind: String
)

/** Standard stage-exit records shown as a checklist for the currently selected project. */
private val stageDocumentDeliverables = listOf(
    StageDocumentDeliverable(1, "SAL", "ENQ", "Customer enquiry and opportunity brief", "AUTO_DRAFT"),
    StageDocumentDeliverable(2, "EHW", "FEA", "Hardware feasibility and assumptions", "AUTO_DRAFT"),
    StageDocumentDeliverable(2, "PUR", "SOURCING", "Critical-part availability and cost check", "AUTO_DRAFT"),
    StageDocumentDeliverable(3, "EHW", "PRS", "Product requirement specification", "AUTO_DRAFT"),
    StageDocumentDeliverable(3, "SAL", "SOR", "Customer SOR and commercial baseline", "UPLOAD_BINARY"),
    StageDocumentDeliverable(3, "HROPS", "CAP", "Skills and capacity readiness", "AUTO_DRAFT"),
    StageDocumentDeliverable(4, "EHW", "SCH", "Circuit schematic and ECAD netlist", "UPLOAD_BINARY"),
    StageDocumentDeliverable(4, "EHW", "GER", "PCB Gerber / fabrication package", "UPLOAD_BINARY"),
    StageDocumentDeliverable(4, "EHW", "EBOM", "Tentative BOM linked to inventory and reusable assets", "AUTO_DRAFT"),
    StageDocumentDeliverable(4, "MECH", "STEP", "Enclosure and mechanical STEP model", "UPLOAD_BINARY"),
    StageDocumentDeliverable(4, "MECH", "DRW", "Dimensioned mechanical drawings", "UPLOAD_BINARY"),
    StageDocumentDeliverable(4, "FW", "FWPKG", "Firmware source / release package", "UPLOAD_BINARY"),
    StageDocumentDeliverable(4, "SW", "SAD", "Software architecture and interface baseline", "AUTO_DRAFT"),
    StageDocumentDeliverable(5, "EMF", "PILOT", "Pilot assembly traveler and line setup", "AUTO_DRAFT"),
    StageDocumentDeliverable(6, "QA", "DVP", "DVP&R plan and results register", "AUTO_DRAFT"),
    StageDocumentDeliverable(6, "QA", "TEST", "Executed test reports and raw evidence", "UPLOAD_BINARY"),
    StageDocumentDeliverable(7, "QA", "CERT", "Product / configuration certification reports", "UPLOAD_BINARY"),
    StageDocumentDeliverable(8, "EMF", "WI", "Electronics manufacturing work instructions", "AUTO_DRAFT"),
    StageDocumentDeliverable(8, "MMF", "MWI", "Mechanical manufacturing work instructions", "AUTO_DRAFT"),
    StageDocumentDeliverable(9, "SIT", "FIELD", "Service / RMA feedback and closure record", "AUTO_DRAFT")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDocumentsScreen(
    scmsRepository: ScmsRepository,
    onOpenRoleSwitcher: () -> Unit = {},
    isAuthenticated: Boolean = false,
    projectCatalog: List<ProjectMetadata> = emptyList(),
    selectedProjectId: String = "ALL",
    sharedSnapshot: JsonObject? = null,
    lifecycleWorkspace: JsonObject? = null,
    lifecycleWorkspaceError: String? = null,
    lifecycleWorkspaceBusy: Boolean = false,
    onRefreshLifecycleWorkspace: (() -> Unit)? = null,
    onLinkReusableAsset: ((String, String, String) -> Unit)? = null,
    loadOriginalRevisions: (suspend (String) -> JsonArray)? = null,
    originalDownload: (suspend (String) -> String)? = null,
    createSharedDocument: (suspend (String, String, String, String, String, String, ByteArray) -> String)? = null
) {
    val documents by scmsRepository.documents.collectAsState()
    val clientDeliverables by scmsRepository.clientDeliverables.collectAsState()
    val apqpStages by scmsRepository.apqpStages.collectAsState()
    val currentRole by scmsRepository.currentRole.collectAsState()
    val localSelectedProject by scmsRepository.selectedProject.collectAsState()

    val activeProjectId = if (isAuthenticated) {
        resolveProjectSelection(selectedProjectId, projectCatalog)
    } else {
        localSelectedProject
    }
    val selectedProjectObj = projectCatalog.firstOrNull { it.id == activeProjectId }
    val sharedRole = (sharedSnapshot?.get("membership") as? JsonObject)
        ?.get("role")?.jsonPrimitive?.contentOrNull.orEmpty()
    val canCreateSharedDocument = isAuthenticated && sharedRole != "viewer" &&
        SharedAccess.departments(sharedSnapshot).isNotEmpty() && createSharedDocument != null

    var selectedTopTab by remember { mutableIntStateOf(0) }
    val currentDeptCode = when (currentRole.code) {
        "ESW" -> "FW"
        "TST" -> "QA"
        else -> currentRole.code
    }
    var selectedDeptFilter by remember(currentRole) {
        mutableStateOf(if (currentRole == com.example.myapplication.data.model.DepartmentRole.ADMIN) "All" else currentDeptCode)
    }
    val effectiveDeptFilter = if (currentRole != com.example.myapplication.data.model.DepartmentRole.ADMIN && selectedDeptFilter == "All") currentDeptCode else selectedDeptFilter
    var selectedDocForPreview by remember { mutableStateOf<ControlledDoc?>(null) }
    var selectedSharedDocForInspect by remember { mutableStateOf<SharedDocumentItem?>(null) }
    val sharedMembership = sharedSnapshot?.get("membership")?.toString()
    LaunchedEffect(isAuthenticated, sharedMembership) {
        selectedSharedDocForInspect = null
    }
    var showDraftModal by remember { mutableStateOf(false) }
    var showSharedDocumentModal by remember { mutableStateOf(false) }
    var reusableAssetMenuExpanded by remember { mutableStateOf(false) }
    var selectedReusableAssetId by remember(activeProjectId) { mutableStateOf("") }
    var reusableAssetAssessment by remember(activeProjectId) { mutableStateOf("") }
    val sharedReusableAssets = (lifecycleWorkspace?.get("assets") as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
    val sharedAssetUses = (lifecycleWorkspace?.get("uses") as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
    LaunchedEffect(isAuthenticated, sharedRole) {
        if (isAuthenticated) onRefreshLifecycleWorkspace?.invoke()
    }
    var editingDraft by remember { mutableStateOf<ControlledDoc?>(null) }
    var pendingUploadDoc by remember { mutableStateOf<ControlledDoc?>(null) }
    val selectedUploadNames = remember { mutableStateMapOf<String, String>() }
    val context = LocalContext.current
    val uploadPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = pendingUploadDoc
        if (uri != null && target != null) {
            val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                ?: uri.lastPathSegment
                ?: "Selected file"
            selectedUploadNames[target.docId] = displayName
            val updatedDoc = target.copy(
                status = "Local file attached (not uploaded)",
                storagePath = "local://$displayName",
                draftContent = (target.draftContent.ifBlank { "" } + "\n\n[Local Attachment: $displayName · Local device only · No backend upload contract available]").trim()
            )
            scmsRepository.addControlledDoc(updatedDoc)
            pendingUploadDoc = null
        }
    }

    val sharedDocs = remember(sharedSnapshot, activeProjectId) {
        if (isAuthenticated) SharedAccess.sharedDocuments(sharedSnapshot, activeProjectId)
        else emptyList()
    }
    val filteredSharedDocs = remember(sharedDocs, effectiveDeptFilter) {
        sharedDocs.filter {
            effectiveDeptFilter == "All" || it.departmentCode.equals(effectiveDeptFilter, ignoreCase = true)
        }
    }
    val localProjectDocs = remember(documents, activeProjectId, effectiveDeptFilter) {
        documents.filter { doc ->
            (activeProjectId == "ALL" || doc.projectId == activeProjectId) &&
                (effectiveDeptFilter == "All" || doc.dept.equals(effectiveDeptFilter, ignoreCase = true))
        }
    }
    val projectClientDeliverables = remember(clientDeliverables, activeProjectId) {
        clientDeliverables.filter {
            activeProjectId == "ALL" ||
            it.projectId == activeProjectId ||
            it.code.startsWith("$activeProjectId-", ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP PROJECT CARD & HEADER
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
                                text = "PROJECT DOSSIER & DOCUMENT CONTROL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isAuthenticated) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = if (isAuthenticated) "Authorized Workspace" else "TEST WORKSPACE · SIMULATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAuthenticated) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (isAuthenticated) {
                            Text(
                                text = if (activeProjectId == "ALL") "Department Documents · All Projects View"
                                else "${selectedProjectObj?.title ?: "Project"} (${selectedProjectObj?.reference ?: activeProjectId})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (activeProjectId == "ALL") "All document metadata returned within your authorized department scope"
                                else "Project view filter (client-side) · Stage: ${selectedProjectObj?.stage?.ifBlank { "Active" }} · Status: ${selectedProjectObj?.status?.ifBlank { "Operational" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = selectedProjectObj?.title ?: "Department Documents",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${selectedProjectObj?.reference ?: activeProjectId} · ${selectedProjectObj?.stage ?: "Project stage unavailable"} · ${selectedProjectObj?.status ?: "Status unavailable"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isAuthenticated) {
                        Button(
                            onClick = { showSharedDocumentModal = true },
                            enabled = activeProjectId != "ALL" && canCreateSharedDocument,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (activeProjectId == "ALL") "Select Project" else "Add document", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    } else {
                        Button(
                            onClick = { showDraftModal = true },
                            enabled = activeProjectId != "ALL",
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Draft Doc", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (activeProjectId == "ALL") "Select Project" else "Draft", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Project View Filter Row
                if (isAuthenticated) {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "PROJECT VIEW FILTER (CLIENT-SIDE):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        ProjectFilterDropdown(
                            selectedProjectId = activeProjectId,
                            onProjectSelected = scmsRepository::selectProject,
                            projects = projectCatalog,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text("PROJECT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(2.dp))
                        ProjectFilterDropdown(
                            selectedProjectId = activeProjectId,
                            onProjectSelected = scmsRepository::selectProject,
                            projects = projectCatalog,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (!isAuthenticated && activeProjectId == "ALL" && selectedTopTab == 0) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Select a project to view its stage-exit checklist.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Sub Tabs
        val tabs = if (isAuthenticated) {
            listOf("Controlled Docs (${filteredSharedDocs.size})", "Security & Keystore")
        } else {
            listOf("Stage Exit Docs", "Project Docs (${localProjectDocs.size})", "Hardware Master Data", "Client Deliverables (${projectClientDeliverables.size})", "Security & Keystore")
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTopTab.coerceIn(0, tabs.lastIndex),
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTopTab == index,
                    onClick = { selectedTopTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTopTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        if (isAuthenticated && activeProjectId != "ALL" && onLinkReusableAsset != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Reusable original revisions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Link a precise revision to this project; source files and stock remain unchanged.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onRefreshLifecycleWorkspace?.invoke() }, enabled = !lifecycleWorkspaceBusy) { Text(if (lifecycleWorkspaceBusy) "Loading…" else "Refresh") }
                    }
                    if (sharedReusableAssets.isEmpty()) {
                        Text(if (lifecycleWorkspaceError != null) "Shared revisions unavailable. ${lifecycleWorkspaceError}" else "No shared reusable revisions are available in your authorized TEST scope.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Box {
                            OutlinedButton(onClick = { reusableAssetMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                val selectedAsset = sharedReusableAssets.firstOrNull { it["id"]?.jsonPrimitive?.contentOrNull == selectedReusableAssetId }
                                val def = selectedAsset?.get("definition") as? JsonObject
                                Text(selectedAsset?.let { "${def?.get("title")?.jsonPrimitive?.contentOrNull ?: "Asset"} · r${def?.get("revision")?.jsonPrimitive?.contentOrNull ?: "?"} · ${def?.get("status")?.jsonPrimitive?.contentOrNull ?: "Draft"} · ${it["id"]?.jsonPrimitive?.contentOrNull?.take(8)}" } ?: "Choose exact reusable revision", maxLines = 1)
                            }
                            DropdownMenu(expanded = reusableAssetMenuExpanded, onDismissRequest = { reusableAssetMenuExpanded = false }) {
                                sharedReusableAssets.forEach { asset ->
                                    val def = asset["definition"] as? JsonObject
                                    DropdownMenuItem(
                                        text = { Text("${def?.get("title")?.jsonPrimitive?.contentOrNull ?: "Asset"} · r${def?.get("revision")?.jsonPrimitive?.contentOrNull ?: "?"} · ${def?.get("status")?.jsonPrimitive?.contentOrNull ?: "Draft"}") },
                                        onClick = { selectedReusableAssetId = asset["id"]?.jsonPrimitive?.contentOrNull.orEmpty(); reusableAssetMenuExpanded = false }
                                    )
                                }
                            }
                        }
                        selectedReusableAssetId.takeIf { it.isNotBlank() }?.let { assetId ->
                            val selectedAsset = sharedReusableAssets.firstOrNull { it["id"]?.jsonPrimitive?.contentOrNull == assetId }
                            val definition = selectedAsset?.get("definition") as? JsonObject
                            Text("Source project ${selectedAsset?.get("sourceProject")?.jsonPrimitive?.contentOrNull.orEmpty()} · document ${selectedAsset?.get("documentId")?.jsonPrimitive?.contentOrNull.orEmpty()} · file ${selectedAsset?.get("fileId")?.jsonPrimitive?.contentOrNull.orEmpty()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("SHA-256 ${definition?.get("checksum")?.jsonPrimitive?.contentOrNull.orEmpty().take(16)}… · ${definition?.get("kind")?.jsonPrimitive?.contentOrNull.orEmpty()} · ${definition?.get("compatibility")?.jsonPrimitive?.contentOrNull.orEmpty()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(value = reusableAssetAssessment, onValueChange = { reusableAssetAssessment = it }, label = { Text("Intended use and compatibility assessment") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                val alreadyLinked = sharedAssetUses.any { it["projectId"]?.jsonPrimitive?.contentOrNull == activeProjectId && it["assetRevisionId"]?.jsonPrimitive?.contentOrNull == assetId }
                                if (alreadyLinked) Text("This exact revision is already linked", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Button(onClick = { onLinkReusableAsset?.invoke(activeProjectId, assetId, reusableAssetAssessment.trim()) }, enabled = !lifecycleWorkspaceBusy && !alreadyLinked && reusableAssetAssessment.trim().length >= 5) { Text(if (lifecycleWorkspaceBusy) "Saving…" else "Link revision") }
                            }
                        }
                    }
                    lifecycleWorkspaceError?.takeIf { selectedReusableAssetId.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                }
            }
        }

        // Department filter row
        val depts = if (isAuthenticated) {
            val serverDepts = SharedAccess.departments(sharedSnapshot).map { SharedAccess.text(it, "code") }.filter { it.isNotBlank() }.distinct()
            if (currentRole == com.example.myapplication.data.model.DepartmentRole.ADMIN) {
                listOf("All") + serverDepts
            } else {
                if (serverDepts.contains(currentDeptCode)) listOf(currentDeptCode) else if (serverDepts.isNotEmpty()) serverDepts else listOf(currentDeptCode)
            }
        } else {
            val availableDepartments = (documents.map { it.dept } + stageDocumentDeliverables.map { it.dept }).distinct().sorted()
            (if (currentRole == com.example.myapplication.data.model.DepartmentRole.ADMIN) listOf("All") else emptyList()) + availableDepartments
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dept Filter:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(depts) { dept ->
                    val isDept = effectiveDeptFilter == dept
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDept) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (!isDept) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
                        modifier = Modifier.clickable { selectedDeptFilter = dept }
                    ) {
                        Text(
                            text = dept,
                            fontSize = 10.sp,
                            fontWeight = if (isDept) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDept) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (!isAuthenticated) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Local TEST Workspace · fictional sample data only. Uploaded files remain on this device and are not shared (no backend upload or signed-URL contract exists). No real customer decision, approval, certification, or release is represented.",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // CONTENT
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isAuthenticated) {
                when (selectedTopTab) {
                    0 -> {
                        // CONTROLLED DOCS (AUTHENTICATED TEST METADATA)
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Department-Authorized Metadata · Client-Side Project View Filter (get_shared_test_workspace)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Mobile edit/upload needs a verified backend contract",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (sharedSnapshot == null) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Shared document metadata unavailable",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "The server workspace snapshot could not be loaded or you do not have an active TEST membership. Local demo records are hidden while authenticated.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (filteredSharedDocs.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = if (activeProjectId != "ALL") "No department documents linked to this project"
                                            else "No department documents found",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (activeProjectId != "ALL")
                                                "No documents returned within your department scope have an explicit link to \"${selectedProjectObj?.title ?: activeProjectId}\". (Project selection is a client-side view filter; backend access is scoped by department.)"
                                            else "No document records were returned within your authorized department scope for this workspace.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredSharedDocs) { doc ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedSharedDocForInspect = doc },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = doc.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = doc.documentType,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Dept: ${doc.departmentCode.ifBlank { "Unassigned" }}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            if (doc.linkedRecordReference.isNotBlank()) {
                                                Text(
                                                    text = "Record: ${doc.linkedRecordReference}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "ID: ${doc.id.take(8)}...",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                text = "Inspect Metadata →",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // SECURITY & KEYSTORE
                        item {
                            SecurityAndKeystoreCard()
                        }
                    }
                }
            } else {
                when (selectedTopTab) {
                    0 -> {
                        // 9-STAGE APQP DOSSIER
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Automotive APQP 9-Stage Document Binder",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "All project engineering gates, feasibility reviews, schematics, and certificates linked to stages.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(apqpStages) { stage ->
                            val stageDocs = localProjectDocs.filter { it.stage.contains("${stage.stageNumber}") || it.stage.contains(stage.name, ignoreCase = true) }
                            val plannedDocs = stageDocumentDeliverables.filter {
                                it.stage == stage.stageNumber && (effectiveDeptFilter == "All" || it.dept.equals(effectiveDeptFilter, true))
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "S${stage.stageNumber}",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stage.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (stage.status == "Approved") Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                                        ) {
                                            Text(
                                                text = stage.status,
                                                color = if (stage.status == "Approved") Color(0xFF2E7D32) else Color(0xFF1565C0),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text("Prerequisite Gate: ${stage.prerequisiteDoc ?: "None"} · ${stage.description}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)

                                    if (stageDocs.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        stageDocs.forEach { doc ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp)
                                                    .clickable { selectedDocForPreview = doc },
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(doc.docId, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        Text(doc.title, fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Inspect", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (plannedDocs.isNotEmpty()) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                        Text("Stage exit deliverables · ${plannedDocs.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        plannedDocs.forEach { requirement ->
                                            val expectedId = "${activeProjectId}-${requirement.dept}-${requirement.code}-01"
                                            val savedDoc = localProjectDocs.firstOrNull {
                                                it.docId.equals(expectedId, true) ||
                                                (activeProjectId != "ALL" && it.projectId == activeProjectId && it.dept.equals(requirement.dept, true) && it.docId.contains(requirement.code, ignoreCase = true))
                                            }
                                            val fileName = selectedUploadNames[expectedId] ?: selectedUploadNames[savedDoc?.docId ?: ""] ?: savedDoc?.storagePath?.takeIf { it.startsWith("local://") }?.removePrefix("local://")
                                            val plannedDoc = savedDoc ?: ControlledDoc(
                                                docId = expectedId,
                                                title = requirement.title,
                                                dept = requirement.dept,
                                                stage = "Stage ${requirement.stage}",
                                                owner = com.example.myapplication.data.model.DepartmentRole.fromCode(requirement.dept).leadName,
                                                revision = "Rev 01",
                                                status = "Expected at stage exit",
                                                projectId = activeProjectId,
                                                documentKind = requirement.kind,
                                                draftContent = "Project: $activeProjectId\nStage exit deliverable: ${requirement.title}\nDepartment: ${requirement.dept}\n\nDraft entries and evidence are local test data until reviewed and authorized."
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Column(Modifier.weight(1f)) {
                                                    Text("${requirement.dept} · ${requirement.title}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                                    Text(
                                                        text = fileName?.let { "Selected on device: $it · local only (not uploaded)" } ?: (savedDoc?.status ?: "Expected at exit · ${if (requirement.kind == "AUTO_DRAFT") "editable draft" else "binary file required"}"),
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (requirement.kind == "AUTO_DRAFT") {
                                                    OutlinedButton(
                                                        onClick = { editingDraft = plannedDoc },
                                                        enabled = activeProjectId != "ALL",
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Edit", fontSize = 10.sp)
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = {
                                                            pendingUploadDoc = plannedDoc
                                                            uploadPicker.launch(arrayOf("*/*"))
                                                        },
                                                        enabled = activeProjectId != "ALL",
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Upload", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // ALL CONTROLLED DOCS REGISTER
                        val filteredDocs = localProjectDocs
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                    Text(
                                        text = "${activeProjectId} · ${filteredDocs.size} project documents · editable drafts and uploaded files are separate",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(filteredDocs) { doc ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDocForPreview = doc },
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
                                        Text(doc.docId, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (doc.status == "Approved") Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
                                        ) {
                                            Text(
                                                text = doc.status,
                                                color = if (doc.status == "Approved") Color(0xFF2E7D32) else Color(0xFFF57F17),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(doc.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Dept: ${doc.dept} · Stage: ${doc.stage} · Owner: ${doc.owner}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(doc.revision, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                        if (doc.documentKind == "AUTO_DRAFT") {
                                            OutlinedButton(onClick = { editingDraft = doc }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)) {
                                                Text("Edit draft", fontSize = 11.sp)
                                            }
                                        } else {
                                            OutlinedButton(onClick = { pendingUploadDoc = doc; uploadPicker.launch(arrayOf("*/*")) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Upload file", fontSize = 11.sp)
                                            }
                                            val attachedName = selectedUploadNames[doc.docId] ?: doc.storagePath.takeIf { it.startsWith("local://") }?.removePrefix("local://")
                                            attachedName?.let { name ->
                                                Text("Selected on device: $name · local only (not uploaded)", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.align(Alignment.CenterVertically))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // HARDWARE MASTER DATA & FEASIBILITY VIEW
                        val hw = scmsRepository.getHardwareMasterData(activeProjectId)
                        item {
                            HardwareMasterDataView(hw = hw)
                        }
                    }

                    3 -> {
                        // CLIENT OEM DELIVERABLES
                        items(projectClientDeliverables) { deliv ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                        Text(deliv.code, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                                            Text(deliv.status, color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text(deliv.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(deliv.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Standard: ${deliv.standard} · Owner: ${deliv.owner}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(deliv.date, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // SECURITY & KEYSTORE
                        item {
                            SecurityAndKeystoreCard()
                        }
                    }
                }
            }
        }
    }

    // FULL PAGE DOCUMENT PREVIEW MODAL WITH COMPANY HEADER & FOOTER (DEMO ONLY)
    if (!isAuthenticated) {
        selectedDocForPreview?.let { doc ->
            ModalBottomSheet(
                onDismissRequest = { selectedDocForPreview = null },
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                LocalDocumentDetailsSheet(
                    doc = doc,
                    onClose = { selectedDocForPreview = null },
                    selectedFileName = selectedUploadNames[doc.docId]
                )
            }
        }
    }

    // SHARED DOCUMENT METADATA SHEET (AUTHENTICATED ONLY)
    if (isAuthenticated) {
        selectedSharedDocForInspect?.let { doc ->
            ModalBottomSheet(
                onDismissRequest = { selectedSharedDocForInspect = null },
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                SharedDocumentSheetView(
                    doc = doc,
                    onClose = { selectedSharedDocForInspect = null },
                    loadOriginalRevisions = loadOriginalRevisions,
                    originalDownload = originalDownload
                )
            }
        }
        if (showSharedDocumentModal && activeProjectId != "ALL" && createSharedDocument != null) {
            SharedTestDocumentDialog(
                project = selectedProjectObj,
                departmentOptions = SharedAccess.departments(sharedSnapshot),
                initialDepartmentId = run {
                    val member = sharedSnapshot?.get("membership") as? JsonObject
                    val departments = SharedAccess.departments(sharedSnapshot)
                    val selectedCode = member?.get("department_code")?.jsonPrimitive?.contentOrNull.orEmpty()
                    departments.firstOrNull { SharedAccess.text(it, "code") == selectedCode }?.let { SharedAccess.text(it, "id") }
                        ?: departments.firstOrNull()?.let { SharedAccess.text(it, "id") }.orEmpty()
                },
                onDismiss = { showSharedDocumentModal = false },
                onSave = createSharedDocument,
                onSaved = { showSharedDocumentModal = false }
            )
        }
    }

    // DRAFT NEW DOCUMENT DIALOG (DEMO ONLY)
    if (!isAuthenticated && showDraftModal) {
        DraftDocumentDialog(
            scmsRepository = scmsRepository,
            projectId = activeProjectId,
            initialDepartment = if (currentRole == com.example.myapplication.data.model.DepartmentRole.ADMIN) "EHW" else currentRole.code,
            onDismiss = { showDraftModal = false },
            onSuccess = { showDraftModal = false }
        )
    }
    if (!isAuthenticated) {
        editingDraft?.let { doc ->
            DraftDocumentDialog(
                scmsRepository = scmsRepository,
                projectId = activeProjectId,
                initialDepartment = doc.dept,
                existingDoc = doc,
                onDismiss = { editingDraft = null },
                onSuccess = { editingDraft = null }
            )
        }
    }
}

@Composable
private fun SharedTestDocumentDialog(
    project: ProjectMetadata?,
    departmentOptions: List<JsonObject>,
    initialDepartmentId: String,
    onDismiss: () -> Unit,
    onSave: suspend (String, String, String, String, String, String, ByteArray) -> String,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var departmentId by remember(initialDepartmentId) { mutableStateOf(initialDepartmentId) }
    var departmentMenu by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("Technical document") }
    var typeMenu by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    var contentType by remember { mutableStateOf("application/octet-stream") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                    ?: uri.lastPathSegment.orEmpty()
                val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw java.io.IOException("The selected file could not be read")
                require(data.isNotEmpty() && data.size <= 25 * 1024 * 1024) { "Choose a file up to 25 MB" }
                fileName = name
                bytes = data
                contentType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                error = null
            }.onFailure { error = it.message ?: "Unable to read selected file" }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Add project document", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("${project?.reference.orEmpty()} · ${project?.title.orEmpty()}", style = MaterialTheme.typography.labelMedium)
                Text("Current phase: ${project?.stage?.ifBlank { "Not set" } ?: "Not set"}. The original remains an unapproved Draft until reviewed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (departmentOptions.size > 1) {
                    Box {
                        OutlinedButton(onClick = { departmentMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Department · ${departmentOptions.firstOrNull { SharedAccess.text(it, "id") == departmentId }?.let { SharedAccess.text(it, "code") }.orEmpty()}")
                        }
                        DropdownMenu(expanded = departmentMenu, onDismissRequest = { departmentMenu = false }) {
                            departmentOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(SharedAccess.text(option, "name")) }, onClick = {
                                    departmentId = SharedAccess.text(option, "id")
                                    departmentMenu = false
                                })
                            }
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Document title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Document type · $type") }
                    DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        listOf("Technical document", "Schematic", "BOM", "Gerber ZIP", "Mechanical CAD", "Firmware package", "Test report", "Other").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { type = option; typeMenu = false })
                        }
                    }
                }
                OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (fileName.isBlank()) "Choose original file" else "Original · $fileName")
                }
                Text("Saved once to the authenticated TEST workspace; byte checksum is verified. No approval or release is implied.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = !busy && title.isNotBlank() && bytes != null && departmentId.isNotBlank(), onClick = {
                val selectedProject = project ?: return@Button
                val selectedBytes = bytes ?: return@Button
                busy = true
                error = null
                scope.launch {
                    runCatching { onSave(selectedProject.id, departmentId, title.trim(), type, fileName, contentType, selectedBytes) }
                        .onSuccess { onSaved() }
                        .onFailure { error = it.message ?: "Save failed. Retry with the same document and file." }
                    busy = false
                }
            }) { Text(if (busy) "Saving…" else "Save draft & upload") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } }
    )
}

// -----------------------------------------------------------------------------
// Hardware Master Data & Feasibility Analysis View (Local Simulation)
// -----------------------------------------------------------------------------

@Composable
fun HardwareMasterDataView(hw: com.example.myapplication.data.model.HardwareMasterData) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Requirements & Provenance
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("1. Requirements & Hardware Provenance", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                DocMetadataRow("Project Scope", hw.projectName)
                DocMetadataRow("Requirements Reference", hw.requirementsAndProvenance.customerSorReference)
                DocMetadataRow("Provenance Core", hw.requirementsAndProvenance.provenanceBaseline)
                DocMetadataRow("Voltage Range", hw.requirementsAndProvenance.voltageRange)
                DocMetadataRow("Operating Temp", hw.requirementsAndProvenance.operatingTemp)
                DocMetadataRow("Enclosure Rating", hw.requirementsAndProvenance.ingressProtection)
                DocMetadataRow("Deep Sleep Budget", hw.requirementsAndProvenance.powerBudgetDeepSleep)
                DocMetadataRow("Active Burst Budget", hw.requirementsAndProvenance.powerBudgetActive)
            }
        }

        // 2. Certification Separation Notice (Component vs System Configuration)
        Surface(
            color = Color(0xFFFFF3E0),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CONFIGURATION CERTIFICATION: PENDING / NOT CERTIFIED", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Component-level qualifications (AEC-Q100 IC silicon, UL94-V0 enclosure resin, USCAR-2 harness terminals) apply only to isolated parts. Complete system configuration certification requires physical multi-track DVP&R, vehicle chamber EMC testing, and formal ARAI/ICAT type approval.",
                    fontSize = 11.sp,
                    color = Color(0xFF5D4037)
                )
            }
        }

        // 3. Reusable Modular Hardware Assets & Revisions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("2. Reusable Modular Hardware Assets & Revisions", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                hw.reusableAssets.forEach { asset ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${asset.assetId} · ${asset.name}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(asset.revision, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Text("Reusability: ${asset.reusabilityScope}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Component Cert: ${asset.componentCertification}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                            Text("System Cert: ${asset.systemCertificationStatus}", fontSize = 10.sp, color = Color(0xFFE65100))
                        }
                    }
                }
            }
        }

        // 4. Tentative BOM Lines Linked to Inventory Items
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("3. Tentative Engineering BOM (Linked Inventory Lines)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                hw.tentativeBom.forEach { line ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(line.mpn, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Qty: ${line.quantityPerUnit} · $${line.estimatedUnitCostUsd}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(line.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Supplier: ${line.supplier} (${line.leadTimeWeeks}w lead)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                if (line.inventoryLinkMpn != null) {
                                    Text("Inventory Link: ${line.inventoryLinkMpn}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0))
                                } else {
                                    Text("No catalogue match", fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                val bomSubtotalUsd = hw.tentativeBom.sumOf { it.quantityPerUnit * it.estimatedUnitCostUsd }
                val formattedSubtotal = String.format(java.util.Locale.US, "%.2f", bomSubtotalUsd)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Included Lines Subtotal", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(
                        text = if (hw.budgetSummary.isPartial) "$$formattedSubtotal (Partial)" else "$$formattedSubtotal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (hw.budgetSummary.isPartial && hw.budgetSummary.excludedItemsNote != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Exclusions: ${hw.budgetSummary.excludedItemsNote}",
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // 5. Project Budget Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("4. Project Budget & Feasibility Analysis", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                DocMetadataRow("Prototype Lot Target", "${hw.budgetSummary.prototypeLotSize} units (PoC)")
                val formattedBomCostUsd = String.format(java.util.Locale.US, "%.2f", hw.budgetSummary.estimatedBomCostPerUnitUsd)
                val bomLabel = if (hw.budgetSummary.isPartial) {
                    "$$formattedBomCostUsd (~₹${hw.budgetSummary.estimatedBomCostPerUnitInr.toInt()}) [Partial BOM]"
                } else {
                    "$$formattedBomCostUsd (~₹${hw.budgetSummary.estimatedBomCostPerUnitInr.toInt()})"
                }
                DocMetadataRow("Estimated Unit BOM", bomLabel)
                if (hw.budgetSummary.isPartial && hw.budgetSummary.excludedItemsNote != null) {
                    DocMetadataRow("BOM Exclusions", hw.budgetSummary.excludedItemsNote)
                }
                DocMetadataRow("NRE & Tooling Estimate", "₹${hw.budgetSummary.estimatedNreAndToolingInr.toInt()}")
                DocMetadataRow("Target Volume BOM", "₹${hw.budgetSummary.targetMassProductionBomInr.toInt()}")
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = hw.budgetSummary.statusDisclaimer,
                        color = Color(0xFFF57F17),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }

        // 6. Separation: Editable Generated Drafts vs Uploaded Binary Deliverables
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("5. Artifact Separation: Editable Drafts vs Binary Deliverables", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("A. Editable Generated Drafts (Workspace Text/Data):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Text("• Customer SOR / Requirements Synthesis (Draft text · editable in workspace)", fontSize = 10.sp)
                Text("• Cross-Department Feasibility Assessment (Draft text · editable in workspace)", fontSize = 10.sp)
                Text("• Tentative Engineering BOM Specification (Draft data · editable in workspace)", fontSize = 10.sp)
                Text("• Prototype Budget & Cost Modeling Sheet (Draft numbers · editable in workspace)", fontSize = 10.sp)

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Text("B. Uploaded Binary Deliverables (Repository File Status):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                hw.binaryDeliverables.forEach { bin ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${bin.deliverableType} (${bin.fileExtension})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(bin.details, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFEBEE)) {
                            Text(bin.status, color = Color(0xFFC62828), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Security & Keystore Card (Shared)
// -----------------------------------------------------------------------------

@Composable
fun SecurityAndKeystoreCard() {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Android Keystore & Encrypted Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("• Token Storage: EncryptedSharedPreferences (AES256_GCM)", fontSize = 11.sp)
            Text("• Keystore Master Key: _androidx_security_master_key_", fontSize = 11.sp)
            Text("• Plaintext SharedPreferences: Permanently Disabled", fontSize = 11.sp)
            Text("• ISO/IEC 27001:2022 Private Storage TTL: 300 seconds", fontSize = 11.sp)
            Text("• Service Keys / DB Passwords in APK: 0 (Zero)", fontSize = 11.sp)
        }
    }
}

// -----------------------------------------------------------------------------
// Shared Document Metadata Inspection Sheet (Authenticated Only)
// -----------------------------------------------------------------------------

@Composable
fun SharedDocumentSheetView(
    doc: SharedDocumentItem,
    onClose: () -> Unit,
    loadOriginalRevisions: (suspend (String) -> JsonArray)? = null,
    originalDownload: (suspend (String) -> String)? = null
) {
    var originals by remember(doc.id) { mutableStateOf<JsonArray?>(null) }
    var originalError by remember(doc.id) { mutableStateOf<String?>(null) }
    var opening by remember(doc.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(doc.id, loadOriginalRevisions) {
        if (loadOriginalRevisions != null) {
            try { originals = loadOriginalRevisions(doc.id) }
            catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                originalError = "Original revisions could not be loaded. Reopen this document to retry."
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Controlled Document", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = doc.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    DocMetadataRow("Document Type", doc.documentType)
                    DocMetadataRow("Department", doc.departmentCode.ifBlank { "Unassigned" })
                    DocMetadataRow("Linked Record", doc.linkedRecordReference.ifBlank { doc.linkedRecordId.ifBlank { "Direct Project Link" } })
                    DocMetadataRow("Project Link", doc.projectId.ifBlank { "None (Department-wide)" })
                    Text("Original revisions", fontWeight = FontWeight.SemiBold)
                    originalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (loadOriginalRevisions == null) Text("Original access is unavailable in this view.")
                    else if (originals == null && originalError == null) Text("Loading originals…")
                    else if (originals?.isEmpty() == true) Text("No original file has been uploaded.")
                    originals?.forEach { value ->
                        val file = value.jsonObject
                        val fileId = file["id"]?.jsonPrimitive?.content.orEmpty()
                        Text("${file["original_name"]?.jsonPrimitive?.content.orEmpty()} · Rev ${file["revision"]?.jsonPrimitive?.content.orEmpty()}")
                        OutlinedButton(enabled = !opening && fileId.isNotBlank() && originalDownload != null, onClick = {
                            scope.launch {
                                opening = true
                                originalError = null
                                try { uriHandler.openUri(originalDownload!!(fileId)) }
                                catch (e: Exception) {
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    originalError = "Original could not be opened. Check access and retry."
                                } finally { opening = false }
                            }
                        }) { Text(if (opening) "Verifying…" else "Open original") }
                    }
                    DocMetadataRow("Server Document ID", doc.id)
                    DocMetadataRow("Created Timestamp", doc.createdAt.ifBlank { "Unknown" })

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Opening an original checks your access and verifies the stored file. Uploading does not approve or release a document.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun DocMetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// -----------------------------------------------------------------------------
// Full In-App Document Preview Sheet with Company Header & Compliance Footer (Demo)
// -----------------------------------------------------------------------------

@Composable
private fun LocalDocumentDetailsSheet(doc: ControlledDoc, onClose: () -> Unit, selectedFileName: String?) {
    val attachedFileName = selectedFileName ?: doc.storagePath.takeIf { it.startsWith("local://") }?.removePrefix("local://")
    Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Document record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
        Text(doc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        DocMetadataRow("Project", doc.projectId.ifBlank { "Not linked" })
        DocMetadataRow("Department", doc.dept)
        DocMetadataRow("Lifecycle stage", doc.stage)
        DocMetadataRow("Document ID / revision", "${doc.docId} · ${doc.revision}")
        DocMetadataRow("Record state", doc.status)
        DocMetadataRow("Document handling", if (doc.documentKind == "AUTO_DRAFT") "Editable draft fields" else "External binary file")

        // Attachment / Local Storage status
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "STORAGE BOUNDARY: LOCAL-ONLY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (attachedFileName != null) {
                        "File attached locally: $attachedFileName · Remains on device only (no shared backend upload contract)."
                    } else {
                        "No shared upload contract exists in Android mobile. Files selected on device remain strictly local-only."
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (doc.documentKind == "AUTO_DRAFT" && doc.draftContent.isNotBlank()) {
            HorizontalDivider()
            Text("Draft entries", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(doc.draftContent, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (doc.documentKind != "AUTO_DRAFT") {
            Text(
                attachedFileName?.let { "Selected on this device: $it. It has not been uploaded or shared." }
                    ?: "A design, test, or certification file must be selected. No file is attached to this local record yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Text("Local test workspace record · not an approval or released controlled copy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
fun FullDocumentSheetView(
    doc: ControlledDoc,
    scmsRepository: ScmsRepository,
    onClose: () -> Unit,
    onSignedUrlGenerated: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Action bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Controlled Document Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        // WHITE A4 SHEET DOCUMENT CONTAINER
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // COMPANY HEADER TABLE
                    Surface(
                        color = Color(0xFFF8F9FA),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0D7DE)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                        color = Color(0xFF1565C0),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("SS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("NORTHSTAR SAMPLE LABS · FICTIONAL TEST", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                        Text("LOCAL WORKSPACE DOCUMENT PREVIEW", color = Color.DarkGray, fontSize = 9.sp)
                                    }
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFF3E0)) {
                                    Text("UNCONTROLLED SAMPLE", color = Color(0xFFE65100), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFD0D7DE))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Document No:", color = Color.Gray, fontSize = 9.sp)
                                    Text(doc.docId, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Column {
                                    Text("Revision:", color = Color.Gray, fontSize = 9.sp)
                                    Text(doc.revision, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Column {
                                    Text("Status:", color = Color.Gray, fontSize = 9.sp)
                                    Text(doc.status, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // DOCUMENT TITLE
                    Text(
                        text = doc.title,
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Department: ${doc.dept} · Lifecycle Stage: ${doc.stage} · Author: ${doc.owner}",
                        color = Color.DarkGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fictional, editable sample content; never evidence of an actual customer or release.
                    Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "FICTIONAL TEST DRAFT · Illustrative assumptions only. No real customer review, approval, test result, certification, or production release is represented.",
                            modifier = Modifier.padding(8.dp), color = Color(0xFF8A3B00), fontSize = 10.sp, fontWeight = FontWeight.Bold
                        )
                    }

                    // SECTION 1.0 SCOPE & VEHICLE IDENTIFICATION
                    Text("1.0 Scope & Sample Project", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        text = "This editable example describes a fictional 4G field-telematics gateway concept for Northstar Mobility Works. Scope, quantities, interfaces, and target environments are placeholders for workflow testing and have not been agreed with a customer.",
                        color = Color(0xFF333333),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // SECTION 2.0 TECHNICAL SPECIFICATIONS TABLE
                    Text("2.0 Technical Architecture Parameters", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Surface(
                        color = Color(0xFFF8F9FA),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Microcontroller", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("Candidate MCU · TBD after feasibility", color = Color.DarkGray, fontSize = 10.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cellular Modem", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("Candidate cellular/GNSS module · unverified", color = Color.DarkGray, fontSize = 10.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vehicle Bus Interface", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("Vehicle interface · requirements not frozen", color = Color.DarkGray, fontSize = 10.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Enclosure Protection", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("Enclosure target · not tested or rated", color = Color.DarkGray, fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Unsigned stage-exit review placeholders.
                    Text("3.0 Stage Exit Checklist · Not Verified", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Surface(
                        color = Color(0xFFFFF3E0),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCC80)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("○ EHW — schematic and netlist evidence not attached", color = Color(0xFF8A3B00), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("○ QA — no measured test results or certification evidence", color = Color(0xFF8A3B00), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("○ Customer review — no real customer decision is represented", color = Color(0xFF8A3B00), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // COMPLIANCE FOOTER
                    HorizontalDivider(color = Color(0xFFD0D7DE))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("FICTIONAL TEST WORKSPACE · NOT A CONTROLLED COPY", color = Color(0xFF8A3B00), fontSize = 8.sp)
                        Text("Illustrative preview · Page 1 of 1", color = Color.Gray, fontSize = 9.sp)
                    }
                }
            }
        }

        // Actions: Storage Notice and Close Preview (No mock signed URLs)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Signed URL / remote storage RPC unavailable in Android contract · local preview only",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }

                Button(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close Preview", fontSize = 11.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// -----------------------------------------------------------------------------
// Draft Document Dialog (Demo Only)
// -----------------------------------------------------------------------------

@Composable
fun DraftDocumentDialog(
    scmsRepository: ScmsRepository,
    projectId: String,
    initialDepartment: String,
    existingDoc: ControlledDoc? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var docType by remember(existingDoc?.docId) { mutableStateOf(existingDoc?.docId?.split("-")?.getOrNull(2) ?: "PRS") }
    var dept by remember(existingDoc?.docId) { mutableStateOf(existingDoc?.dept ?: initialDepartment) }
    var title by remember(existingDoc?.docId) { mutableStateOf(existingDoc?.title ?: "") }
    var stage by remember(existingDoc?.docId) { mutableStateOf(existingDoc?.stage ?: "Stage 3") }
    var content by remember(existingDoc?.docId) {
        mutableStateOf(existingDoc?.draftContent?.takeIf { it.isNotBlank() } ?: "Purpose and scope\nRequirements / inputs\nProject-specific assumptions\nEvidence and references\nReview notes")
    }
    val autoDocId = existingDoc?.docId ?: "$projectId-$dept-$docType-01"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingDoc == null) "Create editable document draft" else "Edit document draft", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Auto-Computed Document ID:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(autoDocId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    placeholder = { Text("e.g. Firmware Architecture Spec") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = stage,
                    onValueChange = { stage = it },
                    label = { Text("Lifecycle stage") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Editable draft entries") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dept,
                        onValueChange = { dept = it },
                        label = { Text("Dept") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = existingDoc == null
                    )
                    OutlinedTextField(
                        value = docType,
                        onValueChange = { docType = it },
                        label = { Text("Type") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = existingDoc == null
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newDoc = ControlledDoc(
                        docId = autoDocId,
                        title = if (title.isNotBlank()) title else "${docType.uppercase()} document draft",
                        dept = dept,
                        stage = stage,
                        owner = existingDoc?.owner ?: com.example.myapplication.data.model.DepartmentRole.fromCode(dept).leadName,
                        revision = existingDoc?.revision ?: "Rev 01",
                        status = if (existingDoc == null) "Draft · local test" else "Draft · local test",
                        projectId = projectId,
                        documentKind = "AUTO_DRAFT",
                        draftContent = content
                    )
                    scmsRepository.addControlledDoc(newDoc)
                    onSuccess()
                }
            ) {
                Text(if (existingDoc == null) "Save draft" else "Save changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
