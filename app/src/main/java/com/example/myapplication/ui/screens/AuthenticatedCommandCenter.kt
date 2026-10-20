package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.SharedAccess
import com.example.myapplication.ui.viewmodel.MainViewModel
import kotlinx.serialization.json.*

@Composable
fun AuthenticatedCommandCenter(vm: MainViewModel, onSwitchDepartment: () -> Unit, onOpenShared: () -> Unit) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refreshSharedWorkspace()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val snapshot by vm.sharedSnapshot.collectAsState()
    val department by vm.viewDepartment.collectAsState()
    val error by vm.sharedError.collectAsState()
    val busy by vm.sharedBusy.collectAsState()
    val member = snapshot?.get("membership") as? JsonObject
    fun text(row: JsonObject?, key: String) = row?.let { SharedAccess.text(it, key) }.orEmpty()
    val dept = SharedAccess.rows(snapshot, "departments").find { text(it, "id") == department }
    val tasks = SharedAccess.rows(snapshot, "tasks").filter { department == null || text(it, "department_id") == department }
    val approvals = SharedAccess.rows(snapshot, "approvals").filter { department == null || text(it, "reviewer_department_id") == department || text(it, "department_id") == department }
    val projects = SharedAccess.rows(snapshot, "records").filter { text(it, "record_type") == "Project" }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(text(dept, "name").ifBlank { text(member, "department_name").ifBlank { "Command Center" } }, style = MaterialTheme.typography.titleMedium)
                    Text("${text(member, "display_name")} | ${text(member, "role")}")
                    Row { TextButton(onClick = onSwitchDepartment) { Text("Department view") }; TextButton(onClick = vm::refreshSharedWorkspace, enabled = !busy) { Text("Refresh") } }
                }
            }
            Text("Shared TEST account | last server read: ${text(snapshot, "serverTime")}", style = MaterialTheme.typography.labelSmall)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Approvals & Governance", style = MaterialTheme.typography.titleMedium)
                    Text("${approvals.count { text(it, "outcome") == "pending" }} pending reviews in this view")
                    TextButton(onClick = onOpenShared) { Text("Open shared approvals") }
                }
            }
        }
        item { Text("Authorized projects", style = MaterialTheme.typography.titleMedium) }
        projects.forEach { project -> item {
            Card { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(text(project, "reference"), style = MaterialTheme.typography.labelLarge)
                Text(text(project, "title"), style = MaterialTheme.typography.titleMedium)
                Text("${text(project, "stage")} | ${text(project, "status")}")
            } }
        } }
        if (projects.isEmpty()) item { Text("No authorized projects returned by the server.") }
        item { Text("Daily Action Items & Checklist", style = MaterialTheme.typography.titleMedium) }
        tasks.forEach { task -> item {
            Card { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(text(task, "title"), style = MaterialTheme.typography.titleSmall)
                Text("${text(task, "status")} | ${text(task, "priority")} | ${text(task, "due_at")}")
                if (SharedAccess.canUpdateTask(snapshot, task)) {
                    TextButton(enabled = !busy, onClick = { vm.mutateSharedWorkspace("update_task", buildJsonObject {
                        put("id", text(task, "id")); put("expected_updated_at", text(task, "updated_at")); put("status", if (text(task, "status") == "Complete") "Open" else "Complete")
                    }) }) { Text(if (text(task, "status") == "Complete") "Reopen" else "Complete") }
                }
            } }
        } }
        if (tasks.isEmpty()) item { Text("No permitted tasks in this department view.") }
        item { OutlinedButton(onClick = onOpenShared) { Text("Records, documents & activity") } }
    }
}
