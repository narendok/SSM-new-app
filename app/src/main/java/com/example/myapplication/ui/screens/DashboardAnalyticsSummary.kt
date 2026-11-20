package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.ProjectMetadata
import com.example.myapplication.data.model.SharedAccess
import kotlinx.serialization.json.JsonObject

/** Compact, data-backed analytics for the first dashboard page. It only summarizes rows returned by the authorized Shared TEST snapshot. */
@Composable
fun DashboardAnalyticsSummary(
    snapshot: JsonObject?,
    projectId: String,
    projectCatalog: List<ProjectMetadata>,
    viewDepartment: String?
) {
    val membership = snapshot?.get("membership") as? JsonObject
    val role = membership?.let { SharedAccess.text(it, "role") }.orEmpty()
    val isAdmin = role == "admin"
    val departments = SharedAccess.departments(snapshot)
    val selectedDepartment = departments.firstOrNull { SharedAccess.text(it, "id") == viewDepartment }
    val memberDepartmentCode = membership?.let { SharedAccess.text(it, "department_code") }.orEmpty().uppercase()
    val departmentCode = when (memberDepartmentCode) {
        "EFW" -> "FW"
        "CSW" -> "SW"
        "HR", "OPS", "HR/OPS" -> "HROPS"
        else -> memberDepartmentCode
    }
    val memberDepartmentIds = buildSet {
        membership?.let { SharedAccess.text(it, "department_id") }?.takeIf(String::isNotBlank)?.let(::add)
        departments.filter { SharedAccess.text(it, "code").equals(departmentCode, true) }
            .map { SharedAccess.text(it, "id") }.filter(String::isNotBlank).forEach(::add)
    }
    val scopedDepartmentIds = when {
        isAdmin && viewDepartment == null -> departments.map { SharedAccess.text(it, "id") }.filter(String::isNotBlank).toSet()
        isAdmin -> setOfNotNull(viewDepartment)
        else -> memberDepartmentIds
    }
    val scopedDepartmentCode = if (isAdmin) SharedAccess.text(selectedDepartment ?: JsonObject(emptyMap()), "code") else departmentCode
    val departmentLabel = when {
        isAdmin && viewDepartment == null -> "Admin · All departments"
        isAdmin -> selectedDepartment?.let { SharedAccess.text(it, "name") }
            .orEmpty().ifBlank { "Selected department" }
        else -> SharedAccess.text(membership ?: JsonObject(emptyMap()), "department_name")
            .ifBlank { departmentCode.ifBlank { "My department" } }
    }
    val project = projectCatalog.firstOrNull { it.id == projectId }
    val projectMode = projectId != "ALL" && project != null

    val departmentTasks = SharedAccess.rows(snapshot, "tasks").filter { task ->
        if (isAdmin && viewDepartment == null) true
        else SharedAccess.text(task, "department_id") in scopedDepartmentIds ||
            SharedAccess.text(task, "department_id") == viewDepartment ||
            SharedAccess.text(task, "department_code").equals(scopedDepartmentCode, true)
    }
    val departmentRecords = SharedAccess.rows(snapshot, "records").filter { record ->
        if (isAdmin && viewDepartment == null) true
        else SharedAccess.text(record, "department_id") in scopedDepartmentIds ||
            SharedAccess.text(record, "department_id") == viewDepartment ||
            SharedAccess.text(record, "department_code").equals(scopedDepartmentCode, true)
    }
    val scopedTasks = if (projectMode) departmentTasks.filter {
        SharedAccess.projectIdForTask(snapshot, it) == projectId
    } else departmentTasks
    val scopedRecords = if (projectMode) departmentRecords.filter { record ->
        val id = SharedAccess.text(record, "id")
        if (id == projectId) return@filter true
        val fields = record["fields"] as? JsonObject
        val linkedProject = sequenceOf("project_id", "projectId", "project_code", "projectCode")
            .map { SharedAccess.text(fields ?: JsonObject(emptyMap()), it) }
            .firstOrNull(String::isNotBlank).orEmpty()
        linkedProject == projectId || linkedProject == project?.reference
    } else departmentRecords
    val actionableApprovals = SharedAccess.rows(snapshot, "approvals").count { approval ->
        SharedAccess.canDecide(snapshot, approval) &&
            (!projectMode || SharedAccess.text(approval, "record_id") in scopedRecords.map { SharedAccess.text(it, "id") }.toSet()) &&
            (isAdmin && viewDepartment == null ||
                SharedAccess.text(approval, "reviewer_department_id") in scopedDepartmentIds ||
                SharedAccess.text(approval, "reviewer_department_id") == viewDepartment)
    }
    val openTasks = scopedTasks.count {
        SharedAccess.text(it, "status").lowercase() !in setOf("complete", "completed")
    }
    val overdueTasks = scopedTasks.count { task ->
        val date = SharedAccess.text(task, "due_at").take(10)
        date.isNotBlank() && runCatching { java.time.LocalDate.parse(date) }
            .getOrNull()?.isBefore(java.time.LocalDate.now()) == true &&
            SharedAccess.text(task, "status").lowercase() !in setOf("complete", "completed")
    }
    val title = if (projectMode) "Project analytics" else "Department analytics"

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                if (projectMode) "${project?.title ?: "Selected project"} · $departmentLabel"
                else departmentLabel,
                style = MaterialTheme.typography.bodyMedium
            )
            if (projectMode) {
                Text(
                    "Project record · Stage: ${project?.stage?.ifBlank { "Not provided" } ?: "Not provided"} · Status: ${project?.status?.ifBlank { "Not provided" } ?: "Not provided"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (snapshot == null) {
                Text("Shared analytics are loading or unavailable. No local sample numbers are shown.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Open work" to openTasks.toString(),
                        "Overdue" to overdueTasks.toString(),
                        if (projectMode) "Linked records" to scopedRecords.size.toString() else "Records" to scopedRecords.size.toString(),
                        "Reviews" to actionableApprovals.toString()
                    ).forEach { (label, value) ->
                        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(9.dp)) {
                            Column(Modifier.padding(horizontal = 7.dp, vertical = 8.dp)) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Text(
                    if (projectMode) "Project counts use explicitly linked records and tasks in your authorized department view. Lifecycle stage details appear only when returned by the shared project feed."
                    else "Select a project above to switch this summary to project analytics. Counts use only records returned for your signed-in access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
