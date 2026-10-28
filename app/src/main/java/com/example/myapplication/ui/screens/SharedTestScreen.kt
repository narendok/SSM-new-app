package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.example.myapplication.data.model.LifecycleScenarios
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication.ui.viewmodel.MainViewModel
import com.example.myapplication.ui.components.AuthDialog
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
    var account by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("Projects") }
    var title by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf("") }
    LaunchedEffect(saved) { title = ""; reference = "" }
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
            listOf("Projects", "Records", "Tasks", "Approvals", "Documents", "Scenarios", "Activity").forEach { name ->
                FilterChip(selected = tab == name, onClick = { tab = name; title = ""; reference = "" }, label = { Text(name) })
            }
        }
        if (snapshot == null) Text("Waiting for server records. No local fallback.")
        else {
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
                        }) { Text("Load Canonical Samples") }
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
            if (tab == "Scenarios") {
                Text(
                    text = "Lifecycle Scenarios (§14.14) & Connected Verification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LifecycleScenarios.all.forEach { scenario ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "${scenario.id} · ${scenario.fixtureKey}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = scenario.projectCode ?: "No Project Created (§14.14 Invariant)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (scenario.projectCode == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(text = scenario.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Client: ${scenario.clientName ?: "Internal Tooling"}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Position: ${scenario.currentPosition}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Remark: ${scenario.stageRemark}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            Text(text = "Next: ${scenario.currentNextAction}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            if (scenario.productionReconciliation != null) {
                                val pr = scenario.productionReconciliation
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "Reconciliation Matrix (§14.14): ${pr.acceptedUnits} Accepted + ${pr.heldUnits} Held + ${pr.pendingInspectionUnits} Pending Inspection = ${pr.lotQuantity} Lot 1 | Total Planned: ${pr.totalPlannedUnits} (Balance: ${pr.remainingPlannedUnits})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            if (scenario.dependencies.isNotEmpty()) {
                                Text(
                                    text = "Dependencies (${scenario.dependencies.size}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                scenario.dependencies.forEach { dep ->
                                    Text(
                                        text = "• ${dep.deliverableCode}: ${dep.whatNeeded} [${dep.state}] -> ${dep.nextAction}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                                listOf("Open", "In Progress", "Complete").forEach { status ->
                                    OutlinedButton(enabled = !busy && row.text("status") != status, onClick = {
                                        vm.mutateSharedWorkspace("update_task", buildJsonObject {
                                            put("id", row.text("id")); put("status", status); put("expected_updated_at", row.text("updated_at"))
                                        })
                                    }) { Text(status) }
                                }
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
                        if (tab == "Approvals") {
                            Text("Evidence revision: ${row.text("evidence_revision")}")
                            val record = snapshot?.rows("records")?.find { it.text("id") == row.text("record_id") }
                            record?.let { Text("${it.text("reference")} | ${it.text("title")}\n${it["fields"] ?: ""}") }
                            if (com.example.myapplication.data.model.SharedAccess.canDecide(snapshot, row)) {
                                Row { listOf("approved", "rejected").forEach { outcome ->
                                    OutlinedButton(enabled = !busy, onClick = {
                                        vm.mutateSharedWorkspace("decide_approval", buildJsonObject {
                                            put("id", row.text("id")); put("outcome", outcome); put("expected_updated_at", row.text("updated_at")); put("notes", "TEST review from Android")
                                        })
                                    }) { Text(outcome) }
                                } }
                            }
                        }
                    }
                }
            }
            }
        }
    }
    if (account) AuthDialog(vm.authRepository, vm::signInWithPassword, auth, onDismiss = { account = false })
}
