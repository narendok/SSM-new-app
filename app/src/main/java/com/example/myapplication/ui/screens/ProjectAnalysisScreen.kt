package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.ScmsRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Dedicated Project Analysis Screen (Tab 2)
 * Provides comprehensive visibility into Multi-Track Engineering Lifecycles (TG-0 to TG-9),
 * 110 configured APQP stages, prerequisite interlocks, departmental RACI workload, and budget burn.
 */
@Composable
fun ProjectAnalysisScreen(
    scmsRepository: ScmsRepository,
    selectedProjectId: String,
    projectCatalog: List<ProjectMetadata>,
    isAuthenticated: Boolean = false,
    sharedSnapshot: JsonObject? = null,
    lifecycleWorkspace: JsonObject? = null,
    viewDepartment: String? = null,
    onOpenBusinessWorkspace: ((String) -> Unit)? = null,
    onOpenDepartmentRecords: (() -> Unit)? = null,
    onOpenApprovals: (() -> Unit)? = null
) {
    val stages by scmsRepository.apqpStages.collectAsState()
    val tasks by scmsRepository.tasks.collectAsState()
    val documents by scmsRepository.documents.collectAsState()
    val activeProjectObj = projectCatalog.firstOrNull { it.id == selectedProjectId }
        ?: AutomotiveProjects.all.firstOrNull { it.id == selectedProjectId }

    if (isAuthenticated && selectedProjectId == "ALL") {
        DepartmentAnalyticsOverview(
            snapshot = sharedSnapshot,
            viewDepartment = viewDepartment,
            onOpenBusinessWorkspace = onOpenBusinessWorkspace,
            onOpenDepartmentRecords = onOpenDepartmentRecords,
            onOpenApprovals = onOpenApprovals
        )
        return
    }

    if (isAuthenticated) {
        val membership = sharedSnapshot?.get("membership") as? JsonObject
        val authenticatedRole = membership?.let { SharedAccess.text(it, "role") }.orEmpty()
        val departmentCodeRaw = membership?.let { SharedAccess.text(it, "department_code") }.orEmpty().uppercase()
        val departmentCode = when (departmentCodeRaw) {
            "EFW" -> "FW"
            "CSW" -> "SW"
            "HR", "OPS", "HR/OPS" -> "HROPS"
            else -> departmentCodeRaw
        }
        val memberDepartmentIds = buildSet {
            membership?.let { SharedAccess.text(it, "department_id") }?.takeIf(String::isNotBlank)?.let(::add)
            SharedAccess.departments(sharedSnapshot)
                .filter { SharedAccess.text(it, "code").equals(departmentCode, true) }
                .map { SharedAccess.text(it, "id") }.filter(String::isNotBlank).forEach(::add)
        }
        val visibleRecordTypes = when {
            authenticatedRole == "admin" -> null
            departmentCode == "SAL" -> setOf("Project", "Sales", "Customer", "SOR", "RFQ", "Feasibility")
            departmentCode == "PUR" -> setOf("Project", "BOM", "Purchase order", "Supplier", "Quotation", "Purchase requisition")
            departmentCode == "FIN" -> setOf("Project", "Invoice", "Expense", "Payment", "Purchase order")
            departmentCode in setOf("QA", "TST") -> setOf("Project", "Inspection", "Test report", "DVP&R", "Calibration", "Nonconformance", "Engineering change")
            departmentCode in setOf("EHW", "FW", "SW", "MECH") -> setOf("Project", "BOM", "Schematic", "CAD", "Firmware", "Software", "Engineering change", "Test report")
            departmentCode in setOf("EMF", "MMF") -> setOf("Project", "Production order", "Part", "Inspection", "Equipment", "BOM")
            departmentCode == "HROPS" -> setOf("Project", "HR document", "Training", "Compliance")
            departmentCode == "SIT" -> setOf("Project", "IT ticket", "Asset", "Service ticket")
            else -> emptySet()
        }
        val sharedTasks = SharedAccess.rows(sharedSnapshot, "tasks")
            .filter { selectedProjectId != "ALL" && SharedAccess.projectIdForTask(sharedSnapshot, it) == selectedProjectId }
            .filter { task ->
                authenticatedRole == "admin" ||
                    SharedAccess.text(task, "department_id") in memberDepartmentIds ||
                    SharedAccess.text(task, "department_code").equals(departmentCode, ignoreCase = true)
            }
        val sharedRecords = SharedAccess.rows(sharedSnapshot, "records").filter { record ->
            if (visibleRecordTypes != null && SharedAccess.text(record, "record_type") !in visibleRecordTypes) return@filter false
            if (selectedProjectId == "ALL") return@filter false
            if (SharedAccess.text(record, "id") == selectedProjectId) return@filter true
            if (authenticatedRole != "admin") {
                val ownerDepartmentId = SharedAccess.text(record, "department_id")
                val ownerDepartmentCode = SharedAccess.text(record, "department_code")
                if (ownerDepartmentId.isNotBlank() && ownerDepartmentId !in memberDepartmentIds) return@filter false
                if (ownerDepartmentCode.isNotBlank() && !ownerDepartmentCode.equals(departmentCode, true)) return@filter false
                if (ownerDepartmentId.isBlank() && ownerDepartmentCode.isBlank() && SharedAccess.text(record, "record_type") !in setOf("Project", "Sales", "Customer", "SOR", "RFQ", "Feasibility")) return@filter false
            }
            val fields = record["fields"] as? JsonObject
            val linkedProject = listOf("project_id", "projectId", "project_code", "projectCode")
                .firstNotNullOfOrNull { key -> fields?.get(key)?.jsonPrimitive?.contentOrNull }
                .orEmpty()
            linkedProject == selectedProjectId || linkedProject == activeProjectObj?.reference
        }
        val selectedProjectRecord = SharedAccess.rows(sharedSnapshot, "records")
            .firstOrNull { SharedAccess.text(it, "id") == selectedProjectId && SharedAccess.text(it, "record_type") == "Project" }
        val projectFields = selectedProjectRecord?.get("fields") as? JsonObject
        val currentStage = listOf("currentStage", "current_stage", "stage")
            .firstNotNullOfOrNull { key -> projectFields?.get(key)?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) }
            .orEmpty().ifBlank { selectedProjectRecord?.let { SharedAccess.text(it, "stage") }.orEmpty() }
        val projectTrack = listOf("track", "lifecycleTrack", "lifecycle_track")
            .firstNotNullOfOrNull { key -> projectFields?.get(key)?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) }
            .orEmpty()
        val completedStages = (projectFields?.get("completedStages") as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }?.distinct().orEmpty()
        val dependencies = listOf("dependsOn", "dependencies", "predecessors")
            .firstNotNullOfOrNull { key ->
                when (val value = projectFields?.get(key)) {
                    is JsonArray -> value.mapNotNull { it.jsonPrimitive.contentOrNull }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                    is JsonPrimitive -> value.contentOrNull?.takeIf(String::isNotBlank)
                    else -> null
                }
            }.orEmpty()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Project lifecycle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(activeProjectObj?.title ?: "Select a permitted project", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (selectedProjectId == "ALL") "Choose one project to see its authorized tasks and linked records."
                            else "${sharedTasks.size} authorized tasks · ${sharedRecords.size} linked records. Stage progress and approvals are not shown until a shared execution-state feed is available.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Lifecycle from shared project record", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Current stage: ${currentStage.ifBlank { "Not recorded" }}", style = MaterialTheme.typography.bodySmall)
                        if (projectTrack.isNotBlank()) Text("Track: $projectTrack", style = MaterialTheme.typography.bodySmall)
                        Text("Completed stages recorded: ${completedStages.size}${if (completedStages.isNotEmpty()) " · ${completedStages.joinToString(" · ")}" else ""}", style = MaterialTheme.typography.bodySmall)
                        Text("Dependencies: ${dependencies.ifBlank { "Not recorded in the shared project record" }}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (selectedProjectId != "ALL" && sharedTasks.isEmpty() && sharedRecords.isEmpty()) {
                item { Text("No project-linked records or tasks were returned for your account.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(sharedTasks) { task ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(SharedAccess.text(task, "title").ifBlank { "Untitled task" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${SharedAccess.text(task, "status").ifBlank { "Status unavailable" }} · ${SharedAccess.text(task, "priority").ifBlank { "Priority unavailable" }}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            items(sharedRecords) { record ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(SharedAccess.text(record, "reference").ifBlank { SharedAccess.text(record, "record_type") }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(SharedAccess.text(record, "title"), style = MaterialTheme.typography.bodySmall)
                        Text(SharedAccess.text(record, "status").ifBlank { "Status unavailable" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        return
    }

    var selectedPhaseFilter by remember { mutableStateOf("ALL") } // ALL, TG_0..TG_9
    var selectedStageForDetail by remember { mutableStateOf<ApqpStage?>(null) }

    val approvedCount = stages.count { it.status == "Approved" }
    val inProgressCount = stages.count { it.status == "In Progress" }
    val lockedCount = stages.count { it.status == "Locked" }
    val totalStages = stages.size.coerceAtLeast(1)
    val overallProgressPct = (approvedCount * 100) / totalStages

    // Departmental RACI Breakdown
    val depts = listOf("EHW", "FW", "SW", "QA", "PUR", "EMF")
    val deptTaskCounts = remember(tasks) {
        depts.associateWith { dept ->
            tasks.count { it.dept.equals(dept, ignoreCase = true) && !it.isCompleted }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Project Analysis Header & Track Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Project Work & APQP Health",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${activeProjectObj?.title ?: "Multi-Track Engineering"} · ${activeProjectObj?.client ?: "Northstar Field Systems (TEST)"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "TG-0 to TG-9",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Overall Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lifecycle Progress: $overallProgressPct%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$approvedCount approved · $inProgressCount active · $lockedCount locked",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { overallProgressPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }
        }

        // 2. High-Level KPI Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // APQP Gate Status Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gate Velocity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lockedCount > 0) "Prereq Locked" else "On Track",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (lockedCount > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                        Text("$lockedCount gate blockers", style = MaterialTheme.typography.bodySmall, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Sourced Commercial Burn Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Commercial", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹20.33 Cr",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("Locked Volume Deal", style = MaterialTheme.typography.bodySmall, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Deliverables Readiness
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Deliverables", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${documents.size} Docs",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("38-Family Audit", style = MaterialTheme.typography.bodySmall, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 3. Departmental Workload & RACI Contribution
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Departmental Workload & RACI Queue",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Open action items and cross-department handoff status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        depts.forEach { dept ->
                            val count = deptTaskCounts[dept] ?: 0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (count > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$count",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = dept,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Phase Selection Filter Chips (TG-0 to TG-9)
        item {
            Text(
                text = "Tollgates & Stage Execution (TG-0 to TG-9)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedPhaseFilter == "ALL",
                        onClick = { selectedPhaseFilter = "ALL" },
                        label = { Text("All Stages (${stages.size})", fontSize = 11.sp) }
                    )
                }
                (0..9).forEach { phaseNum ->
                    val phaseCode = "TG_$phaseNum"
                    val phaseLabel = "TG-$phaseNum"
                    item {
                        FilterChip(
                            selected = selectedPhaseFilter == phaseCode,
                            onClick = { selectedPhaseFilter = phaseCode },
                            label = { Text(phaseLabel, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // 5. APQP Stages Stepper & Execution Cards
        val filteredStages = if (selectedPhaseFilter == "ALL") stages else {
            stages.filter { it.phase.equals(selectedPhaseFilter, ignoreCase = true) }
        }

        items(filteredStages) { stage ->
            val statusColor = when (stage.status) {
                "Approved" -> Color(0xFF2E7D32)
                "In Progress" -> Color(0xFF1565C0)
                "Locked" -> Color(0xFFC62828)
                else -> Color(0xFFF57C00)
            }
            val statusBg = when (stage.status) {
                "Approved" -> Color(0xFFE8F5E9)
                "In Progress" -> Color(0xFFE3F2FD)
                "Locked" -> Color(0xFFFFEBEE)
                else -> Color(0xFFFFF3E0)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedStageForDetail = stage },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = statusBg,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${stage.stageNumber}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = statusColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stage.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Lead: ${stage.leadDepartment} · ${stage.deliverablesCount} Deliverables · ${stage.progress}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = stage.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Stage Detail Inspection Modal
    selectedStageForDetail?.let { stage ->
        AlertDialog(
            onDismissRequest = { selectedStageForDetail = null },
            confirmButton = {
                TextButton(onClick = { selectedStageForDetail = null }) {
                    Text("Close")
                }
            },
            title = {
                Text("Stage ${stage.stageNumber}: ${stage.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Phase: ${stage.phase} · Lead Dept: ${stage.leadDepartment}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Prerequisite Interlock:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = if (stage.prerequisitesMet) "✓ All previous stage gate prerequisites confirmed" else "⚠ Prerequisite pending review or freeze approval",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (stage.prerequisitesMet) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Controlled Deliverables:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text("${stage.deliverablesCount} deliverables linked to Document Catalogue families.", style = MaterialTheme.typography.bodySmall)
                }
            }
        )
    }
}

val ApqpStage.phase: String
    get() = "TG_${(stageNumber / 11).coerceIn(0, 9)}"

val ApqpStage.leadDepartment: String
    get() = when (stageNumber % 6) {
        0 -> "EHW"
        1 -> "FW"
        2 -> "SW"
        3 -> "QA"
        4 -> "PUR"
        else -> "EMF"
    }

val ApqpStage.deliverablesCount: Int
    get() = if (prerequisiteDoc != null) 3 else 2

val ApqpStage.progress: Int
    get() = when (status) {
        "Approved" -> 100
        "In Progress" -> 60
        else -> 0
    }

val ApqpStage.prerequisitesMet: Boolean
    get() = !isBlockedByPrerequisite
