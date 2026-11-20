package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.SharedAccess
import com.example.myapplication.ui.viewmodel.MainViewModel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put

private fun invText(row: JsonObject, key: String) = SharedAccess.text(row, key)
private fun invRows(row: JsonObject?, key: String) = (row?.get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
private fun invNumber(row: JsonObject, key: String) = row[key]?.let { runCatching { it.toString().trim('"').toDouble() }.getOrNull() } ?: 0.0
private fun invDepartments(row: JsonObject) = ((row["departmentViews"] as? JsonArray)?.map { it.toString().trim('"') }.orEmpty() + invText(row, "primaryDepartment")).filter(String::isNotBlank).distinct()
private fun assignmentsForKit(rows: List<JsonObject>, partId: String, projectId: String) = rows.filter {
    invText(it, "partId") == partId && invText(it, "projectId") == projectId && invNumber(it, "quantity") > 0
}

@Composable
fun SharedInventoryMobileScreen(
    viewModel: MainViewModel,
    sharedSnapshot: JsonObject?,
    viewDepartment: String?,
    selectedProjectId: String = "ALL"
) {
    val catalog by viewModel.sharedInventorySnapshot.collectAsState()
    val busy by viewModel.sharedInventoryBusy.collectAsState()
    val error by viewModel.sharedInventoryError.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAssign by remember { mutableStateOf<JsonObject?>(null) }
    var showReceipt by remember { mutableStateOf<JsonObject?>(null) }
    var showAssignmentClose by remember { mutableStateOf<JsonObject?>(null) }
    var showKitIssue by remember { mutableStateOf<JsonObject?>(null) }
    var kitBuildQuantity by remember { mutableStateOf("1") }
    var kitAssignments by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var quantity by remember { mutableStateOf("1") }
    var location by remember { mutableStateOf("Main stores") }
    var bin by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedRoleId by remember { mutableStateOf("") }
    var assignmentCloseAction by remember { mutableStateOf("return") }
    var assignmentCloseQuantity by remember { mutableStateOf("1") }
    var assignmentCloseNote by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { if (catalog == null) viewModel.refreshSharedInventory() }
    val member = sharedSnapshot?.get("membership") as? JsonObject
    val isAdmin = member?.let { invText(it, "role") } == "admin"
    val memberCode = member?.let { invText(it, "department_code") }.orEmpty().uppercase()
    val contextCode = if (isAdmin && viewDepartment != null) {
        SharedAccess.departments(sharedSnapshot).firstOrNull { invText(it, "id") == viewDepartment }?.let { invText(it, "code") }.orEmpty()
    } else if (isAdmin) "" else memberCode
    val allItems = invRows(catalog, "items")
    val itemsInDepartment = allItems.filter { item ->
        isAdmin && contextCode.isBlank() || invDepartments(item).any { it.equals(contextCode, true) }
    }
    val parts = itemsInDepartment.filter { !invText(it, "category").contains("harness", true) }
    val harnesses = itemsInDepartment.filter { invText(it, "category").contains("harness", true) || invText(it, "subcategory").contains("harness", true) }
    val visiblePartIds = itemsInDepartment.map { invText(it, "id") }.toSet()
    val roles = invRows(catalog, "roles").filter { isAdmin && contextCode.isBlank() || invText(it, "departmentCode").equals(contextCode, true) }
    val projects = invRows(catalog, "projects")
    val kitBoms = invRows(catalog, "kitBoms").filter { bom ->
        val bomProject = invText(bom, "projectId")
        val lines = invRows(bom, "lines")
        lines.isNotEmpty() && lines.all { line ->
            assignmentsForKit(invRows(catalog, "assignments"), invText(line, "partId"), bomProject).isNotEmpty()
        }
    }
    val assignments = invRows(catalog, "assignments").filter { invText(it, "partId") in visiblePartIds && (selectedProjectId == "ALL" || invText(it, "projectId") == selectedProjectId) }
    val receiptOptions = invRows(catalog, "receiptOptions").filter { invText(it, "partId") in visiblePartIds }
    val movements = invRows(catalog, "movements").filter { invText(it, "partId") in visiblePartIds }
    val tabs = listOf("Parts", "Harness", "Assignments", "Receiving / IQC", "Assets", "Audit")
    val safeSelectedTab = selectedTab.coerceIn(0, tabs.lastIndex)

    Column(Modifier.fillMaxSize()) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Shared TEST inventory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text((if (contextCode.isBlank()) "Admin · All mapped departments" else "Preferred department · $contextCode") + " · project " + if (selectedProjectId == "ALL") "All" else selectedProjectId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.refreshSharedInventory() }, enabled = !busy) { Icon(Icons.Default.Refresh, contentDescription = "Refresh shared inventory") }
                }
                Text("Role-profile assignments and stock actions are submitted to the shared ledger; server permissions decide whether they are allowed.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(selected = selectedProjectId == "ALL", onClick = { viewModel.scmsRepository.selectProject("ALL") }, label = { Text("All projects") }) }
                    items(projects) { project ->
                        val projectId = invText(project, "id")
                        FilterChip(
                            selected = selectedProjectId == projectId,
                            onClick = { viewModel.scmsRepository.selectProject(projectId) },
                            label = { Text(invText(project, "reference").ifBlank { invText(project, "name") }) }
                        )
                    }
                }
            }
        }
        error?.let { Text(it, modifier = Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (busy && catalog == null) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.size(30.dp))
                Text("Loading authorized Shared TEST inventory…", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
            return
        }
        ScrollableTabRow(selectedTabIndex = safeSelectedTab, edgePadding = 8.dp) {
            tabs.forEachIndexed { index, title -> Tab(selected = safeSelectedTab == index, onClick = { selectedTab = index }, text = { Text(title, maxLines = 1) }) }
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (safeSelectedTab) {
                0, 1 -> {
                    val currentItems = if (safeSelectedTab == 0) parts else harnesses
                    if (currentItems.isEmpty()) item { Text("No shared ${(if (safeSelectedTab == 0) "parts" else "harnesses")} are mapped to this department.", style = MaterialTheme.typography.bodySmall) }
                    items(currentItems) { part ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(invText(part, "title") + " · " + invText(part, "reference"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(invText(part, "mpn").ifBlank { "MPN not recorded" } + " · " + invText(part, "category").ifBlank { "Uncategorized" }, style = MaterialTheme.typography.bodySmall)
                                Text("On hand " + invNumber(part, "onHand") + " · held " + invNumber(part, "held") + " · allocated " + invNumber(part, "allocated") + " · available " + invNumber(part, "available") + " " + invText(part, "unit"), style = MaterialTheme.typography.labelSmall)
                                Text(invDepartments(part).joinToString(" · ").ifBlank { "No department map" } + " · " + invText(part, "status"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (invText(part, "isDemo") == "true") Text("Fictional Shared TEST balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                if (roles.isNotEmpty() && invNumber(part, "available") > 0) OutlinedButton(onClick = {
                                    showAssign = part; selectedRoleId = roles.firstOrNull()?.let { invText(it, "id") }.orEmpty(); selectedProjectId = projects.firstOrNull()?.let { invText(it, "id") }.orEmpty(); quantity = "1"; note = ""
                                }, enabled = !busy) { Text("Assign to role profile") }
                            }
                        }
                    }
                }
                2 -> {
                    if (kitBoms.isNotEmpty()) item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("BOM kit issue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Component stock is consumed at build / kit issue from the released Shared TEST BOM and matching role allocations.", style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = {
                                    showKitIssue = kitBoms.firstOrNull()
                                    kitBuildQuantity = "1"
                                    kitAssignments = emptyMap()
                                }, enabled = !busy) { Text("Issue BOM kit to build") }
                            }
                        }
                    }
                    if (assignments.isEmpty()) item { Text("No active shared role allocations for this department or project.", style = MaterialTheme.typography.bodySmall) }
                    items(assignments) { row -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(invText(row, "partReference") + " · " + invText(row, "partTitle"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Project " + invText(row, "projectReference") + " · L" + invText(row, "hierarchyLevel") + " " + invText(row, "roleName") + " (" + invText(row, "departmentCode") + ")", style = MaterialTheme.typography.bodySmall)
                        Text("Remaining " + invText(row, "quantity") + " · " + invText(row, "location") + " · " + invText(row, "bin"), style = MaterialTheme.typography.labelSmall)
                        OutlinedButton(onClick = {
                            showAssignmentClose = row
                            assignmentCloseAction = "return"
                            assignmentCloseQuantity = invText(row, "quantity").ifBlank { "1" }
                            assignmentCloseNote = ""
                        }, enabled = !busy && (invNumber(row, "quantity") > 0)) { Text("Record use / return") }
                    } } }
                }
                3 -> {
                    if (receiptOptions.isEmpty()) item { Text("No approved Shared TEST purchase-order lines are available for receiving under this profile.", style = MaterialTheme.typography.bodySmall) }
                    items(receiptOptions) { option -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(invText(option, "purchaseOrderReference") + " · " + invText(option, "partReference"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(invText(option, "supplierName") + " · remaining " + invText(option, "remainingQuantity"), style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { showReceipt = option; quantity = "1"; note = "" }, enabled = !busy) { Text("Record TEST receipt · enters IQC hold") }
                    } } }
                    item { Text("Receipts never release stock; quality disposition remains a separate review gate.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                4 -> item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Shared asset register is not connected in this mobile view", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("No company asset, calibration or maintenance records are substituted from local samples. The current Shared TEST inventory contract returns parts, role allocations, receipt options and stock movements only.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                5 -> {
                    if (movements.isEmpty()) item { Text("No shared stock movements are recorded for this department.", style = MaterialTheme.typography.bodySmall) }
                    items(movements) { row -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(invText(row, "partReference") + " · " + invText(row, "eventKind"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(invText(row, "createdAt") + " · " + invText(row, "location") + " · " + invText(row, "bin"), style = MaterialTheme.typography.labelSmall)
                        Text(invText(row, "note").ifBlank { "No note" }, style = MaterialTheme.typography.bodySmall)
                    } } }
                }
            }
        }
    }

    showAssign?.let { part ->
        AlertDialog(
            onDismissRequest = { if (!busy) showAssign = null },
            title = { Text("Assign shared inventory") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(invText(part, "reference") + " · available " + invNumber(part, "available") + " " + invText(part, "unit"), style = MaterialTheme.typography.bodySmall)
                Text("Project", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { items(projects) { project -> FilterChip(selected = selectedProjectId == invText(project, "id"), onClick = { selectedProjectId = invText(project, "id") }, label = { Text(invText(project, "reference")) }) } }
                Text("Assignee profile", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { items(roles) { profile -> FilterChip(selected = selectedRoleId == invText(profile, "id"), onClick = { selectedRoleId = invText(profile, "id") }, label = { Text("L" + invText(profile, "hierarchyLevel") + " · " + invText(profile, "name")) }) } }
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, singleLine = true)
                OutlinedTextField(location, { location = it }, label = { Text("Location") }, singleLine = true)
                OutlinedTextField(bin, { bin = it }, label = { Text("Bin (optional)") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Reason") }, singleLine = true)
            } },
            confirmButton = { TextButton(enabled = !busy && selectedProjectId.isNotBlank() && selectedRoleId.isNotBlank() && (quantity.toDoubleOrNull() ?: 0.0) > 0 && note.trim().length >= 3, onClick = {
                viewModel.recordSharedInventoryEvent("assign", buildJsonObject {
                    put("partId", invText(part, "id")); put("projectId", selectedProjectId); put("roleId", selectedRoleId)
                    put("quantity", quantity.toDoubleOrNull() ?: 0.0); put("location", location.trim()); put("bin", bin.trim()); put("note", note.trim())
                }); showAssign = null
            }) { Text(if (busy) "Saving…" else "Save TEST assignment") } },
            dismissButton = { TextButton(onClick = { if (!busy) showAssign = null }) { Text("Cancel") } }
        )
    }

    showReceipt?.let { option ->
        AlertDialog(
            onDismissRequest = { if (!busy) showReceipt = null },
            title = { Text("Record approved PO receipt") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(invText(option, "purchaseOrderReference") + " · " + invText(option, "partReference") + " · remaining " + invText(option, "remainingQuantity"), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, singleLine = true)
                OutlinedTextField(location, { location = it }, label = { Text("Receiving location") }, singleLine = true)
                OutlinedTextField(bin, { bin = it }, label = { Text("Bin") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Receipt / lot note") }, singleLine = true)
            } },
            confirmButton = { TextButton(enabled = !busy && (quantity.toDoubleOrNull() ?: 0.0) > 0 && note.trim().length >= 3, onClick = {
                viewModel.recordSharedInventoryEvent("receipt", buildJsonObject {
                    put("partId", invText(option, "partId")); put("purchaseOrderId", invText(option, "purchaseOrderId")); put("supplierId", invText(option, "supplierId"))
                    put("quantity", quantity.toDoubleOrNull() ?: 0.0); put("location", location.trim()); put("bin", bin.trim()); put("note", note.trim())
                }); showReceipt = null
            }) { Text(if (busy) "Saving…" else "Save receipt to IQC hold") } },
            dismissButton = { TextButton(onClick = { if (!busy) showReceipt = null }) { Text("Cancel") } }
        )
    }

    showAssignmentClose?.let { assignment ->
        AlertDialog(
            onDismissRequest = { if (!busy) showAssignmentClose = null },
            title = { Text("Record allocated stock use or return") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(invText(assignment, "partReference") + " · remaining " + invText(assignment, "quantity") + " " + invText(assignment, "unit"), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = assignmentCloseAction == "return",
                        onClick = { assignmentCloseAction = "return" },
                        label = { Text("Return usable") }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = assignmentCloseAction == "consume",
                        onClick = { assignmentCloseAction = "consume" },
                        label = { Text("Consume / scrap") }
                    )
                }
                Text(
                    if (assignmentCloseAction == "consume") "This records consumed assigned quantity and lowers on-hand stock. It is a manual ledger action; BOM or device dispatch does not trigger it automatically."
                    else "This returns usable assigned quantity to available stock.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(assignmentCloseQuantity, { assignmentCloseQuantity = it }, label = { Text("Quantity") }, singleLine = true)
                OutlinedTextField(assignmentCloseNote, { assignmentCloseNote = it }, label = { Text("Reason") }, singleLine = true)
            } },
            confirmButton = { TextButton(
                enabled = !busy && assignmentCloseNote.trim().length >= 3 &&
                    (assignmentCloseQuantity.toDoubleOrNull() ?: 0.0) > 0 &&
                    (assignmentCloseQuantity.toDoubleOrNull() ?: Double.POSITIVE_INFINITY) <= invNumber(assignment, "quantity"),
                onClick = {
                    viewModel.recordSharedInventoryEvent(assignmentCloseAction, buildJsonObject {
                        put("partId", invText(assignment, "partId"))
                        put("assignmentId", invText(assignment, "id"))
                        put("quantity", assignmentCloseQuantity.toDoubleOrNull() ?: 0.0)
                        put("location", invText(assignment, "location").ifBlank { "Fictional TEST stockroom" })
                        put("bin", invText(assignment, "bin"))
                        put("note", assignmentCloseNote.trim())
                        if (assignmentCloseAction == "return") put("disposition", "usable")
                    })
                    showAssignmentClose = null
                }
            ) { Text(if (busy) "Saving…" else if (assignmentCloseAction == "consume") "Record consumed stock" else "Return to available stock") } },
            dismissButton = { TextButton(onClick = { if (!busy) showAssignmentClose = null }) { Text("Cancel") } }
        )
    }

    showKitIssue?.let { initialBom ->
        val selectedBom = kitBoms.firstOrNull { invText(it, "id") == invText(initialBom, "id") } ?: initialBom
        val bomLines = invRows(selectedBom, "lines")
        val buildUnits = kitBuildQuantity.toIntOrNull() ?: 0
        val canIssue = buildUnits > 0 && bomLines.isNotEmpty() && bomLines.all { line ->
            val required = invNumber(line, "quantityPerUnit") * buildUnits
            assignmentsForKit(invRows(catalog, "assignments"), invText(line, "partId"), invText(selectedBom, "projectId"))
                .any { invText(it, "id") == kitAssignments[invText(line, "partId")] && invNumber(it, "quantity") >= required }
        }
        AlertDialog(
            onDismissRequest = { if (!busy) showKitIssue = null },
            title = { Text("Issue released BOM kit") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stock decreases atomically at build / kit issue. Shipping a finished device does not consume the component BOM again.", style = MaterialTheme.typography.bodySmall)
                Text("Released BOM · ${invText(selectedBom, "reference")} · ${invText(selectedBom, "projectReference")}", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(kitBoms) { bom ->
                        FilterChip(selected = invText(selectedBom, "id") == invText(bom, "id"), onClick = {
                            showKitIssue = bom
                            kitAssignments = emptyMap()
                        }, label = { Text(invText(bom, "reference")) })
                    }
                }
                OutlinedTextField(kitBuildQuantity, { kitBuildQuantity = it; kitAssignments = emptyMap() }, label = { Text("Build quantity") }, singleLine = true)
                bomLines.forEach { line ->
                    val partId = invText(line, "partId")
                    val needed = invNumber(line, "quantityPerUnit") * buildUnits
                    val candidates = assignmentsForKit(invRows(catalog, "assignments"), partId, invText(selectedBom, "projectId"))
                        .filter { invNumber(it, "quantity") >= needed }
                    Text("${invText(line, "partReference")} · ${invNumber(line, "quantityPerUnit")} ${invText(line, "unit")}/unit · issue $needed", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(candidates) { allocation ->
                            FilterChip(
                                selected = kitAssignments[partId] == invText(allocation, "id"),
                                onClick = { kitAssignments = kitAssignments + (partId to invText(allocation, "id")) },
                                label = { Text("${invText(allocation, "roleName")} · ${invText(allocation, "quantity")} left") }
                            )
                        }
                    }
                    if (candidates.isEmpty()) Text("No matching authorized project allocation has enough quantity for this BOM line.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            } },
            confirmButton = { TextButton(enabled = !busy && canIssue, onClick = {
                viewModel.issueSharedInventoryBomKit(buildJsonObject {
                    put("bomId", invText(selectedBom, "id"))
                    put("projectId", invText(selectedBom, "projectId"))
                    put("buildQuantity", buildUnits)
                    put("allocations", buildJsonArray {
                        bomLines.forEach { line -> add(buildJsonObject {
                            put("partId", invText(line, "partId"))
                            put("assignmentId", kitAssignments[invText(line, "partId")].orEmpty())
                        }) }
                    })
                })
                showKitIssue = null
            }) { Text(if (busy) "Issuing…" else "Confirm kit issue") } },
            dismissButton = { TextButton(onClick = { if (!busy) showKitIssue = null }) { Text("Cancel") } }
        )
    }
}
