package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.SharedAccess
import kotlinx.serialization.json.JsonObject

@Composable
fun DepartmentAnalyticsOverview(
    snapshot: JsonObject?,
    viewDepartment: String?,
    onOpenBusinessWorkspace: ((String) -> Unit)?,
    onOpenDepartmentRecords: (() -> Unit)?,
    onOpenApprovals: (() -> Unit)?
) {
    val member = snapshot?.get("membership") as? JsonObject
    val role = member?.let { SharedAccess.text(it, "role") }.orEmpty()
    val isAdmin = role == "admin"
    val departments = SharedAccess.departments(snapshot)
    val focusedDepartment = departments.firstOrNull { SharedAccess.text(it, "id") == viewDepartment }
    val memberDepartmentRaw = member?.let { SharedAccess.text(it, "department_code") }.orEmpty().uppercase()
    val memberDepartmentCode = when (memberDepartmentRaw) {
        "EFW" -> "FW"
        "CSW" -> "SW"
        "HR", "OPS", "HR/OPS" -> "HROPS"
        else -> memberDepartmentRaw
    }
    val departmentCode = if (isAdmin) {
        if (viewDepartment == null) "All departments"
        else focusedDepartment?.let { SharedAccess.text(it, "code").ifBlank { SharedAccess.text(it, "name") } }.orEmpty()
    }
    else memberDepartmentCode
    val memberDepartmentIds = buildSet {
        member?.let { SharedAccess.text(it, "department_id") }?.takeIf(String::isNotBlank)?.let(::add)
        departments.filter { SharedAccess.text(it, "code").equals(memberDepartmentCode, true) }
            .map { SharedAccess.text(it, "id") }.filter(String::isNotBlank).forEach(::add)
    }
    val allowedIds = when {
        isAdmin && viewDepartment == null -> departments.map { SharedAccess.text(it, "id") }.filter(String::isNotBlank).toSet()
        isAdmin -> setOfNotNull(viewDepartment)
        else -> memberDepartmentIds
    }
    val tasks = SharedAccess.rows(snapshot, "tasks").filter { task ->
        if (isAdmin && viewDepartment == null) true
        else SharedAccess.text(task, "department_id") in allowedIds ||
            SharedAccess.text(task, "department_id") == viewDepartment ||
            SharedAccess.text(task, "department_code").equals(if (isAdmin) departmentCode else memberDepartmentCode, true)
    }
    val records = SharedAccess.rows(snapshot, "records").filter { record ->
        if (isAdmin && viewDepartment == null) true
        else SharedAccess.text(record, "department_id") in allowedIds ||
            SharedAccess.text(record, "department_id") == viewDepartment ||
            SharedAccess.text(record, "department_code").equals(if (isAdmin) departmentCode else memberDepartmentCode, true)
    }
    val reviews = SharedAccess.rows(snapshot, "approvals").filter { approval ->
        SharedAccess.canDecide(snapshot, approval) &&
            ((isAdmin && viewDepartment == null) || SharedAccess.text(approval, "reviewer_department_id") in allowedIds || SharedAccess.text(approval, "reviewer_department_id") == viewDepartment)
    }
    val openTasks = tasks.count { SharedAccess.text(it, "status").lowercase() !in setOf("complete", "completed") }
    val lateTasks = tasks.count { task ->
        val date = SharedAccess.text(task, "due_at").take(10)
        date.isNotBlank() && runCatching { java.time.LocalDate.parse(date) }.getOrNull()?.isBefore(java.time.LocalDate.now()) == true &&
            SharedAccess.text(task, "status").lowercase() !in setOf("complete", "completed")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 10.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Department analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (isAdmin && viewDepartment == null) "Admin · All departments" else "$departmentCode · Authorized view", style = MaterialTheme.typography.bodyMedium)
                    Text("Choose a project in the top selector to see project lifecycle analysis. This view summarizes only authorized department data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Open work" to openTasks.toString(), "Overdue" to lateTasks.toString(), "Records" to records.size.toString(), "Reviews" to reviews.size.toString()).forEach { (label, value) ->
                    Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(8.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (reviews.isNotEmpty() && onOpenApprovals != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Actionable approvals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${reviews.size} review${if (reviews.size == 1) "" else "s"} allowed for this signed-in profile", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = onOpenApprovals) { Text("Review") }
                    }
                }
            }
        }
        if (isAdmin && viewDepartment == null) {
            items(departments) { department ->
                val id = SharedAccess.text(department, "id")
                val code = SharedAccess.text(department, "code")
                val deptTasks = tasks.filter { SharedAccess.text(it, "department_id") == id || SharedAccess.text(it, "department_code").equals(code, true) }
                val deptRecords = records.filter { SharedAccess.text(it, "department_id") == id || SharedAccess.text(it, "department_code").equals(code, true) }
                val deptReviews = reviews.filter { SharedAccess.text(it, "reviewer_department_id") == id }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(SharedAccess.text(department, "name") + " · " + code, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(deptTasks.size.toString() + " tasks · " + deptRecords.size + " records · " + deptReviews.size + " actionable reviews", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item { Text("Department task queue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            if (tasks.isEmpty()) {
                item {
                    Text("No shared tasks are currently returned for this department.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(tasks.take(12)) { task ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(SharedAccess.text(task, "title").ifBlank { "Untitled task" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(SharedAccess.text(task, "status").ifBlank { "Status unavailable" } + " · " + SharedAccess.text(task, "assignee_name").ifBlank { SharedAccess.text(task, "assignee").ifBlank { "Department queue" } } + " · due " + SharedAccess.text(task, "due_at").take(10).ifBlank { "not scheduled" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Department workspace shortcuts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Quick links open your department’s shared workflow areas. Tasks are listed above.", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        onOpenBusinessWorkspace?.let { open ->
                            Button(onClick = { open("budgets") }) { Text("Expenses") }
                            OutlinedButton(onClick = { open("tickets") }) { Text("Tickets") }
                        }
                        onOpenDepartmentRecords?.let { OutlinedButton(onClick = it) { Text("Records") } }
                    }
                }
            }
        }
    }
}
