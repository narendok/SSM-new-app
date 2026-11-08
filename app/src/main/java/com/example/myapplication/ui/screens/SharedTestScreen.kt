package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication.ui.viewmodel.MainViewModel
import com.example.myapplication.ui.components.AuthDialog
import com.example.myapplication.data.model.reviewForProject
import kotlinx.serialization.json.*

private fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
private fun JsonObject.rows(key: String) = (get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SharedTestScreen(vm: MainViewModel, onBack: () -> Unit) {
    val snapshot by vm.sharedSnapshot.collectAsState()
    val error by vm.sharedError.collectAsState()
    val busy by vm.sharedBusy.collectAsState()
    val auth by vm.authState.collectAsState()
    val saved by vm.sharedSaved.collectAsState()
    val coordination by vm.projectCoordination.collectAsState()
    val coordinationError by vm.projectCoordinationError.collectAsState()
    val coordinationBusy by vm.projectCoordinationBusy.collectAsState()
    var account by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("Projects") }
    var title by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf("") }
    var coordinationProjectId by remember { mutableStateOf("") }
    var pendingDecision by remember { mutableStateOf<Pair<JsonObject, String>?>(null) }
    var decisionNotes by remember { mutableStateOf("") }
    var decisionSavedAtLaunch by remember { mutableStateOf<Int?>(null) }
    var reviewRequestRecord by remember { mutableStateOf<JsonObject?>(null) }
    var reviewDepartmentId by remember { mutableStateOf("") }
    var reviewRequestSavedAtLaunch by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(saved) {
        title = ""
        reference = ""
        if (decisionSavedAtLaunch != null && saved > decisionSavedAtLaunch!!) {
            pendingDecision = null
            decisionNotes = ""
            decisionSavedAtLaunch = null
        }
        if (reviewRequestSavedAtLaunch != null && saved > reviewRequestSavedAtLaunch!!) {
            reviewRequestRecord = null
            reviewDepartmentId = ""
            reviewRequestSavedAtLaunch = null
        }
    }
    LaunchedEffect(tab) {
        if (tab == "Project Review") vm.refreshProjectCoordination()
    }
    LaunchedEffect(auth) {
        pendingDecision = null
        decisionNotes = ""
        decisionSavedAtLaunch = null
        reviewRequestRecord = null
        reviewDepartmentId = ""
        reviewRequestSavedAtLaunch = null
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshSharedWorkspace()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val member = snapshot?.get("membership") as? JsonObject
    val role = member?.text("role").orEmpty()
    val projects = snapshot?.rows("records").orEmpty().filter { it.text("record_type") == "Project" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back to dashboard") }
        Text("Shared TEST workspace", style = MaterialTheme.typography.headlineSmall)
        Text("${member?.text("display_name").orEmpty()} | $role | ${member?.text("department_code").orEmpty()}")
        Text("Server read: ${snapshot?.text("serverTime").orEmpty()}", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::refreshSharedWorkspace, enabled = !busy) { Text("Refresh") }
            OutlinedButton(onClick = { account = true }) { Text("Account / Switch") }
        }
        Text("Online shared records. Inventory simulations and local prototype records are separate.", style = MaterialTheme.typography.bodySmall)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Projects", "Records", "Tasks", "Approvals", "Documents", "Project Review", "Activity").forEach { name ->
                FilterChip(selected = tab == name, onClick = { tab = name; title = ""; reference = "" }, label = { Text(name) })
            }
        }
        if (snapshot == null && tab != "Project Review") Text("Waiting for server records. No local fallback.")
        else {
            if (tab == "Projects") {
                Text(
                    "Canonical sample records are synthetic shared TEST data, not real customer work or proof of a completed lifecycle gate.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (tab == "Projects" && (role == "admin" || (role == "department_lead" && member?.text("department_code") == "SAL"))) {
                OutlinedTextField(title, { title = it }, label = { Text("New TEST project title") })
                OutlinedTextField(reference, { reference = it }, label = { Text("Project reference (HLPL-...)") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !busy && title.isNotBlank() && reference.isNotBlank(), onClick = {
                        vm.mutateSharedWorkspace("create_project", buildJsonObject { put("title", title); put("reference", reference) })
                    }) { Text("Create shared project") }
                    if (role == "admin") {
                        Button(enabled = !busy, onClick = {
                            vm.mutateSharedWorkspace("sync_canonical_sample_pack", buildJsonObject {
                                put("p_run_id", "LC2-2026-10-28")
                            })
                        }) { Text("Sync TEST sample pack") }
                    }
                }
            }
            if (tab == "Tasks" && role != "viewer") {
                Text("Project for new task")
                FlowRow { projects.forEach { p -> FilterChip(selected = projectId == p.text("id"),
                    onClick = { projectId = p.text("id") }, label = { Text(p.text("reference")) }) } }
                OutlinedTextField(title, { title = it }, label = { Text("New task title (assigned to me)") })
                Button(enabled = !busy && title.isNotBlank() && projectId.isNotBlank(), onClick = {
                    vm.mutateSharedWorkspace("create_task", buildJsonObject {
                        put("title", title); put("project_id", projectId); put("assignee_id", member?.text("membership_id").orEmpty())
                    })
                }) { Text("Create shared task") }
            }
            if (tab == "Project Review") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Project-linked coordination review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Read-only server projection: record status, lifecycle notes, tasks, and linked document metadata. Engineering authoring and document files remain on web.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = vm::refreshProjectCoordination, enabled = !coordinationBusy) { Text("Refresh project review") }
                    if (coordinationBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    coordinationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    val reviewProjects = coordination?.records.orEmpty().filter { it.type == "Project" }
                    val selectedProject = reviewProjects.firstOrNull { it.id == coordinationProjectId } ?: reviewProjects.firstOrNull()
                    if (selectedProject == null) {
                        Text(if (coordination == null && coordinationError == null) "Project review has not loaded." else "No project coordination records are available to this account.")
                    } else {
                        LaunchedEffect(selectedProject.id) { coordinationProjectId = selectedProject.id }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            reviewProjects.forEach { project ->
                                FilterChip(
                                    selected = project.id == selectedProject.id,
                                    onClick = { coordinationProjectId = project.id },
                                    label = { Text(project.reference.ifBlank { project.title }) }
                                )
                            }
                        }
                        Text("${selectedProject.title} · ${selectedProject.status}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        selectedProject.fields.companyPhase?.takeIf(String::isNotBlank)?.let {
                            Text("Phase: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        selectedProject.fields.currentStage?.takeIf(String::isNotBlank)?.let {
                            Text("Current department stage: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        selectedProject.fields.lifecycleBaseline?.takeIf(String::isNotBlank)?.let {
                            Text("Pinned lifecycle: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        val review = coordination?.reviewForProject(selectedProject)
                        Text(
                            "Grouped by backend-provided stage. Tasks appear only when explicitly linked to this project or a lifecycle record; no subtask or dependency state is inferred.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Department stages (${review?.stages?.size ?: 0})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (review?.stages.isNullOrEmpty()) Text("No permitted lifecycle records are linked to this project.")
                        review?.stages.orEmpty().forEach { stage ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stage.stage, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    stage.records.forEach { record ->
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("${record.type} · ${record.reference} · ${record.status}", fontWeight = FontWeight.SemiBold)
                                            record.fields.stageRemark?.takeIf(String::isNotBlank)?.let { Text("Remark: $it") }
                                            record.fields.nextAction?.takeIf(String::isNotBlank)?.let { Text("Next action: $it") }
                                            record.fields.targetDate?.takeIf(String::isNotBlank)?.let { Text("Target: $it") }
                                            stage.tasksByRecordId[record.id].orEmpty().forEach { task ->
                                                Text(
                                                    listOf(
                                                        "Task: ${task.title}",
                                                        task.status,
                                                        task.fields.department,
                                                        task.fields.assignee,
                                                        task.fields.dueDate
                                                    ).filterNotNull().filter(String::isNotBlank).joinToString(" · "),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Text("Project-linked tasks (${review?.projectTasks?.size ?: 0})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        review?.projectTasks.orEmpty().forEach { task ->
                            Text(
                                listOf(task.title, task.status, task.fields.department, task.fields.assignee, task.fields.dueDate)
                                    .filterNotNull().filter(String::isNotBlank).joinToString(" · ")
                            )
                        }
                        Text("Linked documents (${review?.documents?.size ?: 0})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        review?.documents.orEmpty().forEach { document ->
                            Text("${document.title} · ${document.type}")
                        }
                    }
                }
            } else {
            val key = when(tab) { "Projects", "Records" -> "records"; "Tasks" -> "tasks"; "Approvals" -> "approvals"; "Documents" -> "documents"; else -> "audit" }
            val rows = snapshot?.rows(key).orEmpty().filter { tab != "Projects" || it.text("record_type") == "Project" }
                .filter { tab != "Records" || it.text("record_type") != "Project" }
            if (rows.isEmpty()) Text("No permitted $tab records.")
            rows.forEach { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(row.text("title").ifBlank { row.text("action") }, style = MaterialTheme.typography.titleMedium)
                        Text(listOf("reference", "record_type", "stage", "status", "outcome", "document_type", "created_at").map { row.text(it) }.filter { it.isNotBlank() }.joinToString(" | "))
                        if (tab == "Tasks" && com.example.myapplication.data.model.SharedAccess.canUpdateTask(snapshot, row)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Open", "In Progress", "In review", "Complete").forEach { status ->
                                    val canSetStatus = com.example.myapplication.data.model.SharedAccess.canSetTaskStatus(row, status)
                                    OutlinedButton(enabled = !busy && row.text("status") != status && canSetStatus, onClick = {
                                        vm.mutateSharedWorkspace("update_task", buildJsonObject {
                                            put("id", row.text("id")); put("status", status); put("expected_updated_at", row.text("updated_at"))
                                        })
                                    }) { Text(status) }
                                }
                            }
                            if (row.text("status") == "In review") {
                                Text("Completion is controlled by the linked review; resolve that review first.", style = MaterialTheme.typography.bodySmall)
                                if ((role == "admin" || role == "department_lead") && row.text("status") != "Cancelled") {
                                    OutlinedButton(enabled = !busy, onClick = {
                                        vm.mutateSharedWorkspace("cancel_task", buildJsonObject {
                                            put("id", row.text("id"))
                                            put("reason", "Cancelled from Android test client")
                                            put("expected_updated_at", row.text("updated_at"))
                                        })
                                    }) { Text("Cancel") }
                                }
                                if ((role == "admin" || role == "department_lead") && row.text("assignee_id").isBlank() && row.text("status") == "Open") {
                                    OutlinedButton(enabled = !busy, onClick = {
                                        vm.mutateSharedWorkspace("delete_task", buildJsonObject {
                                            put("id", row.text("id"))
                                            put("expected_updated_at", row.text("updated_at"))
                                        })
                                    }) { Text("Del") }
                                }
                            }
                        }
                        if (tab == "Records" && com.example.myapplication.data.model.SharedAccess.canRequestReview(snapshot, row)) {
                            OutlinedButton(enabled = !busy, onClick = {
                                reviewRequestRecord = row
                                reviewDepartmentId = ""
                                reviewRequestSavedAtLaunch = null
                            }) { Text("Request review") }
                        }
                        if (tab == "Approvals") {
                            Text("Evidence revision: ${row.text("evidence_revision")}")
                            val record = snapshot?.rows("records")?.find { it.text("id") == row.text("record_id") }
                            record?.let { Text("${it.text("reference")} | ${it.text("title")}\n${it["fields"] ?: ""}") }
                            if (com.example.myapplication.data.model.SharedAccess.canDecide(snapshot, row)) {
                                Row { listOf("approved", "rejected", "returned").forEach { outcome ->
                                    OutlinedButton(enabled = !busy, onClick = {
                                        decisionNotes = ""
                                        decisionSavedAtLaunch = null
                                        pendingDecision = row to outcome
                                    }) { Text(if (outcome == "returned") "Return for rework" else outcome.replaceFirstChar(Char::uppercase)) }
                                } }
                            }
                        }
                    }
                }
            }
            }
        }
    }
    reviewRequestRecord?.let { record ->
        val isAdmin = role == "admin"
        val reviewerDepartments = snapshot?.rows("departments").orEmpty()
            .filter { it.text("code") != "CLIENT" && (isAdmin || it.text("id") != record.text("department_id")) }
        AlertDialog(
            onDismissRequest = {
                if (!busy) {
                    reviewRequestRecord = null
                    reviewDepartmentId = ""
                    reviewRequestSavedAtLaunch = null
                }
            },
            title = { Text("Request review") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${record.text("reference")} · ${record.text("title")}")
                    Text("Select a reviewer department. The server validates your request authority, reviewer, and current record.")
                    if (reviewerDepartments.isEmpty()) Text("No permitted reviewer departments are available.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        reviewerDepartments.forEach { department ->
                            val id = department.text("id")
                            FilterChip(
                                selected = reviewDepartmentId == id,
                                onClick = { reviewDepartmentId = id },
                                label = { Text(department.text("code")) },
                                enabled = !busy
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && reviewDepartmentId.isNotBlank(),
                    onClick = {
                        reviewRequestSavedAtLaunch = saved
                        vm.mutateSharedWorkspace("request_approval", buildJsonObject {
                            put("record_id", record.text("id"))
                            put("reviewer_department_id", reviewDepartmentId)
                        })
                    }
                ) { Text("Send request") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = {
                    reviewRequestRecord = null
                    reviewDepartmentId = ""
                    reviewRequestSavedAtLaunch = null
                }) { Text("Cancel") }
            }
        )
    }
    pendingDecision?.let { (approval, outcome) ->
        AlertDialog(
            onDismissRequest = {
                if (!busy) {
                    pendingDecision = null
                    decisionNotes = ""
                    decisionSavedAtLaunch = null
                }
            },
            title = { Text(when (outcome) {
                "approved" -> "Approve review"
                "rejected" -> "Reject review"
                else -> "Return for rework"
            }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Decision applies to evidence revision ${approval.text("evidence_revision")}. The server will recheck signer authority and current state.")
                    OutlinedTextField(
                        value = decisionNotes,
                        onValueChange = { decisionNotes = it },
                        label = { Text(if (outcome == "approved") "Decision notes (optional)" else "Rationale (required)") },
                        minLines = 2,
                        enabled = !busy
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && (outcome == "approved" || decisionNotes.isNotBlank()),
                    onClick = {
                        decisionSavedAtLaunch = saved
                        vm.mutateSharedWorkspace("decide_approval", buildJsonObject {
                            put("id", approval.text("id"))
                            put("outcome", outcome)
                            put("expected_updated_at", approval.text("updated_at"))
                            put("notes", decisionNotes.trim())
                        })
                    }
                ) { Text("Submit decision") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = {
                    pendingDecision = null
                    decisionNotes = ""
                    decisionSavedAtLaunch = null
                }) { Text("Cancel") }
            }
        )
    }
    if (account) AuthDialog(vm.authRepository, vm::signInWithPassword, auth, onDismiss = { account = false })
}
